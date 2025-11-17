package com.finca.service;

import com.finca.model.*;
import com.finca.repo.AnimalRepository;
import com.finca.util.DateUtils;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio principal: maneja creación/edición/borrado, búsquedas, registro de eventos y actualización de etapas.
 */
public class AnimalService {
    private final AnimalRepository repo;
    private final EventService eventService;

    public AnimalService(AnimalRepository repo, EventService evService){
        this.repo = repo;
        this.eventService = evService;
    }

    // --- CRUD básico ---
    public void createAnimal(Animal a){
        if (repo.exists(a.getId())) throw new IllegalArgumentException("ID duplicado: " + a.getId());
        updateLifeStage(a, LocalDate.now());
        repo.save(a);
    }

    public void editAnimal(String id, java.util.function.Consumer<Animal> updater){
        Animal a = repo.findById(id).orElseThrow(()->new NoSuchElementException("No existe " + id));
        updater.accept(a);
        updateLifeStage(a, LocalDate.now());
        repo.save(a);
    }

    public void deleteAnimal(String id){
        repo.delete(id);
    }

    public Optional<Animal> getById(String id){
        return repo.findById(id);
    }

    public List<String> findByBreed(String breed){
        return repo.findByBreed(breed).stream().map(Animal::getId).collect(Collectors.toList());
    }

    public List<String> findByLifeStage(LifeStage stage){
        return repo.findByLifeStage(stage).stream().map(Animal::getId).collect(Collectors.toList());
    }

    // --- Actualización de etapa de vida basada en edad y sexo ---
    public void updateAllLifeStages(LocalDate refDate){
        for (Animal a: repo.findAll()){
            updateLifeStage(a, refDate);
            repo.save(a);
        }
    }

    public void updateLifeStage(Animal a, LocalDate refDate){
        int months = DateUtils.monthsBetween(a.getBirthDate(), refDate);
        if (a.getSex() == Sex.MACHO) {
            if (months < 12) a.setLifeStage(LifeStage.CRIA);
            else if (months < 24) a.setLifeStage(LifeStage.LEVANTE);
            else a.setLifeStage(LifeStage.NOVILLO);
            a.setReproductiveState(ReproductiveState.NO_APLICA);
        } else { // HEMBRA
            if (months < 12) a.setLifeStage(LifeStage.CRIA);
            else if (months < 24) a.setLifeStage(LifeStage.LEVANTE);
            else {
                if (!a.hadFirstCalving()) a.setLifeStage(LifeStage.NOVILLA);
                else a.setLifeStage(LifeStage.VACA);
            }
            // reproductiveState remains as set by events (VACIA/PREÑADA/LACTANDO)
        }
    }

    // --- Registro de eventos especiales ---
    /**
     * Registrar nacimiento:
     * - genera IDs para cada cría según algoritmo simple: madreID-YY-<ddMM>-<seq>
     * - hereda raza, fecha igual al evento, etapa CRIA
     * - si hembra, estado reproductivo VACIA
     */
    public List<Animal> registerBirth(LocalDate date, String motherId, List<Sex> sexesOfCalves){
        Animal mother = repo.findById(motherId).orElseThrow(()->new NoSuchElementException("Madre no encontrada"));
        List<Animal> newborns = new ArrayList<>();
        int seq = 1;
        for (Sex sex: sexesOfCalves){
            String yy = String.format("%02d", date.getYear()%100);
            String ddMM = String.format("%02d%02d", date.getDayOfMonth(), date.getMonthValue());
            String newid = String.format("%s-%s%s%d", motherId, yy, ddMM, seq++);
            // si newid existe (extra seguridad), añadir sufijo único
            while (repo.exists(newid)) newid = newid + "x";
            Animal baby = new Animal(newid, mother.getBreed(), date, sex);
            baby.setMotherId(motherId);
            baby.setLifeStage(LifeStage.CRIA);
            if (sex == Sex.HEMBRA) baby.setReproductiveState(ReproductiveState.VACIA);
            else baby.setReproductiveState(ReproductiveState.NO_APLICA);
            repo.save(baby);
            newborns.add(baby);
        }
        // marcar primer parto si corresponde
        if (!mother.hadFirstCalving()){
            mother.setHadFirstCalving(true);
            // si ya tuvo parto, cambiar etapa si aplica
            updateLifeStage(mother, date);
        }
        // evento de nacimiento en madre
        List<String> ids = newborns.stream().map(Animal::getId).collect(Collectors.toList());
        BirthEvent be = new BirthEvent(date, motherId, ids);
        eventService.recordEventForAnimal(motherId, be);
        // añadir evento de nacimiento a cada cría (registro)
        for (Animal b: newborns){
            b.addEvent(new BirthEvent(date, motherId, Collections.singletonList(b.getId())));
            repo.save(b);
        }
        repo.save(mother);
        return newborns;
    }

    public void registerDeath(String id, LocalDate date, String cause){
        Animal a = repo.findById(id).orElseThrow(()->new NoSuchElementException(id));
        a.setStatus(AnimalStatus.DEAD);
        DeathEvent de = new DeathEvent(date, cause==null||cause.isBlank() ? "Desconocida" : cause);
        eventService.recordEventForAnimal(id, de);
        a.addEvent(de);
        repo.save(a);
    }

    public void registerSale(String id, LocalDate date){
        Animal a = repo.findById(id).orElseThrow(()->new NoSuchElementException(id));
        a.setStatus(AnimalStatus.SOLD);
        SaleEvent se = new SaleEvent(date);
        eventService.recordEventForAnimal(id, se);
        a.addEvent(se);
        repo.save(a);
    }

    public void registerMount(String cowId, LocalDate date, String bullId){
        Animal cow = repo.findById(cowId).orElseThrow(()->new NoSuchElementException(cowId));
        cow.addEvent(new MountEvent(date, bullId));
        eventService.recordEventForAnimal(cowId, new MountEvent(date, bullId));
        repo.save(cow);
    }

    public void registerInsemination(String cowId, LocalDate date, String bullId){
        Animal cow = repo.findById(cowId).orElseThrow(()->new NoSuchElementException(cowId));
        cow.addEvent(new InseminationEvent(date, bullId));
        eventService.recordEventForAnimal(cowId, new InseminationEvent(date, bullId));
        repo.save(cow);
    }

    public void registerPalpation(String cowId, LocalDate date, boolean pregnant){
        Animal cow = repo.findById(cowId).orElseThrow(()->new NoSuchElementException(cowId));
        PalpationEvent p = new PalpationEvent(date, pregnant);
        cow.addEvent(p);
        cow.setLastPalpationDate(date);
        if (pregnant) cow.setReproductiveState(ReproductiveState.PREÑADA);
        else cow.setReproductiveState(ReproductiveState.VACIA);
        eventService.recordEventForAnimal(cowId, p);
        repo.save(cow);
    }

    // utilidad para calcular días preñez dada fecha de cria (o monta/insem -> palpación)
    public int daysOfPregnancy(LocalDate inseminationOrMount, LocalDate palpation){
        return DateUtils.daysBetween(inseminationOrMount, palpation);
    }

    public LocalDate estimateBirthDate(LocalDate inseminationOrMount){
        return inseminationOrMount.plusDays(270); // +9 meses aprox = 270 días
    }

    // devolver historial completo de eventos de animal
    public List<Event> history(String id){
        Animal a = repo.findById(id).orElseThrow(()->new NoSuchElementException(id));
        return new ArrayList<>(a.getEvents());
    }

    // consulta rápida: inventario activo
    public int inventoryCount(){
        return repo.countActive();
    }

    public List<Animal> listAll(){
        return repo.findAll();
    }
}
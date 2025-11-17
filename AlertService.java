package com.finca.service;

import com.finca.model.*;
import com.finca.repo.AnimalRepository;
import com.finca.util.DateUtils;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Genera alertas: próximos partos, sin palpación >90 días, animales fuera de rango.
 */
public class AlertService {
    private final AnimalRepository repo;
    public AlertService(AnimalRepository repo){ this.repo = repo; }

    // próximos a parir: animales preñadas cuya fecha estimada de parto está dentro de los próximos "daysWindow" días
    public List<Animal> nearParturition(LocalDate asOf, int daysWindow){
        List<Animal> res = new ArrayList<>();
        for (Animal a: repo.findAll()){
            if (a.getSex()==Sex.HEMBRA && a.getReproductiveState()==ReproductiveState.PREÑADA){
                // buscar último evento de inseminación o monta
                Optional<LocalDate> inseminationOrMount = a.getEvents().stream()
                    .filter(ev->ev instanceof InseminationEvent || ev instanceof MountEvent)
                    .map(Event::getDate)
                    .max(LocalDate::compareTo);
                if (inseminationOrMount.isPresent()){
                    LocalDate est = inseminationOrMount.get().plusDays(270);
                    long daysTo = DateUtils.daysBetween(asOf, est);
                    if (daysTo >= 0 && daysTo <= daysWindow) res.add(a);
                }
            }
        }
        return res;
    }

    // animales no palpados en más de N días
    public List<Animal> notPalpatedInMoreThan(int days){
        LocalDate now = LocalDate.now();
        return repo.findAll().stream()
            .filter(a->a.getSex()==Sex.HEMBRA)
            .filter(a->a.getLastPalpationDate()==null || DateUtils.daysBetween(a.getLastPalpationDate(), now) > days)
            .collect(Collectors.toList());
    }

    // animales que superaron el rango de edad para su etapa (por ejemplo: cría >12 meses)
    public List<Animal> exceededAgeRange(LocalDate asOf){
        List<Animal> res = new ArrayList<>();
        for (Animal a: repo.findAll()){
            int months = DateUtils.monthsBetween(a.getBirthDate(), asOf);
            LifeStage stage = a.getLifeStage();
            boolean exceeded = false;
            if (a.getSex()==Sex.MACHO){
                if (stage==LifeStage.CRIA && months>12) exceeded=true;
                if (stage==LifeStage.LEVANTE && months>24) exceeded=true;
            } else {
                if (stage==LifeStage.CRIA && months>12) exceeded=true;
                if (stage==LifeStage.LEVANTE && months>24) exceeded=true;
                // novilla/vaca: no limit aquí
            }
            if (exceeded) res.add(a);
        }
        return res;
    }
}
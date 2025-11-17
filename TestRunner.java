package com.finca.test;

import com.finca.model.*;
import com.finca.repo.AnimalRepository;
import com.finca.service.*;
import com.finca.util.DateUtils;

import java.time.LocalDate;
import java.util.*;

public class TestRunner {
    private final AnimalRepository repo = new AnimalRepository();
    private final EventService eventService = new EventService(repo);
    private final AnimalService animalService = new AnimalService(repo, eventService);
    private final ReportService reportService = new ReportService(repo, eventService);
    private final AlertService alertService = new AlertService(repo);

    public boolean runAll(){
        try {
            testCreateEditDelete();
            testBirthsAndIds();
            testReproductiveFlow();
            testReportsAndAlerts();
            System.out.println("=== TODOS LOS TESTS PASARON ===");
            return true;
        } catch (AssertionError e){
            System.err.println("FALLO EN TEST: " + e.getMessage());
            return false;
        } catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    private void assertTrue(boolean cond, String msg){
        if (!cond) throw new AssertionError(msg);
    }

    private void testCreateEditDelete(){
        Animal a = new Animal("A1","Holstein", DateUtils.parse("01/01/2024"), Sex.MACHO);
        animalService.createAnimal(a);
        assertTrue(repo.findById("A1").isPresent(), "Creación failed");
        animalService.editAnimal("A1", an->an.setBreed("Angus"));
        assertTrue(repo.findById("A1").get().getBreed().equals("Angus"), "Edición failed");
        animalService.deleteAnimal("A1");
        assertTrue(repo.findById("A1").isEmpty(), "Eliminación failed");
        System.out.println("testCreateEditDelete OK");
    }

    private void testBirthsAndIds(){
        Animal mom = new Animal("M1","Angus", DateUtils.parse("01/01/2020"), Sex.HEMBRA);
        animalService.createAnimal(mom);
        // registrar nacimiento de 2 crías (mellizas)
        LocalDate birth = DateUtils.parse("15/10/2025");
        List<Animal> babies = animalService.registerBirth(birth, "M1", Arrays.asList(Sex.HEMBRA, Sex.MACHO));
        assertTrue(babies.size()==2, "No se generaron 2 crías");
        for (Animal b: babies) assertTrue(b.getMotherId().equals("M1"), "Vinculación madre incorrecta");
        System.out.println("testBirthsAndIds OK");
    }

    private void testReproductiveFlow(){
        // crear vaca, inseminar, palpación positiva, calcular días y fecha probable
        Animal cow = new Animal("C1","Holstein", DateUtils.parse("01/01/2022"), Sex.HEMBRA);
        animalService.createAnimal(cow);
        LocalDate insemination = DateUtils.parse("01/06/2025");
        animalService.registerInsemination("C1", insemination, "T1");
        LocalDate palp = insemination.plusDays(60);
        animalService.registerPalpation("C1", palp, true);
        int days = animalService.daysOfPregnancy(insemination, palp);
        assertTrue(days==60, "Cálculo días preñez incorrecto");
        LocalDate est = animalService.estimateBirthDate(insemination);
        assertTrue(est.equals(insemination.plusDays(270)), "Estimación parto incorrecta");
        System.out.println("testReproductiveFlow OK");
    }

    private void testReportsAndAlerts(){
        // generar reportes y alerts básicos
        Map<LifeStage, Long> totals = reportService.totalByLifeStage();
        // no assert fuerte aquí: solo que no falle
        reportService.birthsDeathsSalesPerMonth();
        reportService.pregnancyRateByStage();
        reportService.inventoryEvolution();
        alertService.notPalpatedInMoreThan(90);
        alertService.exceededAgeRange(LocalDate.now());
        System.out.println("testReportsAndAlerts OK");
    }
}
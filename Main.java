package com.finca;

import com.finca.repo.AnimalRepository;
import com.finca.service.*;
import com.finca.test.TestRunner;

public class Main {
    public static void main(String[] args){
        // Si se pasa --run-tests ejecutamos tests automáticos
        boolean runTests = false;
        for (String a: args) if ("--run-tests".equals(a)) runTests = true;

        AnimalRepository repo = new AnimalRepository();
        EventService eventService = new EventService(repo);
        AnimalService animalService = new AnimalService(repo, eventService);
        ReportService reportService = new ReportService(repo, eventService);
        AlertService alertService = new AlertService(repo);

        if (runTests) {
            TestRunner tr = new TestRunner();
            boolean ok = tr.runAll();
            if (!ok) System.exit(1);
            else System.exit(0);
        }

        
    }
}
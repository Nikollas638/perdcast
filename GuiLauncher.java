package com.finca;

import com.finca.repo.AnimalRepository;
import com.finca.service.*;
import com.finca.test.TestRunner;
import com.finca.ui.MainGUI;

import javax.swing.*;
import java.io.File;
import java.time.LocalDate;

public class GuiLauncher {
    public static void main(String[] args){
        AnimalRepository repo = new AnimalRepository();
        EventService eventService = new EventService(repo);
        AnimalService animalService = new AnimalService(repo, eventService);
        ReportService reportService = new ReportService(repo, eventService);
        AlertService alertService = new AlertService(repo);
        PersistenceService persistence = new PersistenceService(repo);

        // Archivo por defecto
        File dataFile = new File("data/finca_data.bin");
        try {
            // cargar binario si existe
            persistence.loadBinary(dataFile);
            // actualizar etapas según fecha actual
            animalService.updateAllLifeStages(LocalDate.now());
            System.out.println("Datos cargados desde " + dataFile.getAbsolutePath());
        } catch (Exception ex) {
            System.out.println("No se cargaron datos previos: " + ex.getMessage());
        }

        // Registrar shutdown hook para salvar al cerrar
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                persistence.saveBinary(dataFile);
                System.out.println("Datos guardados en " + dataFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        boolean runTests = false;
        for (String a: args) if ("--run-tests".equals(a)) runTests = true;
        if (runTests) {
            TestRunner tr = new TestRunner();
            tr.runAll();
            // NOTA: no salimos porque podemos querer abrir la GUI
        }

        SwingUtilities.invokeLater(() -> {
            MainGUI gui = new MainGUI(animalService, reportService, alertService, persistence);
            gui.setVisible(true);
        });
    }
}

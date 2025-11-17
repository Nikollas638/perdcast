package com.finca.ui;

import com.finca.model.*;
import com.finca.repo.AnimalRepository;
import com.finca.service.*;
import com.finca.util.DateUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GUI minimal en Swing actualizado con Import/Export (CSV/JSON) y Ver Eventos.
 */
public class MainGUI extends JFrame {
    private final AnimalService animalService;
    private final ReportService reportService;
    private final AlertService alertService;
    private final PersistenceService persistence;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextArea logArea;

    public MainGUI(AnimalService animalService, ReportService reportService, AlertService alertService, PersistenceService persistence) {
        super("Registro de Animales - Demo");
        this.animalService = animalService;
        this.reportService = reportService;
        this.alertService = alertService;
        this.persistence = persistence;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        String[] cols = new String[]{"ID", "Raza", "Nacimiento", "Sexo", "Etapa", "Repro", "Estado", "Madre"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);

        JPanel buttonPanel = new JPanel();
        JButton btnNew = new JButton("Nuevo");
        JButton btnEdit = new JButton("Editar");
        JButton btnDelete = new JButton("Eliminar");
        JButton btnBirth = new JButton("Nacimiento");
        JButton btnDeath = new JButton("Muerte");
        JButton btnSale = new JButton("Venta");
        JButton btnInseminate = new JButton("Inseminar");
        JButton btnPalpate = new JButton("Palpar");
        JButton btnEvents = new JButton("Ver Eventos");
        JButton btnRefresh = new JButton("Refrescar");

        // Import/Export buttons
        JButton btnImportCsv = new JButton("Importar CSV");
        JButton btnExportCsv = new JButton("Exportar CSV");
        JButton btnImportJson = new JButton("Importar JSON");
        JButton btnExportJson = new JButton("Exportar JSON");

        buttonPanel.add(btnNew); buttonPanel.add(btnEdit); buttonPanel.add(btnDelete);
        buttonPanel.add(btnBirth); buttonPanel.add(btnDeath); buttonPanel.add(btnSale);
        buttonPanel.add(btnInseminate); buttonPanel.add(btnPalpate); buttonPanel.add(btnEvents);
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnImportCsv); buttonPanel.add(btnExportCsv); buttonPanel.add(btnImportJson); buttonPanel.add(btnExportJson);

        logArea = new JTextArea(6, 80);
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tableScroll, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.NORTH);
        getContentPane().add(logScroll, BorderLayout.SOUTH);

        btnNew.addActionListener(e -> showCreateDialog());
        btnEdit.addActionListener(e -> showEditDialog());
        btnDelete.addActionListener(e -> doDelete());
        btnBirth.addActionListener(e -> showBirthDialog());
        btnDeath.addActionListener(e -> showDeathDialog());
        btnSale.addActionListener(e -> showSaleDialog());
        btnInseminate.addActionListener(e -> showInseminationDialog());
        btnPalpate.addActionListener(e -> showPalpationDialog());
        btnEvents.addActionListener(e -> showEventsDialog());
        btnRefresh.addActionListener(e -> refreshTable());

        btnImportCsv.addActionListener(e -> doImportCsv());
        btnExportCsv.addActionListener(e -> doExportCsv());
        btnImportJson.addActionListener(e -> doImportJson());
        btnExportJson.addActionListener(e -> doExportJson());

        refreshTable();
    }

    private void log(String s){
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void refreshTable(){
        tableModel.setRowCount(0);
        List<Animal> all = animalService.listAll();
        for (Animal a: all){
            tableModel.addRow(new Object[]{
                    a.getId(),
                    a.getBreed(),
                    DateUtils.format(a.getBirthDate()),
                    a.getSex(),
                    a.getLifeStage(),
                    a.getReproductiveState(),
                    a.getStatus(),
                    a.getMotherId()
            });
        }
        log("Tabla actualizada. Total: " + all.size());
    }

    private String getSelectedId(){
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un animal en la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (String) tableModel.getValueAt(row, 0);
    }

    // --- CREAR / EDITAR / BORRAR / EVENTOS (igual que antes) ---
    private void showCreateDialog(){
        try {
            String id = JOptionPane.showInputDialog(this, "ID:");
            if (id == null || id.trim().isEmpty()) return;
            String breed = JOptionPane.showInputDialog(this, "Raza:");
            String birth = JOptionPane.showInputDialog(this, "Fecha de nacimiento (dd/MM/yyyy):");
            String sexS = JOptionPane.showInputDialog(this, "Sexo (MACHO/HEMBRA):");
            Sex sex = Sex.valueOf(sexS.trim().toUpperCase());
            Animal a = new Animal(id.trim(), breed.trim(), DateUtils.parse(birth.trim()), sex);
            animalService.createAnimal(a);
            refreshTable();
            log("Creado: " + id);
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error al crear: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showEditDialog(){
        String id = getSelectedId();
        if (id == null) return;
        animalService.getById(id).ifPresent(a->{
            try {
                String breed = JOptionPane.showInputDialog(this, "Raza:", a.getBreed());
                String birth = JOptionPane.showInputDialog(this, "Fecha de nacimiento (dd/MM/yyyy):", DateUtils.format(a.getBirthDate()));
                animalService.editAnimal(id, an->{
                    if (breed!=null && !breed.trim().isEmpty()) an.setBreed(breed.trim());
                    if (birth!=null && !birth.trim().isEmpty()) an.setBirthDate(DateUtils.parse(birth.trim()));
                });
                refreshTable();
                log("Editado: " + id);
            } catch (Exception ex){
                JOptionPane.showMessageDialog(this, "Error al editar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void doDelete(){
        String id = getSelectedId();
        if (id == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar " + id + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            animalService.deleteAnimal(id);
            refreshTable();
            log("Eliminado: " + id);
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showBirthDialog(){
        String motherId = JOptionPane.showInputDialog(this, "ID de la madre:");
        if (motherId == null || motherId.trim().isEmpty()) return;
        try {
            String dateS = JOptionPane.showInputDialog(this, "Fecha de nacimiento (dd/MM/yyyy):");
            LocalDate date = DateUtils.parse(dateS.trim());
            String nS = JOptionPane.showInputDialog(this, "Número de crías (1,2,...) :");
            int n = Integer.parseInt(nS.trim());
            java.util.List<Sex> sexes = new java.util.ArrayList<>();
            for (int i=0;i<n;i++){
                String s = JOptionPane.showInputDialog(this, "Sexo cría " + (i+1) + " (MACHO/HEMBRA):");
                sexes.add(Sex.valueOf(s.trim().toUpperCase()));
            }
            var newborns = animalService.registerBirth(date, motherId.trim(), sexes);
            refreshTable();
            String ids = newborns.stream().map(Animal::getId).collect(Collectors.joining(", "));
            log("Nacimiento registrado. Crías: " + ids);
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error nacimiento: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDeathDialog(){
        String id = getSelectedId();
        if (id == null) return;
        try {
            String dateS = JOptionPane.showInputDialog(this, "Fecha muerte (dd/MM/yyyy):");
            LocalDate date = DateUtils.parse(dateS.trim());
            String cause = JOptionPane.showInputDialog(this, "Causa (si vacío -> Desconocida):");
            animalService.registerDeath(id, date, cause);
            refreshTable();
            log("Muerte registrada para: " + id);
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSaleDialog(){
        String id = getSelectedId();
        if (id == null) return;
        try {
            String dateS = JOptionPane.showInputDialog(this, "Fecha venta (dd/MM/yyyy):");
            LocalDate date = DateUtils.parse(dateS.trim());
            animalService.registerSale(id, date);
            refreshTable();
            log("Venta registrada para: " + id);
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showInseminationDialog(){
        String cowId = getSelectedId();
        if (cowId == null) return;
        try {
            String dateS = JOptionPane.showInputDialog(this, "Fecha inseminación (dd/MM/yyyy):");
            LocalDate date = DateUtils.parse(dateS.trim());
            String bullId = JOptionPane.showInputDialog(this, "ID del toro:");
            animalService.registerInsemination(cowId, date, bullId);
            refreshTable();
            log("Inseminación registrada para: " + cowId);
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPalpationDialog(){
        String cowId = getSelectedId();
        if (cowId == null) return;
        try {
            String dateS = JOptionPane.showInputDialog(this, "Fecha palpación (dd/MM/yyyy):");
            LocalDate date = DateUtils.parse(dateS.trim());
            String res = JOptionPane.showInputDialog(this, "Resultado (POSITIVO/NEGATIVO):");
            boolean preg = "POSITIVO".equalsIgnoreCase(res.trim());
            animalService.registerPalpation(cowId, date, preg);
            refreshTable();
            log("Palpación para " + cowId + " resultado: " + (preg? "PREÑADA" : "NO PREÑADA"));
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Mostrar eventos completos de un animal ---
    private void showEventsDialog(){
        String id = getSelectedId();
        if (id == null) return;
        try {
            var events = animalService.history(id);
            if (events.isEmpty()){
                JOptionPane.showMessageDialog(this, "No hay eventos para " + id);
                return;
            }
            StringBuilder sb = new StringBuilder();
            events.stream().sorted((e1,e2)->e1.getDate().compareTo(e2.getDate()))
                    .forEach(e -> sb.append(e.toString()).append("\n"));
            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(700,400));
            JOptionPane.showMessageDialog(this, sp, "Eventos de " + id, JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error mostrando eventos: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Import / Export (CSV & JSON) usando persistence ---
    private void doImportCsv(){
        JFileChooser fc = new JFileChooser();
        int res = fc.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            persistence.importAnimalsFromCsv(f);
            // actualizar etapas
            animalService.updateAllLifeStages(LocalDate.now());
            refreshTable();
            log("CSV importado: " + f.getAbsolutePath());
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error importando CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doExportCsv(){
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("animals.csv"));
        int res = fc.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File animalsFile = fc.getSelectedFile();
        File eventsFile = new File(animalsFile.getParentFile(), "events.csv");
        try {
            persistence.exportCsv(animalsFile, eventsFile);
            log("CSV exportado: " + animalsFile.getAbsolutePath() + " (events en " + eventsFile.getAbsolutePath() + ")");
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error exportando CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doImportJson(){
        JFileChooser fc = new JFileChooser();
        int res = fc.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            persistence.importJson(f);
            animalService.updateAllLifeStages(LocalDate.now());
            refreshTable();
            log("JSON importado: " + f.getAbsolutePath());
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error importando JSON: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doExportJson(){
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("animals.ndjson"));
        int res = fc.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            persistence.exportJson(f);
            log("JSON exportado (NDJSON): " + f.getAbsolutePath());
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error exportando JSON: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
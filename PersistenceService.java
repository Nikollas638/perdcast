package com.finca.service;

import com.finca.model.*;
import com.finca.repo.AnimalRepository;
import com.finca.util.DateUtils;
import com.finca.util.CSVImporter;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Persistencia y utilidades de import/export.
 * - save/load binario (ObjectOutputStream/ObjectInputStream)
 * - export/import CSV (animals + events)
 * - export/import JSON (NDJSON) — el import acepta el JSON que genera exportJson()
 */
public class PersistenceService {

    private final AnimalRepository repo;

    public PersistenceService(AnimalRepository repo){
        this.repo = repo;
    }

    // ---------- BINARIO (persistencia interna) ----------
    public void saveBinary(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            List<Animal> all = repo.findAll();
            oos.writeObject(all);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadBinary(File file) throws IOException, ClassNotFoundException {
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                List<Animal> list = (List<Animal>) obj;
                // limpiar repo actual y cargar
                for (Animal a : repo.findAll()) {
                    repo.delete(a.getId());
                }
                for (Animal a : list) {
                    repo.save(a);
                }
            }
        }
    }

    // ---------- CSV ----------
    // Exporta animals.csv y events.csv (events separado)
    public void exportCsv(File animalsFile, File eventsFile) throws IOException {
        File parent = animalsFile.getParentFile();
        if (parent != null) parent.mkdirs();

        try (PrintWriter pw = new PrintWriter(new FileWriter(animalsFile))) {
            pw.println("id,breed,birthDate,sex,status,motherId,lifeStage,reproductiveState,hadFirstCalving,lastPalpationDate");
            for (Animal a : repo.findAll()) {
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%b,%s%n",
                        escape(a.getId()),
                        escape(a.getBreed()),
                        DateUtils.format(a.getBirthDate()),
                        a.getSex(),
                        a.getStatus(),
                        (a.getMotherId()==null?"":escape(a.getMotherId())),
                        a.getLifeStage(),
                        a.getReproductiveState(),
                        a.hadFirstCalving(),
                        a.getLastPalpationDate()==null?"":DateUtils.format(a.getLastPalpationDate())
                );
            }
        }

        // events.csv
        try (PrintWriter pw = new PrintWriter(new FileWriter(eventsFile))) {
            pw.println("animalId,eventType,date,note,extra");
            for (Animal a : repo.findAll()) {
                for (Event e : a.getEvents()) {
                    String extra = "";
                    if (e instanceof DeathEvent) extra = ((DeathEvent)e).getCause();
                    if (e instanceof MountEvent) extra = ((MountEvent)e).getBullId();
                    if (e instanceof InseminationEvent) extra = ((InseminationEvent)e).getBullId();
                    if (e instanceof BirthEvent) extra = String.join(";", ((BirthEvent)e).getNewbornIds());
                    if (e instanceof PalpationEvent) extra = String.valueOf(((PalpationEvent)e).isPregnant());
                    pw.printf("%s,%s,%s,%s,%s%n",
                            escape(a.getId()), e.getType(), DateUtils.format(e.getDate()), escape(e.getNote()), escape(extra));
                }
            }
        }
    }

    // Importar animals (usa CSVImporter para mantener compatibilidad)
    public void importAnimalsFromCsv(File csvFile) throws IOException {
        CSVImporter.importAnimals(csvFile, repo);
    }

    // ---------- JSON (NDJSON): una línea por animal con su lista de events ----------
    public void exportJson(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            for (Animal a : repo.findAll()) {
                pw.println(animalToJson(a));
            }
        }
    }

    // Importa el NDJSON generado por exportJson
    public void importJson(File file) throws IOException {
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            List<Animal> loaded = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                Animal a = parseAnimalJsonLine(line);
                if (a != null) loaded.add(a);
            }
            // limpiar y guardar
            for (Animal a : repo.findAll()) repo.delete(a.getId());
            for (Animal a : loaded) repo.save(a);
        } catch (Exception ex) {
            throw new IOException("Error parseando JSON: " + ex.getMessage(), ex);
        }
    }

    // ---------- Helpers JSON (generador y parser) ----------
    private String animalToJson(Animal a) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendJsonKV(sb, "id", a.getId()).append(",");
        appendJsonKV(sb, "breed", a.getBreed()).append(",");
        appendJsonKV(sb, "birthDate", DateUtils.format(a.getBirthDate())).append(",");
        appendJsonKV(sb, "sex", a.getSex().name()).append(",");
        appendJsonKV(sb, "lifeStage", a.getLifeStage().name()).append(",");
        appendJsonKV(sb, "reproductiveState", a.getReproductiveState().name()).append(",");
        appendJsonKV(sb, "status", a.getStatus().name()).append(",");
        appendJsonKV(sb, "motherId", a.getMotherId()==null? "": a.getMotherId()).append(",");
        appendJsonKV(sb, "hadFirstCalving", String.valueOf(a.hadFirstCalving())).append(",");
        appendJsonKV(sb, "lastPalpationDate", a.getLastPalpationDate()==null?"":DateUtils.format(a.getLastPalpationDate())).append(",");
        // events
        sb.append("\"events\":[");
        List<Event> evs = a.getEvents();
        for (int i=0;i<evs.size();i++){
            Event e = evs.get(i);
            sb.append("{");
            appendJsonKV(sb, "type", e.getType()).append(",");
            appendJsonKV(sb, "date", DateUtils.format(e.getDate())).append(",");
            appendJsonKV(sb, "note", e.getNote());
            if (e instanceof BirthEvent) {
                sb.append(",\"newbornIds\":[");
                List<String> nb = ((BirthEvent) e).getNewbornIds();
                for (int j=0;j<nb.size();j++){
                    sb.append("\"").append(escapeJson(nb.get(j))).append("\"");
                    if (j<nb.size()-1) sb.append(",");
                }
                sb.append("]");
            }
            if (e instanceof DeathEvent) {
                sb.append(",\"cause\":\"").append(escapeJson(((DeathEvent)e).getCause())).append("\"");
            }
            if (e instanceof MountEvent) {
                sb.append(",\"bullId\":\"").append(escapeJson(((MountEvent)e).getBullId())).append("\"");
            }
            if (e instanceof InseminationEvent) {
                sb.append(",\"bullId\":\"").append(escapeJson(((InseminationEvent)e).getBullId())).append("\"");
            }
            if (e instanceof PalpationEvent) {
                sb.append(",\"pregnant\":").append(((PalpationEvent)e).isPregnant());
            }
            sb.append("}");
            if (i<evs.size()-1) sb.append(",");
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private Animal parseAnimalJsonLine(String json) {
        // Parser simple y tolerante: extrae campos por regex (el JSON lo generamos nosotros)
        String id = regexStringVal(json, "\"id\"\\s*:\\s*\"([^\"]*)\"");
        if (id == null) return null;
        String breed = regexStringVal(json, "\"breed\"\\s*:\\s*\"([^\"]*)\"");
        String bd = regexStringVal(json, "\"birthDate\"\\s*:\\s*\"([^\"]*)\"");
        String sexS = regexStringVal(json, "\"sex\"\\s*:\\s*\"([^\"]*)\"");
        Sex sex = Sex.valueOf(sexS);
        Animal a = new Animal(id, breed, DateUtils.parse(bd), sex);
        // optional fields
        String motherId = regexStringVal(json, "\"motherId\"\\s*:\\s*\"([^\"]*)\"");
        if (motherId != null && !motherId.isEmpty()) a.setMotherId(motherId);
        String hadFirst = regexStringVal(json, "\"hadFirstCalving\"\\s*:\\s*\"?([^\"]*)\"?");
        if (hadFirst != null) a.setHadFirstCalving(Boolean.parseBoolean(hadFirst));
        String lp = regexStringVal(json, "\"lastPalpationDate\"\\s*:\\s*\"([^\"]*)\"");
        if (lp != null && !lp.isEmpty()) a.setLastPalpationDate(DateUtils.parse(lp));
        // events: capture the array substring
        Pattern pe = Pattern.compile("\"events\"\\s*:\\s*\\[(.*)\\]\\s*}", Pattern.DOTALL);
        Matcher me = pe.matcher(json);
        if (me.find()){
            String inside = me.group(1).trim();
            if (!inside.isEmpty()){
                // split events by "},{" roughly — funcionará porque nuestro JSON es simple
                String[] evs = inside.split("\\},\\s*\\{");
                for (String evstr : evs){
                    // normalize curly braces for easier matching
                    String evj = evstr;
                    if (!evj.startsWith("{")) evj = "{" + evj;
                    if (!evj.endsWith("}")) evj = evj + "}";
                    String type = regexStringVal(evj, "\"type\"\\s*:\\s*\"([^\"]*)\"");
                    String date = regexStringVal(evj, "\"date\"\\s*:\\s*\"([^\"]*)\"");
                    String note = regexStringVal(evj, "\"note\"\\s*:\\s*\"([^\"]*)\"");
                    LocalDate d = date==null? LocalDate.now() : DateUtils.parse(date);
                    Event e = null;
                    if ("BIRTH".equals(type)){
                        // newbornIds array
                        List<String> ids = new ArrayList<>();
                        Pattern pnb = Pattern.compile("\"newbornIds\"\\s*:\\s*\\[([^\\]]*)\\]");
                        Matcher mnb = pnb.matcher(evj);
                        if (mnb.find()){
                            String list = mnb.group(1);
                            // split by commas and remove quotes
                            for (String s : list.split(",")){
                                s = s.trim().replaceAll("^\"|\"$", "");
                                if (!s.isEmpty()) ids.add(s);
                            }
                        }
                        e = new BirthEvent(d, a.getMotherId()==null? "": a.getMotherId(), ids);
                    } else if ("DEATH".equals(type)){
                        String cause = regexStringVal(evj, "\"cause\"\\s*:\\s*\"([^\"]*)\"");
                        e = new DeathEvent(d, cause==null?"Desconocida":cause);
                    } else if ("SALE".equals(type)){
                        e = new SaleEvent(d);
                    } else if ("MOUNT".equals(type)){
                        String bull = regexStringVal(evj, "\"bullId\"\\s*:\\s*\"([^\"]*)\"");
                        e = new MountEvent(d, bull==null?"":bull);
                    } else if ("INSEMINATION".equals(type)){
                        String bull = regexStringVal(evj, "\"bullId\"\\s*:\\s*\"([^\"]*)\"");
                        e = new InseminationEvent(d, bull==null?"":bull);
                    } else if ("PALPATION".equals(type)){
                        String preg = regexStringVal(evj, "\"pregnant\"\\s*:\\s*([a-zA-Z0-9]+)");
                        boolean p = preg!=null && Boolean.parseBoolean(preg);
                        e = new PalpationEvent(d, p);
                    } else {
                        // tipo desconocido -> crear Event básico
                        e = new Event(d, note==null?"":note) {
                            @Override public String getType(){ return type == null ? "GENERIC" : type; }
                        };
                    }
                    if (e != null) a.addEvent(e);
                }
            }
        }
        return a;
    }

    // regex helper
    private String regexStringVal(String text, String regex){
        Pattern p = Pattern.compile(regex, Pattern.DOTALL);
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    // JSON helpers
    private StringBuilder appendJsonKV(StringBuilder sb, String k, String v){
        sb.append("\"").append(k).append("\":");
        if (v == null) sb.append("null");
        else {
            sb.append("\"").append(escapeJson(v)).append("\"");
        }
        return sb;
    }

    private String escapeJson(String s){
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n","\\n").replace("\r","\\r");
    }

    // CSV escaping simple
    private String escape(String s){
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}

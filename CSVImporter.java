package com.finca.util;

import com.finca.model.*;
import com.finca.repo.AnimalRepository;
import java.io.*;
import java.time.LocalDate;

public class CSVImporter {
    /**
     * Formato CSV (con header):
     * id,breed,birthDate(dd/MM/yyyy),sex,estado,motherId
     */
    public static void importAnimals(File csvFile, AnimalRepository repo) throws IOException {
        try(BufferedReader br = new BufferedReader(new FileReader(csvFile))){
            String header = br.readLine();
            if (header == null) return;
            String line;
            while((line = br.readLine()) != null){
                String[] cols = line.split(",");
                if (cols.length < 4) continue;
                String id = cols[0].trim();
                String breed = cols[1].trim();
                LocalDate bd = DateUtils.parse(cols[2].trim());
                Sex sex = Sex.valueOf(cols[3].trim().toUpperCase());
                Animal a = new Animal(id, breed, bd, sex);
                if (cols.length >= 5 && !cols[4].trim().isEmpty()) {
                    a.setStatus(AnimalStatus.valueOf(cols[4].trim().toUpperCase()));
                }
                if (cols.length >= 6 && !cols[5].trim().isEmpty()){
                    a.setMotherId(cols[5].trim());
                }
                repo.save(a);
            }
        }
    }
}
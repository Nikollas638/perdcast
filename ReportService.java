package com.finca.service;

import com.finca.model.*;
import com.finca.repo.AnimalRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Genera reportes básicos a partir de repo y eventService.
 */
public class ReportService {
    private final AnimalRepository repo;
    private final EventService eventService;

    public ReportService(AnimalRepository repo, EventService eventService){
        this.repo = repo;
        this.eventService = eventService;
    }

    public Map<LifeStage, Long> totalByLifeStage(){
        return repo.findAll().stream()
            .collect(Collectors.groupingBy(Animal::getLifeStage, Collectors.counting()));
    }

    // nacimientos/muertes/ventas por mes (YearMonth -> counts)
    public Map<YearMonth, Map<String, Integer>> birthsDeathsSalesPerMonth(){
        Map<YearMonth, Map<String, Integer>> res = new TreeMap<>();
        for (Animal a: repo.findAll()){
            for (Event e: a.getEvents()){
                YearMonth ym = YearMonth.from(e.getDate());
                res.putIfAbsent(ym, new HashMap<>());
                Map<String,Integer> bucket = res.get(ym);
                bucket.put(e.getType(), bucket.getOrDefault(e.getType(), 0) + 1);
            }
        }
        return res;
    }

    // tasa de preñez por grupo (por lifeStage)
    public Map<LifeStage, Double> pregnancyRateByStage(){
        Map<LifeStage, List<Animal>> grouped = repo.findAll().stream()
            .filter(a->a.getSex()==Sex.HEMBRA)
            .collect(Collectors.groupingBy(Animal::getLifeStage));
        Map<LifeStage, Double> out = new HashMap<>();
        for (Map.Entry<LifeStage, List<Animal>> e: grouped.entrySet()){
            long total = e.getValue().size();
            long pregnant = e.getValue().stream().filter(a->a.getReproductiveState()==ReproductiveState.PREÑADA).count();
            out.put(e.getKey(), total == 0 ? 0.0 : (pregnant * 100.0 / total));
        }
        return out;
    }

    // evolucion del inventario en el tiempo (fecha->cantidad)
    public NavigableMap<LocalDate, Integer> inventoryEvolution(){
        return eventService.getInventoryTimeline();
    }
}
package com.finca.service;

import com.finca.model.*;
import com.finca.repo.AnimalRepository;
import java.time.LocalDate;
import java.util.*;

/**
 * Servicio para centralizar registro de eventos y snapshots para reportes.
 */
public class EventService {
    private final AnimalRepository repo;
    // historial por animal manejado dentro del Animal, pero mantenemos resumen global
    private final List<Event> globalEvents = new ArrayList<>();
    // inventario a lo largo del tiempo (fecha->cantidad activa)
    private final NavigableMap<LocalDate, Integer> inventoryTimeline = new TreeMap<>();

    public EventService(AnimalRepository repo){
        this.repo = repo;
        // snapshot inicial
        inventoryTimeline.put(LocalDate.now(), repo.countActive());
    }

    public void recordEventForAnimal(String id, Event e){
        // anadir evento al animal si existe
        repo.findById(id).ifPresent(a -> {
            a.addEvent(e);
            repo.save(a);
        });
        globalEvents.add(e);
        // actualizar snapshot del inventario (simple: after the event date snapshot)
        inventoryTimeline.put(e.getDate(), repo.countActive());
    }

    public List<Event> getGlobalEvents(){ return new ArrayList<>(globalEvents); }

    public NavigableMap<LocalDate, Integer> getInventoryTimeline(){ return inventoryTimeline; }
}
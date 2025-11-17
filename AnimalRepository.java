package com.finca.repo;

import com.finca.model.*;
import java.util.*;
import java.util.stream.Collectors;

public class AnimalRepository {
    private final Map<String, Animal> store = new HashMap<>();

    public synchronized void save(Animal a) {
        store.put(a.getId(), a);
    }
    public synchronized Optional<Animal> findById(String id){
        return Optional.ofNullable(store.get(id));
    }
    public synchronized void delete(String id){
        store.remove(id);
    }
    public synchronized List<Animal> findAll(){
        return new ArrayList<>(store.values());
    }
    public synchronized List<Animal> findByBreed(String breed){
        return store.values().stream()
            .filter(a->a.getBreed().equalsIgnoreCase(breed))
            .collect(Collectors.toList());
    }
    public synchronized List<Animal> findByLifeStage(LifeStage stage){
        return store.values().stream()
            .filter(a->a.getLifeStage() == stage)
            .collect(Collectors.toList());
    }
    public synchronized boolean exists(String id){ return store.containsKey(id); }
    public synchronized int countActive(){
        return (int) store.values().stream().filter(a->a.getStatus()==AnimalStatus.ACTIVE).count();
    }
}
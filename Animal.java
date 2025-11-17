package com.finca.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class Animal implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String breed;
    private LocalDate birthDate;
    private Sex sex;
    private LifeStage lifeStage = LifeStage.DESCONOCIDO;
    private ReproductiveState reproductiveState = ReproductiveState.NO_APLICA;
    private AnimalStatus status = AnimalStatus.ACTIVE;
    private String motherId; // opcional
    private boolean hadFirstCalving = false; // marca si la hembra ya tuvo primer parto
    private LocalDate lastPalpationDate = null;
    private final List<Event> events = new ArrayList<>();

    public Animal(String id, String breed, LocalDate birthDate, Sex sex) {
        this.id = id;
        this.breed = breed;
        this.birthDate = birthDate;
        this.sex = sex;
        if (sex == Sex.HEMBRA) reproductiveState = ReproductiveState.VACIA;
        else reproductiveState = ReproductiveState.NO_APLICA;
    }
    // getters y setters (solo los esenciales para brevity)
    public String getId(){ return id; }
    public void setId(String id){ this.id = id; }
    public String getBreed(){ return breed; }
    public void setBreed(String b){ this.breed = b; }
    public LocalDate getBirthDate(){ return birthDate; }
    public void setBirthDate(LocalDate d){ this.birthDate = d; }
    public Sex getSex(){ return sex; }
    public LifeStage getLifeStage(){ return lifeStage; }
    public void setLifeStage(LifeStage ls){ this.lifeStage = ls; }
    public ReproductiveState getReproductiveState(){ return reproductiveState; }
    public void setReproductiveState(ReproductiveState rs){ this.reproductiveState = rs; }
    public AnimalStatus getStatus(){ return status; }
    public void setStatus(AnimalStatus s){ this.status = s; }
    public String getMotherId(){ return motherId; }
    public void setMotherId(String m){ this.motherId = m; }
    public boolean hadFirstCalving(){ return hadFirstCalving; }
    public void setHadFirstCalving(boolean v){ this.hadFirstCalving = v; }
    public LocalDate getLastPalpationDate(){ return lastPalpationDate; }
    public void setLastPalpationDate(LocalDate d){ this.lastPalpationDate = d; }
    public List<Event> getEvents(){ return events; }
    public void addEvent(Event e){ events.add(e); }
    @Override
    public String toString(){
        return String.format("Animal[id=%s, breed=%s, birth=%s, sex=%s, stage=%s, repro=%s, status=%s, mother=%s]",
            id, breed, birthDate, sex, lifeStage, reproductiveState, status, motherId);
    }
}
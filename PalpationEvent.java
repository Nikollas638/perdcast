package com.finca.model;

import java.io.Serializable;
import java.time.LocalDate;

public class PalpationEvent extends Event implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean pregnant;
    public PalpationEvent(LocalDate date, boolean pregnant) {
        super(date, "Palpación: " + (pregnant ? "POSITIVO" : "NEGATIVO"));
        this.pregnant = pregnant;
    }
    public boolean isPregnant(){ return pregnant; }
    @Override public String getType(){ return "PALPATION"; }
}
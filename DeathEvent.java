package com.finca.model;

import java.io.Serializable;
import java.time.LocalDate;

public class DeathEvent extends Event implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String cause;
    public DeathEvent(LocalDate date, String cause) {
        super(date, "Muerte: " + cause);
        this.cause = cause;
    }
    public String getCause(){ return cause; }
    @Override public String getType(){ return "DEATH"; }
}
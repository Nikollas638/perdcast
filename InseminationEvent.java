package com.finca.model;

import java.io.Serializable;
import java.time.LocalDate;

public class InseminationEvent extends Event implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String bullId;
    public InseminationEvent(LocalDate date, String bullId){
        super(date, "Inseminación con semen de " + bullId);
        this.bullId = bullId;
    }
    public String getBullId(){ return bullId; }
    @Override public String getType(){ return "INSEMINATION"; }
}
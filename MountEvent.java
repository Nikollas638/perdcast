package com.finca.model;

import java.io.Serializable;
import java.time.LocalDate;

public class MountEvent extends Event implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String bullId;
    public MountEvent(LocalDate date, String bullId){
        super(date, "Monta natural con toro " + bullId);
        this.bullId = bullId;
    }
    public String getBullId(){ return bullId; }
    @Override public String getType(){ return "MOUNT"; }
}
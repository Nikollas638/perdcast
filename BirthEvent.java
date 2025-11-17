package com.finca.model;

import java.time.LocalDate;
import java.util.List;
import java.io.Serializable;

public class BirthEvent extends Event implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String motherId;
    private final List<String> newbornIds;
    public BirthEvent(LocalDate date, String motherId, List<String> newbornIds) {
        super(date, "Nacimiento de " + newbornIds.size() + " cría(s)");
        this.motherId = motherId;
        this.newbornIds = newbornIds;
    }
    public String getMotherId(){ return motherId; }
    public List<String> getNewbornIds(){ return newbornIds; }
    @Override public String getType(){ return "BIRTH"; }
}
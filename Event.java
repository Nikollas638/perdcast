package com.finca.model;

import java.time.LocalDate;
import java.io.Serializable;

public abstract class Event implements Serializable {
    private static final long serialVersionUID = 1L;
    private final LocalDate date;
    private final String note;
    public Event(LocalDate date, String note) {
        this.date = date;
        this.note = note;
    }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public abstract String getType();
    @Override
    public String toString() {
        return "["+getType()+"] " + date + " - " + note;
    }
}
package com.finca.model;

import java.io.Serializable;
import java.time.LocalDate;

public class SaleEvent extends Event implements Serializable {
    private static final long serialVersionUID = 1L;
    public SaleEvent(LocalDate date) { super(date, "Venta"); }
    @Override public String getType(){ return "SALE"; }
}
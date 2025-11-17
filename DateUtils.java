package com.finca.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.Period;

public class DateUtils {
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static LocalDate parse(String s){
        return LocalDate.parse(s, FORMAT);
    }
    public static String format(LocalDate d){
        return d.format(FORMAT);
    }
    public static int monthsBetween(LocalDate from, LocalDate to){
        Period p = Period.between(from, to);
        return p.getYears()*12 + p.getMonths();
    }
    public static int daysBetween(LocalDate from, LocalDate to){
        return (int) java.time.temporal.ChronoUnit.DAYS.between(from, to);
    }
}
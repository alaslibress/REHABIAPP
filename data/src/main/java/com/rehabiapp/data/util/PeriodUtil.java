package com.rehabiapp.data.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;

@Component
public class PeriodUtil {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC);

    // Devuelve "YYYY-Www" (ISO 8601), ej. "2026-W10"
    public String toWeekPeriod(Instant instant) {
        var zdt = instant.atZone(ZoneOffset.UTC);
        int year = zdt.get(IsoFields.WEEK_BASED_YEAR);
        int week = zdt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return "%d-W%02d".formatted(year, week);
    }

    // Devuelve "YYYY-MM", ej. "2026-03"
    public String toMonthPeriod(Instant instant) {
        return MONTH_FMT.format(instant);
    }
}

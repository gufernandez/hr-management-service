package com.banco.hr.infrastructure.adapters.inbound.rest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ApiDateTimeFormatter {

    private static final ZoneId PROJECT_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(PROJECT_ZONE);

    private ApiDateTimeFormatter() {
    }

    public static String format(Instant instant) {
        return FORMATTER.format(instant);
    }
}

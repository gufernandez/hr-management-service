package com.banco.hr.infrastructure.adapters.inbound.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiDateTimeFormatterTest {

    @Test
    @DisplayName("Deve formatar instant em ISO 8601 com offset de Brasília")
    void formatUsesIsoOffsetDateTimeInBrasiliaTime() {
        // Preparação: instant UTC com nanos para validar conversão de fuso e fração de segundo.
        Instant instant = Instant.parse("2026-11-11T12:00:00.123456789Z");

        // Execução: converte o instant para o formato textual padrão das respostas REST.
        String formatted = ApiDateTimeFormatter.format(instant);

        // Verificação: o resultado deve usar horário de Brasília e conter o offset ISO explícito.
        assertEquals("2026-11-11T09:00:00.123456789-03:00", formatted);
    }

    @Test
    @DisplayName("Deve formatar instant sem fração preservando segundos e offset")
    void formatOmitsFractionWhenInstantHasNoFractionalSeconds() {
        // Preparação: instant UTC em segundos exatos para validar o formato sem frações.
        Instant instant = Instant.parse("2026-11-11T12:00:00Z");

        // Execução: converte o instant para o formato ISO 8601 com fuso do projeto.
        String formatted = ApiDateTimeFormatter.format(instant);

        // Verificação: a resposta deve manter segundos e informar o offset GMT-3.
        assertEquals("2026-11-11T09:00:00-03:00", formatted);
    }
}

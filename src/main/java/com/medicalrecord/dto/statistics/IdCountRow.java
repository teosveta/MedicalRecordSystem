package com.medicalrecord.dto.statistics;

// Вътрешен проекционен запис за агрегирани заявки, върващи id + count
public record IdCountRow(Long id, Long count) {}

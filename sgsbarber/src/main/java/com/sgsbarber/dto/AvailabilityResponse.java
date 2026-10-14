package com.sgsbarber.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(Long barberId, Long serviceId, LocalDate date, List<Slot> slots) {
    public record Slot(String time, boolean available, String reason) {}
}

package dk.ek.barbercom.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ManualBookingRequest(
        Long chairId,
        LocalDate date,
        LocalTime startTime,
        Integer durationMinutes,
        String customerName,
        String customerEmail,
        String customerPhone
) { }

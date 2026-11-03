package dk.ek.barbercom.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record BusinessBookingDTO(
        Long id,
        String customerName,
        String customerEmail,
        LocalDate date,
        LocalTime startTime
) {
}

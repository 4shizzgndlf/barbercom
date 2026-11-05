package dk.ek.barbercom.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record BusinessBookingDTO(
        Long id,
        String customerName,
        String customerEmail,
        String chairName,
        LocalDate date,
        LocalTime startTime
) {
}

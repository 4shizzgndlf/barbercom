package dk.ek.barbercom.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingDTO(Long id, String barberName, String chairName, LocalDate date, LocalTime startTime, Integer durationMinutes) { }

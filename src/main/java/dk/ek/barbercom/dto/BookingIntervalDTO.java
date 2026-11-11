package dk.ek.barbercom.dto;

import java.time.LocalTime;

public record BookingIntervalDTO(LocalTime startTime, LocalTime endTime) { }

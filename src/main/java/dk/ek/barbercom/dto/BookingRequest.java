package dk.ek.barbercom.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingRequest(Long barberId, LocalDate date, LocalTime startTime) { }

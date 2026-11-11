package dk.ek.barbercom.dto;

import java.util.List;

public record ChairAvailabilityDTO(Long chairId, String chairName, List<BookingIntervalDTO> bookings) { }

package dk.ek.barbercom.repository;

import dk.ek.barbercom.model.Booking;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByBarberIdAndBookingDate(Long barberId, LocalDate bookingDate);
    List<Booking> findByBarberIdAndBookingDateOrderByStartTimeAsc(Long barberId, LocalDate bookingDate);
    List<Booking> findByCustomerIdOrderByBookingDateDesc(Long customerId);
    boolean existsByBarberIdAndBookingDateAndStartTime(Long barberId, LocalDate bookingDate, java.time.LocalTime startTime);
    long count();
}

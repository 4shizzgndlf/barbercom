package dk.ek.barbercom.repository;

import dk.ek.barbercom.model.Booking;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByBarberIdAndBookingDate(Long barberId, LocalDate bookingDate);
    List<Booking> findByBarberIdAndBookingDateOrderByStartTimeAsc(Long barberId, LocalDate bookingDate);
    List<Booking> findByCustomerIdOrderByBookingDateDesc(Long customerId);
    boolean existsByBarberId(Long barberId);
    List<Booking> findByChairIdAndBookingDate(Long chairId, LocalDate bookingDate);
    boolean existsByChairId(Long chairId);
    long countByBarberIdAndCreatedAtAfter(Long barberId, LocalDateTime createdAt);
    long count();
}

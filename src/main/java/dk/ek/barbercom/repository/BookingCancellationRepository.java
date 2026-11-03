package dk.ek.barbercom.repository;

import dk.ek.barbercom.model.BookingCancellation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingCancellationRepository extends JpaRepository<BookingCancellation, Long> { }

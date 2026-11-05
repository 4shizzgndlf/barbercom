package dk.ek.barbercom.repository;
import dk.ek.barbercom.model.SalonChair;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SalonChairRepository extends JpaRepository<SalonChair, Long> {
    List<SalonChair> findByBusinessIdOrderByIdAsc(Long businessId);
}

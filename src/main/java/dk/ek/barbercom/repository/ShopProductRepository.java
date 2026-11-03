package dk.ek.barbercom.repository;
import dk.ek.barbercom.model.ShopProduct;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ShopProductRepository extends JpaRepository<ShopProduct, Long> { }

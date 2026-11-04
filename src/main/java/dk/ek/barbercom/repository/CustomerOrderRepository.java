package dk.ek.barbercom.repository;

import dk.ek.barbercom.model.CustomerOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByCustomerIdOrderByPurchasedAtDesc(Long customerId);
    java.util.Optional<CustomerOrder> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    @Query("select distinct o from CustomerOrder o join fetch o.items order by o.purchasedAt desc")
    List<CustomerOrder> findAllWithItems();
}

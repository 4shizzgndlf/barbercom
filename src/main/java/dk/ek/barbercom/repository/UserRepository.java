package dk.ek.barbercom.repository;

import dk.ek.barbercom.model.User;
import dk.ek.barbercom.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(Integer phoneNumber);
    Optional<User> findByBusinessSubscriptionCheckoutSessionId(String sessionId);
    Optional<User> findByStripeSubscriptionId(String subscriptionId);
    java.util.List<User> findByRole(Role role);
    java.util.List<User> findByRoleOrderByIdAsc(Role role);
}

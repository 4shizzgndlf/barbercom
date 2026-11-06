package dk.ek.barbercom.controller;

import dk.ek.barbercom.dto.UserDTO;
import dk.ek.barbercom.mapper.UserMapper;
import dk.ek.barbercom.model.User;
import dk.ek.barbercom.model.Role;
import dk.ek.barbercom.model.CustomerOrder;
import dk.ek.barbercom.repository.BookingRepository;
import dk.ek.barbercom.repository.CustomerOrderRepository;
import dk.ek.barbercom.repository.UserRepository;
import dk.ek.barbercom.service.UserService;
import dk.ek.barbercom.service.BusinessSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final UserRepository userRepository;
    private final UserService userService;
    private final BookingRepository bookingRepository;
    private final CustomerOrderRepository orderRepository;
    private final BusinessSubscriptionService businessSubscriptions;

    public AccountController(UserRepository userRepository, UserService userService,
            BookingRepository bookingRepository, CustomerOrderRepository orderRepository,
            BusinessSubscriptionService businessSubscriptions) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.bookingRepository = bookingRepository;
        this.orderRepository = orderRepository;
        this.businessSubscriptions = businessSubscriptions;
    }

    @GetMapping
    public ResponseEntity<UserDTO> currentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .map(UserMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<UserDTO> updateCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserDTO userDTO) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        return userService.updateUser(user.getId(), userDTO)
                .map(updated -> {
                    UserDetails updatedPrincipal = org.springframework.security.core.userdetails.User
                            .withUsername(updated.email())
                            .password(userDetails.getPassword())
                            .authorities(userDetails.getAuthorities())
                            .build();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    updatedPrincipal, null,
                                    updatedPrincipal.getAuthorities()));
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/notifications")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Long>> notifications(@AuthenticationPrincipal UserDetails userDetails) {
        User business = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (business == null || business.getRole() != Role.BUSINESS) return ResponseEntity.ok(Map.of("count", 0L));
        LocalDateTime savedLastRead = business.getBusinessNotificationsReadAt();
        final LocalDateTime lastRead = savedLastRead == null
                ? LocalDateTime.of(1970, 1, 1, 0, 0) : savedLastRead;
        long bookingCount = bookingRepository.countByBarberIdAndCreatedAtAfter(business.getId(), lastRead);
        long orderCount = orderRepository.findAllWithItems().stream()
                .filter(CustomerOrder::isPaid)
                .filter(order -> order.getPurchasedAt() != null && order.getPurchasedAt().isAfter(lastRead))
                .filter(order -> order.getItems().stream().anyMatch(item -> business.getId().equals(item.getSellerId())))
                .count();
        return ResponseEntity.ok(Map.of("count", bookingCount + orderCount));
    }

    @PostMapping("/notifications/read")
    public ResponseEntity<Void> markNotificationsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User business = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (business == null || business.getRole() != Role.BUSINESS) return ResponseEntity.noContent().build();
        business.setBusinessNotificationsReadAt(LocalDateTime.now());
        userRepository.save(business);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/business-subscription")
    public ResponseEntity<?> businessSubscription(@AuthenticationPrincipal UserDetails userDetails) {
        User business = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (business == null || business.getRole() != Role.BUSINESS) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(Map.of(
                "cancellationScheduled", business.isBusinessSubscriptionCancelAtPeriodEnd(),
                "hasSubscription", business.getStripeSubscriptionId() != null && !business.getStripeSubscriptionId().isBlank()));
    }

    @PostMapping("/business-subscription/cancel")
    public ResponseEntity<?> cancelBusinessSubscription(@AuthenticationPrincipal UserDetails userDetails) {
        User business = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (business == null || business.getRole() != Role.BUSINESS) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Kun business-profiler kan opsige abonnementet."));
        }
        try {
            businessSubscriptions.cancelAtPeriodEnd(business);
            return ResponseEntity.ok(Map.of("message", "Dit abonnement er opsagt og fortsætter resten af den allerede betalte periode."));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "Abonnementet kunne ikke opsiges hos Stripe. Prøv igen senere."));
        }
    }
}

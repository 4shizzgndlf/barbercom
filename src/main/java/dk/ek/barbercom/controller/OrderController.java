package dk.ek.barbercom.controller;

import dk.ek.barbercom.dto.*;
import dk.ek.barbercom.model.CustomerOrder;
import dk.ek.barbercom.model.OrderItem;
import dk.ek.barbercom.model.Role;
import dk.ek.barbercom.model.User;
import dk.ek.barbercom.repository.CustomerOrderRepository;
import dk.ek.barbercom.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final List<String> CATALOG_PRODUCTS = List.of(
            "Mat clay", "Havsaltspray", "Sculpt paste", "Daglig shampoo", "Finish pomade",
            "Volumenpudder", "Skægolie", "Hårtonic", "Stærkt hold", "Rejsesæt",
            "Curl cream", "Pre-shave oil", "Skægbalm", "Cooling tonic", "Fiber wax",
            "Conditioner", "Barber brush", "Aftershave mist", "Texture dust", "Grooming kit");
    private final CustomerOrderRepository orders;
    private final UserRepository users;

    public OrderController(CustomerOrderRepository orders, UserRepository users) {
        this.orders = orders;
        this.users = users;
    }

    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public List<OrderDTO> mine(@AuthenticationPrincipal UserDetails principal) {
        User customer = currentUser(principal);
        return orders.findByCustomerIdOrderByPurchasedAtDesc(customer.getId()).stream().map(this::toDto).toList();
    }

    @GetMapping("/sales")
    @Transactional(readOnly = true)
    public ResponseEntity<?> sales(@AuthenticationPrincipal UserDetails principal) {
        User business = currentUser(principal);
        if (business.getRole() != Role.BUSINESS) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Kun business-brugere kan se salg.");
        }
        List<BusinessSaleDTO> sales = orders.findAllWithItems().stream()
                .flatMap(order -> order.getItems().stream()
                        .filter(item -> belongsToBusiness(item, business))
                        .map(item -> new BusinessSaleDTO(
                        order.getId(), order.getPurchasedAt(),
                        order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName(),
                        order.getCustomer().getEmail(), order.getCustomer().getPhoneNumber(),
                        order.getCustomer().getAddress(), order.getCustomer().getZipCode(), item.getProductName(),
                        item.getQuantity(), item.getUnitPrice())))
                .toList();
        return ResponseEntity.ok(sales);
    }

    @PostMapping
    public ResponseEntity<?> purchase(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody OrderRequest request) {
        User customer = currentUser(principal);
        if (customer.getRole() == Role.BUSINESS) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Virksomhedsprofiler kan ikke købe produkter. Log ind med en privat- eller administratorprofil.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            return ResponseEntity.badRequest().body("Din kurv er tom.");
        }

        List<OrderItem> items = request.items().stream().map(item -> {
            if (item.name() == null || item.name().isBlank() || item.quantity() == null
                    || item.quantity() < 1 || item.price() == null || item.price().signum() < 0) {
                throw new IllegalArgumentException("En vare i kurven er ugyldig.");
            }
            User seller = item.sellerId() == null
                    ? catalogSeller(item.name())
                    : users.findById(item.sellerId()).filter(user -> user.getRole() == Role.BUSINESS).orElse(null);
            if (seller == null) throw new IllegalArgumentException("Den valgte barbershop findes ikke.");
            String sellerName = seller.getBusinessName() == null || seller.getBusinessName().isBlank()
                    ? seller.getFirstName() + " " + seller.getLastName() : seller.getBusinessName();
            return new OrderItem(item.name().trim(), item.quantity(), item.price(), seller.getId(), sellerName);
        }).toList();

        BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setPurchasedAt(LocalDateTime.now());
        order.setItems(items);
        order.setTotalAmount(total);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(orders.save(order)));
    }

    private User currentUser(UserDetails principal) {
        return users.findByEmail(principal.getUsername()).orElseThrow();
    }

    private OrderDTO toDto(CustomerOrder order) {
        List<OrderItemDTO> items = order.getItems().stream()
                .map(item -> new OrderItemDTO(
                        item.getProductName(), item.getQuantity(), item.getUnitPrice(), sellerNameFor(item)))
                .toList();
        return new OrderDTO(order.getId(), order.getPurchasedAt(), order.getTotalAmount(), items);
    }

    private String sellerNameFor(OrderItem item) {
        if (item.getSellerName() != null && !item.getSellerName().isBlank()) {
            return item.getSellerName();
        }
        User seller = catalogSeller(item.getProductName());
        if (seller == null) return "Barber.com";
        return seller.getBusinessName() == null || seller.getBusinessName().isBlank()
                ? seller.getFirstName() + " " + seller.getLastName()
                : seller.getBusinessName();
    }

    private boolean belongsToBusiness(OrderItem item, User business) {
        if (business.getId().equals(item.getSellerId())) return true;

        // One older order predates seller tracking. It is the documented
        // Havsaltspray purchase from Northside Barber, so retain only that
        // verified legacy sale instead of guessing about other old products.
        User legacySeller = catalogSeller("Havsaltspray");
        return item.getSellerId() == null
                && "havsaltspray".equals(item.getProductName().toLowerCase(Locale.ROOT))
                && legacySeller != null
                && business.getId().equals(legacySeller.getId());
    }

    private User catalogSeller(String productName) {
        List<User> businesses = users.findByRoleOrderByIdAsc(Role.BUSINESS);
        if (businesses.isEmpty()) return null;
        int productIndex = CATALOG_PRODUCTS.indexOf(productName);
        return businesses.get((productIndex < 0 ? 0 : productIndex) % businesses.size());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> invalidOrder(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(error.getMessage());
    }
}

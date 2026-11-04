package dk.ek.barbercom.controller;

import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import dk.ek.barbercom.dto.OrderItemRequest;
import dk.ek.barbercom.dto.OrderRequest;
import dk.ek.barbercom.model.CustomerOrder;
import dk.ek.barbercom.model.OrderItem;
import dk.ek.barbercom.model.Role;
import dk.ek.barbercom.model.ShopProduct;
import dk.ek.barbercom.model.User;
import dk.ek.barbercom.repository.CustomerOrderRepository;
import dk.ek.barbercom.repository.ShopProductRepository;
import dk.ek.barbercom.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class StripeCheckoutController {
    private final CustomerOrderRepository orders;
    private final UserRepository users;
    private final ShopProductRepository products;
    private final String stripeSecretKey;
    private final String publicUrl;

    public StripeCheckoutController(CustomerOrderRepository orders, UserRepository users,
            ShopProductRepository products,
            @Value("${barbercom.stripe.secret-key:}") String stripeSecretKey,
            @Value("${barbercom.public-url:http://localhost:8080}") String publicUrl) {
        this.orders = orders;
        this.users = users;
        this.products = products;
        this.stripeSecretKey = stripeSecretKey;
        this.publicUrl = publicUrl.replaceAll("/$", "");
    }

    @PostMapping("/session")
    @Transactional
    public ResponseEntity<?> createSession(@AuthenticationPrincipal UserDetails principal,
            @RequestBody OrderRequest request) throws Exception {
        if (stripeSecretKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Stripe er ikke konfigureret endnu. Tilføj STRIPE_SECRET_KEY i secrets.properties eller Azure Application Settings.");
        }
        User customer = users.findByEmail(principal.getUsername()).orElseThrow();
        if (customer.getRole() == Role.BUSINESS) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Virksomhedsprofiler kan ikke købe produkter.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            return ResponseEntity.badRequest().body("Din kurv er tom.");
        }

        List<OrderItem> orderItems = request.items().stream()
                .map(this::validatedItem).toList();
        BigDecimal total = orderItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setPurchasedAt(LocalDateTime.now());
        order.setItems(orderItems);
        order.setTotalAmount(total);
        order.setPaymentStatus("PENDING");
        order = orders.save(order);

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setIntegrationIdentifier("barbercom_checkout_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .setCustomerEmail(customer.getEmail())
                .setClientReferenceId(order.getId().toString())
                .putMetadata("barbercom_order_id", order.getId().toString())
                .setSuccessUrl(publicUrl + "/account.html?payment=success&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(publicUrl + "/checkout.html?cancelled=1");
        for (OrderItem item : orderItems) {
            params.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(item.getQuantity().longValue())
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("dkk")
                            .setUnitAmount(item.getUnitPrice().movePointRight(2).longValueExact())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(item.getProductName()).build())
                            .build())
                    .build());
        }
        Session session = new StripeClient(stripeSecretKey).v1().checkout().sessions().create(params.build());
        order.setStripeCheckoutSessionId(session.getId());
        orders.save(order);
        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    private OrderItem validatedItem(OrderItemRequest request) {
        if (request.name() == null || request.name().isBlank() || request.quantity() == null
                || request.quantity() < 1 || request.quantity() > 20) {
            throw new IllegalArgumentException("En vare i kurven er ugyldig.");
        }
        String name = request.name().trim();
        ShopProduct product = products.findAll().stream()
                .filter(candidate -> candidate.getName().equalsIgnoreCase(name))
                .filter(candidate -> request.sellerId() == null || candidate.getSeller().getId().equals(request.sellerId()))
                .findFirst().orElse(null);
        if (product == null) {
            throw new IllegalArgumentException("Produktet findes ikke længere i shoppen.");
        }
        User seller = product.getSeller();
        String sellerName = seller.getBusinessName() == null || seller.getBusinessName().isBlank()
                ? seller.getFirstName() + " " + seller.getLastName() : seller.getBusinessName();
        return new OrderItem(product.getName(), request.quantity(), product.getPrice(), seller.getId(), sellerName);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> invalidCheckout(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(error.getMessage());
    }
}

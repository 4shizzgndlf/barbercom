package dk.ek.barbercom.service;

import com.stripe.StripeClient;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import dk.ek.barbercom.dto.UserDTO;
import dk.ek.barbercom.model.Role;
import dk.ek.barbercom.model.User;
import dk.ek.barbercom.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BusinessSubscriptionService {
    private final UserService users;
    private final UserRepository userRepository;
    private final RegistrationNotificationService notifications;
    private final String stripeSecretKey;
    private final String priceId;
    private final String publicUrl;

    public BusinessSubscriptionService(UserService users, UserRepository userRepository,
            RegistrationNotificationService notifications,
            @Value("${barbercom.stripe.secret-key:}") String stripeSecretKey,
            @Value("${barbercom.stripe.business-subscription-price-id:}") String priceId,
            @Value("${barbercom.public-url:http://localhost:8080}") String publicUrl) {
        this.users = users;
        this.userRepository = userRepository;
        this.notifications = notifications;
        this.stripeSecretKey = stripeSecretKey;
        this.priceId = priceId;
        this.publicUrl = publicUrl.replaceAll("/$", "");
    }

    public Map<String, String> startRegistration(UserDTO request) throws Exception {
        if (request.role() != Role.BUSINESS) {
            throw new IllegalArgumentException("Abonnementet gælder kun business-konti.");
        }
        if (stripeSecretKey.isBlank() || priceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Business-abonnementet er ikke konfigureret endnu.");
        }

        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        User business = userRepository.findByEmail(email).orElse(null);
        boolean newlyCreated = false;
        if (business != null) {
            if (business.getRole() != Role.BUSINESS || !"PENDING".equals(business.getBusinessSubscriptionStatus())) {
                throw new IllegalArgumentException("Der findes allerede en konto med denne e-mail");
            }
        } else {
            UserDTO created = users.createUser(request);
            business = userRepository.findById(created.id()).orElseThrow();
            business.setBusinessSubscriptionStatus("PENDING");
            userRepository.save(business);
            newlyCreated = true;
        }

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setIntegrationIdentifier("barbercom_business_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                    .setCustomerEmail(business.getEmail())
                    .setClientReferenceId(business.getId().toString())
                    .putMetadata("barbercom_business_user_id", business.getId().toString())
                    .setSuccessUrl(publicUrl + "/login.html?businessSubscription=success&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(publicUrl + "/register.html?businessSubscription=cancelled")
                    .addLineItem(SessionCreateParams.LineItem.builder().setPrice(priceId).setQuantity(1L).build())
                    .build();
            Session session = new StripeClient(stripeSecretKey).v1().checkout().sessions().create(params);
            business.setBusinessSubscriptionCheckoutSessionId(session.getId());
            userRepository.save(business);
            return Map.of("checkoutUrl", session.getUrl());
        } catch (Exception exception) {
            if (newlyCreated) userRepository.delete(business);
            throw exception;
        }
    }

    public void activateFromCheckout(String checkoutSessionId, String subscriptionId) {
        userRepository.findByBusinessSubscriptionCheckoutSessionId(checkoutSessionId).ifPresent(user -> {
            user.setBusinessSubscriptionStatus("ACTIVE");
            user.setStripeSubscriptionId(subscriptionId);
            if (!user.isBusinessSubscriptionConfirmationEmailSent()
                    && notifications.sendBusinessSubscriptionConfirmation(user)) {
                user.setBusinessSubscriptionConfirmationEmailSent(true);
            }
            userRepository.save(user);
        });
    }

    /**
     * Recovers a paid signup if the browser returns before a local webhook
     * listener receives the event. Stripe must report the exact stored Checkout
     * Session as paid before the business account is activated.
     */
    public void reconcilePendingSubscription(String email) {
        if (stripeSecretKey.isBlank() || email == null) return;
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {
            if (user.getRole() != Role.BUSINESS || !"PENDING".equals(user.getBusinessSubscriptionStatus())
                    || user.getBusinessSubscriptionCheckoutSessionId() == null) return;
            try {
                Session session = new StripeClient(stripeSecretKey).v1().checkout().sessions()
                        .retrieve(user.getBusinessSubscriptionCheckoutSessionId());
                if ("subscription".equals(session.getMode()) && "paid".equals(session.getPaymentStatus())) {
                    activateFromCheckout(session.getId(), session.getSubscription());
                }
            } catch (Exception ignored) {
                // A Stripe outage must not prevent other users from logging in.
            }
        });
    }

    public void updateSubscriptionStatus(Subscription subscription) {
        userRepository.findByStripeSubscriptionId(subscription.getId()).ifPresent(user -> {
            boolean active = "active".equals(subscription.getStatus()) || "trialing".equals(subscription.getStatus());
            user.setBusinessSubscriptionStatus(active ? "ACTIVE" : "INACTIVE");
            userRepository.save(user);
        });
    }
}

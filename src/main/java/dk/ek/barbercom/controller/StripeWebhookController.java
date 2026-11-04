package dk.ek.barbercom.controller;

import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.StripeClient;
import dk.ek.barbercom.model.CustomerOrder;
import dk.ek.barbercom.repository.CustomerOrderRepository;
import dk.ek.barbercom.service.OrderNotificationService;
import dk.ek.barbercom.service.BusinessSubscriptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {
    private final CustomerOrderRepository orders;
    private final OrderNotificationService notifications;
    private final BusinessSubscriptionService businessSubscriptions;
    private final String webhookSecret;
    private final String stripeSecretKey;

    public StripeWebhookController(CustomerOrderRepository orders, OrderNotificationService notifications,
            BusinessSubscriptionService businessSubscriptions,
            @Value("${barbercom.stripe.webhook-secret:}") String webhookSecret,
            @Value("${barbercom.stripe.secret-key:}") String stripeSecretKey) {
        this.orders = orders;
        this.notifications = notifications;
        this.businessSubscriptions = businessSubscriptions;
        this.webhookSecret = webhookSecret;
        this.stripeSecretKey = stripeSecretKey;
    }

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<Void> webhook(@RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (webhookSecret.isBlank() || stripeSecretKey.isBlank() || signature == null) return ResponseEntity.badRequest().build();
        final Event event;
        try {
            event = new StripeClient(stripeSecretKey).constructEvent(payload, signature, webhookSecret);
        } catch (Exception ignored) {
            return ResponseEntity.badRequest().build();
        }
        if ("checkout.session.completed".equals(event.getType())
                || "checkout.session.async_payment_succeeded".equals(event.getType())) {
            StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
            if (object instanceof Session session && !"unpaid".equals(session.getPaymentStatus())) {
                orders.findByStripeCheckoutSessionId(session.getId()).ifPresent(order -> {
                    markPaidAndNotify(order);
                });
                if ("subscription".equals(session.getMode())) {
                    businessSubscriptions.activateFromCheckout(session.getId(), session.getSubscription());
                }
            }
        }
        if ("checkout.session.async_payment_failed".equals(event.getType())) {
            StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
            if (object instanceof Session session) {
                orders.findByStripeCheckoutSessionId(session.getId()).ifPresent(order -> {
                    order.setPaymentStatus("FAILED");
                    orders.save(order);
                });
            }
        }
        if ("customer.subscription.updated".equals(event.getType())
                || "customer.subscription.deleted".equals(event.getType())) {
            StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
            if (object instanceof com.stripe.model.Subscription subscription) {
                businessSubscriptions.updateSubscriptionStatus(subscription);
            }
        }
        return ResponseEntity.ok().build();
    }

    private void markPaidAndNotify(CustomerOrder order) {
        order.setPaymentStatus("PAID");
        if (!order.isOrderConfirmationEmailSent() && notifications.sendOrderConfirmation(order)) {
            order.setOrderConfirmationEmailSent(true);
        }
        orders.save(order);
    }
}

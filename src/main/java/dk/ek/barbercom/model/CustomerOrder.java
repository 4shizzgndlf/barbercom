package dk.ek.barbercom.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private User customer;

    @Column(nullable = false)
    private LocalDateTime purchasedAt;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    private String paymentStatus = "PAID";
    private String stripeCheckoutSessionId;
    private boolean orderConfirmationEmailSent;

    @ElementCollection
    @CollectionTable(name = "customer_order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(LocalDateTime purchasedAt) { this.purchasedAt = purchasedAt; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public boolean isPaid() { return paymentStatus == null || "PAID".equals(paymentStatus); }
    public String getStripeCheckoutSessionId() { return stripeCheckoutSessionId; }
    public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) { this.stripeCheckoutSessionId = stripeCheckoutSessionId; }
    public boolean isOrderConfirmationEmailSent() { return orderConfirmationEmailSent; }
    public void setOrderConfirmationEmailSent(boolean orderConfirmationEmailSent) { this.orderConfirmationEmailSent = orderConfirmationEmailSent; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}

package dk.ek.barbercom.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    @Column(nullable = false, length = 255)
    private String password;
    private Integer phoneNumber;
    private String address;
    private Integer zipCode;
    private String businessName;
    private String businessSubscriptionStatus;
    private String businessSubscriptionCheckoutSessionId;
    private String stripeSubscriptionId;
    private boolean businessSubscriptionConfirmationEmailSent;
    private LocalDateTime businessNotificationsReadAt;
    @Enumerated(EnumType.STRING)
    private Role role;

    // Default constructor required by JPA
    public User() {}

    // Constructor with fields
    public User(Long id, String firstName, String lastName, String email, String password, Integer phoneNumber, String address, Integer zipCode, Role role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.zipCode = zipCode;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(Integer phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getZipCode() {
        return zipCode;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessSubscriptionStatus() { return businessSubscriptionStatus; }
    public void setBusinessSubscriptionStatus(String businessSubscriptionStatus) { this.businessSubscriptionStatus = businessSubscriptionStatus; }
    public String getBusinessSubscriptionCheckoutSessionId() { return businessSubscriptionCheckoutSessionId; }
    public void setBusinessSubscriptionCheckoutSessionId(String value) { this.businessSubscriptionCheckoutSessionId = value; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String value) { this.stripeSubscriptionId = value; }
    public boolean isBusinessSubscriptionConfirmationEmailSent() { return businessSubscriptionConfirmationEmailSent; }
    public void setBusinessSubscriptionConfirmationEmailSent(boolean value) { this.businessSubscriptionConfirmationEmailSent = value; }
    public boolean hasActiveBusinessSubscription() {
        return role != Role.BUSINESS || businessSubscriptionStatus == null || "ACTIVE".equals(businessSubscriptionStatus);
    }

    public LocalDateTime getBusinessNotificationsReadAt() { return businessNotificationsReadAt; }
    public void setBusinessNotificationsReadAt(LocalDateTime value) { this.businessNotificationsReadAt = value; }

    public void setZipCode(Integer zipCode) {
        this.zipCode = zipCode;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}

package dk.ek.barbercom.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "customer_id")
    private User customer;
    @Column(name = "manual_customer_name") private String manualCustomerName;
    @Column(name = "manual_customer_email") private String manualCustomerEmail;
    @Column(name = "manual_customer_phone") private String manualCustomerPhone;
    @ManyToOne(optional = false) @JoinColumn(name = "barber_id")
    private User barber;
    @ManyToOne @JoinColumn(name = "chair_id")
    private SalonChair chair;
    @Column(name = "booking_date", nullable = false) private LocalDate bookingDate;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "duration_minutes", nullable = false, columnDefinition = "integer default 30") private Integer durationMinutes = 30;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public String getManualCustomerName() { return manualCustomerName; }
    public void setManualCustomerName(String manualCustomerName) { this.manualCustomerName = manualCustomerName; }
    public String getManualCustomerEmail() { return manualCustomerEmail; }
    public void setManualCustomerEmail(String manualCustomerEmail) { this.manualCustomerEmail = manualCustomerEmail; }
    public String getManualCustomerPhone() { return manualCustomerPhone; }
    public void setManualCustomerPhone(String manualCustomerPhone) { this.manualCustomerPhone = manualCustomerPhone; }
    public User getBarber() { return barber; }
    public void setBarber(User barber) { this.barber = barber; }
    public SalonChair getChair() { return chair; }
    public void setChair(SalonChair chair) { this.chair = chair; }
    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

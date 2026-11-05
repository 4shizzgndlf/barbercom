package dk.ek.barbercom.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"chair_id", "booking_date", "start_time"}))
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "customer_id")
    private User customer;
    @ManyToOne(optional = false) @JoinColumn(name = "barber_id")
    private User barber;
    @ManyToOne @JoinColumn(name = "chair_id")
    private SalonChair chair;
    @Column(name = "booking_date", nullable = false) private LocalDate bookingDate;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public User getBarber() { return barber; }
    public void setBarber(User barber) { this.barber = barber; }
    public SalonChair getChair() { return chair; }
    public void setChair(SalonChair chair) { this.chair = chair; }
    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
}

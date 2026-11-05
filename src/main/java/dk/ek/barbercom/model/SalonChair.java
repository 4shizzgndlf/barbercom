package dk.ek.barbercom.model;

import jakarta.persistence.*;

@Entity
@Table(name = "salon_chair", uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "name"}))
public class SalonChair {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "business_id") private User business;
    @Column(nullable = false) private String name;
    public Long getId() { return id; }
    public User getBusiness() { return business; } public void setBusiness(User value) { business = value; }
    public String getName() { return name; } public void setName(String value) { name = value; }
}

package dk.ek.barbercom.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class ShopProduct {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String description;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;
    @Column(nullable = false) private String category;
    @Column(nullable = false) private String type;
    private String imageUrl;
    private Integer homepageSlider;
    @ManyToOne(optional = false) @JoinColumn(name = "seller_id") private User seller;
    public Long getId() { return id; } public String getName() { return name; } public void setName(String value) { name = value; }
    public String getDescription() { return description; } public void setDescription(String value) { description = value; }
    public BigDecimal getPrice() { return price; } public void setPrice(BigDecimal value) { price = value; }
    public String getCategory() { return category; } public void setCategory(String value) { category = value; }
    public String getType() { return type; } public void setType(String value) { type = value; }
    public String getImageUrl() { return imageUrl; } public void setImageUrl(String value) { imageUrl = value; }
    public int getHomepageSlider() { return homepageSlider == null ? 0 : homepageSlider; }
    public void setHomepageSlider(Integer value) { homepageSlider = value; }
    public User getSeller() { return seller; } public void setSeller(User value) { seller = value; }
}

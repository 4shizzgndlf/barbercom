package dk.ek.barbercom.model;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class OrderItem {
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Long sellerId;
    private String sellerName;

    public OrderItem() { }

    public OrderItem(String productName, Integer quantity, BigDecimal unitPrice, Long sellerId, String sellerName) {
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
    }

    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Long getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
}

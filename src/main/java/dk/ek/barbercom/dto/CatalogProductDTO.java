package dk.ek.barbercom.dto;
import java.math.BigDecimal;
public record CatalogProductDTO(Long id, String name, String description, BigDecimal price, String category, String type, String imageUrl, Long sellerId, String sellerName, int homepageSlider) { }

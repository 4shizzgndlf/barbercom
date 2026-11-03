package dk.ek.barbercom.controller;

import dk.ek.barbercom.dto.*;
import dk.ek.barbercom.mapper.UserMapper;
import dk.ek.barbercom.model.*;
import dk.ek.barbercom.repository.CustomerOrderRepository;
import dk.ek.barbercom.repository.BookingRepository;
import dk.ek.barbercom.repository.BookingCancellationRepository;
import dk.ek.barbercom.repository.ShopProductRepository;
import dk.ek.barbercom.repository.UserRepository;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AdminController {
    private final UserRepository users;
    private final CustomerOrderRepository orders;
    private final BookingRepository bookings;
    private final BookingCancellationRepository cancellations;
    private final ShopProductRepository products;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository users, CustomerOrderRepository orders, BookingRepository bookings, BookingCancellationRepository cancellations, ShopProductRepository products, PasswordEncoder passwordEncoder) {
        this.users = users; this.orders = orders; this.bookings = bookings; this.cancellations = cancellations; this.products = products; this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/api/catalog/products")
    public List<CatalogProductDTO> catalogProducts() { return products.findAll().stream().map(this::toDto).toList(); }

    @GetMapping("/api/admin/dashboard")
    public AdminDashboardDTO dashboard(@AuthenticationPrincipal UserDetails principal) {
        requireAdmin(principal);
        List<UserDTO> allUsers = users.findAll().stream().map(UserMapper::toDTO).toList();
        List<UserDTO> businesses = users.findByRoleOrderByIdAsc(Role.BUSINESS).stream().map(UserMapper::toDTO).toList();
        BigDecimal revenue = orders.findAll().stream().map(CustomerOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AdminDashboardDTO(allUsers.size(), businesses.size(), orders.count(), bookings.count(), cancellations.count(), revenue, allUsers, businesses, products.findAll().stream().map(this::toDto).toList());
    }

    @PostMapping(value = "/api/admin/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addProduct(@AuthenticationPrincipal UserDetails principal, @RequestParam String name, @RequestParam(required = false) String description, @RequestParam BigDecimal price, @RequestParam(required = false) String category, @RequestParam(required = false) String type, @RequestParam Long sellerId, @RequestParam(required = false) MultipartFile image) {
        requireAdmin(principal);
        if (name.isBlank() || price.signum() < 0) return ResponseEntity.badRequest().body("Udfyld produktnavn, pris og barbershop.");
        User seller = users.findById(sellerId).filter(user -> user.getRole() == Role.BUSINESS).orElse(null);
        if (seller == null) return ResponseEntity.badRequest().body("Vælg en gyldig barbershop.");
        ShopProduct product = new ShopProduct(); product.setName(name.trim()); product.setDescription(blankOr(description, "Udvalgt produkt fra din lokale barber.")); product.setPrice(price); product.setCategory(blankOr(category, "Styling")); product.setType(validType(type)); product.setSeller(seller);
        if (image != null && !image.isEmpty()) product.setImageUrl(storeImage(image));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(products.save(product)));
    }

    @DeleteMapping("/api/admin/products/{id}")
    public ResponseEntity<Void> removeProduct(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        requireAdmin(principal); if (!products.existsById(id)) return ResponseEntity.notFound().build(); products.deleteById(id); return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/barbers")
    public ResponseEntity<?> addBusiness(@AuthenticationPrincipal UserDetails principal, @RequestBody BusinessRequest request) {
        requireAdmin(principal);
        if (request.firstName() == null || request.firstName().isBlank() || request.businessName() == null || request.businessName().isBlank() || request.email() == null || request.email().isBlank()) return ResponseEntity.badRequest().body("Udfyld navn, virksomhedsnavn og e-mail.");
        String email = request.email().trim().toLowerCase(); if (users.existsByEmail(email)) return ResponseEntity.badRequest().body("E-mailen er allerede i brug.");
        User business = new User(null, request.firstName().trim(), blankOr(request.lastName(), ""), email, passwordEncoder.encode(blankOr(request.password(), "barberpass")), request.phoneNumber(), request.address(), request.zipCode(), Role.BUSINESS);
        business.setBusinessName(request.businessName().trim()); return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toDTO(users.save(business)));
    }

    @DeleteMapping("/api/admin/barbers/{id}")
    public ResponseEntity<?> removeBusiness(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        requireAdmin(principal); User business = users.findById(id).filter(user -> user.getRole() == Role.BUSINESS).orElse(null);
        if (business == null) return ResponseEntity.notFound().build(); users.delete(business); return ResponseEntity.noContent().build();
    }

    private void requireAdmin(UserDetails principal) { User user = users.findByEmail(principal.getUsername()).orElseThrow(); if (user.getRole() != Role.ADMIN) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Kun administratorer har adgang."); }
    private CatalogProductDTO toDto(ShopProduct product) { User seller = product.getSeller(); String name = seller.getBusinessName() == null || seller.getBusinessName().isBlank() ? seller.getFirstName() + " " + seller.getLastName() : seller.getBusinessName(); return new CatalogProductDTO(product.getId(), product.getName(), product.getDescription(), product.getPrice(), product.getCategory(), product.getType(), product.getImageUrl(), seller.getId(), name); }
    private String storeImage(MultipartFile image) { try { if (!image.getContentType().startsWith("image/")) throw new IllegalArgumentException("Vælg en billedfil."); String original = image.getOriginalFilename(); String extension = original != null && original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".png"; String fileName = "product-upload-" + UUID.randomUUID() + extension; Path source = Paths.get("src/main/resources/static/images", fileName); Files.createDirectories(source.getParent()); Files.copy(image.getInputStream(), source, StandardCopyOption.REPLACE_EXISTING); Path running = Paths.get("target/classes/static/images", fileName); Files.createDirectories(running.getParent()); Files.copy(source, running, StandardCopyOption.REPLACE_EXISTING); return "/images/" + fileName; } catch (Exception error) { throw new IllegalArgumentException("Billedet kunne ikke gemmes."); } }
    private String blankOr(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private String validType(String value) { return List.of("clay", "spray", "paste", "shampoo", "pomade", "powder", "oil", "tonic", "gel", "set").contains(value) ? value : "clay"; }
}

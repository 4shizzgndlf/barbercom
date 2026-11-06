package dk.ek.barbercom.controller;

import dk.ek.barbercom.dto.*;
import dk.ek.barbercom.mapper.UserMapper;
import dk.ek.barbercom.model.*;
import dk.ek.barbercom.repository.CustomerOrderRepository;
import dk.ek.barbercom.repository.BookingRepository;
import dk.ek.barbercom.repository.BookingCancellationRepository;
import dk.ek.barbercom.repository.ShopProductRepository;
import dk.ek.barbercom.repository.UserRepository;
import dk.ek.barbercom.repository.SalonChairRepository;
import dk.ek.barbercom.service.BusinessSubscriptionService;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

@RestController
public class AdminController {
    private final UserRepository users;
    private final CustomerOrderRepository orders;
    private final BookingRepository bookings;
    private final BookingCancellationRepository cancellations;
    private final ShopProductRepository products;
    private final PasswordEncoder passwordEncoder;
    private final Path productImageDirectory;
    private final SalonChairRepository chairs;
    private final BusinessSubscriptionService businessSubscriptions;

    public AdminController(UserRepository users, CustomerOrderRepository orders, BookingRepository bookings, BookingCancellationRepository cancellations, ShopProductRepository products, SalonChairRepository chairs, BusinessSubscriptionService businessSubscriptions, PasswordEncoder passwordEncoder, @Value("${barbercom.product-image-directory:uploads}") String productImageDirectory) {
        this.users = users; this.orders = orders; this.bookings = bookings; this.cancellations = cancellations; this.products = products; this.passwordEncoder = passwordEncoder;
        this.chairs = chairs;
        this.businessSubscriptions = businessSubscriptions;
        this.productImageDirectory = Paths.get(productImageDirectory).toAbsolutePath().normalize();
    }

    @GetMapping("/api/catalog/products")
    public List<CatalogProductDTO> catalogProducts() { return products.findAll().stream().map(this::toDto).toList(); }

    @GetMapping("/api/admin/dashboard")
    public AdminDashboardDTO dashboard(@AuthenticationPrincipal UserDetails principal) {
        requireAdmin(principal);
        List<UserDTO> allUsers = users.findAll().stream().map(UserMapper::toDTO).toList();
        List<User> businessUsers = users.findByRoleOrderByIdAsc(Role.BUSINESS);
        businessUsers.forEach(businessSubscriptions::synchronizeSubscriptionStatus);
        businessUsers = users.findByRoleOrderByIdAsc(Role.BUSINESS);
        List<UserDTO> businesses = businessUsers.stream().map(UserMapper::toDTO).toList();
        long subscriptionCount = businessUsers.stream()
                .filter(User::hasActiveBusinessSubscription)
                .filter(user -> user.getStripeSubscriptionId() != null && !user.getStripeSubscriptionId().isBlank())
                .count();
        long cancelledSubscriptionCount = businessUsers.stream()
                .filter(user -> user.isBusinessSubscriptionCancelAtPeriodEnd()
                        || "INACTIVE".equals(user.getBusinessSubscriptionStatus()))
                .count();
        BigDecimal revenue = orders.findAll().stream().filter(CustomerOrder::isPaid)
                .map(CustomerOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidOrderCount = orders.findAll().stream().filter(CustomerOrder::isPaid).count();
        return new AdminDashboardDTO(allUsers.size(), businesses.size(), subscriptionCount, cancelledSubscriptionCount, paidOrderCount, bookings.count(), cancellations.count(), revenue, allUsers, businesses, products.findAll().stream().map(this::toDto).toList());
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

    @PostMapping("/api/admin/products/import-standard")
    public ResponseEntity<?> importStandardProducts(@AuthenticationPrincipal UserDetails principal) {
        requireAdmin(principal);
        List<User> businesses = users.findByRoleOrderByIdAsc(Role.BUSINESS);
        if (businesses.isEmpty()) return ResponseEntity.badRequest().body("Opret mindst én barbershop først.");
        List<String> existingNames = products.findAll().stream().map(product -> product.getName().toLowerCase()).toList();
        List<ShopProduct> imported = new java.util.ArrayList<>();
        int businessIndex = 0;
        for (StandardProduct standard : STANDARD_PRODUCTS) {
            if (existingNames.contains(standard.name().toLowerCase())) continue;
            ShopProduct product = new ShopProduct();
            product.setName(standard.name());
            product.setDescription(standard.description());
            product.setPrice(standard.price());
            product.setCategory(standard.category());
            product.setType(standard.type());
            product.setSeller(businesses.get(businessIndex++ % businesses.size()));
            imported.add(product);
        }
        products.saveAll(imported);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("count", imported.size()));
    }

    @DeleteMapping("/api/admin/products/{id}")
    public ResponseEntity<Void> removeProduct(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        requireAdmin(principal); if (!products.existsById(id)) return ResponseEntity.notFound().build(); products.deleteById(id); return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/admin/products/{id}/homepage")
    public ResponseEntity<?> setHomepageSlider(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id, @RequestParam int slider) {
        requireAdmin(principal);
        if (slider < 0 || slider > 2) return ResponseEntity.badRequest().body("Vælg en gyldig placering på forsiden.");
        ShopProduct product = products.findById(id).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();
        product.setHomepageSlider(slider);
        return ResponseEntity.ok(toDto(products.save(product)));
    }

    @PostMapping(value = "/api/admin/products/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> replaceProductImage(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id, @RequestParam MultipartFile image) {
        requireAdmin(principal);
        if (image.isEmpty()) return ResponseEntity.badRequest().body("Vælg et billede først.");
        ShopProduct product = products.findById(id).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();
        product.setImageUrl(storeImage(image));
        return ResponseEntity.ok(toDto(products.save(product)));
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
    @Transactional
    public ResponseEntity<?> removeBusiness(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        requireAdmin(principal);
        User business = users.findById(id).filter(user -> user.getRole() == Role.BUSINESS).orElse(null);
        if (business == null) return ResponseEntity.notFound().build();
        if (bookings.existsByBarberId(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Barbershoppen kan ikke fjernes, fordi den har bookinger. Annullér eller afslut først de tilknyttede bookinger.");
        }

        // Products and unused chairs point directly at this business user.
        products.deleteAll(products.findBySellerId(id));
        chairs.deleteAll(chairs.findByBusinessIdOrderByIdAsc(id));
        products.flush();
        chairs.flush();
        users.delete(business);
        users.flush();
        return ResponseEntity.noContent().build();
    }

    /** Removes operational history while retaining users, shops, products, and subscriptions. */
    @PostMapping("/api/admin/reset-commerce-data")
    @Transactional
    public ResponseEntity<Void> resetCommerceData(@AuthenticationPrincipal UserDetails principal) {
        requireAdmin(principal);
        bookings.deleteAll();
        orders.deleteAll();
        cancellations.deleteAll();
        bookings.flush();
        orders.flush();
        cancellations.flush();
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(UserDetails principal) { User user = users.findByEmail(principal.getUsername()).orElseThrow(); if (user.getRole() != Role.ADMIN) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Kun administratorer har adgang."); }
    private CatalogProductDTO toDto(ShopProduct product) { User seller = product.getSeller(); String name = seller.getBusinessName() == null || seller.getBusinessName().isBlank() ? seller.getFirstName() + " " + seller.getLastName() : seller.getBusinessName(); return new CatalogProductDTO(product.getId(), product.getName(), product.getDescription(), product.getPrice(), product.getCategory(), product.getType(), product.getImageUrl(), seller.getId(), name, product.getHomepageSlider()); }
    private String storeImage(MultipartFile image) {
        try {
            if (image.getContentType() == null || !image.getContentType().startsWith("image/")) throw new IllegalArgumentException("Vælg en billedfil.");
            String original = image.getOriginalFilename();
            String extension = original != null && original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase() : ".png";
            if (!List.of(".png", ".jpg", ".jpeg", ".webp", ".gif").contains(extension)) throw new IllegalArgumentException("Billedformatet understøttes ikke.");
            String fileName = "product-upload-" + UUID.randomUUID() + extension;
            Files.createDirectories(productImageDirectory);
            Files.copy(image.getInputStream(), productImageDirectory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + fileName;
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException("Billedet kunne ikke gemmes.");
        }
    }
    private String blankOr(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private String validType(String value) { return List.of("clay", "spray", "paste", "shampoo", "pomade", "powder", "oil", "tonic", "gel", "set").contains(value) ? value : "clay"; }

    private record StandardProduct(String name, String description, BigDecimal price, String category, String type) { }

    private static final List<StandardProduct> STANDARD_PRODUCTS = List.of(
            new StandardProduct("Mat clay", "Teksturerende clay med naturligt hold", new BigDecimal("189"), "Styling", "clay"),
            new StandardProduct("Havsaltspray", "Teksturspray med let volumen", new BigDecimal("149"), "Styling", "spray"),
            new StandardProduct("Sculpt paste", "Fleksibelt hold til hverdagsstyling", new BigDecimal("179"), "Styling", "paste"),
            new StandardProduct("Daglig shampoo", "Ren og frisk pleje til hver dag", new BigDecimal("139"), "Hårpleje", "shampoo"),
            new StandardProduct("Finish pomade", "Naturlig glans og klassisk finish", new BigDecimal("199"), "Styling", "pomade"),
            new StandardProduct("Volumenpudder", "Løft ved rødderne med mat finish", new BigDecimal("159"), "Styling", "powder"),
            new StandardProduct("Skægolie", "Blødgør og plejer skægget", new BigDecimal("169"), "Skæg", "oil"),
            new StandardProduct("Hårtonic", "Frisk pleje til hovedbunden", new BigDecimal("129"), "Hårpleje", "tonic"),
            new StandardProduct("Stærkt hold", "Definerende gel med stærkt hold", new BigDecimal("149"), "Styling", "gel"),
            new StandardProduct("Rejsesæt", "Hverdagens essentials i rejseformat", new BigDecimal("249"), "Sæt", "set"),
            new StandardProduct("Curl cream", "Bløde og definerede krøller", new BigDecimal("179"), "Styling", "paste"),
            new StandardProduct("Pre-shave oil", "En behagelig og tæt barbering", new BigDecimal("159"), "Skæg", "oil"),
            new StandardProduct("Skægbalm", "Form og fugt til skægget", new BigDecimal("169"), "Skæg", "pomade"),
            new StandardProduct("Cooling tonic", "Frisk finish til hovedbunden", new BigDecimal("139"), "Hårpleje", "tonic"),
            new StandardProduct("Fiber wax", "Stærkt og naturligt hold", new BigDecimal("189"), "Styling", "clay"),
            new StandardProduct("Conditioner", "Plejende hverdagsrutine", new BigDecimal("149"), "Hårpleje", "shampoo"),
            new StandardProduct("Barber brush", "Klassisk påføring til barbering", new BigDecimal("219"), "Værktøj", "set"),
            new StandardProduct("Aftershave mist", "Kølig og rolig hud efter barbering", new BigDecimal("159"), "Skæg", "spray"),
            new StandardProduct("Texture dust", "Let volumen hele dagen", new BigDecimal("169"), "Styling", "powder"),
            new StandardProduct("Grooming kit", "Alt til din daglige rutine", new BigDecimal("299"), "Sæt", "set")
    );
}

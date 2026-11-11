package dk.ek.barbercom.controller;

import dk.ek.barbercom.dto.*;
import dk.ek.barbercom.model.*;
import dk.ek.barbercom.repository.BookingRepository;
import dk.ek.barbercom.repository.BookingCancellationRepository;
import dk.ek.barbercom.repository.SalonChairRepository;
import dk.ek.barbercom.repository.UserRepository;
import dk.ek.barbercom.service.BookingNotificationService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final UserRepository users;
    private final BookingRepository bookings;
    private final BookingCancellationRepository cancellations;
    private final SalonChairRepository chairs;
    private final BookingNotificationService notifications;

    public BookingController(UserRepository users, BookingRepository bookings, BookingCancellationRepository cancellations, SalonChairRepository chairs, BookingNotificationService notifications) {
        this.users = users;
        this.bookings = bookings;
        this.cancellations = cancellations;
        this.chairs = chairs;
        this.notifications = notifications;
    }

    @GetMapping("/barbers")
    public List<BarberDTO> barbers() {
        return users.findByRoleOrderByIdAsc(Role.BUSINESS).stream()
                .map(user -> {
                    String personName = user.getFirstName() + " " + user.getLastName();
                    String displayName = user.getBusinessName() == null || user.getBusinessName().isBlank()
                            ? personName : user.getBusinessName();
                    return new BarberDTO(user.getId(), user.getFirstName(), user.getLastName(), user.getBusinessName(), displayName);
                }).toList();
    }

    @GetMapping("/bookings/availability")
    @Transactional
    public List<ChairAvailabilityDTO> availability(@RequestParam Long barberId, @RequestParam LocalDate date) {
        User business = users.findById(barberId).filter(user -> user.getRole() == Role.BUSINESS).orElseThrow();
        return chairsFor(business).stream().map(chair -> new ChairAvailabilityDTO(chair.getId(), chair.getName(),
                bookings.findByChairIdAndBookingDate(chair.getId(), date).stream()
                        .map(booking -> new BookingIntervalDTO(booking.getStartTime(), endTime(booking)))
                        .toList())).toList();
    }

    @GetMapping("/bookings/mine")
    public List<BookingDTO> mine(@AuthenticationPrincipal UserDetails principal) {
        User customer = currentUser(principal);
        return bookings.findByCustomerIdOrderByBookingDateDesc(customer.getId()).stream().map(this::toDto).toList();
    }

    @GetMapping("/bookings/business")
    public ResponseEntity<?> businessBookings(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam LocalDate date) {
        User business = currentUser(principal);
        if (business.getRole() != Role.BUSINESS) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Kun business-brugere kan se denne oversigt.");
        }
        List<BusinessBookingDTO> result = bookings
                .findByBarberIdAndBookingDateOrderByStartTimeAsc(business.getId(), date)
                .stream()
                .map(this::toBusinessDto)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bookings")
    @Transactional
    public ResponseEntity<?> create(@AuthenticationPrincipal UserDetails principal, @RequestBody BookingRequest request) {
        User customer = currentUser(principal);
        if (customer.getRole() == Role.BUSINESS) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Virksomhedsprofiler kan ikke booke tider. Log ind med en privat- eller administratorprofil.");
        }
        if (request.barberId() == null || request.chairId() == null || request.date() == null || request.startTime() == null || !validDuration(request.durationMinutes()))
            return ResponseEntity.badRequest().body("Vælg barbershop, stol, dato, tidspunkt og en varighed mellem 5 og 480 minutter.");
        if (request.date().isBefore(LocalDate.now()) || (request.date().isEqual(LocalDate.now()) && request.startTime().isBefore(LocalTime.now())))
            return ResponseEntity.badRequest().body("Tidspunktet er allerede passeret.");
        User barber = users.findById(request.barberId()).filter(user -> user.getRole() == Role.BUSINESS).orElse(null);
        if (barber == null) return ResponseEntity.badRequest().body("Den valgte barber findes ikke.");
        SalonChair chair = chairs.findById(request.chairId()).filter(value -> value.getBusiness().getId().equals(barber.getId())).orElse(null);
        if (chair == null) return ResponseEntity.badRequest().body("Den valgte stol findes ikke hos barbershoppen.");
        if (!sameDay(request.startTime(), request.durationMinutes()))
            return ResponseEntity.badRequest().body("Bookingen skal slutte samme dag.");
        if (!chairIsAvailable(chair, request.date(), request.startTime(), request.durationMinutes()))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Tiden overlapper en eksisterende booking. Vælg en anden stol eller et andet tidspunkt.");
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setBarber(barber);
        booking.setChair(chair);
        booking.setBookingDate(request.date());
        booking.setStartTime(request.startTime());
        booking.setDurationMinutes(request.durationMinutes());
        Booking savedBooking = bookings.save(booking);
        notifications.sendBookingConfirmation(savedBooking);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(savedBooking));
    }

    @PostMapping("/account/bookings")
    @Transactional
    public ResponseEntity<?> createForBusiness(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody ManualBookingRequest request) {
        User business = currentUser(principal);
        if (business.getRole() != Role.BUSINESS) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Kun virksomheder kan oprette bookinger her.");
        }
        String customerName = request.customerName() == null ? "" : request.customerName().trim();
        String customerEmail = request.customerEmail() == null ? "" : request.customerEmail().trim();
        String customerPhone = request.customerPhone() == null ? "" : request.customerPhone().replaceAll("\\D", "");
        if (customerPhone.length() == 10 && customerPhone.startsWith("45")) customerPhone = customerPhone.substring(2);
        if (request.chairId() == null || request.date() == null || request.startTime() == null || !validDuration(request.durationMinutes()) || customerName.isBlank()) {
            return ResponseEntity.badRequest().body("Udfyld kundenavn, stol, dato, tidspunkt og en varighed mellem 5 og 480 minutter.");
        }
        if (customerName.length() > 100 || customerEmail.length() > 160 || customerPhone.length() > 20) {
            return ResponseEntity.badRequest().body("Kundeoplysningerne er for lange.");
        }
        if (!sameDay(request.startTime(), request.durationMinutes())) {
            return ResponseEntity.badRequest().body("Bookingen skal slutte samme dag.");
        }
        SalonChair chair = chairs.findById(request.chairId())
                .filter(value -> value.getBusiness().getId().equals(business.getId()))
                .orElse(null);
        if (chair == null) return ResponseEntity.badRequest().body("Den valgte stol findes ikke hos din virksomhed.");
        if (!chairIsAvailable(chair, request.date(), request.startTime(), request.durationMinutes())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Tiden overlapper en eksisterende booking på stolen.");
        }
        Booking booking = new Booking();
        User existingCustomer = customerEmail.isBlank() ? null : users.findByEmail(customerEmail.toLowerCase(Locale.ROOT))
                .filter(user -> user.getRole() != Role.BUSINESS)
                .orElse(null);
        booking.setCustomer(existingCustomer);
        booking.setBarber(business);
        booking.setChair(chair);
        booking.setManualCustomerName(customerName);
        booking.setManualCustomerEmail(customerEmail.isBlank() ? null : customerEmail);
        booking.setManualCustomerPhone(customerPhone.isBlank() ? null : customerPhone);
        booking.setBookingDate(request.date());
        booking.setStartTime(request.startTime());
        booking.setDurationMinutes(request.durationMinutes());
        Booking savedBooking = bookings.save(booking);
        notifications.sendBookingConfirmation(savedBooking);
        return ResponseEntity.status(HttpStatus.CREATED).body(toBusinessDto(savedBooking));
    }

    @DeleteMapping("/bookings/{bookingId}")
    @Transactional
    public ResponseEntity<?> cancel(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long bookingId) {
        User customer = currentUser(principal);
        Booking booking = bookings.findById(bookingId).orElse(null);
        if (booking == null || booking.getCustomer() == null || !booking.getCustomer().getId().equals(customer.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Bookingen blev ikke fundet.");
        }
        cancellations.save(new BookingCancellation());
        bookings.delete(booking);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/account/chairs")
    @Transactional
    public ResponseEntity<?> accountChairs(@AuthenticationPrincipal UserDetails principal) {
        User business = currentUser(principal);
        if (business.getRole() != Role.BUSINESS) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Kun virksomheder kan administrere stole.");
        return ResponseEntity.ok(chairsFor(business).stream().map(chair -> new ChairDTO(chair.getId(), chair.getName())).toList());
    }

    @PostMapping("/account/chairs")
    public ResponseEntity<?> addChair(@AuthenticationPrincipal UserDetails principal, @RequestBody ChairRequest request) {
        User business = currentUser(principal);
        if (business.getRole() != Role.BUSINESS) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Kun virksomheder kan administrere stole.");
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isBlank() || name.length() > 40) return ResponseEntity.badRequest().body("Skriv et stolenavn på højst 40 tegn.");
        SalonChair chair = new SalonChair(); chair.setBusiness(business); chair.setName(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ChairDTO(chairs.save(chair).getId(), name));
    }

    @DeleteMapping("/account/chairs/{chairId}")
    public ResponseEntity<?> removeChair(@AuthenticationPrincipal UserDetails principal, @PathVariable Long chairId) {
        User business = currentUser(principal);
        SalonChair chair = chairs.findById(chairId).filter(value -> value.getBusiness().getId().equals(business.getId())).orElse(null);
        if (business.getRole() != Role.BUSINESS || chair == null) return ResponseEntity.notFound().build();
        if (bookings.existsByChairId(chairId)) return ResponseEntity.status(HttpStatus.CONFLICT).body("Stolen har bookinger og kan ikke fjernes.");
        chairs.delete(chair); return ResponseEntity.noContent().build();
    }

    private User currentUser(UserDetails principal) {
        return users.findByEmail(principal.getUsername()).orElseThrow();
    }

    private BookingDTO toDto(Booking booking) {
        User barber = booking.getBarber();
        String name = barber.getBusinessName() == null || barber.getBusinessName().isBlank()
                ? barber.getFirstName() + " " + barber.getLastName()
                : barber.getBusinessName();
        return new BookingDTO(booking.getId(), name, booking.getChair() == null ? "Stol 1" : booking.getChair().getName(), booking.getBookingDate(), booking.getStartTime(), duration(booking));
    }

    private BusinessBookingDTO toBusinessDto(Booking booking) {
        return new BusinessBookingDTO(
                booking.getId(), customerName(booking), customerEmail(booking), customerPhone(booking),
                booking.getChair() == null ? "Stol 1" : booking.getChair().getName(),
                booking.getBookingDate(), booking.getStartTime(), duration(booking));
    }

    private boolean chairIsAvailable(SalonChair chair, LocalDate date, LocalTime start, int durationMinutes) {
        LocalTime end = start.plusMinutes(durationMinutes);
        return bookings.findByChairIdAndBookingDate(chair.getId(), date).stream()
                .noneMatch(existing -> start.isBefore(endTime(existing)) && existing.getStartTime().isBefore(end));
    }

    private boolean validDuration(Integer durationMinutes) {
        return durationMinutes != null && durationMinutes >= 5 && durationMinutes <= 480;
    }

    private boolean sameDay(LocalTime start, int durationMinutes) {
        return start.toSecondOfDay() + durationMinutes * 60 <= 24 * 60 * 60;
    }

    private LocalTime endTime(Booking booking) { return booking.getStartTime().plusMinutes(duration(booking)); }
    private int duration(Booking booking) { return booking.getDurationMinutes() == null ? 30 : booking.getDurationMinutes(); }
    private String customerName(Booking booking) {
        return booking.getCustomer() == null ? booking.getManualCustomerName() : booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName();
    }
    private String customerEmail(Booking booking) {
        return booking.getCustomer() == null ? booking.getManualCustomerEmail() : booking.getCustomer().getEmail();
    }
    private String customerPhone(Booking booking) {
        return booking.getCustomer() == null ? booking.getManualCustomerPhone()
                : booking.getCustomer().getPhoneNumber() == null ? null : String.valueOf(booking.getCustomer().getPhoneNumber());
    }

    private List<SalonChair> chairsFor(User business) {
        List<SalonChair> result = chairs.findByBusinessIdOrderByIdAsc(business.getId());
        if (!result.isEmpty()) return result;
        SalonChair chair = new SalonChair(); chair.setBusiness(business); chair.setName("Stol 1");
        return List.of(chairs.save(chair));
    }
}

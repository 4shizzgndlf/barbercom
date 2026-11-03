package dk.ek.barbercom.controller;

import dk.ek.barbercom.dto.*;
import dk.ek.barbercom.model.*;
import dk.ek.barbercom.repository.BookingRepository;
import dk.ek.barbercom.repository.BookingCancellationRepository;
import dk.ek.barbercom.repository.UserRepository;
import dk.ek.barbercom.service.BookingNotificationService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
    private final BookingNotificationService notifications;

    public BookingController(UserRepository users, BookingRepository bookings, BookingCancellationRepository cancellations, BookingNotificationService notifications) {
        this.users = users;
        this.bookings = bookings;
        this.cancellations = cancellations;
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
    public List<String> availability(@RequestParam Long barberId, @RequestParam LocalDate date) {
        return bookings.findByBarberIdAndBookingDate(barberId, date).stream()
                .map(booking -> booking.getStartTime().toString()).toList();
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
                .map(booking -> new BusinessBookingDTO(
                        booking.getId(),
                        booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName(),
                        booking.getCustomer().getEmail(),
                        booking.getBookingDate(),
                        booking.getStartTime()))
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
        if (request.barberId() == null || request.date() == null || request.startTime() == null)
            return ResponseEntity.badRequest().body("Vælg barber, dato og tidspunkt.");
        if (request.date().isBefore(LocalDate.now()) || (request.date().isEqual(LocalDate.now()) && request.startTime().isBefore(LocalTime.now())))
            return ResponseEntity.badRequest().body("Tidspunktet er allerede passeret.");
        User barber = users.findById(request.barberId()).filter(user -> user.getRole() == Role.BUSINESS).orElse(null);
        if (barber == null) return ResponseEntity.badRequest().body("Den valgte barber findes ikke.");
        if (bookings.existsByBarberIdAndBookingDateAndStartTime(barber.getId(), request.date(), request.startTime()))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Tiden er netop blevet reserveret. Vælg en anden tid.");
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setBarber(barber);
        booking.setBookingDate(request.date());
        booking.setStartTime(request.startTime());
        Booking savedBooking = bookings.save(booking);
        notifications.sendBookingConfirmation(savedBooking);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(savedBooking));
    }

    @DeleteMapping("/bookings/{bookingId}")
    @Transactional
    public ResponseEntity<?> cancel(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long bookingId) {
        User customer = currentUser(principal);
        Booking booking = bookings.findById(bookingId).orElse(null);
        if (booking == null || !booking.getCustomer().getId().equals(customer.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Bookingen blev ikke fundet.");
        }
        cancellations.save(new BookingCancellation());
        bookings.delete(booking);
        return ResponseEntity.noContent().build();
    }

    private User currentUser(UserDetails principal) {
        return users.findByEmail(principal.getUsername()).orElseThrow();
    }

    private BookingDTO toDto(Booking booking) {
        User barber = booking.getBarber();
        String name = barber.getBusinessName() == null || barber.getBusinessName().isBlank()
                ? barber.getFirstName() + " " + barber.getLastName()
                : barber.getBusinessName();
        return new BookingDTO(booking.getId(), name, booking.getBookingDate(), booking.getStartTime());
    }
}

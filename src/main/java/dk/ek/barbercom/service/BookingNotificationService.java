package dk.ek.barbercom.service;

import dk.ek.barbercom.model.Booking;
import jakarta.mail.internet.InternetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class BookingNotificationService {
    private final JavaMailSender mailSender;
    private final String from;
    private final String brevoApiKey;
    private final String smsSender;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public BookingNotificationService(
            JavaMailSender mailSender,
            @Value("${barbercom.mail.from}") String from,
            @Value("${barbercom.sms.brevo-api-key:}") String brevoApiKey,
            @Value("${barbercom.sms.sender:BookaBarber}") String smsSender) {
        this.mailSender = mailSender;
        this.from = from;
        this.brevoApiKey = brevoApiKey;
        this.smsSender = smsSender;
    }

    public void sendBookingConfirmation(Booking booking) {
        String shopName = businessName(booking);
        String chairName = booking.getChair() == null ? "Stol 1" : booking.getChair().getName();
        String email = customerEmail(booking);
        if (email != null && !email.isBlank()) {
            try {
                MimeMessageHelper message = new MimeMessageHelper(mailSender.createMimeMessage(), false, StandardCharsets.UTF_8.name());
                message.setFrom(new InternetAddress(from, "BookaBarber", StandardCharsets.UTF_8.name()));
                message.setTo(email);
                message.setSubject("Din booking hos BookaBarber er bekræftet");
                message.setText("Hej " + customerName(booking) + ",\n\n"
                        + "Din tid hos " + shopName + " ved " + chairName
                        + " er bekræftet den " + booking.getBookingDate() + " kl. " + booking.getStartTime()
                        + " i " + duration(booking) + " minutter.\n\n"
                        + "Vi glæder os til at se dig.\nBookaBarber");
                mailSender.send(message.getMimeMessage());
            } catch (Exception exception) {
                // A notification outage must never cancel a confirmed database booking.
                System.err.println("Kunne ikke sende bookingmail: " + exception.getMessage());
            }
        }
        sendBookingSms(booking);
    }

    private void sendBookingSms(Booking booking) {
        if (brevoApiKey.isBlank()) return;
        String phone = customerPhone(booking);
        if (phone == null) return;
        if (!phone.matches("\\d{8}")) {
            System.err.println("Kunne ikke sende bookingsms: kunden mangler et gyldigt dansk 8-cifret telefonnummer.");
            return;
        }

        String content = "BookaBarber: Din tid hos " + businessName(booking) + " ved "
                + (booking.getChair() == null ? "Stol 1" : booking.getChair().getName()) + " er bekræftet den "
                + booking.getBookingDate() + " kl. " + booking.getStartTime() + " i " + duration(booking) + " min.";
        String body = "{\"type\":\"transactional\",\"unicodeEnabled\":true,\"sender\":\""
                + json(smsSender) + "\",\"recipient\":\"+45" + phone
                + "\",\"content\":\"" + json(content) + "\"}";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/transactionalSMS/send"))
                    .header("api-key", brevoApiKey)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) System.err.println("Kunne ikke sende bookingsms (Brevo status " + response.statusCode() + ").");
        } catch (Exception exception) {
            // The confirmed booking must remain valid if the SMS provider is unavailable.
            System.err.println("Kunne ikke sende bookingsms: " + exception.getMessage());
        }
    }

    private String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String businessName(Booking booking) {
        String name = booking.getBarber().getBusinessName();
        return name == null || name.isBlank() ? booking.getBarber().getFirstName() + " " + booking.getBarber().getLastName() : name;
    }

    private String customerName(Booking booking) {
        return booking.getCustomer() == null ? booking.getManualCustomerName() : booking.getCustomer().getFirstName();
    }

    private String customerEmail(Booking booking) {
        return booking.getCustomer() == null ? booking.getManualCustomerEmail() : booking.getCustomer().getEmail();
    }

    private String customerPhone(Booking booking) {
        return booking.getCustomer() == null ? booking.getManualCustomerPhone()
                : booking.getCustomer().getPhoneNumber() == null ? null : String.valueOf(booking.getCustomer().getPhoneNumber());
    }

    private int duration(Booking booking) {
        return booking.getDurationMinutes() == null ? 30 : booking.getDurationMinutes();
    }
}

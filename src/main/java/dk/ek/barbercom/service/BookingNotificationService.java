package dk.ek.barbercom.service;

import dk.ek.barbercom.model.Booking;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
            @Value("${barbercom.sms.sender:BarberCom}") String smsSender) {
        this.mailSender = mailSender;
        this.from = from;
        this.brevoApiKey = brevoApiKey;
        this.smsSender = smsSender;
    }

    public void sendBookingConfirmation(Booking booking) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(booking.getCustomer().getEmail());
            message.setSubject("Din booking hos Barber.com er bekræftet");
            message.setText("Hej " + booking.getCustomer().getFirstName() + ",\n\n"
                    + "Din tid hos " + booking.getBarber().getFirstName() + " " + booking.getBarber().getLastName()
                    + " er bekræftet den " + booking.getBookingDate() + " kl. " + booking.getStartTime() + ".\n\n"
                    + "Vi glæder os til at se dig.\nBarber.com");
            mailSender.send(message);
        } catch (Exception exception) {
            // A notification outage must never cancel a confirmed database booking.
            System.err.println("Kunne ikke sende bookingmail: " + exception.getMessage());
        }
        sendBookingSms(booking);
    }

    private void sendBookingSms(Booking booking) {
        if (brevoApiKey.isBlank() || booking.getCustomer().getPhoneNumber() == null) return;
        String phone = String.valueOf(booking.getCustomer().getPhoneNumber());
        if (!phone.matches("\\d{8}")) {
            System.err.println("Kunne ikke sende bookingsms: kunden mangler et gyldigt dansk 8-cifret telefonnummer.");
            return;
        }

        String content = "Barber.com: Din tid hos " + booking.getBarber().getFirstName() + " "
                + booking.getBarber().getLastName() + " er bekræftet den "
                + booking.getBookingDate() + " kl. " + booking.getStartTime() + ".";
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
}

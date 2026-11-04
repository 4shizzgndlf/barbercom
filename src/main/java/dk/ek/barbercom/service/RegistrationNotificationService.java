package dk.ek.barbercom.service;

import dk.ek.barbercom.dto.UserDTO;
import dk.ek.barbercom.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class RegistrationNotificationService {
    private final JavaMailSender mailSender;
    private final String from;

    public RegistrationNotificationService(JavaMailSender mailSender,
            @Value("${barbercom.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendPrivateAccountConfirmation(UserDTO user) {
        send(user.email(), "Velkommen til Barber.com", "Hej " + user.firstName() + ",\n\n"
                + "Din gratis private konto hos Barber.com er nu oprettet. Du kan logge ind, booke tider og handle produkter.\n\n"
                + "Velkommen — Barber.com");
    }

    /** Returns true only when the mail provider accepted the message. */
    public boolean sendBusinessSubscriptionConfirmation(User user) {
        return send(user.getEmail(), "Dit Barber.com business-abonnement er bekræftet", "Hej " + user.getFirstName() + ",\n\n"
                + "Tak for din betaling. Din business-konto og dit abonnement hos Barber.com er nu aktivt.\n\n"
                + "Abonnement: 199,00 DKK pr. måned\n"
                + "Virksomhed: " + user.getBusinessName() + "\n\n"
                + "Du kan nu logge ind og administrere din barbershop.\n\nBarber.com");
    }

    private boolean send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            return true;
        } catch (Exception exception) {
            System.err.println("Kunne ikke sende kontobekræftelse: " + exception.getMessage());
            return false;
        }
    }
}

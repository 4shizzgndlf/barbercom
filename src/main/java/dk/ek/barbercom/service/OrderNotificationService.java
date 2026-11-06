package dk.ek.barbercom.service;

import dk.ek.barbercom.model.CustomerOrder;
import dk.ek.barbercom.model.OrderItem;
import jakarta.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class OrderNotificationService {
    private final JavaMailSender mailSender;
    private final String from;

    public OrderNotificationService(JavaMailSender mailSender,
            @Value("${barbercom.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /** Returns true only when Brevo/SMTP accepted the confirmation email. */
    public boolean sendOrderConfirmation(CustomerOrder order) {
        try {
            MimeMessageHelper message = new MimeMessageHelper(mailSender.createMimeMessage(), false, StandardCharsets.UTF_8.name());
            message.setFrom(new InternetAddress(from, "BookaBarber", StandardCharsets.UTF_8.name()));
            message.setTo(order.getCustomer().getEmail());
            message.setSubject("Din ordre hos BookaBarber er bekræftet");
            message.setText(buildMessage(order));
            mailSender.send(message.getMimeMessage());
            return true;
        } catch (Exception exception) {
            // A mail outage must never invalidate a completed Stripe payment.
            System.err.println("Kunne ikke sende ordrebekræftelse: " + exception.getMessage());
            return false;
        }
    }

    private String buildMessage(CustomerOrder order) {
        StringBuilder items = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            items.append("- ").append(item.getQuantity()).append(" × ")
                    .append(item.getProductName()).append(" · ")
                    .append(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()))
                            .setScale(2).toPlainString().replace('.', ','))
                    .append(" DKK\n");
        }
        return "Hej " + order.getCustomer().getFirstName() + ",\n\n"
                + "Tak for din handel hos BookaBarber. Din betaling er gennemført, og din ordre er bekræftet.\n\n"
                + "Ordre #" + order.getId() + "\n" + items
                + "\nI alt: " + order.getTotalAmount().setScale(2).toPlainString().replace('.', ',') + " DKK\n\n"
                + "Du kan altid se ordren på din konto.\n\n"
                + "Venlig hilsen\nBookaBarber";
    }
}

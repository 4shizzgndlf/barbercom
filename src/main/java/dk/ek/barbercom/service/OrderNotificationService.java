package dk.ek.barbercom.service;

import dk.ek.barbercom.model.CustomerOrder;
import dk.ek.barbercom.model.OrderItem;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(order.getCustomer().getEmail());
            message.setSubject("Din ordre hos Barber.com er bekræftet");
            message.setText(buildMessage(order));
            mailSender.send(message);
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
                + "Tak for din handel hos Barber.com. Din betaling er gennemført, og din ordre er bekræftet.\n\n"
                + "Ordre #" + order.getId() + "\n" + items
                + "\nI alt: " + order.getTotalAmount().setScale(2).toPlainString().replace('.', ',') + " DKK\n\n"
                + "Du kan altid se ordren på din konto.\n\n"
                + "Venlig hilsen\nBarber.com";
    }
}

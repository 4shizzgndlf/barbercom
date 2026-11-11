package dk.ek.barbercom.config;

import dk.ek.barbercom.model.*;
import dk.ek.barbercom.repository.*;

import java.util.*;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookingChairMigration {
    private final UserRepository users;
    private final SalonChairRepository chairs;
    private final BookingRepository bookings;
    private final JdbcTemplate jdbc;

    public BookingChairMigration(UserRepository users, SalonChairRepository chairs, BookingRepository bookings, JdbcTemplate jdbc) {
        this.users = users;
        this.chairs = chairs;
        this.bookings = bookings;
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrate() {
        boolean hasDurationColumn = jdbc.queryForList("SHOW COLUMNS FROM `booking`").stream()
                .anyMatch(row -> "duration_minutes".equals(String.valueOf(row.get("Field"))));
        if (!hasDurationColumn) {
            jdbc.execute("ALTER TABLE `booking` ADD COLUMN duration_minutes INT NOT NULL DEFAULT 30");
        }
        jdbc.execute("UPDATE `booking` SET duration_minutes = 30 WHERE duration_minutes IS NULL OR duration_minutes < 5");

        boolean customerCanBeEmpty = jdbc.queryForList("SHOW COLUMNS FROM `booking`").stream()
                .filter(row -> "customer_id".equals(String.valueOf(row.get("Field"))))
                .anyMatch(row -> "YES".equalsIgnoreCase(String.valueOf(row.get("Null"))));
        if (!customerCanBeEmpty) {
            // A business can add an external walk-in customer who does not
            // have a BookaBarber account, so customer_id must be nullable.
            jdbc.execute("ALTER TABLE `booking` MODIFY COLUMN customer_id BIGINT NULL");
        }

        Map<Long, SalonChair> defaults = new HashMap<>();
        for (User business : users.findByRoleOrderByIdAsc(Role.BUSINESS)) {
            SalonChair chair = chairs.findByBusinessIdOrderByIdAsc(business.getId()).stream().findFirst().orElseGet(() -> {
                SalonChair value = new SalonChair();
                value.setBusiness(business);
                value.setName("Stol 1");
                return chairs.save(value);
            });
            defaults.put(business.getId(), chair);
        }
        for (Booking booking : bookings.findAll())
            if (booking.getChair() == null) booking.setChair(defaults.get(booking.getBarber().getId()));
        bookings.flush();
        // MySQL needs an index for the existing barber_id foreign key before
        // the old composite booking index can safely be removed. Older MySQL
        // versions do not support CREATE INDEX IF NOT EXISTS, so inspect the
        // table first instead.
        boolean hasBarberIndex = jdbc.queryForList("SHOW INDEX FROM `booking`").stream()
                .anyMatch(row -> "idx_booking_barber_id".equals(String.valueOf(row.get("Key_name"))));
        if (!hasBarberIndex) jdbc.execute("CREATE INDEX idx_booking_barber_id ON `booking` (barber_id)");
        boolean hasChairIndex = jdbc.queryForList("SHOW INDEX FROM `booking`").stream()
                .anyMatch(row -> "idx_booking_chair_id".equals(String.valueOf(row.get("Key_name"))));
        if (!hasChairIndex) jdbc.execute("CREATE INDEX idx_booking_chair_id ON `booking` (chair_id)");
        Map<String, List<String>> indexes = new HashMap<>();
        for (Map<String, Object> row : jdbc.queryForList("SHOW INDEX FROM `booking`"))
            indexes.computeIfAbsent(String.valueOf(row.get("Key_name")), ignored -> new ArrayList<>()).add(String.valueOf(row.get("Column_name")));
        indexes.forEach((name, columns) -> {
            if (!"PRIMARY".equals(name) && columns.containsAll(List.of("chair_id", "booking_date", "start_time")) && name.matches("[A-Za-z0-9_]+"))
                jdbc.execute("ALTER TABLE `booking` DROP INDEX `" + name + "`");
        });
    }
}

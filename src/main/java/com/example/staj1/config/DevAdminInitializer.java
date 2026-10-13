package com.example.staj1.config;

import com.example.staj1.model.User;
import com.example.staj1.model.UserRole;
import com.example.staj1.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.dev-admin.enabled", havingValue = "true")
public class DevAdminInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;
    private final String email;
    private final String password;

    public DevAdminInitializer(UserRepository users, PasswordEncoder encoder, JdbcTemplate jdbc,
            @Value("${app.dev-admin.email:}") String email,
            @Value("${app.dev-admin.password:}") String password) {
        this.users = users; this.encoder = encoder; this.jdbc = jdbc;
        this.email = email.trim(); this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!"cargo_automation_dev".equals(jdbc.queryForObject("SELECT current_database()", String.class))) {
            throw new IllegalStateException("Geliştirme yöneticisi yalnız cargo_automation_dev veritabanında oluşturulabilir.");
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || password.length() < 12 || password.length() > 72 ||
                password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Yerel yönetici için geçerli e-posta ve 12-72 bayt arasında şifre tanımlayın.");
        }
        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            if (!UserRole.ADMIN.name().equals(existing.get().getRole())) {
                throw new IllegalStateException("Bu e-posta başka bir role ait. Yönetici için farklı bir e-posta kullanın.");
            }
            // Mevcut hesabın şifresini, rolünü veya aktifliğini değiştirme.
            return;
        }
        User admin = new User();
        admin.setName("Geliştirme"); admin.setSurname("Yöneticisi");
        admin.setEmail(email); admin.setAge(18);
        admin.setPassword(encoder.encode(password));
        admin.setRole(UserRole.ADMIN.name()); admin.setActive(true);
        users.save(admin);
    }
}

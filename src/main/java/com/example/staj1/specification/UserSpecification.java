package com.example.staj1.specification;

import com.example.staj1.model.User;
import com.example.staj1.model.UserRole;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

public class UserSpecification {
    private static final Set<String> TEXT_FIELDS = Set.of("name", "surname", "email", "tc", "telNo");
    private static final Set<String> ALLOWED_FIELDS = Set.of("id", "age", "name", "surname", "email", "tc", "telNo", "role", "active");

    public static Specification<User> filter(Map<String, String> filters) {
        for (var entry : filters.entrySet()) {
            String field = entry.getKey(); String value = entry.getValue();
            if (!ALLOWED_FIELDS.contains(field)) throw new IllegalArgumentException("Desteklenmeyen arama alanı: " + field);
            if (value == null || value.isBlank() || value.length() > 100) throw new IllegalArgumentException("Arama değeri boş olamaz ve 100 karakteri geçemez.");
            if (field.equals("id") || field.equals("age")) {
                try { if (Integer.parseInt(value) <= 0) throw new NumberFormatException(); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException(field + " pozitif tam sayı olmalıdır."); }
            }
            if (field.equals("role")) {
                try { UserRole.valueOf(value); }
                catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Rol USER, STAFF veya ADMIN olmalıdır."); }
            }
            if (field.equals("active") && !value.equals("true") && !value.equals("false")) {
                throw new IllegalArgumentException("Aktiflik true veya false olmalıdır.");
            }
        }
        Map<String, String> validated = Map.copyOf(filters);
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (var entry : validated.entrySet()) {
                String field = entry.getKey(); String value = entry.getValue();
                if (TEXT_FIELDS.contains(field)) {
                    // % ve _ kullanıcı girdisinde joker değil, normal karakterdir.
                    String escaped = value.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                    predicates.add(builder.like(builder.lower(root.get(field)), "%" + escaped + "%", '\\'));
                } else if (field.equals("active")) {
                    predicates.add(builder.equal(root.get(field), Boolean.parseBoolean(value)));
                } else if (field.equals("role")) {
                    predicates.add(builder.equal(root.get(field), value));
                } else {
                    predicates.add(builder.equal(root.get(field), Integer.parseInt(value)));
                }
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

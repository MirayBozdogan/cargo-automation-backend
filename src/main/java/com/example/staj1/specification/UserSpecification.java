package com.example.staj1.specification;

import com.example.staj1.model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserSpecification {

    public static Specification<User> filter(Map<String, String> filters) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            for (Map.Entry<String, String> filter : filters.entrySet()) {

                String field = filter.getKey();
                String value = filter.getValue();

                Class<?> fieldType = root.get(field).getJavaType();

                if (fieldType == String.class) {

                    predicates.add(
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get(field)),
                                    "%" + value.toLowerCase() + "%"
                            )
                    );

                } else {

                    predicates.add(
                            criteriaBuilder.equal(
                                    root.get(field),
                                    Integer.valueOf(value)
                            )
                    );
                }
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}
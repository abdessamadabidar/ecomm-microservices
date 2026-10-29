package com.ecomm.order_service.configuration;

import com.ecomm.order_service.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Hibernate's schema update does not widen an existing enum check constraint. */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusConstraintMigration implements ApplicationRunner {
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String definition = null;
            try (PreparedStatement query = connection.prepareStatement(
                    "select pg_get_constraintdef(oid) from pg_constraint " +
                    "where conrelid = 'orders'::regclass and conname = 'orders_status_check'")) {
                try (ResultSet result = query.executeQuery()) {
                    if (result.next()) {
                        definition = result.getString(1);
                    }
                }
            }

            String currentDefinition = definition;
            if (currentDefinition != null && Arrays.stream(OrderStatus.values())
                    .allMatch(status -> definitionContains(currentDefinition, status))) {
                return;
            }

            String allowed = Arrays.stream(OrderStatus.values())
                    .map(status -> "'" + status.name() + "'")
                    .collect(Collectors.joining(", "));
            try (Statement statement = connection.createStatement()) {
                statement.execute("alter table orders " +
                        "drop constraint if exists orders_status_check, " +
                        "add constraint orders_status_check check (status in (" + allowed + "))");
            }
            log.info("Updated orders_status_check for order statuses: {}", allowed);
        }
    }

    private static boolean definitionContains(String definition, OrderStatus status) {
        return definition.contains("'" + status.name() + "'");
    }
}

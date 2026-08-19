package com.anytypeview.infra.config;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    @Bean
    DataSource dataSource(
        @Value("${spring.datasource.url}") String jdbcUrl,
        @Value("${spring.datasource.driver-class-name}") String driverClassName
    ) throws Exception {
        createSqliteParentDirectory(jdbcUrl);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setMaximumPoolSize(1);
        return dataSource;
    }

    private void createSqliteParentDirectory(String jdbcUrl) throws Exception {
        String prefix = "jdbc:sqlite:";
        if (!jdbcUrl.startsWith(prefix)) {
            return;
        }

        String databasePath = jdbcUrl.substring(prefix.length());
        if (databasePath.isBlank() || databasePath.equals(":memory:")) {
            return;
        }

        int queryStart = databasePath.indexOf('?');
        if (queryStart >= 0) {
            databasePath = databasePath.substring(0, queryStart);
        }

        Path parent = Path.of(databasePath).toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}

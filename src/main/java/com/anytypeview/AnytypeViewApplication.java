package com.anytypeview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnytypeViewApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnytypeViewApplication.class, args);
    }
}

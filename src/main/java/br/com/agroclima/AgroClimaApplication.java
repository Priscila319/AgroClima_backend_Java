package br.com.agroclima;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgroClimaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgroClimaApplication.class, args);
    }
}

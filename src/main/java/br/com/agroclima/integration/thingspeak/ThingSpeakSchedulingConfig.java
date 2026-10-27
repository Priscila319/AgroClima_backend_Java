package br.com.agroclima.integration.thingspeak;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga o @Scheduled somente quando a integração ThingSpeak está habilitada. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.thingspeak", name = "enabled", havingValue = "true")
public class ThingSpeakSchedulingConfig {
}

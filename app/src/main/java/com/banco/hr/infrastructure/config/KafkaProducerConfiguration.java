package com.banco.hr.infrastructure.config;

import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfiguration {

    @Bean
    public ProducerFactory<String, String> employeeEventProducerFactory(
            KafkaProperties kafkaProperties, SslBundles sslBundles) {
        return new DefaultKafkaProducerFactory<>(
                kafkaProperties.buildProducerProperties(sslBundles),
                new StringSerializer(),
                new StringSerializer());
    }

    @Bean
    public KafkaTemplate<String, String> employeeEventKafkaTemplate(
            ProducerFactory<String, String> employeeEventProducerFactory) {
        return new KafkaTemplate<>(employeeEventProducerFactory);
    }
}

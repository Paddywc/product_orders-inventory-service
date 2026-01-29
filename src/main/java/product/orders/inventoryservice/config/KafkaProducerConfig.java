package product.orders.inventoryservice.config;

import jakarta.persistence.EntityManagerFactory;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    /**
     * The number of partitions for Kafka topics
     */
    private static final int N_PARTITIONS = 3;

    /**
     * The number of replicas for Kafka topics
     */
    private static final int N_REPLICAS = 1;

    /**
     * Configured Kafka properties via application.properties or the YAML
     */
    private final KafkaProperties kafkaProperties;

    /**
     * The name of the Kafka topic as per the settings
     */
    private final String topicName;

    public KafkaProducerConfig(KafkaProperties kafkaProperties,
                               @Value("#{@kafkaTopicsProperties.inventoryEvents}") String topicName) {
        this.kafkaProperties = kafkaProperties;
        this.topicName = topicName;
    }


    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProperties.getBootstrapServers()
        );
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // Many events are created in a loop of all products in an order. Send them in a single batch
        config.put(ProducerConfig.LINGER_MS_CONFIG, 200);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    NewTopic createTopic() {
        return TopicBuilder.name(topicName)
                .partitions(N_PARTITIONS)
                .replicas(N_REPLICAS)
                .build();
    }

    @Bean
    KafkaTransactionManager<String, Object> kafkaTransactionManager(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }

    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory){
        return new JpaTransactionManager(entityManagerFactory);
    }
}

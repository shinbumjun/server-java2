package kr.hhplus.be.server.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Properties;

@TestConfiguration
public class KafkaTestContainersConfig {

    private static final KafkaContainer KAFKA_CONTAINER;

    static {
        KAFKA_CONTAINER = new KafkaContainer(
                DockerImageName.parse("apache/kafka-native:3.8.0")
        )
        // 필요하다면 포트 바인딩도 추가 가능
        //.withExposedPorts(9092)
        ;
        KAFKA_CONTAINER.start();
    }

    /**
     * 테스트 중에 KafkaTemplate 등에서 참조할 부트스트랩 서버 주소 빈
     */
    @Bean
    public String kafkaBootstrapServers() {
        return KAFKA_CONTAINER.getBootstrapServers();
    }

    /**
     * 테스트에서 AdminClient를 쓰고 싶을 때 주입할 빈
     */
    @Bean
    public AdminClient kafkaAdminClient() {
        Properties props = new Properties();
        props.put(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA_CONTAINER.getBootstrapServers()
        );
        return AdminClient.create(props);
    }
}

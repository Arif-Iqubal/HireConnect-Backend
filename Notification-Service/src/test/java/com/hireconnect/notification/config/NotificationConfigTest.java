package com.hireconnect.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationConfigTest {

    @Test
    void openApiShouldExposeNotificationServiceMetadataAndBearerAuth() {
        OpenAPI openAPI = new OpenApiConfig().openAPI();

        assertThat(openAPI.getInfo().getTitle()).contains("Notification Service");
        assertThat(openAPI.getServers()).extracting("url")
                .containsExactly("http://localhost:8080", "http://localhost:8086");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getSecurity()).hasSize(1);
    }

    @Test
    void swaggerCustomizerShouldAddExamplesForKnownParametersOnly() {
        Operation operation = new Operation().parameters(List.of(
                new Parameter().name("notificationId"),
                new Parameter().name("unreadOnly"),
                new Parameter().name("unknown"),
                new Parameter().name("page").example(4)
        ));

        new SwaggerExampleConfig().exampleParameterCustomizer().customize(operation, null);

        assertThat(operation.getParameters().get(0).getExample()).isEqualTo(901L);
        assertThat(operation.getParameters().get(1).getExample()).isEqualTo(true);
        assertThat(operation.getParameters().get(2).getExample()).isNull();
        assertThat(operation.getParameters().get(3).getExample()).isEqualTo(4);
    }

    @Test
    void rabbitMqBeansShouldUseConfiguredExchangeQueueAndRoutingKey() {
        RabbitMQConfig config = new RabbitMQConfig();
        ReflectionTestUtils.setField(config, "exchange", "hireconnect.exchange");
        ReflectionTestUtils.setField(config, "notificationRoutingKey", "notification.routing.key");

        assertThat(config.hireConnectExchange().getName()).isEqualTo("hireconnect.exchange");
        assertThat(config.notificationQueue().getName()).isEqualTo("notification.queue");
        assertThat(config.notificationBinding().getRoutingKey()).isEqualTo("notification.routing.key");
        assertThat(config.messageConverter()).isNotNull();
    }

    @Test
    void httpClientConfigShouldCreateRestTemplate() {
        assertThat(new HttpClientConfig().restTemplate(new RestTemplateBuilder())).isNotNull();
    }
}

package com.hireconnect.job.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JobConfigTest {

    @Test
    void openApiConfiguresJobServiceMetadataAndBearerAuth() {
        var openAPI = new OpenApiConfig().openAPI();

        assertThat(openAPI.getInfo().getTitle()).contains("Job Service");
        assertThat(openAPI.getServers()).hasSize(2);
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getSecurity()).isNotEmpty();
    }

    @Test
    void rabbitMqConfigBuildsQueuesBindingsAndTemplate() {
        RabbitMQConfig config = new RabbitMQConfig();
        ReflectionTestUtils.setField(config, "exchange", "hireconnect.exchange");
        ReflectionTestUtils.setField(config, "analyticsRoutingKey", "analytics.routing.key");
        ReflectionTestUtils.setField(config, "notificationRoutingKey", "notification.routing.key");

        TopicExchange exchange = config.hireConnectExchange();
        Queue analyticsQueue = config.analyticsQueue();
        Queue notificationQueue = config.notificationQueue();
        Binding analyticsBinding = config.analyticsBinding();
        Binding notificationBinding = config.notificationBinding();

        assertThat(exchange.getName()).isEqualTo("hireconnect.exchange");
        assertThat(analyticsQueue.getName()).isEqualTo("analytics.queue");
        assertThat(notificationQueue.getName()).isEqualTo("notification.queue");
        assertThat(analyticsBinding.getRoutingKey()).isEqualTo("analytics.routing.key");
        assertThat(notificationBinding.getRoutingKey()).isEqualTo("notification.routing.key");
        assertThat(config.messageConverter()).isNotNull();
        assertThat(config.rabbitTemplate(mock(ConnectionFactory.class)).getMessageConverter()).isNotNull();
    }

    @Test
    void swaggerExampleCustomizerAddsExamplesOnlyWhenMissing() {
        SwaggerExampleConfig config = new SwaggerExampleConfig();
        Operation operation = new Operation().parameters(List.of(
                new Parameter().name("jobId"),
                new Parameter().name("unknown"),
                new Parameter().name("status").example("CLOSED")
        ));

        Operation customized = config.exampleParameterCustomizer().customize(operation, null);

        assertThat(customized.getParameters().get(0).getExample()).isEqualTo(101L);
        assertThat(customized.getParameters().get(1).getExample()).isNull();
        assertThat(customized.getParameters().get(2).getExample()).isEqualTo("CLOSED");
    }

    @Test
    void swaggerExampleCustomizerAllowsOperationsWithoutParameters() {
        Operation operation = new Operation();

        assertThat(new SwaggerExampleConfig().exampleParameterCustomizer().customize(operation, null))
                .isSameAs(operation);
    }
}

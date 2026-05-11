package com.hireconnect.interview.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewConfigTest {

    @Test
    void openApiShouldExposeServiceMetadataServersAndBearerSecurity() {
        OpenAPI openAPI = new OpenApiConfig().openAPI();

        assertThat(openAPI.getInfo().getTitle()).contains("Interview Service");
        assertThat(openAPI.getServers()).extracting("url")
                .containsExactly("http://localhost:8080", "http://localhost:8085");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getSecurity()).hasSize(1);
    }

    @Test
    void swaggerCustomizerShouldPopulateKnownParameterExamplesOnlyWhenMissing() {
        Operation operation = new Operation().parameters(List.of(
                new Parameter().name("interviewId"),
                new Parameter().name("status"),
                new Parameter().name("unknown"),
                new Parameter().name("page").example(3)
        ));

        new SwaggerExampleConfig().exampleParameterCustomizer().customize(operation, null);

        assertThat(operation.getParameters().get(0).getExample()).isEqualTo(701L);
        assertThat(operation.getParameters().get(1).getExample()).isEqualTo("SCHEDULED");
        assertThat(operation.getParameters().get(2).getExample()).isNull();
        assertThat(operation.getParameters().get(3).getExample()).isEqualTo(3);
    }
}

package org.ecom.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ecom.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ecom_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("app.jwt.secret", () -> "integration-secret-32-bytes-minimum!!!");
        registry.add("app.jwt.previous-secrets", () -> "");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @BeforeEach
    void cleanSessions() {
        userSessionRepository.deleteAll();
    }

    @Test
    void loginRefreshLogoutFlowWorks() throws Exception {
        String username = "user_" + System.currentTimeMillis();
        String password = "Password1!";

        String registerRequest = """
                {
                  "username":"%s",
                  "email":"%s@example.com",
                  "password":"%s",
                  "confirmPassword":"%s"
                }
                """.formatted(username, username, password, password);

        mockMvc.perform(post("/user/create")
                        .contentType(APPLICATION_JSON)
                        .content(registerRequest))
                .andExpect(status().isOk());

        String loginRequest = """
                {
                  "username":"%s",
                  "password":"%s"
                }
                """.formatted(username, password);

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String firstRefreshToken = loginJson.path("data").path("refreshToken").asText();
        assertNotNull(firstRefreshToken);

        String refreshRequest = """
                {
                  "refreshToken":"%s"
                }
                """.formatted(firstRefreshToken);

        String refreshResponse = mockMvc.perform(post("/auth/refresh_token")
                        .contentType(APPLICATION_JSON)
                        .content(refreshRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode refreshJson = objectMapper.readTree(refreshResponse);
        String secondRefreshToken = refreshJson.path("data").path("refreshToken").asText();
        assertNotNull(secondRefreshToken);
        assertNotEquals(firstRefreshToken, secondRefreshToken);

        String logoutRequest = """
                {
                  "refreshToken":"%s"
                }
                """.formatted(secondRefreshToken);

        mockMvc.perform(post("/auth/logout")
                        .contentType(APPLICATION_JSON)
                        .content(logoutRequest))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh_token")
                        .contentType(APPLICATION_JSON)
                        .content(logoutRequest))
                .andExpect(status().isBadRequest());
    }
}

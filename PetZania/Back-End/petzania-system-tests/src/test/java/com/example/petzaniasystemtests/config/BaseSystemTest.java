package com.example.petzaniasystemtests.config;

import com.example.petzaniasystemtests.builders.TestDataBuilder;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;

import static io.restassured.RestAssured.given;

@Testcontainers
public class BaseSystemTest {
    protected static Network network = Network.newNetwork();
    protected static final String PASSWORD = "Password123!";


    @Container
    protected static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("postgres")
            .withUsername("postgres")
            .withPassword("admin")
            .withCommand("postgres", "-c", "max_connections=200")
            .withInitScript("init-test-db.sql")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
    @Container
    protected static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management-alpine")
            .withNetwork(network)
            .withNetworkAliases("rabbitmq")
            .withExposedPorts(5672, 15672)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
    @Container
    protected static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(1)));

    @Container
    protected static GenericContainer<?> registrationService = new GenericContainer<>(
            DockerImageName.parse("registration-module:latest"))
            .withNetwork(network)
            .withNetworkAliases("registration-service")
            .withExposedPorts(8080)
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/postgres")
            .withEnv("SPRING_DATASOURCE_USERNAME", "postgres")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "admin")
            .withEnv("SPRING_RABBITMQ_HOST", "rabbitmq")
            .withEnv("SPRING_RABBITMQ_PORT", "5672")
            .withEnv("SPRING_DATA_REDIS_HOST", "redis")
            .withEnv("SPRING_DATA_REDIS_PORT", "6379")
            .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "create-drop")
            .dependsOn(postgres, rabbitmq, redis)
            .withLogConsumer(outputFrame -> {
                System.err.println("REGISTRATION: " + outputFrame.getUtf8String());
            })
            .waitingFor(Wait.forLogMessage(".*Started RegistrationModuleApplication.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @Container
    protected static GenericContainer<?> friendsService = new GenericContainer<>(
            DockerImageName.parse("friends-module:latest"))
            .withNetwork(network)
            .withNetworkAliases("friends-service")
            .withExposedPorts(8081)
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/friends_chat")
            .withEnv("SPRING_DATASOURCE_USERNAME", "postgres")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "admin")
            .withEnv("SPRING_RABBITMQ_HOST", "rabbitmq")
            .withEnv("SPRING_RABBITMQ_PORT", "5672")
            .withEnv("SPRING_DATA_REDIS_HOST", "redis")
            .withEnv("SPRING_DATA_REDIS_PORT", "6379")
            .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "create-drop")
            .dependsOn(postgres, rabbitmq, redis, registrationService)
            .withLogConsumer(outputFrame -> {
                System.err.println("FriendsAndChats: " + outputFrame.getUtf8String());
            })
            .waitingFor(Wait.forLogMessage(".*Started FriendsAndChatsModuleApplication.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @Container
    protected static GenericContainer<?> adoptionService = new GenericContainer<>(
            "adoption-and-breeding-module:latest")
            .withNetwork(network)
            .withNetworkAliases("adoption-service")
            .withExposedPorts(8082)
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/adoption_breeding")
            .withEnv("SPRING_DATASOURCE_USERNAME", "postgres")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "admin")
            .withEnv("SPRING_RABBITMQ_HOST", "rabbitmq")
            .withEnv("SPRING_RABBITMQ_PORT", "5672")
            .withEnv("SPRING_DATA_REDIS_HOST", "redis")
            .withEnv("SPRING_DATA_REDIS_PORT", "6379")
            .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "create-drop")
            .dependsOn(postgres, rabbitmq, redis, registrationService)
            .withLogConsumer(outputFrame -> {
                System.err.println("ADOPTION: " + outputFrame.getUtf8String());
            })
            .waitingFor(Wait.forLogMessage(".*Started AdoptionAndBreedingModule.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @Container
    protected static GenericContainer<?> notificationService = new GenericContainer<>(
            "notification-module:latest")
            .withNetwork(network)
            .withNetworkAliases("notification-service")
            .withExposedPorts(8083)
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/notifications")
            .withEnv("SPRING_DATASOURCE_USERNAME", "postgres")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "admin")
            .withEnv("SPRING_RABBITMQ_HOST", "rabbitmq")
            .withEnv("SPRING_RABBITMQ_PORT", "5672")
            .withEnv("SPRING_DATA_REDIS_HOST", "redis")
            .withEnv("SPRING_DATA_REDIS_PORT", "6379")
            .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "create-drop")
            .dependsOn(postgres, rabbitmq, redis, registrationService)
            .withLogConsumer(outputFrame -> {
                System.err.println("NOTIFICATION: " + outputFrame.getUtf8String());
            })
            .waitingFor(Wait.forLogMessage(".*Started NotificationModule.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(5)));


    protected static String registrationBaseUrl;
    protected static String friendsBaseUrl;
    protected static String adoptionBaseUrl;
    protected static String notificationBaseUrl;
    protected static String wsUrl;

    @BeforeAll
    static void setUp() {
        registrationBaseUrl = String.format("http://localhost:%d",
                registrationService.getMappedPort(8080));
        friendsBaseUrl = String.format("http://localhost:%d",
                friendsService.getMappedPort(8081));
        adoptionBaseUrl = String.format("http://localhost:%d",
                adoptionService.getMappedPort(8082));
        notificationBaseUrl = String.format("http://localhost:%d",
                notificationService.getMappedPort(8083));


        wsUrl = String.format("ws://localhost:%d/ws",
                friendsService.getMappedPort(8081));

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void setUpEach() {
        // Reset rate limiters between tests if needed
    }

    protected RequestSpecification getAuthenticatedSpec(String token) {
        return new RequestSpecBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .build();
    }

    protected Response registerAndLoginUser(String username, String email) throws Exception {
        // Register
        given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createRegisterUserJson(username, email, PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/signup")
                .then()
                .statusCode(201);

        // Get OTP from database
        String otp = getOtpFromDatabase(email);

        // Verify OTP
        given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createOtpValidationJson(email, otp))
                .when()
                .put(registrationBaseUrl + "/api/user/auth/verify")
                .then()
                .statusCode(200);

        // Login
        return given()
                .contentType("application/json")
                .body(TestDataBuilder.UserBuilder.createLoginJson(email, PASSWORD))
                .when()
                .post(registrationBaseUrl + "/api/user/auth/login")
                .then()
                .statusCode(200)
                .extract().response();
    }

    protected String getOtpFromDatabase(String email) throws Exception {
        String jdbcUrl = String.format("jdbc:postgresql://localhost:%d/postgres",
                postgres.getMappedPort(5432));

        try (Connection conn = DriverManager.getConnection(jdbcUrl, "postgres", "admin")) {

            String query = "SELECT verification_code FROM users WHERE email = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, email);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("verification_code");
                    }
                }
            }
        }
        throw new RuntimeException("OTP not found for email: " + email);
    }


}

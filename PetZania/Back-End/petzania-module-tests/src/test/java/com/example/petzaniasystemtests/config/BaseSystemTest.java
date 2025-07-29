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
import io.github.cdimascio.dotenv.Dotenv;

@Testcontainers
public class BaseSystemTest {
    protected static Network network = Network.newNetwork();
    protected static final String PASSWORD = "Password123!";

    // Load .env file from the project root (adjust path if needed)
    static Dotenv dotenv = Dotenv.configure()
        .directory("..") // Adjust path if needed
        .ignoreIfMalformed()
        .ignoreIfMissing()
        .load();

    static {
        // Debug print to verify SendGrid key is loaded
        System.out.println("SENDGRID KEY: " + dotenv.get("SPRING_SENDGRID_KEY"));
        System.out.println("Postgres Passowrd: " + dotenv.get("DB_PASSWORD"));
    }

    @Container
    protected static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("postgres")
            .withUsername(dotenv.get("DB_USERNAME"))
            .withPassword(dotenv.get("DB_PASSWORD"))
            .withCommand("postgres", "-c", "max_connections=200")
            .withInitScript("init-test-db.sql")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
    @Container
    protected static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management-alpine")
            .withNetwork(network)
            .withNetworkAliases("rabbitmq")
            .withExposedPorts(Integer.parseInt(dotenv.get("RABBITMQ_PORT")), 15672)
            .withUser(dotenv.get("RABBITMQ_USERNAME"), dotenv.get("RABBITMQ_PASSWORD"))
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
    @Container
    protected static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withExposedPorts(Integer.parseInt(dotenv.get("REDIS_PORT", "6379")))
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(1)));

    @Container
    protected static GenericContainer<?> registrationService = new GenericContainer<>(
            DockerImageName.parse("registration-module:latest"))
            .withNetwork(network)
            .withNetworkAliases("registration-service")
            .withExposedPorts(Integer.parseInt(dotenv.get("REGISTRATION_SERVER_PORT", "8080")))
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .withEnv("RABBITMQ_HOST", dotenv.get("RABBITMQ_HOST"))
            .withEnv("RABBITMQ_PORT", dotenv.get("RABBITMQ_PORT"))
            .withEnv("RABBITMQ_USERNAME", dotenv.get("RABBITMQ_USERNAME"))
            .withEnv("RABBITMQ_PASSWORD", dotenv.get("RABBITMQ_PASSWORD"))
            .withEnv("DB_USERNAME", dotenv.get("DB_USERNAME"))
            .withEnv("DB_PASSWORD", dotenv.get("DB_PASSWORD"))
            .withEnv("REGISTRATION_DB_URL", dotenv.get("REGISTRATION_DB_URL"))
            .withEnv("REDIS_HOST", dotenv.get("REDIS_HOST"))
            .withEnv("REDIS_PORT", dotenv.get("REDIS_PORT"))
            .withEnv("SECURITY_USER_NAME", dotenv.get("SECURITY_USER_NAME"))
            .withEnv("SECURITY_USER_PASSWORD", dotenv.get("SECURITY_USER_PASSWORD"))
            .withEnv("SPRING_JWT_SECRET_KEY", dotenv.get("SPRING_JWT_SECRET_KEY"))
            .withEnv("ACCESS_TOKEN_EXPIRATION", dotenv.get("ACCESS_TOKEN_EXPIRATION"))
            .withEnv("REFRESH_TOKEN_EXPIRATION", dotenv.get("REFRESH_TOKEN_EXPIRATION"))
            .withEnv("SPRING_SENDGRID_KEY", dotenv.get("SPRING_SENDGRID_KEY"))
            .withEnv("SPRING_EMAIL_SENDER", dotenv.get("SPRING_EMAIL_SENDER"))
            .withEnv("SPRING_AWS_ACCESS_KEY", dotenv.get("SPRING_AWS_ACCESS_KEY"))
            .withEnv("SPRING_AWS_SECRET_ACCESS_KEY", dotenv.get("SPRING_AWS_SECRET_ACCESS_KEY"))
            .withEnv("SPRING_AWS_REGION", dotenv.get("SPRING_AWS_REGION"))
            .withEnv("SPRING_AWS_BUCKET_NAME", dotenv.get("SPRING_AWS_BUCKET_NAME"))
            .withEnv("SPRING_AWS_CDN_URL", dotenv.get("SPRING_AWS_CDN_URL"))
            .withEnv("SPRING_AWS_MAX_SIZE_IMAGE", dotenv.get("SPRING_AWS_MAX_SIZE_IMAGE"))
            .withEnv("SPRING_AWS_MAX_SIZE_VIDEO", dotenv.get("SPRING_AWS_MAX_SIZE_VIDEO"))
            .withEnv("SPRING_AWS_MAX_SIZE_TEXT", dotenv.get("SPRING_AWS_MAX_SIZE_TEXT"))
            .withEnv("SPRING_AWS_MAX_SIZE_PDF", dotenv.get("SPRING_AWS_MAX_SIZE_PDF"))
            .withEnv("REGISTRATION_SERVER_PORT", dotenv.get("REGISTRATION_SERVER_PORT"))
            .withEnv("SUPER_ADMIN_USERNAME", dotenv.get("SUPER_ADMIN_USERNAME"))
            .withEnv("SUPER_ADMIN_PASSWORD", dotenv.get("SUPER_ADMIN_PASSWORD"))
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
            .withExposedPorts(Integer.parseInt(dotenv.get("FRIENDS_SERVER_PORT", "8081")))
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .withEnv("RABBITMQ_HOST", dotenv.get("RABBITMQ_HOST"))
            .withEnv("RABBITMQ_PORT", dotenv.get("RABBITMQ_PORT"))
            .withEnv("RABBITMQ_USERNAME", dotenv.get("RABBITMQ_USERNAME"))
            .withEnv("RABBITMQ_PASSWORD", dotenv.get("RABBITMQ_PASSWORD"))
            .withEnv("DB_USERNAME", dotenv.get("DB_USERNAME"))
            .withEnv("DB_PASSWORD", dotenv.get("DB_PASSWORD"))
            .withEnv("FRIENDS_DB_URL", dotenv.get("FRIENDS_DB_URL"))
            .withEnv("SECURITY_USER_NAME", dotenv.get("SECURITY_USER_NAME"))
            .withEnv("SECURITY_USER_PASSWORD", dotenv.get("SECURITY_USER_PASSWORD"))
            .withEnv("SPRING_JWT_SECRET_KEY", dotenv.get("SPRING_JWT_SECRET_KEY"))
            .withEnv("FRIENDS_SERVER_PORT", dotenv.get("FRIENDS_SERVER_PORT"))
            .withEnv("SUPER_ADMIN_USERNAME", dotenv.get("SUPER_ADMIN_USERNAME"))
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
            .withExposedPorts(Integer.parseInt(dotenv.get("ADOPTION_SERVER_PORT")))
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .withEnv("RABBITMQ_HOST", dotenv.get("RABBITMQ_HOST"))
            .withEnv("RABBITMQ_PORT", dotenv.get("RABBITMQ_PORT"))
            .withEnv("RABBITMQ_USERNAME", dotenv.get("RABBITMQ_USERNAME"))
            .withEnv("RABBITMQ_PASSWORD", dotenv.get("RABBITMQ_PASSWORD"))
            .withEnv("DB_USERNAME", dotenv.get("DB_USERNAME"))
            .withEnv("DB_PASSWORD", dotenv.get("DB_PASSWORD"))
            .withEnv("ADOPTION_DB_URL", dotenv.get("ADOPTION_DB_URL"))
            .withEnv("SECURITY_USER_NAME", dotenv.get("SECURITY_USER_NAME"))
            .withEnv("SECURITY_USER_PASSWORD", dotenv.get("SECURITY_USER_PASSWORD"))
            .withEnv("SPRING_JWT_SECRET_KEY", dotenv.get("SPRING_JWT_SECRET_KEY"))
            .withEnv("ADOPTION_SERVER_PORT", dotenv.get("ADOPTION_SERVER_PORT"))
            .withEnv("SUPER_ADMIN_USERNAME", dotenv.get("SUPER_ADMIN_USERNAME"))
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .dependsOn(postgres, rabbitmq, redis, registrationService)
            .withLogConsumer(outputFrame -> {
                System.err.println("ADOPTION: " + outputFrame.getUtf8String());
            })
            .waitingFor(Wait.forLogMessage(".*Started AdoptionAndBreedingModule.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(15)));

    @Container
    protected static GenericContainer<?> notificationService = new GenericContainer<>(
            "notification-module:latest")
            .withNetwork(network)
            .withNetworkAliases("notification-service")
            .withExposedPorts(Integer.parseInt(dotenv.get("NOTIFICATION_SERVER_PORT", "8083")))
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .withEnv("RABBITMQ_HOST", dotenv.get("RABBITMQ_HOST"))
            .withEnv("RABBITMQ_PORT", dotenv.get("RABBITMQ_PORT"))
            .withEnv("RABBITMQ_USERNAME", dotenv.get("RABBITMQ_USERNAME"))
            .withEnv("RABBITMQ_PASSWORD", dotenv.get("RABBITMQ_PASSWORD"))
            .withEnv("DB_USERNAME", dotenv.get("DB_USERNAME"))
            .withEnv("DB_PASSWORD", dotenv.get("DB_PASSWORD"))
            .withEnv("NOTIFICATION_DB_URL", dotenv.get("NOTIFICATION_DB_URL"))
            .withEnv("SECURITY_USER_NAME", dotenv.get("SECURITY_USER_NAME"))
            .withEnv("SECURITY_USER_PASSWORD", dotenv.get("SECURITY_USER_PASSWORD"))
            .withEnv("SPRING_JWT_SECRET_KEY", dotenv.get("SPRING_JWT_SECRET_KEY"))
            .withEnv("NOTIFICATION_SERVER_PORT", dotenv.get("NOTIFICATION_SERVER_PORT"))
            .withEnv("SUPER_ADMIN_USERNAME", dotenv.get("SUPER_ADMIN_USERNAME"))
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
        String jdbcUrl = String.format("jdbc:postgresql://localhost:%d/registration",
                postgres.getMappedPort(5432));

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dotenv.get("DB_USERNAME"), dotenv.get("DB_PASSWORD"))) {

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

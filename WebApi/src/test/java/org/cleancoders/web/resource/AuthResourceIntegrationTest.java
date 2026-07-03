package org.cleancoders.web.resource;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.common.domain.User;
import org.cleancoders.common.domain.UserRole;
import org.cleancoders.common.outbound.UserRepository;
import org.cleancoders.infrastructure.persistence.InMemoryUserRepo;
import org.cleancoders.infrastructure.security.BCryptPasswordEncoder;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.web.binder.UserAndAuthBinder;
import org.cleancoders.web.binder.WebAppBinder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthResourceIntegrationTest extends JerseyTest
{

    @Override
    protected Application configure()
    {
        var encoder = new BCryptPasswordEncoder();
        var userRepo = new InMemoryUserRepo();

        var hashedPw = encoder.encode("testpass");
        userRepo.save(new User("test-uuid", "testuser", hashedPw,
                UserRole.STUDENT, "Test User", "test@example.com"));

        var config = new ResourceConfig();
        config.register(WebAppBinder.class);
        config.register(AuthResource.class);
        config.register(UserAndAuthBinder.class);
        config.register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(userRepo).to(UserRepository.class);
                bind(encoder).to(PasswordEncoder.class);
            }
        });

        return config;
    }

    @BeforeEach
    public void setUp() throws Exception
    {
        super.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials()
    {
        Map<String, String> body = Map.of("username", "testuser", "password", "testpass");
        Response response = target("/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(200, response.getStatus());

        // 验证 Token 已写入 Cookie
        String authCookie = response.getCookies().get("Authorization").getValue();
        assertNotNull(authCookie);
        assertFalse(authCookie.isBlank());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertNotNull(entity.get("token"));
        assertNotNull(entity.get("user"));

        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) entity.get("user");
        assertEquals("testuser", userMap.get("username"));
        assertEquals("STUDENT", userMap.get("role"));
    }

    @Test
    void shouldReturn401ForWrongPassword()
    {
        Map<String, String> body = Map.of("username", "testuser", "password", "wrongpass");
        Response response = target("/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(401, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("Invalid credentials", entity.get("error"));
    }

    @Test
    void shouldReturn404ForUnknownUser()
    {
        Map<String, String> body = Map.of("username", "nobody", "password", "any");
        Response response = target("/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(404, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("User not found", entity.get("error"));
    }

    @Test
    void shouldRegisterSuccessfully()
    {
        Map<String, String> body = Map.of(
                "username", "newuser",
                "password", "pass",
                "name", "New",
                "email", "new@example.com"
        );
        Response response = target("/auth/register")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(201, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) entity.get("user");
        assertEquals("newuser", userMap.get("username"));
        assertEquals("STUDENT", userMap.get("role"));
    }

    @Test
    void shouldReturn409ForDuplicateUsername()
    {
        Map<String, String> body = Map.of(
                "username", "testuser",
                "password", "pass",
                "name", "Dup",
                "email", "dup@example.com"
        );
        Response response = target("/auth/register")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(409, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("Username already exists", entity.get("error"));
        assertEquals("testuser", entity.get("username"));
    }

    // === UC-03: GET /api/auth/me ===

    @Test
    void shouldReturnCurrentUserWithValidToken()
    {
        // First login to get a valid token (returned in Set-Cookie)
        Map<String, String> loginBody = Map.of("username", "testuser", "password", "testpass");
        Response loginResponse = target("/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(loginBody));

        assertEquals(200, loginResponse.getStatus());

        // 从 Cookie 中获取 Token
        String authCookie = loginResponse.getCookies().get("Authorization").getValue();
        assertNotNull(authCookie);

        // 用 Cookie 中的 Token 调用 /api/auth/me
        Response response = target("/auth/me")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", authCookie)
                .get();

        assertEquals(200, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertNotNull(entity.get("user"));

        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) entity.get("user");
        assertEquals("testuser", userMap.get("username"));
        assertEquals("STUDENT", userMap.get("role"));
        assertEquals("Test User", userMap.get("name"));
        assertEquals("test@example.com", userMap.get("email"));
    }

    @Test
    void shouldReturn401ForInvalidToken()
    {
        Response response = target("/auth/me")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", "invalid.jwt.token")
                .get();

        assertEquals(401, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("Invalid or expired token", entity.get("error"));
    }

    @Test
    void shouldReturn401ForMissingCookie()
    {
        Response response = target("/auth/me")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(401, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("Invalid or expired token", entity.get("error"));
    }

    @Test
    void meResponseShouldHaveJsonContentType()
    {
        // Login first to get a token
        Map<String, String> loginBody = Map.of("username", "testuser", "password", "testpass");
        Response loginResponse = target("/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(loginBody));

        String authCookie = loginResponse.getCookies().get("Authorization").getValue();

        Response response = target("/auth/me")
                .request(MediaType.APPLICATION_JSON)
                .cookie("Authorization", authCookie)
                .get();

        assertTrue(response.getHeaderString("Content-Type").startsWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void loginResponseShouldHaveJsonContentType()
    {
        Map<String, String> body = Map.of("username", "testuser", "password", "testpass");
        Response response = target("/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertTrue(response.getHeaderString("Content-Type").startsWith(MediaType.APPLICATION_JSON));
    }
}

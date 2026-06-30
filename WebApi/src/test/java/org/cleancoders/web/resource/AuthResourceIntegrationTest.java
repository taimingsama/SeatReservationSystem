package org.cleancoders.web.resource;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.cleancoders.infrastructure.persistence.InMemoryUserRepo;
import org.cleancoders.infrastructure.security.BCryptPasswordEncoder;
import org.cleancoders.infrastructure.security.JjwtTokenService;
import org.cleancoders.userandauth.domain.User;
import org.cleancoders.userandauth.domain.UserRole;
import org.cleancoders.userandauth.outbound.PasswordEncoder;
import org.cleancoders.userandauth.outbound.TokenService;
import org.cleancoders.userandauth.outbound.UserRepository;
import org.cleancoders.userandauth.usecase.LoginUseCase;
import org.cleancoders.web.filter.CorsFilter;
import org.cleancoders.web.presenter.WebApiAuthPresenter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthResourceIntegrationTest extends JerseyTest {

    @Override
    protected Application configure() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        InMemoryUserRepo userRepo = new InMemoryUserRepo();

        String hashedPw = encoder.encode("testpass");
        userRepo.save(new User("test-uuid", "testuser", hashedPw,
                UserRole.STUDENT, "Test User", "test@example.com"));

        WebApiAuthPresenter presenterInstance = new WebApiAuthPresenter();

        ResourceConfig config = new ResourceConfig();
        config.register(AuthResource.class);
        config.register(CorsFilter.class);
        config.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(userRepo).to(UserRepository.class);
                bind(new BCryptPasswordEncoder()).to(PasswordEncoder.class);
                bind(new JjwtTokenService()).to(TokenService.class);
                bind(LoginUseCase.class).to(LoginUseCase.class);
                bind(presenterInstance).to(WebApiAuthPresenter.class);
                bind(presenterInstance).to(LoginUseCase.Presenter.class);
            }
        });
        return config;
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {
        Map<String, String> body = Map.of("username", "testuser", "password", "testpass");
        Response response = target("/api/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(200, response.getStatus());

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
    void shouldReturn401ForWrongPassword() {
        Map<String, String> body = Map.of("username", "testuser", "password", "wrongpass");
        Response response = target("/api/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(401, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("Invalid credentials", entity.get("error"));
    }

    @Test
    void shouldReturn404ForUnknownUser() {
        Map<String, String> body = Map.of("username", "nobody", "password", "any");
        Response response = target("/api/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(404, response.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> entity = response.readEntity(Map.class);
        assertEquals("User not found", entity.get("error"));
    }

    @Test
    void shouldReturn501ForRegister() {
        Map<String, String> body = Map.of(
                "username", "newuser",
                "password", "pass",
                "name", "New",
                "email", "new@example.com"
        );
        Response response = target("/api/auth/register")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertEquals(501, response.getStatus());
    }

    @Test
    void loginResponseShouldHaveJsonContentType() {
        Map<String, String> body = Map.of("username", "testuser", "password", "testpass");
        Response response = target("/api/auth/login")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body));

        assertTrue(response.getHeaderString("Content-Type").startsWith(MediaType.APPLICATION_JSON));
    }
}

package com.example.staj1.security;

import com.example.staj1.model.User;
import com.example.staj1.repository.UserRepository;
import com.example.staj1.service.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EndpointRoleTests {
    AnnotationConfigWebApplicationContext context;
    FilterChainProxy security;
    @Configuration @EnableWebSecurity @Import(SecurityConfig.class)
    static class TestConfig {
        @Bean org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
            return request -> new org.springframework.web.cors.CorsConfiguration();
        }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean JwtService jwt() { return mock(JwtService.class); }
        @Bean JwtAuthenticationFilter jwtFilter(JwtService jwt, UserRepository users) { return new JwtAuthenticationFilter(jwt, users); }
    }
    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext());
        context.register(TestConfig.class); context.refresh();
        security = new FilterChainProxy(context.getBean(SecurityFilterChain.class));
        UserRepository users = context.getBean(UserRepository.class); JwtService jwt = context.getBean(JwtService.class);
        for (String role : new String[]{"USER", "STAFF", "ADMIN"}) {
            User user = new User(); user.setRole(role); user.setEmail(role + "@example.test");
            when(jwt.extractUsername(role)).thenReturn(user.getEmail());
            when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(jwt.isTokenValid(role, user)).thenReturn(true);
        }
    }
    @AfterEach void cleanup() { context.close(); }
    int request(String method, String path, String role) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path); request.setServletPath(path);
        if (role != null) request.addHeader("Authorization", "Bearer " + role);
        MockHttpServletResponse response = new MockHttpServletResponse();
        security.doFilter(request, response, (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(204));
        return response.getStatus();
    }
    @Test void referenceReadsStayPublic() throws Exception {
        for (String path : new String[]{"/city", "/district/city/1", "/price"}) assertEquals(204, request("GET", path, null));
    }
    @Test void referenceWritesRequireAdmin() throws Exception {
        for (String path : new String[]{"/city", "/district", "/price"}) {
            for (String method : new String[]{"POST", "PUT", "DELETE"}) {
                assertEquals(401, request(method, path, null));
                assertEquals(403, request(method, path, "USER"));
                assertEquals(403, request(method, path, "STAFF"));
                assertEquals(204, request(method, path, "ADMIN"));
            }
        }
    }
    @Test void ownUserAndShipmentRoutesRequireSession() throws Exception {
        assertEquals(401, request("GET", "/users/me", null));
        assertEquals(401, request("GET", "/shipment", null));
        for (String role : new String[]{"USER", "STAFF", "ADMIN"}) assertEquals(204, request("GET", "/users/me", role));
    }
    @Test void legacyUserCreationRequiresAdmin() throws Exception {
        assertEquals(403, request("POST", "/users", "USER"));
        assertEquals(403, request("POST", "/users", "STAFF"));
        assertEquals(204, request("POST", "/users", "ADMIN"));
    }
    @Test void registrationLoginAndPricePreviewRemainPublic() throws Exception {
        for (String path : new String[]{"/auth/register", "/auth/login", "/shipment/calculate-price"}) assertEquals(204, request("POST", path, null));
    }
    @Test void staffManagementRequiresAdmin() throws Exception {
        assertEquals(401, request("POST", "/staff", null));
        assertEquals(403, request("POST", "/staff", "USER"));
        assertEquals(403, request("POST", "/staff", "STAFF"));
        assertEquals(204, request("POST", "/staff", "ADMIN"));
    }
    @Test void staffUpdateAndActivityRequireAdmin() throws Exception {
        for (String[] action : new String[][]{{"PUT", "/staff/42"}, {"PATCH", "/staff/42/active"}}) {
            assertEquals(401, request(action[0], action[1], null));
            assertEquals(403, request(action[0], action[1], "USER"));
            assertEquals(403, request(action[0], action[1], "STAFF"));
            assertEquals(204, request(action[0], action[1], "ADMIN"));
        }
    }
    @Test void staffReadRoutesRequireAdmin() throws Exception {
        for (String path : new String[]{"/staff","/staff/42"}) {
            assertEquals(401,request("GET",path,null)); assertEquals(403,request("GET",path,"USER"));
            assertEquals(403,request("GET",path,"STAFF")); assertEquals(204,request("GET",path,"ADMIN"));
        }
    }
}

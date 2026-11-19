package com.example.staj1.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CorsPreflightTests {
    static class Registry extends CorsRegistry {
        Map<String, CorsConfiguration> mappings() { return getCorsConfigurations(); }
    }
    MockHttpServletResponse preflight(String origin, String method) throws Exception {
        return preflight("http://localhost:3000", origin, method);
    }
    MockHttpServletResponse preflight(String origins, String origin, String method) throws Exception {
        Registry registry = new Registry();
        new CorsConfig(origins).corsConfigurer().addCorsMappings(registry);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        registry.mappings().forEach(source::registerCorsConfiguration);
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/shipment/42/accept");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", method);
        request.addHeader("Access-Control-Request-Headers", "authorization,content-type");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CorsFilter(source).doFilter(request,response,(req,res)->fail("Preflight must finish before the controller."));
        return response;
    }
    @Test void frontendCanPreflightPatchWithAuthorizationHeader() throws Exception {
        var response = preflight("http://localhost:3000", "PATCH");
        assertEquals(200,response.getStatus());
        assertEquals("http://localhost:3000",response.getHeader("Access-Control-Allow-Origin"));
        assertTrue(response.getHeader("Access-Control-Allow-Methods").contains("PATCH"));
        assertTrue(response.getHeader("Access-Control-Allow-Headers").toLowerCase().contains("authorization"));
    }
    @Test void unrelatedOriginIsStillRejected() throws Exception {
        assertEquals(403,preflight("https://untrusted.example", "PATCH").getStatus());
    }
    @Test void credentialsAreAllowedForTheFrontendOrigin() throws Exception {
        assertEquals("true",preflight("http://localhost:3000", "POST").getHeader("Access-Control-Allow-Credentials"));
    }
    @Test void originsComeFromConfigurationAsCommaSeparatedList() throws Exception {
        String origins = "http://localhost:3000, https://kargo.example.com";
        assertEquals(200,preflight(origins, "https://kargo.example.com", "PATCH").getStatus());
        assertEquals(200,preflight(origins, "http://localhost:3000", "PATCH").getStatus());
        assertEquals(403,preflight(origins, "https://untrusted.example", "PATCH").getStatus());
    }
    @Test void wildcardOrEmptyOriginIsRejectedAtStartup() {
        assertThrows(IllegalStateException.class, () -> new CorsConfig("*"));
        assertThrows(IllegalStateException.class, () -> new CorsConfig(" , "));
    }
    @Test void existingMethodsRemainAvailable() throws Exception {
        for (String method : new String[]{"GET","POST","PUT","DELETE"})
            assertEquals(200,preflight("http://localhost:3000", method).getStatus());
    }
}

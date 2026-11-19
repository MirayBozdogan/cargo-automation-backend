package com.example.staj1.security;

import com.example.staj1.repository.UserRepository;
import com.example.staj1.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import io.jsonwebtoken.JwtException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthCookies authCookies;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            AuthCookies authCookies) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authCookies = authCookies;
    }

    // Giriş, kayıt ve çıkış eski/bozuk bir çerez yüzünden engellenmemeli.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Tarayıcı HttpOnly çerezi gönderir; Authorization: Bearer başlığı API araçları için desteklenmeye devam eder.
        final String authHeader = request.getHeader("Authorization");
        final String jwt = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authCookies.read(request);

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractUsername(jwt);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userRepository.findByEmail(email).orElse(null);
                if (userDetails == null || !userDetails.isEnabled() || !jwtService.isTokenValid(jwt, userDetails)) {
                    rejectToken(response);
                    return;
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            rejectToken(response);
            return;
        }

        filterChain.doFilter(request, response);
        //Filter işini bitirdi. Request artık controller'a doğru devam edebilir.
    }
    private void rejectToken(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        // Geçersiz çerez tarayıcıda kalmasın.
        response.addHeader(HttpHeaders.SET_COOKIE, authCookies.cleared().toString());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"Oturum geçersiz veya süresi dolmuş. Lütfen yeniden giriş yapın.\"}");
    }
}
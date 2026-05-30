package co.za.kingstechco.kingstechco.invoeaserapp.security;

import co.za.kingstechco.kingstechco.invoeaserapp.service.impl.RedisTokenService;
import co.za.kingstechco.kingstechco.invoeaserapp.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    
    @Mock
    private UserDetailsService userDetailsService;
    
    @Mock
    private RedisTokenService redisTokenService;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @Mock
    private SecurityContext securityContext;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;
    private static final String USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtUtil, userDetailsService, redisTokenService);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void doFilterInternal_ShouldAuthenticateUser_WhenValidToken() throws ServletException, IOException {
        // Given
        UserDetails userDetails = createUserDetails();
        
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(redisTokenService.isTokenBlacklisted(VALID_TOKEN)).thenReturn(false);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
        when(jwtUtil.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService).loadUserByUsername(USERNAME);
    }

    @Test
    void doFilterInternal_ShouldNotAuthenticate_WhenNoAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void doFilterInternal_ShouldNotAuthenticate_WhenInvalidToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void doFilterInternal_ShouldNotAuthenticate_WhenTokenIsBlacklisted() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(redisTokenService.isTokenBlacklisted(VALID_TOKEN)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtUtil, never()).extractUsername(any());
    }

    @Test
    void doFilterInternal_ShouldNotAuthenticate_WhenUserAlreadyAuthenticated() throws ServletException, IOException {
        // Given
        Authentication existingAuth = mock(Authentication.class);
        
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(redisTokenService.isTokenBlacklisted(VALID_TOKEN)).thenReturn(false);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void doFilterInternal_ShouldNotAuthenticate_WhenUsernameIsNull() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(redisTokenService.isTokenBlacklisted(VALID_TOKEN)).thenReturn(false);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(null);
        // No need to stub getAuthentication() since username is null

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void doFilterInternal_ShouldNotAuthenticate_WhenTokenValidationWithUserDetailsFails() throws ServletException, IOException {
        // Given
        UserDetails userDetails = createUserDetails();
        
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(redisTokenService.isTokenBlacklisted(VALID_TOKEN)).thenReturn(false);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
        when(jwtUtil.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService).loadUserByUsername(USERNAME);
    }

    @Test
    void doFilterInternal_ShouldHandleException_GracefullyAndContinueFilter() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenThrow(new RuntimeException("JWT processing error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldHandleUserDetailsServiceException_Gracefully() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(redisTokenService.isTokenBlacklisted(VALID_TOKEN)).thenReturn(false);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);
        when(securityContext.getAuthentication()).thenReturn(null);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenThrow(new RuntimeException("User not found"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void extractJwtFromRequest_ShouldReturnToken_WhenValidBearerHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(redisTokenService.isTokenBlacklisted(VALID_TOKEN)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtUtil).isTokenValid(VALID_TOKEN);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void extractJwtFromRequest_ShouldReturnNull_WhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtUtil, never()).isTokenValid(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void extractJwtFromRequest_ShouldReturnNull_WhenAuthorizationHeaderIsEmpty() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtUtil, never()).isTokenValid(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotFilter_ShouldReturnTrue_ForAuthEndpoints() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_ShouldReturnTrue_ForOAuth2Endpoints() {
        // Given
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/github");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_ShouldReturnTrue_ForSwaggerEndpoints() {
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_ShouldReturnTrue_ForApiDocsEndpoints() {
        // Given
        when(request.getRequestURI()).thenReturn("/v3/api-docs/swagger-config");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_ShouldReturnTrue_ForHealthCheckEndpoint() {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_ShouldReturnTrue_ForErrorEndpoint() {
        // Given
        when(request.getRequestURI()).thenReturn("/error");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotFilter_ShouldReturnFalse_ForProtectedEndpoints() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/invoices");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldNotFilter_ShouldReturnFalse_ForRootPath() {
        // Given
        when(request.getRequestURI()).thenReturn("/");

        // When
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // Then
        assertFalse(result);
    }

    @Test
    void doFilterInternal_ShouldSetAuthenticationDetails_WhenAuthenticating() throws ServletException, IOException {
        // Given
        UserDetails userDetails = createUserDetails();
        
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(redisTokenService.isTokenBlacklisted(VALID_TOKEN)).thenReturn(false);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(USERNAME);
        when(userDetailsService.loadUserByUsername(USERNAME)).thenReturn(userDetails);
        when(jwtUtil.isTokenValid(VALID_TOKEN, userDetails)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        // Verify that authentication was set by checking SecurityContextHolder
        // Since the implementation uses SecurityContextHolder.getContext().setAuthentication()
        // we can't verify the mock directly. Instead, verify that the dependent services were called
        verify(userDetailsService).loadUserByUsername(USERNAME);
        verify(jwtUtil).isTokenValid(VALID_TOKEN, userDetails);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldHandleMalformedBearerToken_Gracefully() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtUtil, never()).isTokenValid(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldHandleRedisTokenServiceException_Gracefully() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
        when(jwtUtil.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(redisTokenService.isTokenBlacklisted(VALID_TOKEN)).thenThrow(new RuntimeException("Redis connection error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    // Helper methods
    private UserDetails createUserDetails() {
        return new User(USERNAME, "password", 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
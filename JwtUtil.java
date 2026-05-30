package co.za.kingstechco.kingstechco.invoeaserapp.util;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;

import co.za.kingstechco.kingstechco.invoeaserapp.config.JwtConfig;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.Role;
import co.za.kingstechco.kingstechco.invoeaserapp.service.impl.RedisTokenService;
import org.springframework.security.core.GrantedAuthority;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtUtil {

    private final JwtConfig jwtConfig;
    private final RedisTokenService redisTokenService;

    @Autowired
    public JwtUtil(JwtConfig jwtConfig, RedisTokenService redisTokenService) {
        this.jwtConfig = jwtConfig;
        this.redisTokenService = redisTokenService;
    }

    /**
     * Generates a JWT token for the specified username.
     *
     * @param username the username for which the token is generated
     * @return a signed JWT token as a String
     */
    public Map<String, String> generateTokens(String username) {
        String accessToken = generateAccessToken(username);
        String refreshToken = generateRefreshToken(username);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", accessToken);
        tokens.put("refresh_token", refreshToken);
        return tokens;
    }

    /**
     * Generates a JWT token for the specified username with roles.
     *
     * @param username the username for which the token is generated
     * @param authorities the user's granted authorities/roles
     * @return a signed JWT token as a String
     */
    public Map<String, String> generateTokensWithRoles(String username, java.util.Collection<? extends GrantedAuthority> authorities) {
        String accessToken = generateAccessTokenWithRoles(username, authorities);
        String refreshToken = generateRefreshToken(username);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", accessToken);
        tokens.put("refresh_token", refreshToken);
        return tokens;
    }

    /**
     * Generates an access token for the specified username.
     *
     * @param username the username for which the token is generated
     * @return a signed JWT token as a String
     */
    public String generateAccessToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessTokenExpiration()))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates an access token for the specified username with roles.
     *
     * @param username the username for which the token is generated
     * @param authorities the user's granted authorities/roles
     * @return a signed JWT token as a String
     */
    public String generateAccessTokenWithRoles(String username, java.util.Collection<? extends GrantedAuthority> authorities) {
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessTokenExpiration()))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a refresh token for the specified username.
     *
     * @param username the username for which the token is generated
     * @return a signed JWT token as a String
     */
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshTokenExpiration()))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Retrieves the username from a JWT token.
     *
     * @param token the JWT token
     * @return the username if the token is valid, null otherwise
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Validates a JWT token with detailed exception handling.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            // First, check if the token is revoked
            if (redisTokenService.isTokenRevoked(token)) {
                return false;
            }

            // Parse token to validate structure and expiration
            Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("JWT token expired at {}", e.getClaims().getExpiration());
        } catch (MalformedJwtException e) {
            log.error("Malformed JWT token");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty");
        } catch (SecurityException e) {
            log.error("Invalid JWT signature");
        }
        return false;
    }

    /**
     * Revokes a JWT token by storing it in Redis with an expiration time.
     *
     * @param token the JWT token to revoke
     */
    public void revokeToken(String token) {
        Date expirationDate = Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();

        long expirationSeconds = (expirationDate.getTime() - System.currentTimeMillis()) / 1000;
        redisTokenService.revokeToken(token, expirationSeconds);
    }

    /**
     * Extracts the username from a JWT token.
     *
     * @param token the JWT token
     * @return the username if the token is valid, null otherwise
     */
    public String extractUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            log.error("Error extracting username from token", e);
            return null;
        }
    }

    /**
     * Validates a JWT token against user details.
     *
     * @param token the JWT token
     * @param userDetails the user details
     * @return true if the token is valid for the user, false otherwise
     */
    public boolean isTokenValid(String token, org.springframework.security.core.userdetails.UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Checks if a JWT token is valid (not expired and not revoked).
     *
     * @param token the JWT token
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        return validateToken(token) && !isTokenExpired(token);
    }

    /**
     * Checks if a JWT token is expired.
     *
     * @param token the JWT token
     * @return true if the token is expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("Error checking token expiration", e);
            return true;
        }
    }

    /**
     * Gets the access token expiration time in milliseconds.
     *
     * @return access token expiration time
     */
    public long getAccessTokenExpiration() {
        return jwtConfig.getAccessTokenExpiration();
    }

    /**
     * Gets the refresh token expiration time in milliseconds.
     *
     * @return refresh token expiration time
     */
    public long getRefreshTokenExpiration() {
        return jwtConfig.getRefreshTokenExpiration();
    }

    /**
     * Retrieves and decodes the secret key from a base64-encoded configuration.
     *
     * @return SecretKey for signing JWTs
     */
    /**
     * Extracts roles from a JWT token.
     *
     * @param token the JWT token
     * @return list of roles from the token
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return (List<String>) claims.get("roles");
        } catch (Exception e) {
            log.error("Error extracting roles from token", e);
            return java.util.Collections.emptyList();
        }
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtConfig.getSecret()));
    }
}

package com.cloudgaming.sessionmanager.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTest {

    private final KeycloakRoleConverter converter = new KeycloakRoleConverter();

    @Test
    void convert_extractsRolesFromRealmAccess() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("PLAYER", "admin")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject("user-123")
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_PLAYER", "ROLE_ADMIN");
    }

    @Test
    void convert_returnsEmpty_whenNoRealmAccess() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertThat(authorities).isEmpty();
    }

    @Test
    void convert_returnsEmpty_whenRolesListIsNull() {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", null);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", realmAccess)
                .subject("user-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertThat(authorities).isEmpty();
    }
}

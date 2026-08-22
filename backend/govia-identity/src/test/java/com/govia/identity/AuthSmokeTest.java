package com.govia.identity;

import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.LoginResponse;
import com.govia.identity.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test: boot toan bo context (Liquibase chay tren H2 in-memory de khong can Postgres/Oracle that),
 * DataSeeder tao tenant/admin, roi thu login - xac nhan khung Identity+Access hoat dong dau-cuoi.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthSmokeTest {

    @Autowired
    private AuthService authService;

    @Test
    void loginWithSeededAdminAccountSucceeds() {
        LoginResponse response = authService.login(new LoginRequest("default", "admin", "Admin@123"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.roles()).contains("SUPER_ADMIN");
    }
}

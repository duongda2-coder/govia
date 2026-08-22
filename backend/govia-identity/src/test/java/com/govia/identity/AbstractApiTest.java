package com.govia.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nen tang cho cac test API People: boot toan bo context tren H2, dang nhap admin
 * seed san de lay JWT that, moi test chay trong 1 transaction va tu rollback
 * (khong con lai du lieu giua cac test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class AbstractApiTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AuthService authService;

    protected String adminToken;

    @BeforeEach
    void loginAsAdmin() {
        adminToken = authService.login(new LoginRequest("default", "admin", "Admin@123")).accessToken();
    }
}

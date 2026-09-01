package com.govia.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govia.identity.dto.LoginOutcome;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.LoginResolveRequest;
import com.govia.identity.dto.LoginResponse;
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
    protected java.util.UUID adminUserId;

    /** Dung resolveLogin(KICK_OTHERS) khi gap CONFLICT thay vi coi la loi - giong het 1 client
     * that gap hoi "da phien cu hay khong". Can thiet vi mot so test (vd FrameworkShowcaseWorkflowTest)
     * co chu y KHONG rollback (Propagation.NOT_SUPPORTED) de kiem tra du lieu that, nen phien admin
     * cua no van con ACTIVE that su khi cac test SAU no cung dang nhap lai admin.
     * CHI dang nhap admin 1 LAN duy nhat o day (luu san ca token lan userId) - test con nao can
     * lai userId/token cua admin PHAI dung lai 2 field nay, KHONG duoc tu goi loginAsUser("admin", ...)
     * lan nua, vi lan goi do se hieu (dung) la dang nhap dong thoi va DA chinh phien vua tao o day. */
    @BeforeEach
    void loginAsAdmin() {
        LoginResponse response = loginAsUser("admin", "Admin@123");
        adminToken = response.accessToken();
        adminUserId = response.userId();
    }

    /** Dang nhap 1 lan nua (vd de lay lai .permissions()/.userId() moi nhat) - tu dong xu ly
     * CONFLICT y het loginAsAdmin() thay vi de test tu goi thang authService.login() (se bi tra ve
     * CONFLICT vi phien truoc do cua CHINH user nay - tu @BeforeEach hoac tu 1 lan goi khac - van
     * con dang ACTIVE). */
    protected LoginResponse loginAsUser(String username, String password) {
        LoginOutcome outcome = authService.login(new LoginRequest("default", username, password), null, null);
        if ("CONFLICT".equals(outcome.status())) {
            return authService.resolveLogin(new LoginResolveRequest(outcome.pendingToken(), "KICK_OTHERS"), null, null);
        }
        return outcome.login();
    }
}

package com.govia.identity;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Man hinh "Demo gui email": luu cau hinh SMTP, kiem tra ket noi, gui mail that qua 1 SMTP stub cuc bo, lich su gui. */
class EmailDemoApiTest extends AbstractApiTest {

    private ServerSocket smtpServer;
    private final List<String> receivedMessages = new CopyOnWriteArrayList<>();

    @BeforeEach
    void startSmtpStub() throws Exception {
        smtpServer = new ServerSocket(0);
        Thread thread = new Thread(() -> {
            while (!smtpServer.isClosed()) {
                try (Socket socket = smtpServer.accept()) {
                    handle(socket);
                } catch (Exception ignored) {
                    // socket dong khi ket thuc test
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @AfterEach
    void stopSmtpStub() throws Exception {
        smtpServer.close();
    }

    /** SMTP toi gian: tra 250 cho moi lenh, gom noi dung DATA cho toi dong ".". */
    private void handle(Socket socket) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        OutputStream out = socket.getOutputStream();
        out.write("220 stub ESMTP\r\n".getBytes(StandardCharsets.US_ASCII));
        String line;
        while ((line = in.readLine()) != null) {
            String cmd = line.toUpperCase();
            if (cmd.startsWith("DATA")) {
                out.write("354 go\r\n".getBytes(StandardCharsets.US_ASCII));
                StringBuilder data = new StringBuilder();
                while ((line = in.readLine()) != null && !line.equals(".")) {
                    data.append(line).append('\n');
                }
                receivedMessages.add(data.toString());
                out.write("250 queued\r\n".getBytes(StandardCharsets.US_ASCII));
            } else if (cmd.startsWith("QUIT")) {
                out.write("221 bye\r\n".getBytes(StandardCharsets.US_ASCII));
                return;
            } else {
                out.write("250 ok\r\n".getBytes(StandardCharsets.US_ASCII));
            }
            out.flush();
        }
    }

    @Test
    void sendBeforeConfiguredIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/email-demo/send").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("to", "a@b.vn", "subject", "x", "html", false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void configureTestAndSendDemoEmail() throws Exception {
        Map<String, Object> setting = Map.of("host", "127.0.0.1", "port", smtpServer.getLocalPort(), "smtpAuth", false,
                "starttls", false, "ssl", false, "fromAddress", "demo@govia.local", "fromName", "GOVIA Demo",
                "password", "secret");
        JsonNode saved = objectMapper.readTree(mockMvc.perform(put("/api/admin/email-demo/setting")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(setting)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        assertThat(saved.path("saved").asBoolean()).isTrue();
        assertThat(saved.path("passwordSet").asBoolean()).isTrue();
        assertThat(saved.has("password")).isFalse();

        mockMvc.perform(post("/api/admin/email-demo/setting/test").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Map<String, Object> mail = Map.of("to", "nguoinhan@govia.local; nguoi2@govia.local", "subject", "Thử gửi email demo",
                "body", "<b>Xin chào</b>", "html", true);
        JsonNode sent = objectMapper.readTree(mockMvc.perform(post("/api/admin/email-demo/send")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mail)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        assertThat(sent.path("success").asBoolean()).as(sent.toString()).isTrue();
        assertThat(sent.path("smtpHost").asText()).isEqualTo("127.0.0.1:" + smtpServer.getLocalPort());
        assertThat(receivedMessages).hasSize(1);
        String raw = receivedMessages.get(0);
        assertThat(raw).contains("nguoinhan@govia.local").contains("nguoi2@govia.local").contains("demo@govia.local").contains("text/html");

        // SMTP khong ton tai -> van tra 200 nhung success=false + ghi lich su loi
        smtpServer.close();
        JsonNode failed = objectMapper.readTree(mockMvc.perform(post("/api/admin/email-demo/send")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mail)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        assertThat(failed.path("success").asBoolean()).isFalse();
        assertThat(failed.path("errorMessage").asText()).isNotBlank();

        JsonNode logs = objectMapper.readTree(mockMvc.perform(get("/api/admin/email-demo/logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).path("sentBy").asText()).isEqualTo("admin");
    }

    @Test
    void approveButtonInEmailRecordsStatusOnlyOnce() throws Exception {
        Map<String, Object> setting = Map.of("host", "127.0.0.1", "port", smtpServer.getLocalPort(), "smtpAuth", false,
                "starttls", false, "ssl", false, "fromAddress", "demo@govia.local");
        mockMvc.perform(put("/api/admin/email-demo/setting").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(setting)))
                .andExpect(status().isOk());

        Map<String, Object> mail = Map.of("to", "duyet@govia.local", "subject", "Đề nghị phê duyệt", "body", "Dòng 1\nDòng 2",
                "html", false, "includeActions", true);
        JsonNode sent = objectMapper.readTree(mockMvc.perform(post("/api/admin/email-demo/send")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(mail)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        assertThat(sent.path("success").asBoolean()).as(sent.toString()).isTrue();
        assertThat(sent.path("actionStatus").asText()).isEqualTo("PENDING");
        assertThat(sent.path("detailUrl").asText()).startsWith("https://");
        assertThat(sent.has("actionToken")).isFalse();

        // giai ma quoted-printable de lay link nut Phe duyet trong mail
        String raw = receivedMessages.get(0).replace("=\n", "").replace("=3D", "=");
        assertThat(raw).contains("text/html").contains("chi-tiet-ho-so-demo");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("token=([0-9a-f]{48})&amp;decision=APPROVE").matcher(raw);
        assertThat(m.find()).as(raw).isTrue();
        String token = m.group(1);

        // endpoint cong khai - KHONG kem JWT
        String page = mockMvc.perform(get("/api/public/email-demo/action").param("token", token).param("decision", "APPROVE"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(page).contains("Đã ghi nhận: PHÊ DUYỆT");

        String again = mockMvc.perform(get("/api/public/email-demo/action").param("token", token).param("decision", "REJECT"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(again).contains("đã được xử lý trước đó").contains("PHÊ DUYỆT");

        mockMvc.perform(get("/api/public/email-demo/action").param("token", "khongtontai").param("decision", "APPROVE"))
                .andExpect(status().isNotFound());

        JsonNode logs = objectMapper.readTree(mockMvc.perform(get("/api/admin/email-demo/logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        assertThat(logs.get(0).path("actionStatus").asText()).isEqualTo("APPROVED");
        assertThat(logs.get(0).path("actionAt").isNull()).isFalse();
    }

    @Test
    void invalidRecipientIsRejected() throws Exception {
        Map<String, Object> setting = Map.of("host", "127.0.0.1", "port", smtpServer.getLocalPort(), "smtpAuth", false,
                "starttls", false, "ssl", false, "fromAddress", "demo@govia.local");
        mockMvc.perform(put("/api/admin/email-demo/setting").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(setting)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/email-demo/send").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("to", "khong-phai-email", "subject", "x", "html", false))))
                .andExpect(status().isBadRequest());
    }
}

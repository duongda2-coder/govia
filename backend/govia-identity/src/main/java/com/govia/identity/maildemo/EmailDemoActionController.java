package com.govia.identity.maildemo;

import com.govia.identity.maildemo.EmailDemoService.ActionResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Dich cua 2 nut Phe duyet/Tu choi trong mail demo - CONG KHAI (khai bao trong SecurityConfig.PUBLIC_ENDPOINTS) vi
 * nguoi nhan bam tu hop thu, khong co JWT. Xac thuc bang actionToken ngau nhien 48 ky tu hex trong link. Tra ve 1
 * trang HTML don gian bao ket qua thay vi JSON vi nguoi bam dang o trinh duyet.
 */
@RestController
@RequestMapping("/api/public/email-demo")
public class EmailDemoActionController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final EmailDemoService service;

    public EmailDemoActionController(EmailDemoService service) {
        this.service = service;
    }

    @GetMapping(value = "/action", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> action(@RequestParam(required = false) String token,
                                         @RequestParam(required = false) String decision) {
        ActionResult result = service.recordAction(token, decision);
        if (!result.found()) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML)
                    .body(page("#dc2626", "Liên kết không hợp lệ", "Không tìm thấy email tương ứng hoặc liên kết đã bị thay đổi."));
        }
        String label = EmailDemoService.STATUS_APPROVED.equals(result.status()) ? "PHÊ DUYỆT" : "TỪ CHỐI";
        String color = EmailDemoService.STATUS_APPROVED.equals(result.status()) ? "#16a34a" : "#dc2626";
        String when = result.actionAt() == null ? "" : " lúc " + TIME_FORMAT.format(result.actionAt());
        String subject = "Email: <b>" + HtmlUtils.htmlEscape(result.subject()) + "</b>";
        if (result.recorded()) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
                    .body(page(color, "Đã ghi nhận: " + label, subject + "<br/>Kết quả được ghi nhận" + when + "."));
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
                .body(page("#d97706", "Email này đã được xử lý trước đó",
                        subject + "<br/>Kết quả đã ghi nhận: <b style=\"color:" + color + "\">" + label + "</b>" + when
                                + ".<br/>Chỉ lần bấm đầu tiên được ghi nhận."));
    }

    private static String page(String color, String title, String message) {
        return "<!doctype html><html lang=\"vi\"><head><meta charset=\"utf-8\"/>"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/><title>GOVIA</title></head>"
                + "<body style=\"margin:0;background:#eff6ff;font-family:Arial,sans-serif\">"
                + "<div style=\"max-width:480px;margin:64px auto;padding:32px;background:#fff;border-radius:8px;"
                + "box-shadow:0 2px 8px rgba(0,0,0,.08);text-align:center\">"
                + "<div style=\"font-size:13px;color:#6b7280;margin-bottom:8px\">GOVIA - Demo gửi email</div>"
                + "<h2 style=\"color:" + color + ";margin:0 0 16px\">" + title + "</h2>"
                + "<p style=\"color:#374151;line-height:1.6;margin:0\">" + message + "</p>"
                + "</div></body></html>";
    }
}

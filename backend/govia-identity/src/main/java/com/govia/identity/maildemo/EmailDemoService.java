package com.govia.identity.maildemo;

import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.maildemo.EmailDemoDtos.LogResponse;
import com.govia.identity.maildemo.EmailDemoDtos.SendRequest;
import com.govia.identity.maildemo.EmailDemoDtos.SettingRequest;
import com.govia.identity.maildemo.EmailDemoDtos.SettingResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Chuc nang "Demo gui email" - DOC LAP voi moi quy trinh nghiep vu. Cau hinh SMTP luu trong bang
 * email_demo_setting (sua duoc tren man hinh, khong can restart backend); moi lan gui tao 1
 * JavaMailSenderImpl moi tu cau hinh do. Chua luu cau hinh thi tra ve gia tri spring.mail.* lam goi y.
 */
@Service
public class EmailDemoService {

    private static final Logger log = LoggerFactory.getLogger(EmailDemoService.class);
    private static final String TIMEOUT_MS = "15000";
    /** Link "xem chi tiet" tam thoi khi nguoi soan khong nhap - chi de demo, chua tro toi man hinh that nao. */
    static final String DEFAULT_DETAIL_URL = "https://example.com/govia/chi-tiet-ho-so-demo";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailDemoSettingRepository settingRepository;
    private final EmailDemoLogRepository logRepository;
    private final String defaultHost;
    private final int defaultPort;
    private final String defaultUsername;
    private final String defaultFrom;
    private final String publicBaseUrl;

    public EmailDemoService(EmailDemoSettingRepository settingRepository, EmailDemoLogRepository logRepository,
                            @Value("${spring.mail.host:}") String defaultHost,
                            @Value("${spring.mail.port:587}") int defaultPort,
                            @Value("${spring.mail.username:}") String defaultUsername,
                            @Value("${govia.notification.email.from:no-reply@govia.local}") String defaultFrom,
                            @Value("${govia.email-demo.public-base-url:http://localhost:8081}") String publicBaseUrl) {
        this.settingRepository = settingRepository;
        this.logRepository = logRepository;
        this.defaultHost = defaultHost;
        this.defaultPort = defaultPort;
        this.defaultUsername = defaultUsername;
        this.defaultFrom = defaultFrom;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Transactional(readOnly = true)
    public SettingResponse getSetting() {
        return settingRepository.findByTenantId(tenantId())
                .map(s -> new SettingResponse(s.getHost(), s.getPort(), s.getUsername(), StringUtils.hasText(s.getPassword()),
                        s.isSmtpAuth(), s.isStarttls(), s.isSsl(), s.getFromAddress(), s.getFromName(), true))
                .orElseGet(() -> new SettingResponse(blankToNull(defaultHost), defaultPort, blankToNull(defaultUsername), false,
                        true, true, false, defaultFrom, "GOVIA", false));
    }

    @Transactional
    public SettingResponse saveSetting(SettingRequest request) {
        parseAddresses(request.fromAddress(), "fromAddress");
        UUID tenantId = tenantId();
        EmailDemoSetting setting = settingRepository.findByTenantId(tenantId).orElseGet(() -> {
            EmailDemoSetting created = new EmailDemoSetting();
            created.setTenantId(tenantId);
            return created;
        });
        setting.setHost(request.host().trim());
        setting.setPort(request.port());
        setting.setUsername(blankToNull(request.username()));
        if (StringUtils.hasText(request.password())) {
            setting.setPassword(request.password());
        }
        setting.setSmtpAuth(request.smtpAuth());
        setting.setStarttls(request.starttls());
        setting.setSsl(request.ssl());
        setting.setFromAddress(request.fromAddress().trim());
        setting.setFromName(blankToNull(request.fromName()));
        settingRepository.save(setting);
        return getSetting();
    }

    /** Thu ket noi + dang nhap SMTP, khong gui mail. Loi -> BusinessException kem thong bao goc cua SMTP. */
    public void testConnection() {
        EmailDemoSetting setting = requireSetting();
        try {
            buildSender(setting).testConnection();
        } catch (MessagingException e) {
            throw new BusinessException("EMAIL_DEMO_CONNECT_FAILED", "Không kết nối được SMTP: " + rootMessage(e));
        }
    }

    /** Gui mail va LUON ghi 1 dong lich su (ca khi that bai) - ket qua tra ve trong LogResponse.success/errorMessage. */
    public LogResponse send(SendRequest request) {
        EmailDemoSetting setting = requireSetting();
        InternetAddress[] to = parseAddresses(request.to(), "to");
        InternetAddress[] cc = StringUtils.hasText(request.cc()) ? parseAddresses(request.cc(), "cc") : new InternetAddress[0];

        EmailDemoLog entry = new EmailDemoLog();
        entry.setTenantId(setting.getTenantId());
        entry.setToAddress(request.to().trim());
        entry.setCcAddress(blankToNull(request.cc()));
        entry.setSubject(request.subject());
        entry.setBody(request.body());
        entry.setHtml(request.html());
        entry.setSmtpHost(setting.getHost() + ":" + setting.getPort());
        if (request.includeActions()) {
            byte[] tokenBytes = new byte[24];
            RANDOM.nextBytes(tokenBytes);
            entry.setActionToken(HexFormat.of().formatHex(tokenBytes));
            entry.setActionStatus(STATUS_PENDING);
            entry.setDetailUrl(StringUtils.hasText(request.detailUrl()) ? request.detailUrl().trim() : DEFAULT_DETAIL_URL);
        }
        try {
            JavaMailSenderImpl sender = buildSender(setting);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(setting.getFromAddress(), setting.getFromName(), StandardCharsets.UTF_8.name()));
            helper.setTo(to);
            if (cc.length > 0) {
                helper.setCc(cc);
            }
            helper.setSubject(request.subject());
            if (request.includeActions()) {
                helper.setText(composeActionBody(request, entry), true);
            } else {
                helper.setText(request.body() == null ? "" : request.body(), request.html());
            }
            sender.send(message);
            entry.setSuccess(true);
            log.info("[email-demo] Da gui mail '{}' toi {} qua {}", request.subject(), request.to(), entry.getSmtpHost());
        } catch (Exception e) {
            entry.setSuccess(false);
            entry.setActionToken(null);
            entry.setActionStatus(null);
            entry.setErrorMessage(truncate(rootMessage(e), 2000));
            log.warn("[email-demo] Gui mail toi {} that bai: {}", request.to(), entry.getErrorMessage());
        }
        return toResponse(logRepository.save(entry));
    }

    /** Ket qua 1 lan bam nut trong mail: recorded=false nghia la mail da duoc xu ly truoc do (giu nguyen ket qua dau). */
    public record ActionResult(boolean found, boolean recorded, String status, Instant actionAt, String subject) {
    }

    /** Goi tu endpoint CONG KHAI (nguoi nhan mail khong dang nhap) - xac thuc bang actionToken ngau nhien. Lan bam
     * DAU TIEN duoc ghi nhan, cac lan sau chi bao lai ket qua da co. */
    @Transactional
    public ActionResult recordAction(String token, String decision) {
        String status = switch (decision == null ? "" : decision.trim().toUpperCase()) {
            case "APPROVE", "APPROVED" -> STATUS_APPROVED;
            case "REJECT", "REJECTED" -> STATUS_REJECTED;
            default -> null;
        };
        if (!StringUtils.hasText(token) || status == null) {
            return new ActionResult(false, false, null, null, null);
        }
        return logRepository.findByActionToken(token.trim()).map(entry -> {
            if (!STATUS_PENDING.equals(entry.getActionStatus())) {
                return new ActionResult(true, false, entry.getActionStatus(), entry.getActionAt(), entry.getSubject());
            }
            entry.setActionStatus(status);
            entry.setActionAt(Instant.now());
            logRepository.save(entry);
            log.info("[email-demo] Mail {} da duoc bam {} tu noi dung email", entry.getId(), status);
            return new ActionResult(true, true, status, entry.getActionAt(), entry.getSubject());
        }).orElseGet(() -> new ActionResult(false, false, null, null, null));
    }

    /** Noi dung nguoi soan + khoi "xem chi tiet" va 2 nut. Nut dung the a + bang + CSS inline vi nhieu trinh doc mail
     * (Gmail/Outlook) bo the style/script - khong dung form/JS. */
    private String composeActionBody(SendRequest request, EmailDemoLog entry) {
        String body = request.body() == null ? "" : request.body();
        if (!request.html()) {
            body = HtmlUtils.htmlEscape(body).replace("\n", "<br/>");
        }
        String actionUrl = publicBaseUrl + "/api/public/email-demo/action?token=" + entry.getActionToken() + "&decision=";
        String detailUrl = HtmlUtils.htmlEscape(entry.getDetailUrl());
        return body
                + "<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:24px 0 16px\"/>"
                + "<p style=\"margin:0 0 16px\"><a href=\"" + detailUrl + "\" style=\"color:#2563eb\">Bấm vào đây để xem chi tiết</a></p>"
                + "<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr>"
                + actionButton(actionUrl + "APPROVE", "Phê duyệt", "#16a34a")
                + "<td style=\"width:12px\"></td>"
                + actionButton(actionUrl + "REJECT", "Từ chối", "#dc2626")
                + "</tr></table>"
                + "<p style=\"margin:16px 0 0;color:#6b7280;font-size:12px\">Email demo GOVIA - chỉ lần bấm đầu tiên được ghi nhận.</p>";
    }

    private static String actionButton(String href, String label, String color) {
        return "<td style=\"border-radius:6px;background:" + color + "\"><a href=\"" + HtmlUtils.htmlEscape(href)
                + "\" style=\"display:inline-block;padding:10px 22px;color:#ffffff;text-decoration:none;font-weight:bold;"
                + "font-family:Arial,sans-serif;font-size:14px\">" + label + "</a></td>";
    }

    @Transactional(readOnly = true)
    public List<LogResponse> listLogs() {
        return logRepository.findTop100ByTenantIdOrderByCreatedAtDesc(tenantId()).stream().map(this::toResponse).toList();
    }

    private JavaMailSenderImpl buildSender(EmailDemoSetting setting) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(setting.getHost());
        sender.setPort(setting.getPort());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        if (setting.isSmtpAuth()) {
            sender.setUsername(setting.getUsername());
            sender.setPassword(setting.getPassword());
        }
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(setting.isSmtpAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(setting.isStarttls()));
        props.put("mail.smtp.ssl.enable", String.valueOf(setting.isSsl()));
        props.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
        props.put("mail.smtp.timeout", TIMEOUT_MS);
        props.put("mail.smtp.writetimeout", TIMEOUT_MS);
        return sender;
    }

    private EmailDemoSetting requireSetting() {
        return settingRepository.findByTenantId(tenantId())
                .orElseThrow(() -> new BusinessException("EMAIL_DEMO_NOT_CONFIGURED", "Chưa lưu cấu hình SMTP - vui lòng cấu hình trước khi gửi"));
    }

    private InternetAddress[] parseAddresses(String raw, String field) {
        try {
            InternetAddress[] addresses = InternetAddress.parse(raw.replace(';', ','), true);
            if (addresses.length == 0) {
                throw new AddressException("empty");
            }
            for (InternetAddress address : addresses) {
                address.validate();
            }
            return addresses;
        } catch (AddressException e) {
            throw new BusinessException("EMAIL_DEMO_INVALID_ADDRESS", "Địa chỉ email không hợp lệ (" + field + "): " + raw);
        }
    }

    private LogResponse toResponse(EmailDemoLog e) {
        return new LogResponse(e.getId(), e.getToAddress(), e.getCcAddress(), e.getSubject(), e.getBody(), e.isHtml(),
                e.isSuccess(), e.getErrorMessage(), e.getSmtpHost(), e.getCreatedBy(), e.getCreatedAt(),
                e.getDetailUrl(), e.getActionStatus(), e.getActionAt());
    }

    private static UUID tenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("TENANT_REQUIRED", "Không xác định được tenant");
        }
        return tenantId;
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String outer = e.getMessage();
        String inner = root.getMessage();
        if (root == e || inner == null) {
            return outer != null ? outer : e.getClass().getSimpleName();
        }
        return (outer != null ? outer + " - " : "") + root.getClass().getSimpleName() + ": " + inner;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }
}

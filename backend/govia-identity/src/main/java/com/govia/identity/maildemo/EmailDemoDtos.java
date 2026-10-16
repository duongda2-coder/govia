package com.govia.identity.maildemo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** DTO cua man hinh "Demo gui email". */
public final class EmailDemoDtos {

    private EmailDemoDtos() {
    }

    /** passwordSet=true nghia la da luu mat khau - KHONG bao gio tra mat khau ve frontend. */
    public record SettingResponse(String host, Integer port, String username, boolean passwordSet,
                                  boolean smtpAuth, boolean starttls, boolean ssl,
                                  String fromAddress, String fromName, boolean saved) {
    }

    /** password de trong = giu nguyen mat khau dang luu. */
    public record SettingRequest(@NotBlank @Size(max = 255) String host,
                                 @NotNull @Min(1) @Max(65535) Integer port,
                                 @Size(max = 255) String username,
                                 @Size(max = 500) String password,
                                 boolean smtpAuth, boolean starttls, boolean ssl,
                                 @NotBlank @Size(max = 255) String fromAddress,
                                 @Size(max = 255) String fromName) {
    }

    /** includeActions=true: chen link "xem chi tiet" (detailUrl) + 2 nut Phe duyet/Tu choi vao cuoi noi dung. */
    public record SendRequest(@NotBlank @Size(max = 1000) String to,
                              @Size(max = 1000) String cc,
                              @NotBlank @Size(max = 500) String subject,
                              @Size(max = 10000) String body,
                              boolean html,
                              boolean includeActions,
                              @Size(max = 1000) String detailUrl) {
    }

    public record LogResponse(UUID id, String toAddress, String ccAddress, String subject, String body, boolean html,
                              boolean success, String errorMessage, String smtpHost, String sentBy, Instant sentAt,
                              String detailUrl, String actionStatus, Instant actionAt) {
    }
}

package com.govia.identity.maildemo;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Cau hinh SMTP cho man hinh "Demo gui email" - 1 dong/tenant. Doc lap voi spring.mail.* cua
 * thong bao workflow (MailWorkflowNotificationService): chua luu dong nao thi man hinh lay gia tri
 * mac dinh tu spring.mail.* lam goi y. */
@Getter
@Setter
@Entity
@Table(name = "email_demo_setting")
public class EmailDemoSetting extends BaseEntity {

    @Column(name = "host", nullable = false, length = 255)
    private String host;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "password", length = 500)
    private String password;

    @Column(name = "smtp_auth", nullable = false)
    private boolean smtpAuth = true;

    @Column(name = "starttls", nullable = false)
    private boolean starttls = true;

    @Column(name = "ssl", nullable = false)
    private boolean ssl = false;

    @Column(name = "from_address", nullable = false, length = 255)
    private String fromAddress;

    @Column(name = "from_name", length = 255)
    private String fromName;
}

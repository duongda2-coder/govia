package com.govia.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cau hinh JWT dung chung cho toan platform.
 * Khai bao trong application.yml cua tung service duoi prefix govia.security.jwt
 */
@Component
@ConfigurationProperties(prefix = "govia.security.jwt")
public class JwtProperties {

    /** Bi mat ky token - production phai lay tu vault/secret manager, khong hardcode. */
    private String secret = "CHANGE_ME_dev_only_secret_key_min_32_bytes_______";

    private long accessTokenMinutes = 30;

    private long refreshTokenDays = 7;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    public void setAccessTokenMinutes(long accessTokenMinutes) {
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public long getRefreshTokenDays() {
        return refreshTokenDays;
    }

    public void setRefreshTokenDays(long refreshTokenDays) {
        this.refreshTokenDays = refreshTokenDays;
    }
}

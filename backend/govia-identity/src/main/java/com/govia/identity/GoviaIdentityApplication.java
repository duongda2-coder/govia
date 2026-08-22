package com.govia.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Quet ca package "com.govia" (khong chi com.govia.identity) de nap luon
 * cac bean/entity dung chung trong govia-core (security, attachment, audit, export).
 * Cac module GOVIA khac sau nay (People, Audit, Risk...) lam tuong tu.
 */
@SpringBootApplication(scanBasePackages = "com.govia")
@EntityScan("com.govia")
@EnableJpaRepositories("com.govia")
public class GoviaIdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoviaIdentityApplication.class, args);
    }
}

package com.govia.core;

import com.govia.core.tenant.AuditorAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Bat JPA auditing (created_by/updated_by tu dong) cho toan bo entity ke thua BaseEntity.
 * Chi can identity/moi service khac quet package "com.govia" la co hieu luc.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class CoreConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl();
    }
}

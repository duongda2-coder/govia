package com.govia.identity.workflow.config;

import org.flowable.spring.boot.ProcessEngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Flowable tu nhan dien "databaseType" (chon bo script DDL rieng cho tung DB) qua ten san pham JDBC
 * (DatabaseMetaData.getDatabaseProductName()). O profile h2/test (deu chay tren H2), du datasource
 * dung MODE=PostgreSQL de dong bo cu phap voi Postgres that (xem application.yml/application-test.yml),
 * driver H2 van bao san pham la "H2" nen Flowable van chon script DDL cho H2 - trong do bang lich su
 * ACT_HI_TSK_LOG dung kieu cot "identity" (cu phap rieng cua H2) ma H2 KHONG chap nhan khi dang o
 * MODE=PostgreSQL (che do nay gioi han cu phap gan voi Postgres that hon). Ep Flowable dung thang
 * script DDL cua Postgres (chay duoc tren H2 o che do tuong thich nay, giong cach Liquibase migration
 * chinh cua platform da lam) de tranh xung dot - CHI ap dung cho profile h2/test, KHONG dung cho
 * postgres/oracle that. ("test" la profile mvn test dung qua application-test.yml.)
 */
@Configuration
@Profile({"h2", "test"})
public class FlowableH2CompatibilityConfig {

    @Bean
    public ProcessEngineConfigurationConfigurer flowablePostgresDialectForH2Configurer() {
        return configuration -> configuration.setDatabaseType("postgres");
    }
}

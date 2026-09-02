package com.govia.identity.config;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat 10.1 (Spring Boot 3.x) tach rieng gioi han header cho request/response thay vi 1 thuoc
 * tinh "maxHttpHeaderSize" duy nhat nhu truoc - property "server.max-http-header-size" cua Spring
 * Boot khong con map dung sang gioi han REQUEST header nua, nen phai set truc tiep tren Connector.
 *
 * Can thiet vi JWT nhung het permission cua mot user (vd SUPER_ADMIN) co the toi ~7-8KB, cong them
 * cac header trinh duyet chuan (Accept, Origin, Referer, Sec-Fetch-*...) de vuot qua gioi han mac
 * dinh 8KB cua Tomcat - khi do Tomcat tu choi request VOI 400 Bad Request TRUOC KHI toi Spring
 * Security/DispatcherServlet, nen loi khong di qua GlobalExceptionHandler duoc.
 */
@Configuration
public class TomcatHeaderSizeConfig {

    private static final int MAX_HTTP_HEADER_SIZE_BYTES = 65536;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatMaxHeaderSizeCustomizer() {
        return factory -> factory.addConnectorCustomizers((TomcatConnectorCustomizer) connector -> {
            if (connector.getProtocolHandler() instanceof AbstractHttp11Protocol<?> protocol) {
                protocol.setMaxHttpRequestHeaderSize(MAX_HTTP_HEADER_SIZE_BYTES);
                protocol.setMaxHttpResponseHeaderSize(MAX_HTTP_HEADER_SIZE_BYTES);
            }
        });
    }
}

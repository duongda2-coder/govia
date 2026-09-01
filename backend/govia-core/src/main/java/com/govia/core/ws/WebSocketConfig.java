package com.govia.core.ws;

import com.govia.core.security.JwtTokenProvider;
import com.govia.core.security.UserSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Kenh WebSocket/STOMP dung chung cho toan platform - hien phuc vu 2 nhu cau:
 *  - "/topic/screen-lock.{screenKey}": broadcast trang thai khoa man hinh (ai dang sua) toi moi
 *    client dang xem man hinh do.
 *  - "/user/queue/session-kicked": push rieng toi 1 phien (jti) cu the khi bi "da" ra do dang
 *    nhap o noi khac (xem UserSessionService.revokeAll).
 * Principal cua 1 ket noi WebSocket la jti (StompPrincipal), KHONG phai username - moi thiet
 * bi/tab dang nhap co jti rieng nen convertAndSendToUser chi toi dung 1 thiet bi, khong dung nham
 * sang tab/thiet bi khac cua cung 1 user.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider tokenProvider;
    private final UserSessionService sessionService;

    @Value("${govia.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    /* @Lazy tren UserSessionService de PHA VONG LAP: UserSessionService can SimpMessagingTemplate
     * (do broker WebSocket cung cap) -> broker can moi WebSocketMessageBrokerConfigurer (gom
     * WebSocketConfig) -> neu WebSocketConfig lai can UserSessionService THAT SU luc khoi tao thi
     * quay vong. Voi @Lazy, Spring tiem 1 proxy roi moi thuc su lay bean khi handshake dau tien
     * goi toi (WebSocketAuthInterceptor), luc do UserSessionService da khoi tao xong tu lau. */
    public WebSocketConfig(JwtTokenProvider tokenProvider, @Lazy UserSessionService sessionService) {
        this.tokenProvider = tokenProvider;
        this.sessionService = sessionService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                .addInterceptors(new WebSocketAuthInterceptor(tokenProvider, sessionService))
                .setHandshakeHandler(new JtiHandshakeHandler());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /** Lay "jti" ma WebSocketAuthInterceptor.beforeHandshake da xac thuc va luu vao attributes,
     * dung lam ten Principal cho ket noi nay. */
    private static final class JtiHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) {
            Object jti = attributes.get("jti");
            return jti != null ? new StompPrincipal(jti.toString()) : super.determineUser(request, wsHandler, attributes);
        }
    }
}

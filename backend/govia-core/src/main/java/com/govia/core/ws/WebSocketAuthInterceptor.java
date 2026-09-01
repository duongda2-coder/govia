package com.govia.core.ws;

import com.govia.core.security.JwtTokenProvider;
import com.govia.core.security.UserSessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/** Handshake WebSocket (native browser API) khong the dinh kem header Authorization nhu request
 * REST binh thuong - token duoc client truyen qua query param "?token=..." va tu xac thuc TAI
 * DAY, truoc khi ket noi duoc chap nhan. Neu token thieu/khong hop le/phien da bi thu hoi thi tu
 * choi handshake (tra false) - khong phai "/ws/**" duoc mo cong khai that su (xem SecurityConfig). */
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final UserSessionService sessionService;

    public WebSocketAuthInterceptor(JwtTokenProvider tokenProvider, UserSessionService sessionService) {
        this.tokenProvider = tokenProvider;
        this.sessionService = sessionService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        List<String> tokenParam = UriComponentsBuilder.fromUri(servletRequest.getURI()).build().getQueryParams().get("token");
        String token = tokenParam == null || tokenParam.isEmpty() ? null : tokenParam.get(0);
        if (token == null) {
            return false;
        }
        try {
            Claims claims = tokenProvider.parseClaims(token);
            String jti = claims.getId();
            if (jti == null || !sessionService.isActive(jti)) {
                return false;
            }
            attributes.put("jti", jti);
            attributes.put("userId", claims.get("userId", String.class));
            attributes.put("tenantId", claims.get("tenantId", String.class));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // khong can lam gi them
    }
}

package com.govia.core.security;

import com.govia.core.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Doc Bearer token tren moi request, xac thuc va set:
 *  - Spring SecurityContext (cho @PreAuthorize theo role)
 *  - TenantContext (cho tang repository loc du lieu theo tenant, va cho @CreatedBy/@LastModifiedBy)
 * Dung chung cho tat ca service, khong can viet lai o tung module.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UserSessionService sessionService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserSessionService sessionService) {
        this.tokenProvider = tokenProvider;
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader(HEADER);
            if (header != null && header.startsWith(PREFIX)) {
                String token = header.substring(PREFIX.length());
                Claims claims = tokenProvider.parseClaims(token);

                /* Phien co the da bi DA (REVOKED) boi 1 lan dang nhap khac tren may khac - chu ky
                 * token van hop le (chua het han tu nhien) nen phai tra rieng bang user_session
                 * moi request de biet co con duoc dung khong, khong the chi tin chu ky. */
                if (claims.getId() != null && !sessionService.isActive(claims.getId())) {
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"SESSION_REVOKED\",\"message\":\"Phien dang nhap da bi thu hoi\"}");
                    return;
                }

                CurrentUserPrincipal principal = tokenProvider.toPrincipal(claims);

                List<GrantedAuthority> authorities = new ArrayList<>();
                if (principal.roles() != null) {
                    principal.roles().forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
                }
                if (principal.permissions() != null) {
                    principal.permissions().forEach(p -> authorities.add(new SimpleGrantedAuthority("PERM_" + p)));
                }

                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                TenantContext.setTenantId(principal.tenantId());
                TenantContext.setCurrentUser(principal.username());
            }
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"" + ex.getMessage() + "\"}");
        } finally {
            TenantContext.clear();
        }
    }
}

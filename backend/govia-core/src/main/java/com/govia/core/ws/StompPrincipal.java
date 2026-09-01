package com.govia.core.ws;

import java.security.Principal;

/** Dinh danh 1 KET NOI WebSocket cu the (khong phai 1 user) - name = jti cua phien dang nhap
 * dang giu ket noi nay. Nho vay {@code convertAndSendToUser(jti, ...)} chi push toi DUNG thiet
 * bi/tab so huu phien do, kho co the tab khac cua CUNG mot user (jti khac) vo tinh nhan nham vd
 * khi 1 nguoi mo 2 tab tren cung may. */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}

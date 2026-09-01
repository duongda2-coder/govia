import { Client, type IMessage } from "@stomp/stompjs";
import { getStoredTokens } from "../api/httpClient";

type Callback = (body: unknown) => void;

let client: Client | null = null;
let currentBaseURL = "";
const subscriptions = new Map<string, Set<Callback>>();
const stompSubscriptions = new Map<string, { unsubscribe: () => void }>();

function wsUrl(baseURL: string): string {
  const token = getStoredTokens()?.accessToken ?? "";
  const httpUrl = new URL(baseURL);
  const proto = httpUrl.protocol === "https:" ? "wss:" : "ws:";
  return `${proto}//${httpUrl.host}/ws?token=${encodeURIComponent(token)}`;
}

function resubscribeAll(): void {
  if (!client?.connected) return;
  for (const [destination, callbacks] of subscriptions) {
    if (stompSubscriptions.has(destination)) continue;
    const sub = client.subscribe(destination, (message: IMessage) => {
      let body: unknown = null;
      try {
        body = JSON.parse(message.body);
      } catch {
        // payload rong (vd tin logout) - bo qua, khong crash
      }
      callbacks.forEach((cb) => cb(body));
    });
    stompSubscriptions.set(destination, sub);
  }
}

/**
 * Khoi tao ket noi WebSocket/STOMP DUY NHAT cho toan app (goi sau khi dang nhap thanh cong / con
 * token luc app khoi dong) - moi noi can subscribe (useScreenLock, thong bao bi da...) deu dung
 * chung 1 ket noi nay qua subscribeTopic(), khong tu mo socket rieng.
 *
 * QUAN TRONG: neu da co 1 ket noi tu TRUOC (vd tab nay tung mang token cua 1 phien khac da bi da,
 * hoac nguoi dung vao thang /login trong khi tab van con dang nhap) thi PHAI reconnect lai bang
 * token MOI NHAT trong localStorage, khong duoc no-op som - neu khong, socket cu se tiep tuc gan
 * voi jti CU (co the vua bi thu hoi boi chinh lan dang nhap nay qua KICK_OTHERS), roi nhan nham
 * tin "session-kicked" cua phien cu va vo tinh dang xuat luon phien MOI vua tao.
 */
export function initSocket(baseURL: string): void {
  currentBaseURL = baseURL;
  if (client) {
    reconnectSocket();
    return;
  }
  client = new Client({
    brokerURL: wsUrl(baseURL),
    reconnectDelay: 4000,
    onConnect: resubscribeAll,
    onWebSocketClose: () => stompSubscriptions.clear(),
  });
  client.activate();
}

/** Goi lai sau khi access token vua duoc lam moi (refresh) - handshake WebSocket chi xac thuc 1
 * lan luc ket noi nen phai tao lai ket noi voi token moi de khong bi ngat khi token cu het han. */
export function reconnectSocket(): void {
  if (!client || !currentBaseURL) return;
  client.deactivate();
  stompSubscriptions.clear();
  client.brokerURL = wsUrl(currentBaseURL);
  client.activate();
}

export function disconnectSocket(): void {
  client?.deactivate();
  client = null;
  currentBaseURL = "";
  subscriptions.clear();
  stompSubscriptions.clear();
}

/** Dang ky nhan tin tren 1 destination (vd "/topic/screen-lock.xxx" hoac
 * "/user/queue/session-kicked"). Tra ve ham huy dang ky. Nhieu noi cung subscribe 1 destination
 * deu nhan duoc tin (fan-out noi bo, khong mo them subscription STOMP that su). */
export function subscribeTopic(destination: string, callback: Callback): () => void {
  let set = subscriptions.get(destination);
  if (!set) {
    set = new Set();
    subscriptions.set(destination, set);
  }
  set.add(callback);
  resubscribeAll();

  return () => {
    set?.delete(callback);
    if (set && set.size === 0) {
      subscriptions.delete(destination);
      stompSubscriptions.get(destination)?.unsubscribe();
      stompSubscriptions.delete(destination);
    }
  };
}

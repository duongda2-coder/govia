import { isAxiosError } from "axios";

/** Doi errorCode cua API AI thanh khoa i18n than thien - dung chung cho moi nut AI tren man hinh nghiep vu. */
export function agentErrorKey(err: unknown): string {
  const code = isAxiosError<{ errorCode?: string }>(err) ? err.response?.data?.errorCode : undefined;
  switch (code) {
    case "AGENT_DRAFT_NO_TTSS":
      return "agent.draft.errorNoTtss";
    case "AGENT_LLM_UNAVAILABLE":
      return "agent.chat.errorPaused";
    case "AGENT_DISABLED":
      return "agent.chat.errorDisabled";
    default:
      return "agent.draft.errorGeneric";
  }
}

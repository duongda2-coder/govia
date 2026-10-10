import { isAxiosError } from "axios";

/** Doi errorCode cua API AI thanh khoa i18n than thien - dung chung cho moi nut AI tren man hinh nghiep vu. */
export function agentErrorKey(err: unknown): string {
  const code = isAxiosError<{ errorCode?: string }>(err) ? err.response?.data?.errorCode : undefined;
  if (isAxiosError(err) && err.response?.status === 403) {
    return "agent.panel.errorForbidden";
  }
  switch (code) {
    case "AGENT_DRAFT_NO_TTSS":
      return "agent.draft.errorNoTtss";
    case "AGENT_REMINDER_NO_ITEMS":
      return "agent.reminder.errorNoItems";
    case "AGENT_DGCL_NO_CRITERIA":
      return "agent.dgcl.errorNoCriteria";
    case "AGENT_DGCL_NO_SUGGESTION":
      return "agent.dgcl.errorNoSuggestion";
    case "EXCEL_INVALID_FORMAT":
    case "EXCEL_ENCRYPTED":
    case "AGENT_IMPORT_FILE_EMPTY":
    case "AGENT_IMPORT_FILE_TOO_LARGE":
      return "agent.importCheck.errorFile";
    case "AGENT_LLM_UNAVAILABLE":
      return "agent.chat.errorPaused";
    case "AGENT_DISABLED":
      return "agent.chat.errorDisabled";
    default:
      return "agent.draft.errorGeneric";
  }
}

package com.govia.audit.agent.tools;

/** Ket qua thuc thi 1 Audit Tool - "body" la JSON tho tra ve tu endpoint that (chi co gia tri khi
 * status = SUCCESS). FORBIDDEN rieng biet voi ERROR de AgentOrchestratorService ap dung rule "khong
 * thu tool khac de ne 403" (xem AgentOrchestratorService). */
public record ToolExecutionResult(Status status, String body, String message) {

    public enum Status { SUCCESS, FORBIDDEN, ERROR }

    public static ToolExecutionResult success(String body) {
        return new ToolExecutionResult(Status.SUCCESS, body, null);
    }

    public static ToolExecutionResult forbidden(String message) {
        return new ToolExecutionResult(Status.FORBIDDEN, null, message);
    }

    public static ToolExecutionResult error(String message) {
        return new ToolExecutionResult(Status.ERROR, null, message);
    }
}

package com.govia.identity.workflow.delegate;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Minh hoa ERROR HANDLING cua BPMN: neu bien process "simulateRiskyError" = true (mac dinh true khi
 * khong truyen), delegate nem BpmnError("RISKY_FAILURE") - KHONG duoc Flowable retry (khac voi
 * RuntimeException thuong o RetryableServiceTaskDelegate), ma duoc bat ngay lap tuc boi Boundary Error
 * Event gan tren service task nay trong framework-showcase.bpmn20.xml, dan toi 1 nhanh xu ly loi rieng.
 */
@Component
public class RiskyServiceTaskDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        Object simulate = execution.getVariable("simulateRiskyError");
        boolean shouldFail = simulate == null || Boolean.TRUE.equals(simulate);
        if (shouldFail) {
            throw new BpmnError("RISKY_FAILURE", "Loi mo phong de kiem chung Boundary Error Event");
        }
        execution.setVariable("riskyServiceTaskSucceeded", true);
    }
}

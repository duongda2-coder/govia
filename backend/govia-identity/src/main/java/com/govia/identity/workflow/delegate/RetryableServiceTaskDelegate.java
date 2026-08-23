package com.govia.identity.workflow.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minh hoa co che RETRY cua Flowable: service task nay duoc khai bao async + flowable:failedJobRetryTimeCycle
 * trong BPMN (xem framework-showcase.bpmn20.xml), nen moi lan chay la 1 Job rieng do Flowable async job
 * executor xu ly tren thread rieng. Delegate mo phong dung 1 lan loi tam thoi cho MOI process instance
 * (dung Set tinh - KHONG dung process variable, vi bien set truoc khi throw se bi rollback cung
 * transaction that bai) - Flowable se tu dong lich lai va retry, lan 2 luon thanh cong.
 */
@Component
public class RetryableServiceTaskDelegate implements JavaDelegate {

    private static final Set<String> FAILED_ONCE_FOR_INSTANCE = ConcurrentHashMap.newKeySet();

    @Override
    public void execute(DelegateExecution execution) {
        if (FAILED_ONCE_FOR_INSTANCE.add(execution.getProcessInstanceId())) {
            throw new RuntimeException("Mo phong loi tam thoi lan dau - Flowable se tu dong retry theo "
                    + "flowable:failedJobRetryTimeCycle, lan chay ke tiep se thanh cong");
        }
        execution.setVariable("retryServiceTaskSucceeded", true);
    }
}

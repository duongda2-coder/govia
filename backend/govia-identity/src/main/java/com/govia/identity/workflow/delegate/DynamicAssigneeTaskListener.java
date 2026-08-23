package com.govia.identity.workflow.delegate;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * Gan assignee cho user task dua tren process variable "approverUserId" duoc truyen
 * luc start process (hoac set boi task truoc do) - thay vi hardcode assignee tinh trong BPMN.
 */
public class DynamicAssigneeTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        Object approverUserId = delegateTask.getVariable("approverUserId");
        if (approverUserId != null) {
            delegateTask.setAssignee(approverUserId.toString());
        }
    }
}

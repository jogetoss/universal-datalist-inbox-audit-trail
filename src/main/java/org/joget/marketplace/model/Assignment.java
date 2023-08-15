package org.joget.marketplace.model;

public class Assignment {

    private String ActivityProcessId;
    private String ActivityId;
    private String ResourceId;
    private String FormData;

    public String getActivityProcessId() {
        return ActivityProcessId;
    }

    public void setActivityProcessId(String ActivityProcessId) {
        this.ActivityProcessId = ActivityProcessId;
    }

    public String getActivityId() {
        return ActivityId;
    }

    public void setActivityId(String ActivityId) {
        this.ActivityId = ActivityId;
    }

    public String getResourceId() {
        return ResourceId;
    }

    public void setResourceId(String ResourceId) {
        this.ResourceId = ResourceId;
    }

    public String getFormData() {
        return FormData;
    }

    public void setFormData(String FormData) {
        this.FormData = FormData;
    }
}
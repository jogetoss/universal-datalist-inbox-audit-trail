package org.joget.marketplace.audittrail.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.AuditTrail;
import org.joget.apps.app.model.PackageActivityForm;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.marketplace.AppContext;
import org.joget.marketplace.dao.AssignmentDao;
import org.joget.marketplace.model.Assignment;
import org.joget.plugin.base.DefaultAuditTrailPlugin;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import com.google.gson.Gson;


public class UniversalDatalistInboxAuditTrail extends DefaultAuditTrailPlugin {
    private final static String MESSAGE_PATH = "messages/audittrail/UniversalDatalistInboxAuditTrail";

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("audittrail.universaldatalistinboxaudittrail.name", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getVersion() {
        final Properties projectProp = new Properties();
        try {
            projectProp.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException ex) {
            LogUtil.error(getClass().getName(), ex, "Unable to get project version from project properties...");
        }
        return projectProp.getProperty("version");
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("audittrail.universaldatalistinboxaudittrail.desc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("audittrail.universaldatalistinboxaudittrail.name", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/audittrail/UniversalDatalistInboxAuditTrail.json", null, true, MESSAGE_PATH);
    }
   
    public boolean validation(AuditTrail auditTrail) {
        return auditTrail.getMethod().equals("getDefaultAssignments");
    }

    @Override
    public Object execute(Map properties) {
        Object result = null;
        try {
            AssignmentDao assignmentdao = (AssignmentDao) AppContext.getInstance().getAppContext().getBean("assignmentDao");
            final AuditTrail auditTrail = (AuditTrail) properties.get("auditTrail");

            if (validation(auditTrail)) {
                WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
                String actId = auditTrail.getMessage();
                WorkflowActivity activity = workflowManager.getActivityById(actId);

                // get form associated with the activity
                ApplicationContext ac = AppUtil.getApplicationContext();
                AppService appService = (AppService) ac.getBean("appService");
                AppDefinition appDef = appService.getAppDefinitionForWorkflowActivity(actId);
                PackageActivityForm activityForm = appService.retrieveMappedForm(appDef.getAppId(), appDef.getVersion().toString(), activity.getProcessDefId(), activity.getActivityDefId());

                if (activity != null) {
                    Assignment assignment = new Assignment();
                    assignment.setActivityProcessId(activity.getProcessId());
                    assignment.setActivityId(activity.getId());
                    assignment.setResourceId(auditTrail.getUsername());

                    final String primaryKey = appService.getOriginProcessId(activity.getProcessId());
                    assignment.setId(primaryKey);

                    if (activityForm != null && !activityForm.getFormId().equals("")) {
                        FormData formData = new FormData();
                        Form loadForm = appService.viewDataForm(appDef.getId(), appDef.getVersion().toString(), activityForm.getFormId(), null, null, null, formData, null, null);
                        FormRowSet rowSet = appService.loadFormData(loadForm, primaryKey);
                        Gson gson = new Gson();
                        String jsonData = gson.toJson(rowSet);
                        assignment.setFormData(jsonData);
                    }
                    
                    assignmentdao.deleteAssignment(appService.getOriginProcessId(activity.getProcessId()));
                    assignmentdao.addAssignment(assignment);
                }
            }
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            return null;
        }
        return result;
    }
}
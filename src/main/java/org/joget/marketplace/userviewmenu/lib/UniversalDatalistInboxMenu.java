package org.joget.marketplace.userviewmenu.lib;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import org.displaytag.tags.TableTagParameters;
import org.joget.apps.app.dao.DatalistDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.DatalistDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListAction;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.service.DataListService;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.lib.InboxMenu;
import org.joget.apps.userview.model.Userview;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.commons.util.TimeZoneUtil;
import org.joget.marketplace.AppContext;
import org.joget.marketplace.dao.AssignmentDao;
import org.joget.marketplace.model.Assignment;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import com.google.gson.Gson;


public class UniversalDatalistInboxMenu extends InboxMenu {
    public static final String PROPERTY_FILTER_APP = "app";
    public static final String PROPERTY_FILTER_ACTIVITY = "activity";
    private DataList cachedDataList;

    private final static String MESSAGE_PATH = "messages/userviewmenu/UniversalDatalistInboxMenu";

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("userviewmenu.universaldatalistinboxmenu.name", getClassName(), MESSAGE_PATH);
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
        return AppPluginUtil.getMessage("userviewmenu.universaldatalistinboxmenu.desc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("userviewmenu.universaldatalistinboxmenu.name", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/userviewmenu/UniversalDatalistInboxMenu.json", null, true, MESSAGE_PATH);
    }
   
    @Override
    public String getIcon() {
        return "<i class=\"fas fa-inbox\"></i>";
    }

    @Override
    public String getCategory() {
        return "Marketplace";
    }

    @Override
    public boolean isHomePageSupported() {
        return true;
    }

    @Override
    public String getDecoratedMenu() {
        String menuItem = null;
        boolean showRowCount = Boolean.valueOf(getPropertyString("rowCount")).booleanValue();
        if (showRowCount) {
            int rowCount = 0;
            
            if (!"true".equalsIgnoreCase(getRequestParameterString("isBuilder"))) {
                rowCount = getDataTotalRowCount();
            }

            // sanitize label
            String label = getPropertyString("label");
            if (label != null) {
                label = StringUtil.stripHtmlRelaxed(label);
            }

            // generate menu link
            menuItem = "<a href=\"" + getUrl() + "\" class=\"menu-link default\"><span>" + label + "</span> <span class='pull-right badge rowCount'>" + rowCount + "</span></a>";
        }
        return menuItem;
    }

    @Override
    protected void viewList() {
        try {
            // get data list
            DataList dataList = getDataList();
            if (dataList != null) {
                dataList.setCheckboxPosition(DataList.CHECKBOX_POSITION_NO);
                dataList.setSize(getDataTotalRowCount());
                dataList.setRows(getRows(dataList));
              
                //overide datalist result to use userview result
                DataListActionResult ac = dataList.getActionResult();
                if (ac != null) {
                    if (ac.getMessage() != null && !ac.getMessage().isEmpty()) {
                        setAlertMessage(ac.getMessage());
                    }
                    if (ac.getType() != null && DataListActionResult.TYPE_REDIRECT.equals(ac.getType()) &&
                            ac.getUrl() != null && !ac.getUrl().isEmpty()) {
                        if ("REFERER".equals(ac.getUrl())) {
                            HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
                            if (request != null && request.getHeader("Referer") != null) {
                                setRedirectUrl(request.getHeader("Referer"));
                            } else {
                                setRedirectUrl("REFERER");
                            }
                        } else {
                            if (ac.getUrl().startsWith("?")) {
                                ac.setUrl(getUrl() + ac.getUrl());
                            }
                            setRedirectUrl(ac.getUrl());
                        }
                    }
                }

                // set data list
                setProperty("dataList", dataList);
            } else {
                setProperty("error", "Data List \"" + getPropertyString("datalistId") + "\" not exist.");
            }
            String datalistId = getPropertyString("datalistId");
            if (datalistId != null && !datalistId.isEmpty()) { 
                dataList.setDisableQuickEdit(false);
            } else {
                dataList.setDisableQuickEdit(true);
            }
        } catch (Exception ex) {
            StringWriter out = new StringWriter();
            ex.printStackTrace(new PrintWriter(out));
            String message = ex.toString();
            message += "\r\n<pre class=\"stacktrace\">" + out.getBuffer() + "</pre>";
            setProperty("error", message);
        }
    }

    @Override
    protected DataList getDataList() {
        DataList dataList = cachedDataList;
        if (cachedDataList == null) {
            ApplicationContext ac = AppUtil.getApplicationContext();
            DataListService dataListService = (DataListService) ac.getBean("dataListService");

            String datalistId = getPropertyString("datalistId");
            if (datalistId != null && !datalistId.isEmpty()) {
                DatalistDefinitionDao datalistDefinitionDao = (DatalistDefinitionDao) ac.getBean("datalistDefinitionDao");
                AppDefinition appDef = AppUtil.getCurrentAppDefinition(); //appService.getAppDefinition(getRequestParameterString("appId"), getRequestParameterString("appVersion"));
                DatalistDefinition datalistDefinition = datalistDefinitionDao.loadById(datalistId, appDef);
                if (datalistDefinition != null) {
                    dataList = dataListService.fromJson(datalistDefinition.getJson());

                    Collection<DataListColumn> columns = new ArrayList<DataListColumn>();
                    if (dataList.getColumns().length > 0) {
                        columns.addAll(Arrays.asList(dataList.getColumns()));
                    }
                    DataListColumn action = new DataListColumn();
                    action.setName("inboxAction");
                    action.setLabel("");
                    action.setSortable(false);
                    action.setRenderHtml(true);
                    if(dataList.getDataListParam(TableTagParameters.PARAMETER_EXPORTTYPE) != null && dataList.getDataListParam(TableTagParameters.PARAMETER_EXPORTING) != null){
                        action.setHidden(true);
                    }
                    columns.add(action);

                    dataList.setColumns((DataListColumn[]) columns.toArray(new DataListColumn[columns.size()]));
                    dataList.setActions(new DataListAction[]{});
                }
            } else {
                String json = AppUtil.readPluginResource(getClass().getName(), "/properties/userviewmenu/UniversalDatalistInboxMenuJson.json", null, true, MESSAGE_PATH);
                dataList = dataListService.fromJson(json);
                dataList.setTotal(dataList.getSize());
            }


            if (getPropertyString(Userview.USERVIEW_KEY_NAME) != null && getPropertyString(Userview.USERVIEW_KEY_NAME).trim().length() > 0) {
                dataList.addBinderProperty(Userview.USERVIEW_KEY_NAME, getPropertyString(Userview.USERVIEW_KEY_NAME));
            }
            if (getKey() != null && getKey().trim().length() > 0) {
                dataList.addBinderProperty(Userview.USERVIEW_KEY_VALUE, getKey());
            }

            cachedDataList = dataList; 
        }
        return dataList;
    }

    @Override
    protected DataListCollection getRows(DataList dataList) {
        try {
            DataListCollection resultList = new DataListCollection();
            if (getPropertyString("datalistId") != null && !getPropertyString("datalistId").isEmpty()) {
                if (getPropertyString("datalistOrigin") != null && !getPropertyString("datalistOrigin").isEmpty() && getPropertyString("datalistOrigin").equals("true")) {
                    resultList = dataList.getRows();
                    for (int i = 0; i < resultList.size(); i++) {
                        Map row = (Map) resultList.get(i);
                        String inboxAction = "";
                     
                        WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
                        WorkflowAssignment assignment = workflowManager.getAssignmentByProcess(row.get("ActivityProcessId").toString());
                        String url = addParamToUrl(getUrl(), "_mode", "assignment");
                        url = addParamToUrl(url, "activityId", assignment.getActivityId());
                        String label  = assignment.getActivityName();
                    
                        inboxAction += "<a href=\"" + url + "\">" + label + "</a> ";

                        row.put("inboxAction", inboxAction);
                    }
                } else {
                    JSONArray jsonArr= getDBDataList();
                    String format = AppUtil.getAppDateFormat();
                    WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
                
                    Gson gson = new Gson();
                    for(int i = 0; i < jsonArr.length(); i++) {
                        String jsonPp = gson.toJson(jsonArr.get(i));
                        Assignment tr = gson.fromJson(jsonPp, Assignment.class);
        
                        WorkflowAssignment assignment = workflowManager.getAssignmentByProcess(tr.getActivityProcessId());
                        String inboxAction = "";
                        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");

                    if(assignment != null){
                            Map data = new HashMap();
                            data.put("processId", assignment.getProcessId());
                            data.put("processRequesterId", assignment.getProcessRequesterId());
                            data.put("activityId", assignment.getActivityId());
                            data.put("processName", assignment.getProcessName());
                            data.put("activityName", assignment.getActivityName());
                            data.put("processVersion", assignment.getProcessVersion());
                            data.put("dateCreated", TimeZoneUtil.convertToTimeZone(assignment.getDateCreated(), null, format));
                            data.put("acceptedStatus", assignment.isAccepted());
                            data.put("dueDate", assignment.getDueDate() != null ? TimeZoneUtil.convertToTimeZone(assignment.getDueDate(), null, format) : "-");
                            data.put("serviceLevelMonitor", WorkflowUtil.getServiceLevelIndicator(assignment.getServiceLevelValue()));

                            String recordId = appService.getOriginProcessId(assignment.getProcessId());
                            data.put("id", recordId);

                            String jsonData = tr.getFormData();
                            JSONArray jsonArray = new JSONArray(jsonData);  
                            JSONObject jsonObject = jsonArray.getJSONObject(0);  

                            for (Object key : jsonObject.keySet()) {
                                String keyStr = (String)key;
                                Object keyvalue = jsonObject.get(keyStr);
                        
                                data.put(keyStr, keyvalue);
                            }

                            String url = addParamToUrl(getUrl(), "_mode", "assignment");
                            url = addParamToUrl(url, "activityId", assignment.getActivityId());
                            String label  = assignment.getActivityName();
                        
                            inboxAction += "<a href=\"" + url + "\">" + label + "</a> ";

                            data.put("inboxAction", inboxAction);
                            // set results
                            resultList.add(data);
                        }      
                    }     
                } 
            } else {
                JSONArray jsonArr= getDBDataList();
                String format = AppUtil.getAppDateFormat();
                WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
               
                for(int i = 0; i < jsonArr.length(); i++) {
                    Gson gson = new Gson();
                    String jsonPp = gson.toJson(jsonArr.get(i));
                    Assignment tr = gson.fromJson(jsonPp, Assignment.class);
                    WorkflowAssignment assignment = workflowManager.getAssignmentByProcess(tr.getActivityProcessId());
                    
                   if(assignment != null){
                        Map data = new HashMap();
                        data.put("processId", assignment.getProcessId());
                        data.put("processRequesterId", assignment.getProcessRequesterId());
                        data.put("activityId", assignment.getActivityId());
                        data.put("processName", assignment.getProcessName());
                        data.put("activityName", assignment.getActivityName());
                        data.put("processVersion", assignment.getProcessVersion());
                        data.put("dateCreated", TimeZoneUtil.convertToTimeZone(assignment.getDateCreated(), null, format));
                        data.put("acceptedStatus", assignment.isAccepted());
                        data.put("dueDate", assignment.getDueDate() != null ? TimeZoneUtil.convertToTimeZone(assignment.getDueDate(), null, format) : "-");
                        data.put("serviceLevelMonitor", WorkflowUtil.getServiceLevelIndicator(assignment.getServiceLevelValue()));
                        resultList.add(data);
                   }           
                }
            }
            return resultList;

        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            return null;
        }
    }

    @Override
    public int getDataTotalRowCount() {
        JSONArray jsonArr= getDBDataList();
        if (jsonArr != null) { 
            return jsonArr.length();
        } else {
            return 0;
        }
    }

    public JSONArray getDBDataList() {
        AssignmentDao assignmentdao = (AssignmentDao) AppContext.getInstance().getAppContext().getBean("assignmentDao");

        JSONArray arr = new JSONArray();

        Collection<Assignment> assignments = assignmentdao.getAssignments();
        for (Assignment a : assignments) {
            arr.put(a);
        }
        return arr;
    }
}

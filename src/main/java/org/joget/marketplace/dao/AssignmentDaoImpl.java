package org.joget.marketplace.dao;

import java.util.Collection;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.spring.model.AbstractSpringDao;
import org.joget.commons.util.LogUtil;
import org.joget.marketplace.model.Assignment;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;


public class AssignmentDaoImpl extends AbstractSpringDao implements AssignmentDao {

    @Override
    public Boolean addAssignment(final Assignment assignment) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            Boolean result = (Boolean)transactionTemplate.execute(new TransactionCallback() {
                @Override
                public Object doInTransaction(TransactionStatus ts) {
                    save("Assignment", assignment);
                    return true;
                }
            });
            return result;
        } catch (Exception e) {
            LogUtil.error(AssignmentDaoImpl.class.getName(), e, "Add Assignment Error!");
            return false;
        }
    }

    @Override
    public Boolean updateAssignment(final Assignment assignment) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            Boolean result = (Boolean)transactionTemplate.execute(new TransactionCallback<Object>() {
                @Override
                public Object doInTransaction(TransactionStatus ts) {
                    merge("Assignment", assignment);
                    return true;
                }
            });
            return result;
        } catch (Exception e) {
            LogUtil.error(AssignmentDaoImpl.class.getName(), e, "Update Assignment Error!");
            return false;
        }
    }

    @Override
    public Boolean deleteAssignment(final String id) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            Boolean result = (Boolean)transactionTemplate.execute(new TransactionCallback() {
                @Override
                public Object doInTransaction(TransactionStatus ts) {
                    Assignment assignment = getAssignment(id);
                    if (assignment != null) {
                        delete("Assignment", assignment);
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            return result;
        } catch (Exception e) {
            LogUtil.error(AssignmentDaoImpl.class.getName(), e, "Delete Assignment Error!");
            return false;
        }
    }

    @Override
    public Assignment getAssignment(final String id) {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            Assignment assignment = (Assignment)transactionTemplate.execute(new TransactionCallback() {
                @Override
                public Object doInTransaction(TransactionStatus ts) {
                    return (Assignment) find("Assignment", id);
                }
            });
            return assignment;
        } catch (Exception e) {
            LogUtil.error(AssignmentDaoImpl.class.getName(), e, "Get Assignment Error!");
            return null;
        }
    }

    @Override
    public Collection<Assignment> getAssignments() {
        try {
            TransactionTemplate transactionTemplate = (TransactionTemplate) AppUtil.getApplicationContext().getBean("transactionTemplate");
            Collection assignments = (Collection)transactionTemplate.execute(new TransactionCallback<Object>() {
                @Override
                public Object doInTransaction(TransactionStatus ts) {
                    return find("Assignment", "", null, null, null, null, null);
                }
            });
            return assignments;
        } catch (Exception e) {
            LogUtil.error(AssignmentDaoImpl.class.getName(), e, "Get Assignments Error!");
            return null;
        }
    }
  
}

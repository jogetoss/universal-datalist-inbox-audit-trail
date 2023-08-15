package org.joget.marketplace.dao;

import java.util.Collection;
import org.joget.marketplace.model.Assignment;

public interface AssignmentDao {

    Boolean addAssignment(Assignment assignment);

    Boolean updateAssignment(Assignment assignment);

    Boolean deleteAssignment(String id);

    Assignment getAssignment(String id);

    Collection<Assignment> getAssignments();
}

package cayenne3t.example.hr.auto;

import java.util.List;

import org.objectstyle.cayenne.PersistentObject;

import cayenne3t.example.hr.CPerson;
import cayenne3t.example.hr.CProject;

/**
 * A generated persistent class mapped as "Department" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public class _CDepartment extends PersistentObject {

    public static final String DESCRIPTION_PROPERTY = "description";
    public static final String NAME_PROPERTY = "name";
    public static final String EMPLOYEES_PROPERTY = "employees";
    public static final String PROJECTS_PROPERTY = "projects";

    protected String description;
    protected String name;
    protected List employees;
    protected List projects;

    public String getDescription() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "description");
        }
        
        return description;
    }
    public void setDescription(String description) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "description");
        }
        
        Object oldValue = this.description;
        this.description = description;
        
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "description", oldValue, description);
        }
    }
    
    
    public String getName() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name");
        }
        
        return name;
    }
    public void setName(String name) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name");
        }
        
        Object oldValue = this.name;
        this.name = name;
        
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "name", oldValue, name);
        }
    }
    
    
    public List getEmployees() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "employees");
        }
        
        return employees;
    }
    public void addToEmployees(CPerson object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "employees");
        }
        
        this.employees.add(object);
    }
    public void removeFromEmployees(CPerson object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "employees");
        }
        
        this.employees.remove(object);
    }
    
    public List getProjects() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "projects");
        }
        
        return projects;
    }
    public void addToProjects(CProject object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "projects");
        }
        
        this.projects.add(object);
    }
    public void removeFromProjects(CProject object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "projects");
        }
        
        this.projects.remove(object);
    }
    
}
package cayenne3t.example.hr.auto;

import java.util.Date;
import java.util.List;

import org.objectstyle.cayenne.ValueHolder;

import cayenne3t.example.hr.CDepartment;
import cayenne3t.example.hr.CProject;
import cayenne3t.example.hr.CustomClientObject;

/**
 * A generated persistent class mapped as "Person" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public class _CPerson extends CustomClientObject {

    public static final String BASE_SALARY_PROPERTY = "baseSalary";
    public static final String DATE_HIRED_PROPERTY = "dateHired";
    public static final String FULL_NAME_PROPERTY = "fullName";
    public static final String DEPARTMENT_PROPERTY = "department";
    public static final String MANAGED_PROJECTS_PROPERTY = "managedProjects";
    public static final String PROJECTS_PROPERTY = "projects";

    protected Double baseSalary;
    protected Date dateHired;
    protected String fullName;
    protected ValueHolder department;
    protected List managedProjects;
    protected List projects;

    public Double getBaseSalary() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "baseSalary");
        }
        
        return baseSalary;
    }
    public void setBaseSalary(Double baseSalary) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "baseSalary");
        }
        
        Object oldValue = this.baseSalary;
        this.baseSalary = baseSalary;
        
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "baseSalary", oldValue, baseSalary);
        }
    }
    
    
    public Date getDateHired() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "dateHired");
        }
        
        return dateHired;
    }
    public void setDateHired(Date dateHired) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "dateHired");
        }
        
        Object oldValue = this.dateHired;
        this.dateHired = dateHired;
        
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "dateHired", oldValue, dateHired);
        }
    }
    
    
    public String getFullName() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fullName");
        }
        
        return fullName;
    }
    public void setFullName(String fullName) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fullName");
        }
        
        Object oldValue = this.fullName;
        this.fullName = fullName;
        
        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "fullName", oldValue, fullName);
        }
    }
    
    
    public CDepartment getDepartment() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "department");
        }
        
        return (CDepartment) department.getValue();
    }
    public void setDepartment(CDepartment department) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "department");
        }
        
        this.department.setValue(department);
    }
    
    public List getManagedProjects() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "managedProjects");
        }
        
        return managedProjects;
    }
    public void addToManagedProjects(CProject object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "managedProjects");
        }
        
        this.managedProjects.add(object);
    }
    public void removeFromManagedProjects(CProject object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "managedProjects");
        }
        
        this.managedProjects.remove(object);
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
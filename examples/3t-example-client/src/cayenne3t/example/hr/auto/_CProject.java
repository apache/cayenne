package cayenne3t.example.hr.auto;

import java.util.List;

import org.objectstyle.cayenne.PersistentObject;
import org.objectstyle.cayenne.ValueHolder;

import cayenne3t.example.hr.CDepartment;
import cayenne3t.example.hr.CPerson;

/**
 * A generated persistent class mapped as "Project" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public class _CProject extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String DEPARTMENT_PROPERTY = "department";
    public static final String MANAGER_PROPERTY = "manager";
    public static final String MEMBERS_PROPERTY = "members";

    protected String name;
    protected ValueHolder department;
    protected ValueHolder manager;
    protected List members;

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
    
    public CPerson getManager() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "manager");
        }
        
        return (CPerson) manager.getValue();
    }
    public void setManager(CPerson manager) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "manager");
        }
        
        this.manager.setValue(manager);
    }
    
    public List getMembers() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "members");
        }
        
        return members;
    }
    public void addToMembers(CPerson object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "members");
        }
        
        this.members.add(object);
    }
    public void removeFromMembers(CPerson object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "members");
        }
        
        this.members.remove(object);
    }
    
}
package org.objectstyle.cayenne.testdo.mt.auto;

import java.util.List;

import org.objectstyle.cayenne.PersistentObject;
import org.objectstyle.cayenne.testdo.mt.ClientMtDeleteCascade;
import org.objectstyle.cayenne.testdo.mt.ClientMtDeleteDeny;
import org.objectstyle.cayenne.testdo.mt.ClientMtDeleteNullify;

/**
 * A generated persistent class mapped as "MtDeleteRule" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public class _ClientMtDeleteRule extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String FROM_CASCADE_PROPERTY = "fromCascade";
    public static final String FROM_DENY_PROPERTY = "fromDeny";
    public static final String FROM_NULLIFY_PROPERTY = "fromNullify";

    protected String name;
    protected List fromCascade;
    protected List fromDeny;
    protected List fromNullify;

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
    
    
    public List getFromCascade() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromCascade");
        }
        
        return fromCascade;
    }
    public void addToFromCascade(ClientMtDeleteCascade object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromCascade");
        }
        
        this.fromCascade.add(object);
    }
    public void removeFromFromCascade(ClientMtDeleteCascade object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromCascade");
        }
        
        this.fromCascade.remove(object);
    }
    
    public List getFromDeny() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromDeny");
        }
        
        return fromDeny;
    }
    public void addToFromDeny(ClientMtDeleteDeny object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromDeny");
        }
        
        this.fromDeny.add(object);
    }
    public void removeFromFromDeny(ClientMtDeleteDeny object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromDeny");
        }
        
        this.fromDeny.remove(object);
    }
    
    public List getFromNullify() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromNullify");
        }
        
        return fromNullify;
    }
    public void addToFromNullify(ClientMtDeleteNullify object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromNullify");
        }
        
        this.fromNullify.add(object);
    }
    public void removeFromFromNullify(ClientMtDeleteNullify object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromNullify");
        }
        
        this.fromNullify.remove(object);
    }
    
}
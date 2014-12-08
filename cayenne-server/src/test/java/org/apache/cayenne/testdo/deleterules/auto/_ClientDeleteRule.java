package org.apache.cayenne.testdo.deleterules.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.testdo.deleterules.ClientDeleteCascade;
import org.apache.cayenne.testdo.deleterules.ClientDeleteDeny;
import org.apache.cayenne.testdo.deleterules.ClientDeleteNullify;

import java.util.List;

/**
 * A generated persistent class mapped as "DeleteRule" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientDeleteRule extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String FROM_CASCADE_PROPERTY = "fromCascade";
    public static final String FROM_DENY_PROPERTY = "fromDeny";
    public static final String FROM_NULLIFY_PROPERTY = "fromNullify";

    protected String name;
    protected List<ClientDeleteCascade> fromCascade;
    protected List<ClientDeleteDeny> fromDeny;
    protected List<ClientDeleteNullify> fromNullify;

    public String getName() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name", false);
        }

        return name;
    }
    public void setName(String name) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "name", false);
        }

        Object oldValue = this.name;
        this.name = name;

        // notify objectContext about simple property change
        if(objectContext != null) {
            objectContext.propertyChanged(this, "name", oldValue, name);
        }
    }

    public List<ClientDeleteCascade> getFromCascade() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromCascade", true);
        }

        return fromCascade;
    }
    public void addToFromCascade(ClientDeleteCascade object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromCascade", true);
        }

        this.fromCascade.add(object);
    }
    public void removeFromFromCascade(ClientDeleteCascade object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromCascade", true);
        }

        this.fromCascade.remove(object);
    }

    public List<ClientDeleteDeny> getFromDeny() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromDeny", true);
        }

        return fromDeny;
    }
    public void addToFromDeny(ClientDeleteDeny object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromDeny", true);
        }

        this.fromDeny.add(object);
    }
    public void removeFromFromDeny(ClientDeleteDeny object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromDeny", true);
        }

        this.fromDeny.remove(object);
    }

    public List<ClientDeleteNullify> getFromNullify() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromNullify", true);
        }

        return fromNullify;
    }
    public void addToFromNullify(ClientDeleteNullify object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromNullify", true);
        }

        this.fromNullify.add(object);
    }
    public void removeFromFromNullify(ClientDeleteNullify object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromNullify", true);
        }

        this.fromNullify.remove(object);
    }

}

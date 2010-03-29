package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.testdo.mt.ClientMtDeleteCascade;
import org.apache.cayenne.testdo.mt.ClientMtDeleteDeny;
import org.apache.cayenne.testdo.mt.ClientMtDeleteNullify;

/**
 * A generated persistent class mapped as "MtDeleteRule" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtDeleteRule extends PersistentObject {

    public static final String NAME_PROPERTY = "name";
    public static final String FROM_CASCADE_PROPERTY = "fromCascade";
    public static final String FROM_DENY_PROPERTY = "fromDeny";
    public static final String FROM_NULLIFY_PROPERTY = "fromNullify";

    protected String name;
    protected List<ClientMtDeleteCascade> fromCascade;
    protected List<ClientMtDeleteDeny> fromDeny;
    protected List<ClientMtDeleteNullify> fromNullify;

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

    public List<ClientMtDeleteCascade> getFromCascade() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromCascade", true);
        }

        return fromCascade;
    }
    public void addToFromCascade(ClientMtDeleteCascade object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromCascade", true);
        }

        this.fromCascade.add(object);
    }
    public void removeFromFromCascade(ClientMtDeleteCascade object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromCascade", true);
        }

        this.fromCascade.remove(object);
    }

    public List<ClientMtDeleteDeny> getFromDeny() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromDeny", true);
        }

        return fromDeny;
    }
    public void addToFromDeny(ClientMtDeleteDeny object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromDeny", true);
        }

        this.fromDeny.add(object);
    }
    public void removeFromFromDeny(ClientMtDeleteDeny object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromDeny", true);
        }

        this.fromDeny.remove(object);
    }

    public List<ClientMtDeleteNullify> getFromNullify() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromNullify", true);
        }

        return fromNullify;
    }
    public void addToFromNullify(ClientMtDeleteNullify object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromNullify", true);
        }

        this.fromNullify.add(object);
    }
    public void removeFromFromNullify(ClientMtDeleteNullify object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "fromNullify", true);
        }

        this.fromNullify.remove(object);
    }

}

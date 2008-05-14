package org.apache.cayenne.testdo.mt.auto;

import java.util.Map;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.testdo.mt.ClientMtMapToManyTarget;

/**
 * A generated persistent class mapped as "MtMapToMany" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtMapToMany extends PersistentObject {

    public static final String TARGETS_PROPERTY = "targets";

    protected Map<Object, ClientMtMapToManyTarget> targets;

    public Map<Object, ClientMtMapToManyTarget> getTargets() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "targets", true);
        }

        return targets;
    }
	public void addToTargets(ClientMtMapToManyTarget object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "targets", true);
        }

        this.targets.put(getMapKey("targets", object), object);
    }
    public void removeFromTargets(ClientMtMapToManyTarget object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "targets", true);
        }

        this.targets.remove(getMapKey("targets", object));
    }

}

package org.apache.cayenne.testdo.map_to_many.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.testdo.map_to_many.ClientIdMapToManyTarget;

import java.util.Map;

/**
 * A generated persistent class mapped as "IdMapToMany" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientIdMapToMany extends PersistentObject {

    public static final String TARGETS_PROPERTY = "targets";

    protected Map<Object, ClientIdMapToManyTarget> targets;

    public Map<Object, ClientIdMapToManyTarget> getTargets() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "targets", true);
        }

        return targets;
    }
	public void addToTargets(ClientIdMapToManyTarget object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "targets", true);
        }

        this.targets.put(getMapKey("targets", object), object);
    }
    public void removeFromTargets(ClientIdMapToManyTarget object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "targets", true);
        }

        this.targets.remove(getMapKey("targets", object));
    }

}

package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.testdo.mt.ClientMtTable4;
import org.apache.cayenne.util.PersistentObjectList;

/**
 * A generated persistent class mapped as "MtTable5" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable5 extends PersistentObject {

    public static final ListProperty<ClientMtTable4> TABLE4S = PropertyFactory.createList("table4s", ClientMtTable4.class);

    protected List<ClientMtTable4> table4s;

    public List<ClientMtTable4> getTable4s() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table4s", true);
        } else if (this.table4s == null) {
        	this.table4s = new PersistentObjectList<>(this, "table4s");
		}

        return table4s;
    }

    public void addToTable4s(ClientMtTable4 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table4s", true);
        } else if (this.table4s == null) {
        	this.table4s = new PersistentObjectList<>(this, "table4s");
		}

        this.table4s.add(object);
    }

    public void removeFromTable4s(ClientMtTable4 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table4s", true);
        } else if (this.table4s == null) {
        	this.table4s = new PersistentObjectList<>(this, "table4s");
		}

        this.table4s.remove(object);
    }

}

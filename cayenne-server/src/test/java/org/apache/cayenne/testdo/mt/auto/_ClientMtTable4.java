package org.apache.cayenne.testdo.mt.auto;

import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.testdo.mt.ClientMtTable5;
import org.apache.cayenne.util.PersistentObjectList;

/**
 * A generated persistent class mapped as "MtTable4" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMtTable4 extends PersistentObject {

    public static final ListProperty<ClientMtTable5> TABLE5S = PropertyFactory.createList("table5s", ClientMtTable5.class);

    protected List<ClientMtTable5> table5s;

    public List<ClientMtTable5> getTable5s() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table5s", true);
        } else if (this.table5s == null) {
        	this.table5s = new PersistentObjectList<>(this, "table5s");
		}

        return table5s;
    }

    public void addToTable5s(ClientMtTable5 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table5s", true);
        } else if (this.table5s == null) {
        	this.table5s = new PersistentObjectList<>(this, "table5s");
		}

        this.table5s.add(object);
    }

    public void removeFromTable5s(ClientMtTable5 object) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "table5s", true);
        } else if (this.table5s == null) {
        	this.table5s = new PersistentObjectList<>(this, "table5s");
		}

        this.table5s.remove(object);
    }

}

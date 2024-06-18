package org.apache.cayenne.testdo.meaningful_pk.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.meaningful_pk.ClientMeaningfulPkDep2;
import org.apache.cayenne.util.PersistentObjectHolder;

/**
 * A generated persistent class mapped as "MeaningfulPk" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMeaningfulPk extends PersistentObject {

    public static final StringProperty<String> PK = PropertyFactory.createString("pk", String.class);
    public static final EntityProperty<ClientMeaningfulPkDep2> MEANINGFUL_PK_DEP2S = PropertyFactory.createEntity("meaningfulPkDep2s", ClientMeaningfulPkDep2.class);

    protected String pk;
    protected ValueHolder<ClientMeaningfulPkDep2> meaningfulPkDep2s;

    public String getPk() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "pk", false);
        }


        return pk;
    }

    public void setPk(String pk) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "pk", false);
            objectContext.propertyChanged(this, "pk", this.pk, pk);
        }

        this.pk = pk;
    }

    public ClientMeaningfulPkDep2 getMeaningfulPkDep2s() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "meaningfulPkDep2s", true);
        } else if (this.meaningfulPkDep2s == null) {
        	this.meaningfulPkDep2s = new PersistentObjectHolder<>(this, "meaningfulPkDep2s");
		}

        return meaningfulPkDep2s.getValue();
    }

    public void setMeaningfulPkDep2s(ClientMeaningfulPkDep2 meaningfulPkDep2s) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "meaningfulPkDep2s", true);
        } else if (this.meaningfulPkDep2s == null) {
        	this.meaningfulPkDep2s = new PersistentObjectHolder<>(this, "meaningfulPkDep2s");
		}

        this.meaningfulPkDep2s.setValue(meaningfulPkDep2s);
    }

}

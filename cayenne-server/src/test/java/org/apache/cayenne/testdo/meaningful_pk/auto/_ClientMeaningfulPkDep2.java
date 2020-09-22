package org.apache.cayenne.testdo.meaningful_pk.auto;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.meaningful_pk.ClientMeaningfulPk;
import org.apache.cayenne.util.PersistentObjectHolder;

/**
 * A generated persistent class mapped as "MeaningfulPkDep2" Cayenne entity. It is a good idea to
 * avoid changing this class manually, since it will be overwritten next time code is
 * regenerated. If you need to make any customizations, put them in a subclass.
 */
public abstract class _ClientMeaningfulPkDep2 extends PersistentObject {

    public static final StringProperty<String> DESCR = PropertyFactory.createString("descr", String.class);
    public static final StringProperty<String> PK = PropertyFactory.createString("pk", String.class);
    public static final EntityProperty<ClientMeaningfulPk> MEANINGFUL_PK = PropertyFactory.createEntity("meaningfulPk", ClientMeaningfulPk.class);

    protected String descr;
    protected String pk;
    protected ValueHolder<ClientMeaningfulPk> meaningfulPk;

    public String getDescr() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "descr", false);
        }


        return descr;
    }

    public void setDescr(String descr) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "descr", false);
            objectContext.propertyChanged(this, "descr", this.descr, descr);
        }

        this.descr = descr;
    }

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

    public ClientMeaningfulPk getMeaningfulPk() {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "meaningfulPk", true);
        } else if (this.meaningfulPk == null) {
        	this.meaningfulPk = new PersistentObjectHolder<>(this, "meaningfulPk");
		}

        return meaningfulPk.getValue();
    }

    public void setMeaningfulPk(ClientMeaningfulPk meaningfulPk) {
        if(objectContext != null) {
            objectContext.prepareForAccess(this, "meaningfulPk", true);
        } else if (this.meaningfulPk == null) {
        	this.meaningfulPk = new PersistentObjectHolder<>(this, "meaningfulPk");
		}

        this.meaningfulPk.setValue(meaningfulPk);
    }

}

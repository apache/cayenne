package org.apache.cayenne.testdo.toone.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.toone.TooneDep;
import org.apache.cayenne.testdo.toone.TooneMaster;

/**
 * Class _TooneMaster was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _TooneMaster extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<TooneMaster> SELF = PropertyFactory.createSelf(TooneMaster.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "TooneMaster", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final EntityProperty<TooneDep> TO_DEPENDENT = PropertyFactory.createEntity("toDependent", TooneDep.class);


    protected Object toDependent;

    public void setToDependent(TooneDep toDependent) {
        setToOneTarget("toDependent", toDependent, true);
    }

    public TooneDep getToDependent() {
        return (TooneDep)readProperty("toDependent");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "toDependent":
                return this.toDependent;
            default:
                return super.readPropertyDirectly(propName);
        }
    }

    @Override
    public void writePropertyDirectly(String propName, Object val) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch (propName) {
            case "toDependent":
                this.toDependent = val;
                break;
            default:
                super.writePropertyDirectly(propName, val);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeSerialized(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readSerialized(in);
    }

    @Override
    protected void writeState(ObjectOutputStream out) throws IOException {
        super.writeState(out);
        out.writeObject(this.toDependent);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.toDependent = in.readObject();
    }

}

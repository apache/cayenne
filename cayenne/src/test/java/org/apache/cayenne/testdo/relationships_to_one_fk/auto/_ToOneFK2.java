package org.apache.cayenne.testdo.relationships_to_one_fk.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.relationships_to_one_fk.ToOneFK1;
import org.apache.cayenne.testdo.relationships_to_one_fk.ToOneFK2;

/**
 * Class _ToOneFK2 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _ToOneFK2 extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<ToOneFK2> SELF = PropertyFactory.createSelf(ToOneFK2.class);

    public static final NumericIdProperty<Integer> TO_ONE_FK2_PK_PK_PROPERTY = PropertyFactory.createNumericId("TO_ONE_FK2_PK", "ToOneFK2", Integer.class);
    public static final String TO_ONE_FK2_PK_PK_COLUMN = "TO_ONE_FK2_PK";

    public static final EntityProperty<ToOneFK1> TO_ONE_TO_FK = PropertyFactory.createEntity("toOneToFK", ToOneFK1.class);


    protected Object toOneToFK;

    public void setToOneToFK(ToOneFK1 toOneToFK) {
        setToOneTarget("toOneToFK", toOneToFK, true);
    }

    public ToOneFK1 getToOneToFK() {
        return (ToOneFK1)readProperty("toOneToFK");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "toOneToFK":
                return this.toOneToFK;
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
            case "toOneToFK":
                this.toOneToFK = val;
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
        out.writeObject(this.toOneToFK);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.toOneToFK = in.readObject();
    }

}

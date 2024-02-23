package org.apache.cayenne.testdo.oneway.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.oneway.OnewayTable1;
import org.apache.cayenne.testdo.oneway.OnewayTable2;

/**
 * Class _OnewayTable2 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _OnewayTable2 extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<OnewayTable2> SELF = PropertyFactory.createSelf(OnewayTable2.class);

    public static final String ID_PK_COLUMN = "ID";

    public static final NumericProperty<Integer> ID = PropertyFactory.createNumeric("id", Integer.class);
    public static final EntityProperty<OnewayTable1> TO_ONE_ONE_WAY_DB = PropertyFactory.createEntity("toOneOneWayDb", OnewayTable1.class);

    protected Integer id;

    protected Object toOneOneWayDb;

    public void setId(Integer id) {
        beforePropertyWrite("id", this.id, id);
        this.id = id;
    }

    public Integer getId() {
        beforePropertyRead("id");
        return this.id;
    }

    public void setToOneOneWayDb(OnewayTable1 toOneOneWayDb) {
        setToOneTarget("toOneOneWayDb", toOneOneWayDb, true);
    }

    public OnewayTable1 getToOneOneWayDb() {
        return (OnewayTable1)readProperty("toOneOneWayDb");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "id":
                return this.id;
            case "toOneOneWayDb":
                return this.toOneOneWayDb;
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
            case "id":
                this.id = (Integer)val;
                break;
            case "toOneOneWayDb":
                this.toOneOneWayDb = val;
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
        out.writeObject(this.id);
        out.writeObject(this.toOneOneWayDb);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.id = (Integer)in.readObject();
        this.toOneOneWayDb = in.readObject();
    }

}

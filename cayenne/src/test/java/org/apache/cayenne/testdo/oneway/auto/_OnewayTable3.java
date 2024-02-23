package org.apache.cayenne.testdo.oneway.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.oneway.OnewayTable3;
import org.apache.cayenne.testdo.oneway.OnewayTable4;

/**
 * Class _OnewayTable3 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _OnewayTable3 extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<OnewayTable3> SELF = PropertyFactory.createSelf(OnewayTable3.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "OnewayTable3", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final ListProperty<OnewayTable4> TO_MANY_ONE_WAY_DB = PropertyFactory.createList("toManyOneWayDb", OnewayTable4.class);


    protected Object toManyOneWayDb;

    public void addToToManyOneWayDb(OnewayTable4 obj) {
        addToManyTarget("toManyOneWayDb", obj, true);
    }

    public void removeFromToManyOneWayDb(OnewayTable4 obj) {
        removeToManyTarget("toManyOneWayDb", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<OnewayTable4> getToManyOneWayDb() {
        return (List<OnewayTable4>)readProperty("toManyOneWayDb");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "toManyOneWayDb":
                return this.toManyOneWayDb;
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
            case "toManyOneWayDb":
                this.toManyOneWayDb = val;
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
        out.writeObject(this.toManyOneWayDb);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.toManyOneWayDb = in.readObject();
    }

}

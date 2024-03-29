package org.apache.cayenne.commitlog.db.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.commitlog.db.E3;
import org.apache.cayenne.commitlog.db.E4;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;

/**
 * Class _E3 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _E3 extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<E3> SELF = PropertyFactory.createSelf(E3.class);

    public static final String ID_PK_COLUMN = "ID";

    public static final ListProperty<E4> E4S = PropertyFactory.createList("e4s", E4.class);


    protected Object e4s;

    public void addToE4s(E4 obj) {
        addToManyTarget("e4s", obj, true);
    }

    public void removeFromE4s(E4 obj) {
        removeToManyTarget("e4s", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<E4> getE4s() {
        return (List<E4>)readProperty("e4s");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "e4s":
                return this.e4s;
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
            case "e4s":
                this.e4s = val;
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
        out.writeObject(this.e4s);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.e4s = in.readObject();
    }

}

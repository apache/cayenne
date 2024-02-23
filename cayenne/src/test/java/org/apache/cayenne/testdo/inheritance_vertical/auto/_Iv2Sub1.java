package org.apache.cayenne.testdo.inheritance_vertical.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.inheritance_vertical.Iv2Root;
import org.apache.cayenne.testdo.inheritance_vertical.Iv2Sub1;
import org.apache.cayenne.testdo.inheritance_vertical.Iv2X;

/**
 * Class _Iv2Sub1 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Iv2Sub1 extends Iv2Root {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<Iv2Sub1> SELF = PropertyFactory.createSelf(Iv2Sub1.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "Iv2Sub1", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final EntityProperty<Iv2X> X = PropertyFactory.createEntity("x", Iv2X.class);


    protected Object x;

    public void setX(Iv2X x) {
        setToOneTarget("x", x, true);
    }

    public Iv2X getX() {
        return (Iv2X)readProperty("x");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "x":
                return this.x;
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
            case "x":
                this.x = val;
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
        out.writeObject(this.x);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.x = in.readObject();
    }

}

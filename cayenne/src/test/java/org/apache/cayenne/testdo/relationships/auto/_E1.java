package org.apache.cayenne.testdo.relationships.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.relationships.E1;
import org.apache.cayenne.testdo.relationships.E2;

/**
 * Class _E1 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _E1 extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<E1> SELF = PropertyFactory.createSelf(E1.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("id", "CYCLE_E1", Integer.class);
    public static final String ID_PK_COLUMN = "id";

    public static final StringProperty<String> TEXT = PropertyFactory.createString("text", String.class);
    public static final EntityProperty<E2> E2 = PropertyFactory.createEntity("e2", E2.class);
    public static final ListProperty<E2> E2S = PropertyFactory.createList("e2s", E2.class);

    protected String text;

    protected Object e2;
    protected Object e2s;

    public void setText(String text) {
        beforePropertyWrite("text", this.text, text);
        this.text = text;
    }

    public String getText() {
        beforePropertyRead("text");
        return this.text;
    }

    public void setE2(E2 e2) {
        setToOneTarget("e2", e2, true);
    }

    public E2 getE2() {
        return (E2)readProperty("e2");
    }

    public void addToE2s(E2 obj) {
        addToManyTarget("e2s", obj, true);
    }

    public void removeFromE2s(E2 obj) {
        removeToManyTarget("e2s", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<E2> getE2s() {
        return (List<E2>)readProperty("e2s");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "text":
                return this.text;
            case "e2":
                return this.e2;
            case "e2s":
                return this.e2s;
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
            case "text":
                this.text = (String)val;
                break;
            case "e2":
                this.e2 = val;
                break;
            case "e2s":
                this.e2s = val;
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
        out.writeObject(this.text);
        out.writeObject(this.e2);
        out.writeObject(this.e2s);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.text = (String)in.readObject();
        this.e2 = in.readObject();
        this.e2s = in.readObject();
    }

}

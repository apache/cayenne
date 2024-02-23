package org.apache.cayenne.commitlog.db.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.commitlog.db.Auditable2;
import org.apache.cayenne.commitlog.db.AuditableChild3;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.exp.property.StringProperty;

/**
 * Class _Auditable2 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Auditable2 extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<Auditable2> SELF = PropertyFactory.createSelf(Auditable2.class);

    public static final String ID_PK_COLUMN = "ID";

    public static final StringProperty<String> CHAR_PROPERTY1 = PropertyFactory.createString("charProperty1", String.class);
    public static final StringProperty<String> CHAR_PROPERTY2 = PropertyFactory.createString("charProperty2", String.class);
    public static final ListProperty<AuditableChild3> CHILDREN = PropertyFactory.createList("children", AuditableChild3.class);

    protected String charProperty1;
    protected String charProperty2;

    protected Object children;

    public void setCharProperty1(String charProperty1) {
        beforePropertyWrite("charProperty1", this.charProperty1, charProperty1);
        this.charProperty1 = charProperty1;
    }

    public String getCharProperty1() {
        beforePropertyRead("charProperty1");
        return this.charProperty1;
    }

    public void setCharProperty2(String charProperty2) {
        beforePropertyWrite("charProperty2", this.charProperty2, charProperty2);
        this.charProperty2 = charProperty2;
    }

    public String getCharProperty2() {
        beforePropertyRead("charProperty2");
        return this.charProperty2;
    }

    public void addToChildren(AuditableChild3 obj) {
        addToManyTarget("children", obj, true);
    }

    public void removeFromChildren(AuditableChild3 obj) {
        removeToManyTarget("children", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<AuditableChild3> getChildren() {
        return (List<AuditableChild3>)readProperty("children");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "charProperty1":
                return this.charProperty1;
            case "charProperty2":
                return this.charProperty2;
            case "children":
                return this.children;
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
            case "charProperty1":
                this.charProperty1 = (String)val;
                break;
            case "charProperty2":
                this.charProperty2 = (String)val;
                break;
            case "children":
                this.children = val;
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
        out.writeObject(this.charProperty1);
        out.writeObject(this.charProperty2);
        out.writeObject(this.children);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.charProperty1 = (String)in.readObject();
        this.charProperty2 = (String)in.readObject();
        this.children = in.readObject();
    }

}

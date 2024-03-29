package org.apache.cayenne.testdo.mt.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.testdo.mt.MtTable2;
import org.apache.cayenne.testdo.mt.MtTable3;

/**
 * Class _MtTable2 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _MtTable2 extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<MtTable2> SELF = PropertyFactory.createSelf(MtTable2.class);

    public static final NumericIdProperty<Integer> TABLE2_ID_PK_PROPERTY = PropertyFactory.createNumericId("TABLE2_ID", "MtTable2", Integer.class);
    public static final String TABLE2_ID_PK_COLUMN = "TABLE2_ID";

    public static final StringProperty<String> GLOBAL_ATTRIBUTE = PropertyFactory.createString("globalAttribute", String.class);
    public static final EntityProperty<MtTable1> TABLE1 = PropertyFactory.createEntity("table1", MtTable1.class);
    public static final EntityProperty<MtTable3> TABLE3 = PropertyFactory.createEntity("table3", MtTable3.class);

    protected String globalAttribute;

    protected Object table1;
    protected Object table3;

    public void setGlobalAttribute(String globalAttribute) {
        beforePropertyWrite("globalAttribute", this.globalAttribute, globalAttribute);
        this.globalAttribute = globalAttribute;
    }

    public String getGlobalAttribute() {
        beforePropertyRead("globalAttribute");
        return this.globalAttribute;
    }

    public void setTable1(MtTable1 table1) {
        setToOneTarget("table1", table1, true);
    }

    public MtTable1 getTable1() {
        return (MtTable1)readProperty("table1");
    }

    public void setTable3(MtTable3 table3) {
        setToOneTarget("table3", table3, true);
    }

    public MtTable3 getTable3() {
        return (MtTable3)readProperty("table3");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "globalAttribute":
                return this.globalAttribute;
            case "table1":
                return this.table1;
            case "table3":
                return this.table3;
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
            case "globalAttribute":
                this.globalAttribute = (String)val;
                break;
            case "table1":
                this.table1 = val;
                break;
            case "table3":
                this.table3 = val;
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
        out.writeObject(this.globalAttribute);
        out.writeObject(this.table1);
        out.writeObject(this.table3);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.globalAttribute = (String)in.readObject();
        this.table1 = in.readObject();
        this.table3 = in.readObject();
    }

}

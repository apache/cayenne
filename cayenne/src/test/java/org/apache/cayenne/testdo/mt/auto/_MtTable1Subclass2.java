package org.apache.cayenne.testdo.mt.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.testdo.mt.MtTable1Subclass2;

/**
 * Class _MtTable1Subclass2 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _MtTable1Subclass2 extends MtTable1 {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<MtTable1Subclass2> SELF = PropertyFactory.createSelf(MtTable1Subclass2.class);

    public static final NumericIdProperty<Integer> TABLE1_ID_PK_PROPERTY = PropertyFactory.createNumericId("TABLE1_ID", "MtTable1Subclass2", Integer.class);
    public static final String TABLE1_ID_PK_COLUMN = "TABLE1_ID";

    public static final StringProperty<String> SUBCLASS2ATTRIBUTE1 = PropertyFactory.createString("subclass2Attribute1", String.class);

    protected String subclass2Attribute1;


    public void setSubclass2Attribute1(String subclass2Attribute1) {
        beforePropertyWrite("subclass2Attribute1", this.subclass2Attribute1, subclass2Attribute1);
        this.subclass2Attribute1 = subclass2Attribute1;
    }

    public String getSubclass2Attribute1() {
        beforePropertyRead("subclass2Attribute1");
        return this.subclass2Attribute1;
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "subclass2Attribute1":
                return this.subclass2Attribute1;
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
            case "subclass2Attribute1":
                this.subclass2Attribute1 = (String)val;
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
        out.writeObject(this.subclass2Attribute1);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.subclass2Attribute1 = (String)in.readObject();
    }

}

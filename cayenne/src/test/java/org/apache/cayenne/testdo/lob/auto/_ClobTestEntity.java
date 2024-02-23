package org.apache.cayenne.testdo.lob.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.lob.ClobTestEntity;
import org.apache.cayenne.testdo.lob.ClobTestRelation;

/**
 * Class _ClobTestEntity was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _ClobTestEntity extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<ClobTestEntity> SELF = PropertyFactory.createSelf(ClobTestEntity.class);

    public static final NumericIdProperty<Integer> CLOB_TEST_ID_PK_PROPERTY = PropertyFactory.createNumericId("CLOB_TEST_ID", "ClobTestEntity", Integer.class);
    public static final String CLOB_TEST_ID_PK_COLUMN = "CLOB_TEST_ID";

    public static final StringProperty<String> CLOB_COL = PropertyFactory.createString("clobCol", String.class);
    public static final ListProperty<ClobTestRelation> CLOB_VALUE = PropertyFactory.createList("clobValue", ClobTestRelation.class);

    protected String clobCol;

    protected Object clobValue;

    public void setClobCol(String clobCol) {
        beforePropertyWrite("clobCol", this.clobCol, clobCol);
        this.clobCol = clobCol;
    }

    public String getClobCol() {
        beforePropertyRead("clobCol");
        return this.clobCol;
    }

    public void addToClobValue(ClobTestRelation obj) {
        addToManyTarget("clobValue", obj, true);
    }

    public void removeFromClobValue(ClobTestRelation obj) {
        removeToManyTarget("clobValue", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<ClobTestRelation> getClobValue() {
        return (List<ClobTestRelation>)readProperty("clobValue");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "clobCol":
                return this.clobCol;
            case "clobValue":
                return this.clobValue;
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
            case "clobCol":
                this.clobCol = (String)val;
                break;
            case "clobValue":
                this.clobValue = val;
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
        out.writeObject(this.clobCol);
        out.writeObject(this.clobValue);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.clobCol = (String)in.readObject();
        this.clobValue = in.readObject();
    }

}

package org.apache.cayenne.crypto.db.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.crypto.db.Table1;
import org.apache.cayenne.crypto.db.Table7;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.exp.property.StringProperty;

/**
 * Class _Table7 was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Table7 extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<Table7> SELF = PropertyFactory.createSelf(Table7.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "Table7", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final NumericProperty<Integer> CRYPTO_INT = PropertyFactory.createNumeric("cryptoInt", Integer.class);
    public static final StringProperty<String> CRYPTO_STRING = PropertyFactory.createString("cryptoString", String.class);
    public static final EntityProperty<Table1> TO_TABLE1 = PropertyFactory.createEntity("toTable1", Table1.class);

    protected Integer cryptoInt;
    protected String cryptoString;

    protected Object toTable1;

    public void setCryptoInt(int cryptoInt) {
        beforePropertyWrite("cryptoInt", this.cryptoInt, cryptoInt);
        this.cryptoInt = cryptoInt;
    }

    public int getCryptoInt() {
        beforePropertyRead("cryptoInt");
        if(this.cryptoInt == null) {
            return 0;
        }
        return this.cryptoInt;
    }

    public void setCryptoString(String cryptoString) {
        beforePropertyWrite("cryptoString", this.cryptoString, cryptoString);
        this.cryptoString = cryptoString;
    }

    public String getCryptoString() {
        beforePropertyRead("cryptoString");
        return this.cryptoString;
    }

    public void setToTable1(Table1 toTable1) {
        setToOneTarget("toTable1", toTable1, true);
    }

    public Table1 getToTable1() {
        return (Table1)readProperty("toTable1");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "cryptoInt":
                return this.cryptoInt;
            case "cryptoString":
                return this.cryptoString;
            case "toTable1":
                return this.toTable1;
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
            case "cryptoInt":
                this.cryptoInt = (Integer)val;
                break;
            case "cryptoString":
                this.cryptoString = (String)val;
                break;
            case "toTable1":
                this.toTable1 = val;
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
        out.writeObject(this.cryptoInt);
        out.writeObject(this.cryptoString);
        out.writeObject(this.toTable1);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.cryptoInt = (Integer)in.readObject();
        this.cryptoString = (String)in.readObject();
        this.toTable1 = in.readObject();
    }

}

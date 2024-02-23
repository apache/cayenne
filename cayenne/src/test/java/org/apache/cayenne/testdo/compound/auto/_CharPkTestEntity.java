package org.apache.cayenne.testdo.compound.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.compound.CharFkTestEntity;
import org.apache.cayenne.testdo.compound.CharPkTestEntity;

/**
 * Class _CharPkTestEntity was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _CharPkTestEntity extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<CharPkTestEntity> SELF = PropertyFactory.createSelf(CharPkTestEntity.class);

    public static final String PK_COL_PK_COLUMN = "PK_COL";

    public static final StringProperty<String> OTHER_COL = PropertyFactory.createString("otherCol", String.class);
    public static final StringProperty<String> PK_COL = PropertyFactory.createString("pkCol", String.class);
    public static final ListProperty<CharFkTestEntity> CHAR_FKS = PropertyFactory.createList("charFKs", CharFkTestEntity.class);

    protected String otherCol;
    protected String pkCol;

    protected Object charFKs;

    public void setOtherCol(String otherCol) {
        beforePropertyWrite("otherCol", this.otherCol, otherCol);
        this.otherCol = otherCol;
    }

    public String getOtherCol() {
        beforePropertyRead("otherCol");
        return this.otherCol;
    }

    public void setPkCol(String pkCol) {
        beforePropertyWrite("pkCol", this.pkCol, pkCol);
        this.pkCol = pkCol;
    }

    public String getPkCol() {
        beforePropertyRead("pkCol");
        return this.pkCol;
    }

    public void addToCharFKs(CharFkTestEntity obj) {
        addToManyTarget("charFKs", obj, true);
    }

    public void removeFromCharFKs(CharFkTestEntity obj) {
        removeToManyTarget("charFKs", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<CharFkTestEntity> getCharFKs() {
        return (List<CharFkTestEntity>)readProperty("charFKs");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "otherCol":
                return this.otherCol;
            case "pkCol":
                return this.pkCol;
            case "charFKs":
                return this.charFKs;
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
            case "otherCol":
                this.otherCol = (String)val;
                break;
            case "pkCol":
                this.pkCol = (String)val;
                break;
            case "charFKs":
                this.charFKs = val;
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
        out.writeObject(this.otherCol);
        out.writeObject(this.pkCol);
        out.writeObject(this.charFKs);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.otherCol = (String)in.readObject();
        this.pkCol = (String)in.readObject();
        this.charFKs = in.readObject();
    }

}

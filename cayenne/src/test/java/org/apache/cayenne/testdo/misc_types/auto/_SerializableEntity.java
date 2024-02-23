package org.apache.cayenne.testdo.misc_types.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.MockSerializable;
import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.misc_types.SerializableEntity;

/**
 * Class _SerializableEntity was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _SerializableEntity extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<SerializableEntity> SELF = PropertyFactory.createSelf(SerializableEntity.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "SerializableEntity", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final BaseProperty<MockSerializable> SERIALIZABLE_FIELD = PropertyFactory.createBase("serializableField", MockSerializable.class);

    protected MockSerializable serializableField;


    public void setSerializableField(MockSerializable serializableField) {
        beforePropertyWrite("serializableField", this.serializableField, serializableField);
        this.serializableField = serializableField;
    }

    public MockSerializable getSerializableField() {
        beforePropertyRead("serializableField");
        return this.serializableField;
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "serializableField":
                return this.serializableField;
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
            case "serializableField":
                this.serializableField = (MockSerializable)val;
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
        out.writeObject(this.serializableField);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.serializableField = (MockSerializable)in.readObject();
    }

}

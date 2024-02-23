package org.apache.cayenne.testdo.inheritance_vertical.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.inheritance_vertical.Iv1Root;

/**
 * Class _Iv1Root was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Iv1Root extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<Iv1Root> SELF = PropertyFactory.createSelf(Iv1Root.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "Iv1Root", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final StringProperty<String> DISCRIMINATOR = PropertyFactory.createString("discriminator", String.class);
    public static final StringProperty<String> NAME = PropertyFactory.createString("name", String.class);

    protected String discriminator;
    protected String name;


    public void setDiscriminator(String discriminator) {
        beforePropertyWrite("discriminator", this.discriminator, discriminator);
        this.discriminator = discriminator;
    }

    public String getDiscriminator() {
        beforePropertyRead("discriminator");
        return this.discriminator;
    }

    public void setName(String name) {
        beforePropertyWrite("name", this.name, name);
        this.name = name;
    }

    public String getName() {
        beforePropertyRead("name");
        return this.name;
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "discriminator":
                return this.discriminator;
            case "name":
                return this.name;
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
            case "discriminator":
                this.discriminator = (String)val;
                break;
            case "name":
                this.name = (String)val;
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
        out.writeObject(this.discriminator);
        out.writeObject(this.name);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.discriminator = (String)in.readObject();
        this.name = (String)in.readObject();
    }

}

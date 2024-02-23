package org.apache.cayenne.testdo.mixed_persistence_strategy.auto;

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
import org.apache.cayenne.testdo.mixed_persistence_strategy.MixedPersistenceStrategy;
import org.apache.cayenne.testdo.mixed_persistence_strategy.MixedPersistenceStrategy2;

/**
 * Class _MixedPersistenceStrategy was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _MixedPersistenceStrategy extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<MixedPersistenceStrategy> SELF = PropertyFactory.createSelf(MixedPersistenceStrategy.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "MixedPersistenceStrategy", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final StringProperty<String> DESCRIPTION = PropertyFactory.createString("description", String.class);
    public static final StringProperty<String> NAME = PropertyFactory.createString("name", String.class);
    public static final ListProperty<MixedPersistenceStrategy2> DETAILS = PropertyFactory.createList("details", MixedPersistenceStrategy2.class);

    protected String description;
    protected String name;

    protected Object details;

    public void setDescription(String description) {
        beforePropertyWrite("description", this.description, description);
        this.description = description;
    }

    public String getDescription() {
        beforePropertyRead("description");
        return this.description;
    }

    public void setName(String name) {
        beforePropertyWrite("name", this.name, name);
        this.name = name;
    }

    public String getName() {
        beforePropertyRead("name");
        return this.name;
    }

    public void addToDetails(MixedPersistenceStrategy2 obj) {
        addToManyTarget("details", obj, true);
    }

    public void removeFromDetails(MixedPersistenceStrategy2 obj) {
        removeToManyTarget("details", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<MixedPersistenceStrategy2> getDetails() {
        return (List<MixedPersistenceStrategy2>)readProperty("details");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "description":
                return this.description;
            case "name":
                return this.name;
            case "details":
                return this.details;
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
            case "description":
                this.description = (String)val;
                break;
            case "name":
                this.name = (String)val;
                break;
            case "details":
                this.details = val;
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
        out.writeObject(this.description);
        out.writeObject(this.name);
        out.writeObject(this.details);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.description = (String)in.readObject();
        this.name = (String)in.readObject();
        this.details = in.readObject();
    }

}

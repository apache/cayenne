package org.apache.cayenne.testdo.map_to_many.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.map_to_many.IdMapToMany;
import org.apache.cayenne.testdo.map_to_many.IdMapToManyTarget;

/**
 * Class _IdMapToManyTarget was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _IdMapToManyTarget extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<IdMapToManyTarget> SELF = PropertyFactory.createSelf(IdMapToManyTarget.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "IdMapToManyTarget", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final EntityProperty<IdMapToMany> MAP_TO_MANY = PropertyFactory.createEntity("mapToMany", IdMapToMany.class);


    protected Object mapToMany;

    public void setMapToMany(IdMapToMany mapToMany) {
        setToOneTarget("mapToMany", mapToMany, true);
    }

    public IdMapToMany getMapToMany() {
        return (IdMapToMany)readProperty("mapToMany");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "mapToMany":
                return this.mapToMany;
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
            case "mapToMany":
                this.mapToMany = val;
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
        out.writeObject(this.mapToMany);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.mapToMany = in.readObject();
    }

}

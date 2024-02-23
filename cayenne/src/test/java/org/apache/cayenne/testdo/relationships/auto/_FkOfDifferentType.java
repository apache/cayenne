package org.apache.cayenne.testdo.relationships.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.relationships.FkOfDifferentType;
import org.apache.cayenne.testdo.relationships.RelationshipHelper;

/**
 * Class _FkOfDifferentType was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _FkOfDifferentType extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<FkOfDifferentType> SELF = PropertyFactory.createSelf(FkOfDifferentType.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "FkOfDifferentType", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final EntityProperty<RelationshipHelper> RELATIONSHIP_HELPER = PropertyFactory.createEntity("relationshipHelper", RelationshipHelper.class);


    protected Object relationshipHelper;

    public void setRelationshipHelper(RelationshipHelper relationshipHelper) {
        setToOneTarget("relationshipHelper", relationshipHelper, true);
    }

    public RelationshipHelper getRelationshipHelper() {
        return (RelationshipHelper)readProperty("relationshipHelper");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "relationshipHelper":
                return this.relationshipHelper;
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
            case "relationshipHelper":
                this.relationshipHelper = val;
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
        out.writeObject(this.relationshipHelper);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.relationshipHelper = in.readObject();
    }

}

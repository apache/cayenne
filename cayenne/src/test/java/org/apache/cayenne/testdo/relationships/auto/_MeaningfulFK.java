package org.apache.cayenne.testdo.relationships.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.relationships.MeaningfulFK;
import org.apache.cayenne.testdo.relationships.RelationshipHelper;

/**
 * Class _MeaningfulFK was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _MeaningfulFK extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<MeaningfulFK> SELF = PropertyFactory.createSelf(MeaningfulFK.class);

    public static final NumericIdProperty<Integer> MEANIGNFUL_FK_ID_PK_PROPERTY = PropertyFactory.createNumericId("MEANIGNFUL_FK_ID", "MeaningfulFK", Integer.class);
    public static final String MEANIGNFUL_FK_ID_PK_COLUMN = "MEANIGNFUL_FK_ID";

    public static final NumericProperty<Integer> RELATIONSHIP_HELPER_ID = PropertyFactory.createNumeric("relationshipHelperID", Integer.class);
    public static final EntityProperty<RelationshipHelper> TO_RELATIONSHIP_HELPER = PropertyFactory.createEntity("toRelationshipHelper", RelationshipHelper.class);

    protected int relationshipHelperID;

    protected Object toRelationshipHelper;

    public void setRelationshipHelperID(int relationshipHelperID) {
        beforePropertyWrite("relationshipHelperID", this.relationshipHelperID, relationshipHelperID);
        this.relationshipHelperID = relationshipHelperID;
    }

    public int getRelationshipHelperID() {
        beforePropertyRead("relationshipHelperID");
        return this.relationshipHelperID;
    }

    public void setToRelationshipHelper(RelationshipHelper toRelationshipHelper) {
        setToOneTarget("toRelationshipHelper", toRelationshipHelper, true);
    }

    public RelationshipHelper getToRelationshipHelper() {
        return (RelationshipHelper)readProperty("toRelationshipHelper");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "relationshipHelperID":
                return this.relationshipHelperID;
            case "toRelationshipHelper":
                return this.toRelationshipHelper;
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
            case "relationshipHelperID":
                this.relationshipHelperID = val == null ? 0 : (int)val;
                break;
            case "toRelationshipHelper":
                this.toRelationshipHelper = val;
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
        out.writeInt(this.relationshipHelperID);
        out.writeObject(this.toRelationshipHelper);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.relationshipHelperID = in.readInt();
        this.toRelationshipHelper = in.readObject();
    }

}

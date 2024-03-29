package org.apache.cayenne.testdo.relationships.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.relationships.ReflexiveAndToOne;
import org.apache.cayenne.testdo.relationships.RelationshipHelper;

/**
 * Class _ReflexiveAndToOne was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _ReflexiveAndToOne extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<ReflexiveAndToOne> SELF = PropertyFactory.createSelf(ReflexiveAndToOne.class);

    public static final NumericIdProperty<Integer> REFLEXIVE_AND_TO_ONE_ID_PK_PROPERTY = PropertyFactory.createNumericId("REFLEXIVE_AND_TO_ONE_ID", "ReflexiveAndToOne", Integer.class);
    public static final String REFLEXIVE_AND_TO_ONE_ID_PK_COLUMN = "REFLEXIVE_AND_TO_ONE_ID";

    public static final StringProperty<String> NAME = PropertyFactory.createString("name", String.class);
    public static final ListProperty<ReflexiveAndToOne> CHILDREN = PropertyFactory.createList("children", ReflexiveAndToOne.class);
    public static final EntityProperty<RelationshipHelper> TO_HELPER = PropertyFactory.createEntity("toHelper", RelationshipHelper.class);
    public static final EntityProperty<ReflexiveAndToOne> TO_PARENT = PropertyFactory.createEntity("toParent", ReflexiveAndToOne.class);

    protected String name;

    protected Object children;
    protected Object toHelper;
    protected Object toParent;

    public void setName(String name) {
        beforePropertyWrite("name", this.name, name);
        this.name = name;
    }

    public String getName() {
        beforePropertyRead("name");
        return this.name;
    }

    public void addToChildren(ReflexiveAndToOne obj) {
        addToManyTarget("children", obj, true);
    }

    public void removeFromChildren(ReflexiveAndToOne obj) {
        removeToManyTarget("children", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<ReflexiveAndToOne> getChildren() {
        return (List<ReflexiveAndToOne>)readProperty("children");
    }

    public void setToHelper(RelationshipHelper toHelper) {
        setToOneTarget("toHelper", toHelper, true);
    }

    public RelationshipHelper getToHelper() {
        return (RelationshipHelper)readProperty("toHelper");
    }

    public void setToParent(ReflexiveAndToOne toParent) {
        setToOneTarget("toParent", toParent, true);
    }

    public ReflexiveAndToOne getToParent() {
        return (ReflexiveAndToOne)readProperty("toParent");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "name":
                return this.name;
            case "children":
                return this.children;
            case "toHelper":
                return this.toHelper;
            case "toParent":
                return this.toParent;
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
            case "name":
                this.name = (String)val;
                break;
            case "children":
                this.children = val;
                break;
            case "toHelper":
                this.toHelper = val;
                break;
            case "toParent":
                this.toParent = val;
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
        out.writeObject(this.name);
        out.writeObject(this.children);
        out.writeObject(this.toHelper);
        out.writeObject(this.toParent);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.name = (String)in.readObject();
        this.children = in.readObject();
        this.toHelper = in.readObject();
        this.toParent = in.readObject();
    }

}

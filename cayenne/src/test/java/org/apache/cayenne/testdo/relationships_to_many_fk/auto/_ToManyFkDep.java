package org.apache.cayenne.testdo.relationships_to_many_fk.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.testdo.relationships_to_many_fk.ToManyFkDep;
import org.apache.cayenne.testdo.relationships_to_many_fk.ToManyFkRoot;
import org.apache.cayenne.testdo.relationships_to_many_fk.ToManyRoot2;

/**
 * Class _ToManyFkDep was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _ToManyFkDep extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<ToManyFkDep> SELF = PropertyFactory.createSelf(ToManyFkDep.class);

    public static final String DEP_ID_PK_COLUMN = "DEP_ID";
    public static final NumericIdProperty<Integer> OTHER_ID_PK_PROPERTY = PropertyFactory.createNumericId("OTHER_ID", "ToManyFkDep", Integer.class);
    public static final String OTHER_ID_PK_COLUMN = "OTHER_ID";

    public static final NumericProperty<Integer> DEP_ID = PropertyFactory.createNumeric("depId", Integer.class);
    public static final StringProperty<String> NAME = PropertyFactory.createString("name", String.class);
    public static final EntityProperty<ToManyFkRoot> ROOT = PropertyFactory.createEntity("root", ToManyFkRoot.class);
    public static final EntityProperty<ToManyRoot2> ROOT2 = PropertyFactory.createEntity("root2", ToManyRoot2.class);

    protected Integer depId;
    protected String name;

    protected Object root;
    protected Object root2;

    public void setDepId(Integer depId) {
        beforePropertyWrite("depId", this.depId, depId);
        this.depId = depId;
    }

    public Integer getDepId() {
        beforePropertyRead("depId");
        return this.depId;
    }

    public void setName(String name) {
        beforePropertyWrite("name", this.name, name);
        this.name = name;
    }

    public String getName() {
        beforePropertyRead("name");
        return this.name;
    }

    public void setRoot(ToManyFkRoot root) {
        setToOneTarget("root", root, true);
    }

    public ToManyFkRoot getRoot() {
        return (ToManyFkRoot)readProperty("root");
    }

    public void setRoot2(ToManyRoot2 root2) {
        setToOneTarget("root2", root2, true);
    }

    public ToManyRoot2 getRoot2() {
        return (ToManyRoot2)readProperty("root2");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "depId":
                return this.depId;
            case "name":
                return this.name;
            case "root":
                return this.root;
            case "root2":
                return this.root2;
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
            case "depId":
                this.depId = (Integer)val;
                break;
            case "name":
                this.name = (String)val;
                break;
            case "root":
                this.root = val;
                break;
            case "root2":
                this.root2 = val;
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
        out.writeObject(this.depId);
        out.writeObject(this.name);
        out.writeObject(this.root);
        out.writeObject(this.root2);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.depId = (Integer)in.readObject();
        this.name = (String)in.readObject();
        this.root = in.readObject();
        this.root2 = in.readObject();
    }

}

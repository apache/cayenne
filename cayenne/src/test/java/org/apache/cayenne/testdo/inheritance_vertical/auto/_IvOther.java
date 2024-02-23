package org.apache.cayenne.testdo.inheritance_vertical.auto;

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
import org.apache.cayenne.testdo.inheritance_vertical.IvBase;
import org.apache.cayenne.testdo.inheritance_vertical.IvImpl;
import org.apache.cayenne.testdo.inheritance_vertical.IvImplWithLock;
import org.apache.cayenne.testdo.inheritance_vertical.IvOther;

/**
 * Class _IvOther was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _IvOther extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<IvOther> SELF = PropertyFactory.createSelf(IvOther.class);

    public static final NumericIdProperty<Integer> ID_PK_PROPERTY = PropertyFactory.createNumericId("ID", "IvOther", Integer.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final StringProperty<String> NAME = PropertyFactory.createString("name", String.class);
    public static final EntityProperty<IvBase> BASE = PropertyFactory.createEntity("base", IvBase.class);
    public static final ListProperty<IvImpl> IMPLS = PropertyFactory.createList("impls", IvImpl.class);
    public static final ListProperty<IvImplWithLock> IMPLS_WITH_LOCK = PropertyFactory.createList("implsWithLock", IvImplWithLock.class);

    protected String name;

    protected Object base;
    protected Object impls;
    protected Object implsWithLock;

    public void setName(String name) {
        beforePropertyWrite("name", this.name, name);
        this.name = name;
    }

    public String getName() {
        beforePropertyRead("name");
        return this.name;
    }

    public void setBase(IvBase base) {
        setToOneTarget("base", base, true);
    }

    public IvBase getBase() {
        return (IvBase)readProperty("base");
    }

    public void addToImpls(IvImpl obj) {
        addToManyTarget("impls", obj, true);
    }

    public void removeFromImpls(IvImpl obj) {
        removeToManyTarget("impls", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<IvImpl> getImpls() {
        return (List<IvImpl>)readProperty("impls");
    }

    public void addToImplsWithLock(IvImplWithLock obj) {
        addToManyTarget("implsWithLock", obj, true);
    }

    public void removeFromImplsWithLock(IvImplWithLock obj) {
        removeToManyTarget("implsWithLock", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<IvImplWithLock> getImplsWithLock() {
        return (List<IvImplWithLock>)readProperty("implsWithLock");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "name":
                return this.name;
            case "base":
                return this.base;
            case "impls":
                return this.impls;
            case "implsWithLock":
                return this.implsWithLock;
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
            case "base":
                this.base = val;
                break;
            case "impls":
                this.impls = val;
                break;
            case "implsWithLock":
                this.implsWithLock = val;
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
        out.writeObject(this.base);
        out.writeObject(this.impls);
        out.writeObject(this.implsWithLock);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.name = (String)in.readObject();
        this.base = in.readObject();
        this.impls = in.readObject();
        this.implsWithLock = in.readObject();
    }

}

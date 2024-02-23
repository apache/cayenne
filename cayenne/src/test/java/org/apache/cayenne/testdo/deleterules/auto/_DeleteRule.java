package org.apache.cayenne.testdo.deleterules.auto;

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
import org.apache.cayenne.testdo.deleterules.DeleteCascade;
import org.apache.cayenne.testdo.deleterules.DeleteDeny;
import org.apache.cayenne.testdo.deleterules.DeleteNullify;
import org.apache.cayenne.testdo.deleterules.DeleteRule;

/**
 * Class _DeleteRule was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _DeleteRule extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<DeleteRule> SELF = PropertyFactory.createSelf(DeleteRule.class);

    public static final NumericIdProperty<Integer> DELETE_RULE_ID_PK_PROPERTY = PropertyFactory.createNumericId("DELETE_RULE_ID", "DeleteRule", Integer.class);
    public static final String DELETE_RULE_ID_PK_COLUMN = "DELETE_RULE_ID";

    public static final StringProperty<String> NAME = PropertyFactory.createString("name", String.class);
    public static final ListProperty<DeleteCascade> FROM_CASCADE = PropertyFactory.createList("fromCascade", DeleteCascade.class);
    public static final ListProperty<DeleteDeny> FROM_DENY = PropertyFactory.createList("fromDeny", DeleteDeny.class);
    public static final ListProperty<DeleteNullify> FROM_NULLIFY = PropertyFactory.createList("fromNullify", DeleteNullify.class);

    protected String name;

    protected Object fromCascade;
    protected Object fromDeny;
    protected Object fromNullify;

    public void setName(String name) {
        beforePropertyWrite("name", this.name, name);
        this.name = name;
    }

    public String getName() {
        beforePropertyRead("name");
        return this.name;
    }

    public void addToFromCascade(DeleteCascade obj) {
        addToManyTarget("fromCascade", obj, true);
    }

    public void removeFromFromCascade(DeleteCascade obj) {
        removeToManyTarget("fromCascade", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<DeleteCascade> getFromCascade() {
        return (List<DeleteCascade>)readProperty("fromCascade");
    }

    public void addToFromDeny(DeleteDeny obj) {
        addToManyTarget("fromDeny", obj, true);
    }

    public void removeFromFromDeny(DeleteDeny obj) {
        removeToManyTarget("fromDeny", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<DeleteDeny> getFromDeny() {
        return (List<DeleteDeny>)readProperty("fromDeny");
    }

    public void addToFromNullify(DeleteNullify obj) {
        addToManyTarget("fromNullify", obj, true);
    }

    public void removeFromFromNullify(DeleteNullify obj) {
        removeToManyTarget("fromNullify", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<DeleteNullify> getFromNullify() {
        return (List<DeleteNullify>)readProperty("fromNullify");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "name":
                return this.name;
            case "fromCascade":
                return this.fromCascade;
            case "fromDeny":
                return this.fromDeny;
            case "fromNullify":
                return this.fromNullify;
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
            case "fromCascade":
                this.fromCascade = val;
                break;
            case "fromDeny":
                this.fromDeny = val;
                break;
            case "fromNullify":
                this.fromNullify = val;
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
        out.writeObject(this.fromCascade);
        out.writeObject(this.fromDeny);
        out.writeObject(this.fromNullify);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.name = (String)in.readObject();
        this.fromCascade = in.readObject();
        this.fromDeny = in.readObject();
        this.fromNullify = in.readObject();
    }

}

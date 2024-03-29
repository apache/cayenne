package org.apache.cayenne.testdo.relationships_delete_rules.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.relationships_delete_rules.DeleteRuleFlatA;
import org.apache.cayenne.testdo.relationships_delete_rules.DeleteRuleFlatB;

/**
 * Class _DeleteRuleFlatB was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _DeleteRuleFlatB extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<DeleteRuleFlatB> SELF = PropertyFactory.createSelf(DeleteRuleFlatB.class);

    public static final NumericIdProperty<Integer> FLATB_ID_PK_PROPERTY = PropertyFactory.createNumericId("FLATB_ID", "DeleteRuleFlatB", Integer.class);
    public static final String FLATB_ID_PK_COLUMN = "FLATB_ID";

    public static final ListProperty<DeleteRuleFlatA> UNTITLED_REL = PropertyFactory.createList("untitledRel", DeleteRuleFlatA.class);


    protected Object untitledRel;

    public void addToUntitledRel(DeleteRuleFlatA obj) {
        addToManyTarget("untitledRel", obj, true);
    }

    public void removeFromUntitledRel(DeleteRuleFlatA obj) {
        removeToManyTarget("untitledRel", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<DeleteRuleFlatA> getUntitledRel() {
        return (List<DeleteRuleFlatA>)readProperty("untitledRel");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "untitledRel":
                return this.untitledRel;
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
            case "untitledRel":
                this.untitledRel = val;
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
        out.writeObject(this.untitledRel);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.untitledRel = in.readObject();
    }

}

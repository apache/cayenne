package org.apache.cayenne.testdo.cay_2032.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.cay_2032.Team;
import org.apache.cayenne.testdo.cay_2032.Users;

/**
 * Class _Users was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Users extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<Users> SELF = PropertyFactory.createSelf(Users.class);

    public static final NumericIdProperty<Integer> USER_ID_PK_PROPERTY = PropertyFactory.createNumericId("user_id", "Users", Integer.class);
    public static final String USER_ID_PK_COLUMN = "user_id";

    public static final BaseProperty<byte[]> NAME = PropertyFactory.createBase("name", byte[].class);
    public static final ListProperty<Team> USER_TEAMS = PropertyFactory.createList("userTeams", Team.class);

    protected byte[] name;

    protected Object userTeams;

    public void setName(byte[] name) {
        beforePropertyWrite("name", this.name, name);
        this.name = name;
    }

    public byte[] getName() {
        beforePropertyRead("name");
        return this.name;
    }

    public void addToUserTeams(Team obj) {
        addToManyTarget("userTeams", obj, true);
    }

    public void removeFromUserTeams(Team obj) {
        removeToManyTarget("userTeams", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<Team> getUserTeams() {
        return (List<Team>)readProperty("userTeams");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "name":
                return this.name;
            case "userTeams":
                return this.userTeams;
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
                this.name = (byte[])val;
                break;
            case "userTeams":
                this.userTeams = val;
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
        out.writeObject(this.userTeams);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.name = (byte[])in.readObject();
        this.userTeams = in.readObject();
    }

}

package org.apache.cayenne.testdo.cay_2032.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SelfProperty;
import org.apache.cayenne.testdo.cay_2032.Team;
import org.apache.cayenne.testdo.cay_2032.Users;

/**
 * Class _Team was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Team extends PersistentObject {

    private static final long serialVersionUID = 1L;

    public static final SelfProperty<Team> SELF = PropertyFactory.createSelf(Team.class);

    public static final NumericIdProperty<Integer> TEAM_ID_PK_PROPERTY = PropertyFactory.createNumericId("team_id", "Team", Integer.class);
    public static final String TEAM_ID_PK_COLUMN = "team_id";

    public static final ListProperty<Users> TEAM_USERS = PropertyFactory.createList("teamUsers", Users.class);


    protected Object teamUsers;

    public void addToTeamUsers(Users obj) {
        addToManyTarget("teamUsers", obj, true);
    }

    public void removeFromTeamUsers(Users obj) {
        removeToManyTarget("teamUsers", obj, true);
    }

    @SuppressWarnings("unchecked")
    public List<Users> getTeamUsers() {
        return (List<Users>)readProperty("teamUsers");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "teamUsers":
                return this.teamUsers;
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
            case "teamUsers":
                this.teamUsers = val;
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
        out.writeObject(this.teamUsers);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.teamUsers = in.readObject();
    }

}

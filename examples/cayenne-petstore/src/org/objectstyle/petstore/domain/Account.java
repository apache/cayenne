package org.objectstyle.petstore.domain;

import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.petstore.domain.auto._Account;

public class Account extends _Account {

    public void setPersistenceState(int state) {

        // create owned objects
        if (state == PersistenceState.NEW) {
            Profile profile = (Profile) getDataContext().createAndRegisterNewObject(
                    Profile.class);
            profile.setAccount(this);

            User user = (User) getDataContext().createAndRegisterNewObject(User.class);
            user.setAccount(this);
        }

        super.setPersistenceState(state);
    }

}

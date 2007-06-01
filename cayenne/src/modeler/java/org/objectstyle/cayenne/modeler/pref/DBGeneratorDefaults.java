package org.objectstyle.cayenne.modeler.pref;

import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.DbGenerator;
import org.objectstyle.cayenne.dba.DbAdapter;

public class DBGeneratorDefaults extends _DBGeneratorDefaults {

    /**
     * Updates its state to provide reasonable defaults for agiven adapter.
     */
    public void adjustForAdapter(DbAdapter adapter) {
        if (!adapter.supportsFkConstraints()
                && booleanForBooleanProperty(CREATE_FK_PROPERTY)) {
            setCreateFK(Boolean.FALSE);
        }
    }

    /**
     * Updates DbGenerator settings, consulting its own state.
     */
    public void configureGenerator(DbGenerator generator) {
        generator
                .setShouldCreateFKConstraints(booleanForBooleanProperty(CREATE_FK_PROPERTY));
        generator.setShouldCreatePKSupport(booleanForBooleanProperty(CREATE_PK_PROPERTY));
        generator
                .setShouldCreateTables(booleanForBooleanProperty(CREATE_TABLES_PROPERTY));
        generator.setShouldDropPKSupport(booleanForBooleanProperty(DROP_PK_PROPERTY));
        generator.setShouldDropTables(booleanForBooleanProperty(DROP_TABLES_PROPERTY));
    }

    public void setPersistenceState(int persistenceState) {

        // init defaults on insert...
        if (this.persistenceState == PersistenceState.TRANSIENT
                && persistenceState == PersistenceState.NEW) {
            setCreateFK(Boolean.TRUE);
            setCreatePK(Boolean.TRUE);
            setCreateTables(Boolean.TRUE);
            setDropPK(Boolean.FALSE);
            setDropTables(Boolean.FALSE);
        }
        super.setPersistenceState(persistenceState);
    }

    protected boolean booleanForBooleanProperty(String property) {
        Boolean b = (Boolean) readProperty(property);
        return (b != null) ? b.booleanValue() : false;
    }
}


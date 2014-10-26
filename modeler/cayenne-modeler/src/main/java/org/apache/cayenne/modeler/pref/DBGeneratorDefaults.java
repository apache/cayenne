/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.modeler.pref;

import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.pref.RenamedPreferences;

import java.util.Collection;
import java.util.prefs.Preferences;

public class DBGeneratorDefaults extends RenamedPreferences {

    public static final String CREATE_FK_PROPERTY = "createFK";
    public static final String CREATE_PK_PROPERTY = "createPK";
    public static final String CREATE_TABLES_PROPERTY = "createTables";
    public static final String DROP_PK_PROPERTY = "dropPK";
    public static final String DROP_TABLES_PROPERTY = "dropTables";

    public boolean createFK;
    public boolean createPK;
    public boolean createTables;
    public boolean dropPK;
    public boolean dropTables;

    public DBGeneratorDefaults(Preferences pref) {
        super(pref);
        this.createFK = getCurrentPreference().getBoolean(CREATE_FK_PROPERTY, true);
        this.createPK = getCurrentPreference().getBoolean(CREATE_PK_PROPERTY, true);
        this.createTables = getCurrentPreference().getBoolean(
                CREATE_TABLES_PROPERTY,
                true);
        this.dropPK = getCurrentPreference().getBoolean(DROP_PK_PROPERTY, false);
        this.dropTables = getCurrentPreference().getBoolean(DROP_TABLES_PROPERTY, false);
    }

    public void setCreateFK(Boolean createFK) {
        this.createFK = createFK;
        getCurrentPreference().putBoolean(CREATE_FK_PROPERTY, createFK);
    }

    public boolean getCreateFK() {
        return createFK;
    }

    public void setCreatePK(Boolean createPK) {
        this.createPK = createPK;
        getCurrentPreference().putBoolean(CREATE_PK_PROPERTY, createPK);
    }

    public boolean getCreatePK() {
        return createPK;
    }

    public void setCreateTables(Boolean createTables) {
        this.createTables = createTables;
        getCurrentPreference().putBoolean(CREATE_TABLES_PROPERTY, createTables);
    }

    public boolean getCreateTables() {
        return createTables;
    }

    public void setDropPK(Boolean dropPK) {
        this.dropPK = dropPK;
        getCurrentPreference().putBoolean(DROP_PK_PROPERTY, dropPK);
    }

    public boolean getDropPK() {
        return dropPK;
    }

    public void setDropTables(Boolean dropTables) {
        this.dropTables = dropTables;
        getCurrentPreference().putBoolean(DROP_TABLES_PROPERTY, dropTables);
    }

    public boolean getDropTables() {
        return dropTables;
    }

    /**
     * Updates DbGenerator settings, consulting its own state.
     */
    public void configureGenerator(Collection<DbGenerator> generators) {
        setCreateFK(createFK);
        setCreatePK(createPK);
        setCreateTables(createTables);
        setDropPK(dropPK);
        setDropTables(dropTables);
        for (DbGenerator generator : generators) {
            generator.setShouldCreateFKConstraints(createFK);
            generator.setShouldCreatePKSupport(createPK);
            generator.setShouldCreateTables(createTables);
            generator.setShouldDropPKSupport(dropPK);
            generator.setShouldDropTables(dropTables);
        }
    }

    /**
     * An initialization callback.
     */
    public void prePersist() {
        setCreateFK(Boolean.TRUE);
        setCreatePK(Boolean.TRUE);
        setCreateTables(Boolean.TRUE);
        setDropPK(Boolean.FALSE);
        setDropTables(Boolean.FALSE);
    }
}

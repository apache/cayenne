/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.pref.adapters;

import org.apache.cayenne.modeler.pref.dbconnector.DBConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataMapPrefsTest {

    private final Preferences root = Preferences.userRoot()
            .node("cayenne-test/" + UUID.randomUUID().toString().replace("-", ""));
    private final DataMapPrefs prefs = new DataMapPrefs(root);

    @AfterEach
    public void cleanup() throws BackingStoreException {
        root.removeNode();
    }

    // --- superclassPackage ---

    @Test
    public void setSuperclassPackageStoresValue() {
        prefs.setSuperclassPackage("com.example");
        assertEquals("com.example", root.get(DataMapPrefs.SUPERCLASS_PACKAGE_PROPERTY, null));
    }

    @Test
    public void setSuperclassPackageNullRemovesKey() {
        prefs.setSuperclassPackage("com.example");
        prefs.setSuperclassPackage((String) null);
        assertNull(root.get(DataMapPrefs.SUPERCLASS_PACKAGE_PROPERTY, null));
    }

    @Test
    public void setSuperclassPackagePrefixSuffixJoinsWithDot() {
        prefs.setSuperclassPackage("com.example", "auto");
        assertEquals("com.example.auto", root.get(DataMapPrefs.SUPERCLASS_PACKAGE_PROPERTY, null));
    }

    @Test
    public void setSuperclassPackagePrefixOnlyNoTrailingDot() {
        prefs.setSuperclassPackage("com.example", null);
        assertEquals("com.example", root.get(DataMapPrefs.SUPERCLASS_PACKAGE_PROPERTY, null));
    }

    @Test
    public void setSuperclassPackageSuffixOnlyNoLeadingDot() {
        prefs.setSuperclassPackage(null, "auto");
        assertEquals("auto", root.get(DataMapPrefs.SUPERCLASS_PACKAGE_PROPERTY, null));
    }

    @Test
    public void setSuperclassPackagePrefixTrailingDotIsNormalized() {
        prefs.setSuperclassPackage("com.example.", "auto");
        assertEquals("com.example.auto", root.get(DataMapPrefs.SUPERCLASS_PACKAGE_PROPERTY, null));
    }

    @Test
    public void setSuperclassPackageSuffixLeadingDotIsNormalized() {
        prefs.setSuperclassPackage("com.example", ".auto");
        assertEquals("com.example.auto", root.get(DataMapPrefs.SUPERCLASS_PACKAGE_PROPERTY, null));
    }

    @Test
    public void setSuperclassPackageBothNullProducesEmpty() {
        prefs.setSuperclassPackage((String) null, null);
        assertEquals("", root.get(DataMapPrefs.SUPERCLASS_PACKAGE_PROPERTY, null));
    }

    // --- connector ---

    @Test
    public void getConnectorReturnsNullWhenNoUrlSet() {
        assertNull(prefs.getConnector());
    }

    @Test
    public void getConnectorReturnsConnectorWhenUrlIsSet() {
        root.put(DBConnector.URL_PROPERTY, "jdbc:h2:mem");
        assertNotNull(prefs.getConnector());
    }

    @Test
    public void getConnectorRoundtrip() {
        DBConnector c = new DBConnector();
        c.setUrl("jdbc:h2:mem:test");
        c.setUserName("sa");
        c.setPassword("secret");
        c.setJdbcDriver("org.h2.Driver");
        c.setDbAdapter("org.apache.cayenne.dba.h2.H2Adapter");
        prefs.setConnector(c);

        DBConnector loaded = prefs.getConnector();
        assertNotNull(loaded);
        assertEquals("jdbc:h2:mem:test", loaded.getUrl());
        assertEquals("sa", loaded.getUserName());
        assertEquals("secret", loaded.getPassword());
        assertEquals("org.h2.Driver", loaded.getJdbcDriver());
        assertEquals("org.apache.cayenne.dba.h2.H2Adapter", loaded.getDbAdapter());
    }

    @Test
    public void setConnectorNullFieldsRemoveKeys() {
        DBConnector c = new DBConnector();
        c.setUrl("jdbc:h2:mem");
        c.setUserName("sa");
        prefs.setConnector(c);

        DBConnector sparse = new DBConnector();
        sparse.setUrl("jdbc:h2:mem");
        prefs.setConnector(sparse);

        assertNull(root.get(DBConnector.USER_NAME_PROPERTY, null));
    }

    // --- hasDbAdapter ---

    @Test
    public void hasDbAdapterFalseWhenNotSet() {
        assertFalse(prefs.hasDbAdapter());
    }

    @Test
    public void hasDbAdapterTrueWhenSet() {
        root.put(DBConnector.DB_ADAPTER_PROPERTY, "org.apache.cayenne.dba.h2.H2Adapter");
        assertTrue(prefs.hasDbAdapter());
    }
}

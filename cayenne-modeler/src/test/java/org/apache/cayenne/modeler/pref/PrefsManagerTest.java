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

package org.apache.cayenne.modeler.pref;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.DefaultConfigurationNameMapper;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.resource.URLResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PrefsManagerTest {

    private PrefsManager prefsManager;

    @BeforeEach
    public void createPrefsManager() {
        prefsManager = new PrefsManager(new DefaultConfigurationNameMapper(), new PrefsLocator(new InMemoryPreferences()));
    }

    @Test
    public void unsavedProjectPref_StableUntilReset() {
        Project p = new Project();
        prefsManager.projectPref(p, null).put("k", "v");

        assertEquals("v", prefsManager.projectPref(p, null).get("k", null));

        prefsManager.resetTransientState(p);
        assertNull(prefsManager.projectPref(p, null).get("k", null));
    }

    @Test
    public void resetTransientState_DoesNotAffectOtherUnsavedProjects() {
        Project p1 = new Project();
        DataMap m1 = new DataMap("m1");
        p1.getDataMaps().add(m1);

        Project p2 = new Project();
        DataMap m2 = new DataMap("m2");
        p2.getDataMaps().add(m2);

        prefsManager.projectPref(p1, null).put("k", "p1");
        prefsManager.dataMapPref(m1, null).put("k", "m1");
        prefsManager.projectPref(p2, null).put("k", "p2");
        prefsManager.dataMapPref(m2, null).put("k", "m2");

        prefsManager.resetTransientState(p1);

        assertNull(prefsManager.projectPref(p1, null).get("k", null));
        assertNull(prefsManager.dataMapPref(m1, null).get("k", null));
        assertEquals("p2", prefsManager.projectPref(p2, null).get("k", null));
        assertEquals("m2", prefsManager.dataMapPref(m2, null).get("k", null));
    }

    @Test
    public void resetTransientState_DoesNotAffectOtherProjectStaging() throws MalformedURLException {
        File dir1 = new File("/tmp/prefs-manager-test/p1");
        File dir2 = new File("/tmp/prefs-manager-test/p2");
        File dir2Moved = new File("/tmp/prefs-manager-test/p2-moved");

        Project p1 = savedProject(dir1);
        Project p2 = savedProject(dir2);

        prefsManager.projectPref(p2, null).put("k", "p2");
        String oldId = prefsManager.projectPref(p2, null).absolutePath();

        // "Save As" of p2 is staged, then another project is closed before p2 is committed
        prefsManager.stageProjectMove(p2, dir2Moved);
        prefsManager.resetTransientState(p1);

        p2.setConfigurationSource(new URLResource(new File(dir2Moved, "cayenne-project.xml").toURI().toURL()));
        prefsManager.commitProject(p2);

        assertNotEquals(oldId, prefsManager.projectPref(p2, null).absolutePath());
        assertEquals("p2", prefsManager.projectPref(p2, null).get("k", null));
    }

    private static Project savedProject(File dir) throws MalformedURLException {
        Project p = new Project();
        p.setConfigurationSource(new URLResource(new File(dir, "cayenne-project.xml").toURI().toURL()));
        return p;
    }
}

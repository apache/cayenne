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
package org.apache.cayenne.project;

import org.apache.cayenne.map.DataMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultProjectMergerTest {

    @Test
    public void singleDescriptor() {
        Project project = new Project();
        project.setName("Zx");

        DefaultProjectMerger merger = new DefaultProjectMerger();

        Project merged = merger.merge(project);
        assertSame(project, merged);
        assertEquals("Zx", merged.getName());
    }

    @Test
    public void merged_Name() {
        Project d1 = new Project();
        d1.setName("Zx");

        Project d2 = new Project();
        d2.setName("Ym");

        DefaultProjectMerger merger = new DefaultProjectMerger();

        Project merged = merger.merge(d1, d2);
        assertNotSame(d1, merged);
        assertNotSame(d2, merged);
        assertEquals("Ym", merged.getName());
    }

    @Test
    public void merged_Settings() {
        Project d1 = new Project();
        d1.setSharedCacheEnabled(false);

        Project d2 = new Project();
        d2.setValidatingObjectsOnCommit(false);

        DefaultProjectMerger merger = new DefaultProjectMerger();

        // the settings of the last project win
        Project merged = merger.merge(d1, d2);
        assertTrue(merged.isSharedCacheEnabled());
        assertFalse(merged.isValidatingObjectsOnCommit());
    }

    @Test
    public void merged_DataMaps() {
        Project d1 = new Project();
        d1.setName("Zx");
        DataMap m11 = new DataMap("A");
        DataMap m12 = new DataMap("B");
        d1.getDataMaps().add(m11);
        d1.getDataMaps().add(m12);

        Project d2 = new Project();
        d2.setName("Ym");
        DataMap m21 = new DataMap("C");
        DataMap m22 = new DataMap("A");
        d2.getDataMaps().add(m21);
        d2.getDataMaps().add(m22);

        DefaultProjectMerger merger = new DefaultProjectMerger();

        Project merged = merger.merge(d1, d2);

        assertEquals(3, merged.getDataMaps().size());
        assertSame(m22, merged.getDataMap("A"));
        assertSame(m12, merged.getDataMap("B"));
        assertSame(m21, merged.getDataMap("C"));
    }
}

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
package org.apache.cayenne.modeler.ui.project.editor.query.selectquery;

import org.apache.cayenne.modeler.ui.project.editor.query.selectquery.ObjectSelectView.RootRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ObjectSelectViewTest {

    @Test
    public void rootRefObjEntity() {
        assertEquals(new RootRef("Artist", false), ObjectSelectView.rootRef("from Artist"));
        assertEquals(new RootRef("Artist", false), ObjectSelectView.rootRef("from Artist where"));
        assertEquals(new RootRef("Artist", false), ObjectSelectView.rootRef("select a, b\nfrom  Artist"));
    }

    @Test
    public void rootRefDbEntity() {
        assertEquals(new RootRef("ARTIST", true), ObjectSelectView.rootRef("from db:ARTIST where"));
        assertNull(ObjectSelectView.rootRef("from db:"));
    }

    @Test
    public void rootRefNoName() {
        assertNull(ObjectSelectView.rootRef(null));
        assertNull(ObjectSelectView.rootRef(""));
        assertNull(ObjectSelectView.rootRef("from"));
        assertNull(ObjectSelectView.rootRef("from \n"));
        assertNull(ObjectSelectView.rootRef("select a where"));
    }

    @Test
    public void rootRefNotAKeyword() {
        assertNull(ObjectSelectView.rootRef("select a.from where name = 'from X'"));
        assertNull(ObjectSelectView.rootRef("select fromDate"));
        assertEquals(new RootRef("Painting", false),
                ObjectSelectView.rootRef("select \"from Artist\" from Painting"));
    }
}

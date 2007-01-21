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

package org.apache.cayenne.access;

import org.apache.art.oneway.Gallery;
import org.apache.art.oneway.Painting;
import org.apache.cayenne.unit.OneWayMappingCase;

public class DeleteRulesOneWayTest extends OneWayMappingCase {
    private DataContext context;

    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = getDomain().createDataContext();
    }

    public void testNullifyToOne() {
        Painting aPainting = (Painting) context.newObject("Painting");
        aPainting.setPaintingTitle("A Title");

        Gallery aGallery = (Gallery) context.newObject("Gallery");
        aGallery.setGalleryName("Gallery Name");

        aPainting.setToGallery(aGallery);
        context.commitChanges();

        try {
            context.deleteObject(aPainting);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }
        // There's no reverse relationship, so there's nothing else to test
        // except to be sure that the commit works
        context.commitChanges();
    }
}

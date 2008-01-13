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

package org.apache.cayenne;

import org.apache.art.ArtGroup;

public class CDOReflexiveRelDeleteTest extends CayenneDOTestBase {

    private ArtGroup parentGroup;
    private ArtGroup childGroup1;
    private ArtGroup childGroup2;
    private ArtGroup childGroup3;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ctxt = createDataContext();

        parentGroup = (ArtGroup) ctxt.newObject("ArtGroup");
        parentGroup.setName("parent");

        childGroup1 = (ArtGroup) ctxt.newObject("ArtGroup");
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);

        childGroup2 = (ArtGroup) ctxt.newObject("ArtGroup");
        childGroup2.setName("child2");
        childGroup2.setToParentGroup(parentGroup);

        childGroup3 = (ArtGroup) ctxt.newObject("ArtGroup");
        childGroup3.setName("subchild");
        childGroup3.setToParentGroup(childGroup1);

        ctxt.commitChanges();
    }

    // Test various delete orders. There are more possible literal combinations, but the
    // ones below fairly well
    // encompass the various orders that might be a problem. Add more if additional
    // problems come to light
    public void testReflexiveRelationshipDelete1() {
        ctxt.deleteObject(parentGroup);
        ctxt.deleteObject(childGroup1);
        ctxt.deleteObject(childGroup2);
        ctxt.deleteObject(childGroup3);
        ctxt.commitChanges();
    }

    public void testReflexiveRelationshipDelete2() {
        ctxt.deleteObject(childGroup1);
        ctxt.deleteObject(parentGroup);
        ctxt.deleteObject(childGroup2);
        ctxt.deleteObject(childGroup3);
        ctxt.commitChanges();
    }

    public void testReflexiveRelationshipDelete3() {
        ctxt.deleteObject(childGroup1);
        ctxt.deleteObject(childGroup3);
        ctxt.deleteObject(parentGroup);
        ctxt.deleteObject(childGroup2);
        ctxt.commitChanges();
    }

    public void testReflexiveRelationshipDelete4() {
        ctxt.deleteObject(childGroup3);
        ctxt.deleteObject(parentGroup);
        ctxt.deleteObject(childGroup1);
        ctxt.deleteObject(childGroup2);
        ctxt.commitChanges();
    }

}

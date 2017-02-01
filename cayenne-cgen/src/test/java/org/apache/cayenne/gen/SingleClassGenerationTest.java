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

package org.apache.cayenne.gen;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.velocity.VelocityContext;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SingleClassGenerationTest extends ClassGenerationCase {

    @Test
    public void testNotContainsPropertyImport() throws Exception {
        ObjEntity objEntity = new ObjEntity("TEST1");

        VelocityContext context = new VelocityContext();
        context.put(Artifact.OBJECT_KEY, objEntity);

        String res = renderTemplate(ClassGenerationAction.SINGLE_CLASS_TEMPLATE, context);
        assertFalse(res.contains("org.apache.cayenne.exp.Property"));
    }

    @Test
    public void testContainsPropertyImportForAttributes() throws Exception {
        ObjEntity objEntity = new ObjEntity("TEST1");
        ObjAttribute attr = new ObjAttribute("attr");
        objEntity.addAttribute(attr);

        VelocityContext context = new VelocityContext();
        context.put(Artifact.OBJECT_KEY, objEntity);

        String res = renderTemplate(ClassGenerationAction.SINGLE_CLASS_TEMPLATE, context);
        assertTrue(res.contains("org.apache.cayenne.exp.Property"));
    }

    @Test
    public void testContainsPropertyImportForRelationships() throws Exception {
        ObjEntity objEntity = new ObjEntity("TEST1");
        ObjRelationship rel = new ObjRelationship("rel");
        objEntity.addRelationship(rel);

        VelocityContext context = new VelocityContext();
        context.put(Artifact.OBJECT_KEY, objEntity);

        String res = renderTemplate(ClassGenerationAction.SINGLE_CLASS_TEMPLATE, context);
        assertTrue(res.contains("org.apache.cayenne.exp.Property"));
    }

    @Test
    public void testContainsPropertyImport() throws Exception {
        ObjEntity objEntity = new ObjEntity("TEST1");
        ObjAttribute attr = new ObjAttribute("attr");
        ObjRelationship rel = new ObjRelationship("rel");

        objEntity.addAttribute(attr);
        objEntity.addRelationship(rel);

        VelocityContext context = new VelocityContext();
        context.put(Artifact.OBJECT_KEY, objEntity);

        String res = renderTemplate(ClassGenerationAction.SINGLE_CLASS_TEMPLATE, context);
        assertTrue(res.contains("org.apache.cayenne.exp.Property"));
    }

}

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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * @since 4.2
 */
@UseCayenneRuntime(CayenneProjects.COMPOUND_PROJECT)
public class ObjPathProcessorIT2 extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    private ObjPathProcessor pathProcessor;

    @Before
    public void prepareTranslationContext() {
        TranslatorContext translatorContext = new TranslatorContext(
                new FluentSelectWrapper(ObjectSelect.query(Object.class)),
                Mockito.mock(DbAdapter.class),
                context.getEntityResolver(),
                null
        );
        ObjEntity entity = context.getEntityResolver().getObjEntity("CompoundFkTestEntity");
        pathProcessor = new ObjPathProcessor(translatorContext, entity, null);
    }

    @Test
    public void testSimpleAttributePathTranslation() {
        PathTranslationResult result = pathProcessor.process(CayennePath.of("name"));
        assertEquals(1, result.getDbAttributes().size());
        assertEquals(1, result.getAttributePaths().size());

        assertEquals("", result.getLastAttributePath());
        assertEquals("NAME", result.getLastAttribute().getName());
    }

    @Test
    public void testCompoundRelationshipPathTranslation() {
        PathTranslationResult result = pathProcessor.process(CayennePath.of("toCompoundPk"));
        assertEquals(2, result.getDbAttributes().size());
        assertEquals(2, result.getAttributePaths().size());

        assertEquals("", result.getAttributePaths().get(0));
        assertEquals("F_KEY1", result.getDbAttributes().get(0).getName());

        assertEquals("", result.getAttributePaths().get(1));
        assertEquals("F_KEY2", result.getDbAttributes().get(1).getName());
    }

    @Test
    public void testCompoundRelationshipFlattenedPathTranslation() {
        PathTranslationResult result = pathProcessor.process(CayennePath.of("toCompoundPk.name"));

        assertEquals(1, result.getDbAttributes().size());

        assertEquals("toCompoundPk", result.getLastAttributePath());
        assertEquals("NAME", result.getLastAttribute().getName());
    }
}
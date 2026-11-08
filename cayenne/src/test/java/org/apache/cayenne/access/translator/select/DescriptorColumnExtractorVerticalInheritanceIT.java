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

import java.util.Set;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.inheritance_vertical.IvRoot;
import org.apache.cayenne.testdo.inheritance_vertical.IvSub3;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link DescriptorColumnExtractor} registers exactly one {@code EntityResult} field per column,
 * under a unique key.
 * <p>
 * See CAY-2911.
 */
public class DescriptorColumnExtractorVerticalInheritanceIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.INHERITANCE_VERTICAL_PROJECT);

    private SelectTranslatorContext extract(ObjectSelect<?> query, String entityName) {
        CompiledMapping mapping = env.mapping();
        CompiledEntity entity = mapping.entity(entityName);

        SelectTranslatorContext translatorContext =
                new SelectTranslatorContext(query.resolve(mapping), Mockito.mock(DbAdapter.class), mapping, null);

        new DescriptorColumnExtractor(translatorContext, entity).extract();
        return translatorContext;
    }

    private void assertFieldCountMatchesColumnCount(ObjectSelect<?> query, String entityName) {
        SelectTranslatorContext context = extract(query, entityName);
        CompiledMapping mapping = env.mapping();

        assertEquals(context.getResultNodeList().size(),
                context.getRootEntityResult().getDbFields(mapping).size(),
                "entity result field count must match the number of emitted columns");
    }

    @Test
    public void fieldCountMatchesColumnCountForInheritanceRoot() {
        assertFieldCountMatchesColumnCount(ObjectSelect.query(IvRoot.class).column(IvRoot.SELF), "IvRoot");
    }

    @Test
    public void fieldCountMatchesColumnCountForSub3() {
        assertFieldCountMatchesColumnCount(ObjectSelect.query(IvSub3.class).column(IvSub3.SELF), "IvSub3");
    }

    @Test
    public void flattenedToOneFieldsArePathQualified() {
        SelectTranslatorContext context = extract(ObjectSelect.query(IvSub3.class).column(IvSub3.SELF), "IvSub3");
        CompiledMapping mapping = env.mapping();

        Set<String> keys = context.getRootEntityResult().getDbFields(mapping).keySet();

        assertTrue(keys.contains("sub3.ID"), () -> "expected path-qualified 'sub3.ID' in " + keys);
        assertTrue(keys.contains("sub3.IV_ROOT_ID"), () -> "expected path-qualified 'sub3.IV_ROOT_ID' in " + keys);
    }
}

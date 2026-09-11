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
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.ClassDescriptor;
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

    private SelectTranslatorContext extract(FluentSelect<?, ?> query, String entityName) {
        EntityResolver resolver = env.context().getEntityResolver();
        ClassDescriptor classDescriptor = resolver.getClassDescriptor(entityName);

        SelectTranslatorContext translatorContext =
                new SelectTranslatorContext(query, Mockito.mock(DbAdapter.class), resolver, null);

        new DescriptorColumnExtractor(translatorContext, classDescriptor).extract();
        return translatorContext;
    }

    private void assertFieldCountMatchesColumnCount(FluentSelect<?, ?> query, String entityName) {
        SelectTranslatorContext context = extract(query, entityName);
        EntityResolver resolver = env.context().getEntityResolver();

        assertEquals(context.getResultNodeList().size(),
                context.getRootEntityResult().getDbFields(resolver).size(),
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
        EntityResolver resolver = env.context().getEntityResolver();

        Set<String> keys = context.getRootEntityResult().getDbFields(resolver).keySet();

        assertTrue(keys.contains("sub3.ID"), () -> "expected path-qualified 'sub3.ID' in " + keys);
        assertTrue(keys.contains("sub3.IV_ROOT_ID"), () -> "expected path-qualified 'sub3.IV_ROOT_ID' in " + keys);
    }
}

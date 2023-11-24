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
import org.apache.cayenne.access.sqlbuilder.SQLGenerationVisitor;
import org.apache.cayenne.access.sqlbuilder.StringBuilderAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CompoundFkTestEntity;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseCayenneRuntime(CayenneProjects.COMPOUND_PROJECT)
public class QualifierTranslatorIT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

    @Inject
    private ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Before
    public void setUp() throws Exception {
        TableHelper tCompoundPKTest = new TableHelper(dbHelper, "COMPOUND_PK_TEST");
        tCompoundPKTest.setColumns("KEY1", "KEY2", "NAME");
        tCompoundPKTest.insert("PK1", "PK2", "BBB");
    }

    @Test
    public void testCompoundPK() {
        CompoundPkTestEntity testEntity = ObjectSelect.query(CompoundPkTestEntity.class).selectFirst(context);
        assertNotNull(testEntity);

        ObjectSelect<CompoundFkTestEntity> query = ObjectSelect.query(CompoundFkTestEntity.class)
                .where(CompoundFkTestEntity.TO_COMPOUND_PK.eq(testEntity))
                .and(CompoundFkTestEntity.NAME.like("test%"))
                .and(CompoundFkTestEntity.NAME.contains("a"));

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, runtime.getDataDomain().getDefaultNode().getAdapter(), context.getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new StringBuilderAppendable());
        node.visit(visitor);

        assertEquals(" ( ( ( t0.F_KEY1 = 'PK1' ) AND ( t0.F_KEY2 = 'PK2' ) ) AND t0.NAME LIKE 'test%' ) AND t0.NAME LIKE '%a%'", visitor.getSQLString());
    }

}

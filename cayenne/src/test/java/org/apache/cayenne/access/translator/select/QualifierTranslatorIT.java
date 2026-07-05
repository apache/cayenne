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
import org.apache.cayenne.access.sqlbuilder.SQLGenerationVisitor;
import org.apache.cayenne.access.sqlbuilder.DefaultSQLAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CompoundFkTestEntity;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class QualifierTranslatorIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.COMPOUND_PROJECT);

    private CayenneRuntime runtime;

    @BeforeEach
    public void setUp() throws Exception {
        runtime = env.runtime();
        TableHelper tCompoundPKTest = env.table("COMPOUND_PK_TEST", "KEY1", "KEY2", "NAME");
        tCompoundPKTest.insert("PK1", "PK2", "BBB");
        tCompoundPKTest.insert("PK3", "PK4", "CCC");
    }

    @Test
    public void compoundPK() {
        CompoundPkTestEntity testEntity = ObjectSelect.query(CompoundPkTestEntity.class).selectFirst(env.context());
        assertNotNull(testEntity);

        ObjectSelect<CompoundFkTestEntity> query = ObjectSelect.query(CompoundFkTestEntity.class)
                .where(CompoundFkTestEntity.TO_COMPOUND_PK.eq(testEntity))
                .and(CompoundFkTestEntity.NAME.like("test%"))
                .and(CompoundFkTestEntity.NAME.contains("a"));

        SelectTranslatorContext context
                = new SelectTranslatorContext(new FluentSelectWrapper(query), runtime.getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver(), null);

        QualifierTranslator qualifierTranslator = context.getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new DefaultSQLAppendable(null), null);
        node.visit(visitor);

        assertEquals(" cft.F_KEY1 = 'PK1' AND cft.F_KEY2 = 'PK2' AND cft.NAME LIKE 'test%' AND cft.NAME LIKE '%a%'", visitor.getSQLString());
    }

    @Test
    public void multipleCompoundPK() {
        List<CompoundPkTestEntity> testEntity = ObjectSelect.query(CompoundPkTestEntity.class)
                .limit(2)
                .select(env.context());
        assertNotNull(testEntity);
        assertEquals(2, testEntity.size());

        ObjectSelect<CompoundFkTestEntity> query = ObjectSelect.query(CompoundFkTestEntity.class)
                .where(CompoundFkTestEntity.TO_COMPOUND_PK.eq(testEntity.get(0)))
                .or(CompoundFkTestEntity.TO_COMPOUND_PK.eq(testEntity.get(1)));

        SelectTranslatorContext context
                = new SelectTranslatorContext(new FluentSelectWrapper(query), runtime.getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver(), null);

        QualifierTranslator qualifierTranslator = context.getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new DefaultSQLAppendable(null), null);
        node.visit(visitor);

        assertEquals(" (cft.F_KEY1 = 'PK1' AND cft.F_KEY2 = 'PK2') OR (cft.F_KEY1 = 'PK3' AND cft.F_KEY2 = 'PK4')", visitor.getSQLString());
    }

    @Test
    public void cAY_2879() {
        ObjectSelect<CompoundFkTestEntity> query = ObjectSelect.query(CompoundFkTestEntity.class)
                .where(ExpressionFactory.exp("name = -1"));

        SelectTranslatorContext context
                = new SelectTranslatorContext(new FluentSelectWrapper(query), runtime.getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver(), null);

        QualifierTranslator qualifierTranslator = context.getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new DefaultSQLAppendable(null), null);
        node.visit(visitor);

        assertEquals(" cft.NAME = - 1", visitor.getSQLString());

    }

}

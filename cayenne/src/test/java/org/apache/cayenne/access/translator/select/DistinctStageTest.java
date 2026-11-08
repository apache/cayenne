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

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.SchemaCompiler;
import org.apache.cayenne.query.resolved.ResolvedSelect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DistinctStageTest {

    @Test
    public void isUnsupportedForDistinct() {
        assertTrue(DistinctStage.isUnsupportedForDistinct(Types.BLOB));
        assertTrue(DistinctStage.isUnsupportedForDistinct(Types.CLOB));
        assertTrue(DistinctStage.isUnsupportedForDistinct(Types.NCLOB));
        assertTrue(DistinctStage.isUnsupportedForDistinct(Types.LONGVARCHAR));
        assertTrue(DistinctStage.isUnsupportedForDistinct(Types.LONGNVARCHAR));
        assertTrue(DistinctStage.isUnsupportedForDistinct(Types.LONGVARBINARY));
        assertFalse(DistinctStage.isUnsupportedForDistinct(Types.INTEGER));
        assertFalse(DistinctStage.isUnsupportedForDistinct(Types.DATE));
        assertFalse(DistinctStage.isUnsupportedForDistinct(Types.CHAR));
        assertFalse(DistinctStage.isUnsupportedForDistinct(Types.DECIMAL));
        assertFalse(DistinctStage.isUnsupportedForDistinct(Types.FLOAT));
        assertFalse(DistinctStage.isUnsupportedForDistinct(Types.VARCHAR));
    }

    @Test
    public void noSuppression() {
        ResolvedSelect query = new MockResolvedSelectBuilder().withDistinct(true).build();
        SelectTranslatorContext context = new MockSelectTranslatorContext(query);

        assertFalse(context.isDistinctSuppression());

        DistinctStage stage = new DistinctStage();
        stage.perform(context);

        assertFalse(context.isDistinctSuppression());
    }

    @Test
    public void explicitSuppression() {
        ResolvedSelect query = new MockResolvedSelectBuilder()
                .withDistinct(true)
                
                        .withSuppressDistinct()
                        
                .build();
        SelectTranslatorContext context = new MockSelectTranslatorContext(query);

        assertFalse(context.isDistinctSuppression());

        DistinctStage stage = new DistinctStage();
        stage.perform(context);

        assertTrue(context.isDistinctSuppression());
    }

    @Test
    public void suppressionByType() {
        ResolvedSelect query = new MockResolvedSelectBuilder()
                .withDistinct(true)
                
                .build();
        SelectTranslatorContext context = new MockSelectTranslatorContext(query);

        CompiledColumn column = compiledColumn("attr", Types.LONGVARBINARY);
        Node node = new ColumnNode("t0", "attr", null, column);
        context.addResultNode(node);

        assertFalse(context.isDistinctSuppression());

        DistinctStage stage = new DistinctStage();
        stage.perform(context);

        assertTrue(context.isDistinctSuppression());
    }

    private static CompiledColumn compiledColumn(String name, int type) {
        DbAttribute attribute = new DbAttribute(name);
        attribute.setType(type);
        DbEntity table = new DbEntity("t0");
        table.addAttribute(attribute);
        DataMap dataMap = new DataMap("mock");
        dataMap.addDbEntity(table);
        return SchemaCompiler.compile(List.of(dataMap)).table("t0").column(name);
    }
}
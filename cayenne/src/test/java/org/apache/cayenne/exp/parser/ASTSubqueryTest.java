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

package org.apache.cayenne.exp.parser;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ASTSubqueryTest {

    private static final ColumnSelect<String> NAMES = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME);

    @Test
    public void existsToString() {
        Expression exists = ExpressionFactory.exists(ObjectSelect.query(Painting.class));
        assertEquals("exists (select ...)", exists.toString());

        Expression notExists = ExpressionFactory.notExists(ObjectSelect.query(Painting.class));
        assertEquals("not exists (select ...)", notExists.toString());

        assertEquals("(a = 1) and (not exists (select ...))",
                ExpressionFactory.exp("a = 1").andExp(notExists).toString());
    }

    @Test
    public void inToString() {
        assertEquals("a in (select ...)", ExpressionFactory.inExp(ExpressionFactory.pathExp("a"), NAMES).toString());
        assertEquals("a not in (select ...)",
                ExpressionFactory.notInExp(ExpressionFactory.pathExp("a"), NAMES).toString());
    }

    @Test
    public void allAndAnyToString() {
        assertEquals("a > all (select ...)",
                ExpressionFactory.greaterExp("a", ExpressionFactory.all(NAMES)).toString());
        assertEquals("a = any (select ...)",
                ExpressionFactory.matchExp("a", ExpressionFactory.any(NAMES)).toString());
    }
}

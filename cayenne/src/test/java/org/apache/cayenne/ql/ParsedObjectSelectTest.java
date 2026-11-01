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
package org.apache.cayenne.ql;

import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.SubqueryExp;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SortOrder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ParsedObjectSelectTest {

    @Test
    public void parse() {
        ParsedObjectSelect select = ParsedObjectSelect.parseUnbound("""
                select distinct name, count(paintings) as total from Artist
                where name like $name and born > $born
                having count(paintings) > 1
                order by total desc, name insensitive
                limit 10 offset 20
                prefetch paintings joint, paintings.gallery""");

        assertEquals("Artist", select.getEntityName());
        assertNull(select.getDbEntityName());
        assertFalse(select.isDbQuery());
        assertTrue(select.isDistinct());

        assertTrue(select.isColumnQuery());
        assertFalse(select.isSingleColumn());
        assertEquals(2, select.getColumns().size());
        assertEquals("total", List.copyOf(select.getColumns()).get(1).getAlias());

        // the parameters stay unbound
        assertEquals(ExpressionFactory.exp("name like $name and born > $born"), select.getWhere());
        assertEquals(ExpressionFactory.exp("count(paintings) > 1"), select.getHaving());

        assertEquals(2, select.getOrderings().size());
        assertEquals(SortOrder.DESCENDING, List.copyOf(select.getOrderings()).get(0).getSortOrder());
        assertEquals(SortOrder.ASCENDING_INSENSITIVE, List.copyOf(select.getOrderings()).get(1).getSortOrder());

        assertEquals(10, select.getLimit());
        assertEquals(20, select.getOffset());

        assertNotNull(select.getPrefetches());
        assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS,
                select.getPrefetches().getNode("paintings").getSemantics());
        assertNotNull(select.getPrefetches().getNode("paintings.gallery"));
    }

    @Test
    public void parseDefaults() {
        ParsedObjectSelect select = ParsedObjectSelect.parseUnbound("from Artist");
        assertFalse(select.isColumnQuery());
        assertFalse(select.isSingleColumn());
        assertNull(select.getColumns());
        assertFalse(select.isDistinct());
        assertNull(select.getWhere());
        assertNull(select.getHaving());
        assertEquals(List.of(), List.copyOf(select.getOrderings()));
        assertEquals(0, select.getLimit());
        assertEquals(0, select.getOffset());
        assertNull(select.getPrefetches());
    }

    @Test
    public void parseDbQuery() {
        ParsedObjectSelect select = ParsedObjectSelect.parseUnbound("from db:ARTIST where NAME = 'a'");
        assertNull(select.getEntityName());
        assertEquals("ARTIST", select.getDbEntityName());
        assertTrue(select.isDbQuery());
        assertEquals(ExpressionFactory.exp("db:NAME = 'a'"), select.getWhere());
    }

    @Test
    public void parseSingleColumn() {
        ParsedObjectSelect select = ParsedObjectSelect.parseUnbound("select name from Artist");
        assertTrue(select.isColumnQuery());
        assertTrue(select.isSingleColumn());
    }

    @Test
    public void parseSubquery() {
        ParsedObjectSelect select = ParsedObjectSelect.parseUnbound(
                "from Artist where exists (from Painting where artist = enclosing(self) and price > $price)");

        // a subquery is parsed the same way, and is reachable through the expression tree
        SubqueryExp subquery = (SubqueryExp) select.getWhere().getOperand(0);
        ParsedObjectSelect nested = assertInstanceOf(ParsedObjectSelect.class, subquery.getQuery());
        assertEquals("Painting", nested.getEntityName());
        assertEquals(ExpressionFactory.exp("artist = enclosing(self) and price > $price"), nested.getWhere());
    }

    @Test
    public void parseWithNullParameters() {
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> ParsedObjectSelect.parse("from Artist", (Map<String, ?>) null));
        assertTrue(e.getMessage().contains("parseUnbound"), e.getMessage());
    }

    @Test
    public void parseInvalid() {
        assertThrows(ExpressionException.class, () -> ParsedObjectSelect.parseUnbound("from Artist where"));
        assertThrows(ExpressionException.class, () -> ParsedObjectSelect.parseUnbound(""));
    }

    @Test
    public void parseWithParameters() {
        ParsedObjectSelect select = ParsedObjectSelect.parse("from Artist where name = $name and born > $born",
                Map.of("name", "a"));

        // the bound parameter is replaced, the condition of the unbound one is pruned
        assertEquals(ExpressionFactory.exp("name = 'a'"), select.getWhere());
    }

    @Test
    public void objectSelectParsesToTheSameType() {
        // every parsed query is a parsed select, the runtime runs it as a plain ObjectSelect
        ObjectSelect<?> select = ObjectSelect.parse("from Artist where exists (from Painting)");
        ParsedObjectSelect parsed = assertInstanceOf(ParsedObjectSelect.class, select);
        SubqueryExp subquery = (SubqueryExp) parsed.getWhere().getOperand(0);
        assertInstanceOf(ParsedObjectSelect.class, subquery.getQuery());
    }
}

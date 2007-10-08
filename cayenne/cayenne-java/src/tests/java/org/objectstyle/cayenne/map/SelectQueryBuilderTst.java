/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.map;

import junit.framework.TestCase;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * @author Andrei Adamchik
 */
public class SelectQueryBuilderTst extends TestCase {

    public void testGetQueryType() throws Exception {
        SelectQueryBuilder builder = new MockupRootQueryBuilder();
        assertTrue(builder.getQuery() instanceof SelectQuery);
    }

    public void testGetQueryName() throws Exception {
        SelectQueryBuilder builder = new MockupRootQueryBuilder();
        builder.setName("xyz");

        assertEquals("xyz", builder.getQuery().getName());
    }

    public void testGetQueryRoot() throws Exception {
        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("A");
        map.addObjEntity(entity);

        SelectQueryBuilder builder = new SelectQueryBuilder();
        builder.setRoot(map, QueryBuilder.OBJ_ENTITY_ROOT, "A");

        assertTrue(builder.getQuery() instanceof SelectQuery);
        assertSame(entity, ((SelectQuery) builder.getQuery()).getRoot());
    }

    public void testGetQueryQualifier() throws Exception {
        SelectQueryBuilder builder = new MockupRootQueryBuilder();
        builder.setQualifier("abc = 5");

        SelectQuery query = (SelectQuery) builder.getQuery();

        assertEquals(Expression.fromString("abc = 5"), query.getQualifier());
    }

    public void testGetQueryProperties() throws Exception {
        SelectQueryBuilder builder = new MockupRootQueryBuilder();
        builder.addProperty(QueryMetadata.FETCH_LIMIT_PROPERTY, "5");

        Query query = builder.getQuery();
        assertTrue(query instanceof SelectQuery);
        assertEquals(5, ((SelectQuery) query).getFetchLimit());

        // TODO: test other properties...
    }

    class MockupRootQueryBuilder extends SelectQueryBuilder {

        public Object getRoot() {
            return "FakeRoot";
        }
    }
}
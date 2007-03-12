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
package org.apache.cayenne.query;

import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.unit.CayenneCase;

public class EJBQLQueryTest extends CayenneCase {

    public void testGetExpression() {
        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        EJBQLCompiledExpression parsed = query.getExpression(getDomain()
                .getEntityResolver());
        assertNotNull(parsed);
        assertEquals(ejbql, parsed.getSource());
    }

    public void testGetName() {
        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        assertNull(query.getName());
        query.setName("XYZ");
        assertEquals("XYZ", query.getName());
    }

    public void testGetMetadata() {

        EntityResolver resolver = getDomain().getEntityResolver();
        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        QueryMetadata md = query.getMetaData(resolver);

        assertNotNull(md);

        assertNotNull(md.getClassDescriptor());
        assertSame(resolver.getClassDescriptor("Artist"), md.getClassDescriptor());

        assertNotNull(md.getObjEntity());
        assertSame(resolver.getObjEntity("Artist"), md.getObjEntity());

        assertFalse(md.isFetchingDataRows());
        assertTrue(md.isRefreshingObjects());
        assertTrue(md.isResolvingInherited());
        assertEquals(QueryMetadata.NO_CACHE, md.getCachePolicy());
    }
}

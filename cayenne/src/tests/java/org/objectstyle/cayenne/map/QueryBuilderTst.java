/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * @author Andrei Adamchik
 */
public class QueryBuilderTst extends BasicTestCase {

    protected QueryBuilder builder;

    protected void setUp() throws Exception {
        builder = new QueryBuilder() {

            public Query getQuery() {
                return null;
            }
        };
    }

    public void testSetName() throws Exception {
        builder.setName("aaa");
        assertEquals("aaa", builder.name);
    }
    
    public void testSetSelecting() throws Exception {
        builder.setSelecting(null);
        assertTrue(builder.selecting);
        
        builder.setSelecting("false");
        assertFalse(builder.selecting);
        
        builder.setSelecting("true");
        assertTrue(builder.selecting);
    }

    public void testSetRootInfoDbEntity() throws Exception {
        DataMap map = new DataMap("map");
        DbEntity entity = new DbEntity("DB1");
        map.addDbEntity(entity);

        builder.setRoot(map, QueryBuilder.DB_ENTITY_ROOT, "DB1");
        assertSame(entity, builder.getRoot());
    }

    public void testSetRootObjEntity() throws Exception {
        DataMap map = new DataMap("map");
        ObjEntity entity = new ObjEntity("OBJ1");
        map.addObjEntity(entity);

        builder.setRoot(map, QueryBuilder.OBJ_ENTITY_ROOT, "OBJ1");
        assertSame(entity, builder.getRoot());
    }

    public void testSetRootDataMap() throws Exception {
        DataMap map = new DataMap("map");

        builder.setRoot(map, QueryBuilder.DATA_MAP_ROOT, null);
        assertSame(map, builder.getRoot());
    }
}
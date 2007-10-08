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
package org.objectstyle.cayenne.wocompat;

import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.query.PrefetchTreeNode;

public class EOQueryTst extends TestCase {

    public void testConstructor() throws Exception {

        EOModelProcessor processor = new EOModelProcessor();
        DataMap map = processor.loadEOModel("wotests/fetchspec.eomodeld");

        Map fspecMap = (Map) PropertyListSerialization.propertyListFromStream(getClass()
                .getClassLoader()
                .getResourceAsStream("wotests/fetchspec.eomodeld/Entity1.fspec"));
        assertNotNull(fspecMap);
        assertNotNull(fspecMap.get("E1FS1"));

        EOQuery query = new EOQuery(map.getObjEntity("Entity1"), (Map) fspecMap
                .get("E1FS1"));
        assertNull(query.getName());

        assertNotNull(query.getQualifier());
        assertEquals(
                "(name = \"aa\") and (db:ID >= 7) and ((e2.name = \"bb\") or (db:e2.ID != 5))",
                query.getQualifier().toString());
        
        assertNotNull(query.getPrefetchTree());
        
        Collection children= query.getPrefetchTree().getChildren();
        assertEquals(1, children.size());
        assertEquals("e2", ((PrefetchTreeNode) children.iterator().next()).getName());
        
        assertTrue(query.isFetchingDataRows());
        assertEquals(500, query.getFetchLimit());
        assertEquals(0, query.getPageSize());
        assertTrue(query.isDistinct());
        assertFalse(query.isRefreshingObjects());
    }
}

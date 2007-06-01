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
package org.objectstyle.cayenne.wocompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * @author Andrei Adamchik
 */
public class EOModelHelperTst extends BasicTestCase {

    protected EOModelHelper helper;

    protected void setUp() throws Exception {
        super.setUp();

        helper = new EOModelHelper("test-resources/wotests/art.eomodeld");
    }

    public void testModelNames() throws Exception {
        Iterator names = helper.modelNames();

        // collect to list and then analyze
        List list = new ArrayList();
        while (names.hasNext()) {
            list.add(names.next());
        }

        assertEquals(8, list.size());
        assertTrue(list.contains("Artist"));
        assertTrue(list.contains("Painting"));
        assertTrue(list.contains("ExhibitType"));
    }

    public void testQueryNames() throws Exception {
        Iterator artistNames = helper.queryNames("Artist");
        assertFalse(artistNames.hasNext());

        Iterator etNames = helper.queryNames("ExhibitType");
        assertTrue(etNames.hasNext());

        // collect to list and then analyze
        List list = new ArrayList();
        while (etNames.hasNext()) {
            list.add(etNames.next());
        }

        assertEquals(2, list.size());
        assertTrue(list.contains("FetchAll"));
        assertTrue(list.contains("TestQuery"));
    }
    
    public void testQueryPListMap() throws Exception {
        assertNull(helper.queryPListMap("Artist", "AAA"));
        assertNull(helper.queryPListMap("ExhibitType", "AAA"));
        
        Map query = helper.queryPListMap("ExhibitType", "FetchAll");
        assertNotNull(query);
        assertFalse(query.isEmpty());
    }

    public void testLoadQueryIndex() throws Exception {
        Map index = helper.loadQueryIndex("ExhibitType");
        assertNotNull(index);
        assertTrue(index.containsKey("FetchAll"));
    }

    public void testOpenQueryStream() throws Exception {
        InputStream in = helper.openQueryStream("ExhibitType");
        assertNotNull(in);
        in.close();
    }

    public void testOpenNonExistentQueryStream() throws Exception {
        try {
            helper.openQueryStream("Artist");
            fail("Exception expected - artist has no fetch spec.");
        }
        catch (IOException ioex) {
            // expected...
        }
    }
}
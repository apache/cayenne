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
package org.objectstyle.cayenne.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * @author Andrei Adamchik
 */
public class SQLTemplateTst extends BasicTestCase {
    public void testGetDefaultTemplate() throws Exception {
        SQLTemplate query = new SQLTemplate(false);
        query.setDefaultTemplate("AAA # BBB");
        assertEquals("AAA # BBB", query.getDefaultTemplate());
    }

    public void testGetTemplate() throws Exception {
        SQLTemplate query = new SQLTemplate(false);

        // no template for key, no default template... must be null
        assertNull(query.getTemplate("key1"));

        // no template for key, must return default
        query.setDefaultTemplate("AAA # BBB");
        assertEquals("AAA # BBB", query.getTemplate("key1"));

        // must find template
        query.setTemplate("key1", "XYZ");
        assertEquals("XYZ", query.getTemplate("key1"));

        // add another template.. still must find 
        query.setTemplate("key2", "123");
        assertEquals("XYZ", query.getTemplate("key1"));
        assertEquals("123", query.getTemplate("key2"));
    }

    public void testSingleParameterSet() throws Exception {
        SQLTemplate query = new SQLTemplate(false);

        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());

        Map params = new HashMap();
        params.put("a", "b");

        query.setParameters(params);
        assertEquals(params, query.getParameters());
        Iterator it = query.parametersIterator();
        assertTrue(it.hasNext());
        assertEquals(params, it.next());
        assertFalse(it.hasNext());

        query.setParameters((Map) null);
        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());
        it = query.parametersIterator();
        assertFalse(it.hasNext());
    }

    public void testBatchParameterSet() throws Exception {
        SQLTemplate query = new SQLTemplate(false);

        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());

        Map params1 = new HashMap();
        params1.put("a", "b");

        Map params2 = new HashMap();
        params2.put("1", "2");

        query.setParameters(new Map[] { params1, params2, null });
        assertEquals(params1, query.getParameters());
        Iterator it = query.parametersIterator();
        assertTrue(it.hasNext());
        assertEquals(params1, it.next());
        assertTrue(it.hasNext());
        assertEquals(params2, it.next());
        assertTrue(it.hasNext());
        assertEquals(Collections.EMPTY_MAP, it.next());
        assertFalse(it.hasNext());

        query.setParameters((Map[]) null);
        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());
        it = query.parametersIterator();
        assertFalse(it.hasNext());
    }
}

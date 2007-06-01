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

import org.apache.commons.beanutils.PropertyUtils;
import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * Tests various implementations of GenericSelectQuery.
 * 
 * @author Andrei Adamchik
 */
public class GenericSelectQueryTst extends BasicTestCase {

    public void testSelectQuery() throws Exception {
        runTest(new SelectQuery("Dummy"));
    }

    public void testSQLTemplate() throws Exception {
        runTest(new SQLTemplate("Dummy", "AAA", true));
    }

    /**
     * Tests properties for an unknown implementation of GenericSelectQuery.
     * Assumes there are standard setters for all the properties.
     */
    protected void runTest(GenericSelectQuery q) throws Exception {
        // fetchingDataRows
        assertEquals(
            GenericSelectQuery.FETCHING_DATA_ROWS_DEFAULT,
            q.isFetchingDataRows());

        PropertyUtils.setProperty(q, "fetchingDataRows", Boolean.FALSE);
        assertFalse(q.isFetchingDataRows());

        PropertyUtils.setProperty(q, "fetchingDataRows", Boolean.TRUE);
        assertTrue(q.isFetchingDataRows());

        PropertyUtils.setProperty(q, "fetchingDataRows", Boolean.FALSE);
        assertFalse(q.isFetchingDataRows());

        // refreshingObjects
        assertEquals(
            GenericSelectQuery.REFRESHING_OBJECTS_DEFAULT,
            q.isRefreshingObjects());

        PropertyUtils.setProperty(q, "refreshingObjects", Boolean.FALSE);
        assertFalse(q.isRefreshingObjects());

        PropertyUtils.setProperty(q, "refreshingObjects", Boolean.TRUE);
        assertTrue(q.isRefreshingObjects());

        PropertyUtils.setProperty(q, "refreshingObjects", Boolean.FALSE);
        assertFalse(q.isRefreshingObjects());

        // resolvingInherited
        assertEquals(
            GenericSelectQuery.RESOLVING_INHERITED_DEFAULT,
            q.isResolvingInherited());

        PropertyUtils.setProperty(q, "resolvingInherited", Boolean.FALSE);
        assertFalse(q.isResolvingInherited());

        PropertyUtils.setProperty(q, "resolvingInherited", Boolean.TRUE);
        assertTrue(q.isResolvingInherited());

        PropertyUtils.setProperty(q, "resolvingInherited", Boolean.FALSE);
        assertFalse(q.isResolvingInherited());

        // fetchLimit
        assertEquals(GenericSelectQuery.FETCH_LIMIT_DEFAULT, q.getFetchLimit());

        PropertyUtils.setProperty(q, "fetchLimit", new Integer(1001));
        assertEquals(1001, q.getFetchLimit());

        PropertyUtils.setProperty(q, "fetchLimit", new Integer(0));
        assertEquals(0, q.getFetchLimit());

        // pageSize
        assertEquals(GenericSelectQuery.PAGE_SIZE_DEFAULT, q.getPageSize());

        PropertyUtils.setProperty(q, "pageSize", new Integer(1001));
        assertEquals(1001, q.getPageSize());

        PropertyUtils.setProperty(q, "pageSize", new Integer(0));
        assertEquals(0, q.getPageSize());
        
        // caching policy
        assertEquals(GenericSelectQuery.CACHE_POLICY_DEFAULT, q.getCachePolicy());

        PropertyUtils.setProperty(q, "cachePolicy", GenericSelectQuery.LOCAL_CACHE);
        assertEquals(GenericSelectQuery.LOCAL_CACHE, q.getCachePolicy());

        PropertyUtils.setProperty(q, "cachePolicy", GenericSelectQuery.SHARED_CACHE);
        assertEquals(GenericSelectQuery.SHARED_CACHE, q.getCachePolicy());
    }
}

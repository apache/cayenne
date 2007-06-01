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
package org.objectstyle.cayenne.unit.jira;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DefaultDataContextDelegate;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.testdo.inherit.Employee;
import org.objectstyle.cayenne.testdo.inherit.Manager;
import org.objectstyle.cayenne.unit.PeopleTestCase;

/**
 * @author Andrei Adamchik
 */
public class CAY_218Tst extends PeopleTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    /**
     * Testing a case when a to-many relationship defined in super entity is prefetched. A
     * suspected bug is that a to-one back to super entity is resolved inefficiently, by
     * performing individual queries.
     */
    public void testPrefetchSuperRel() throws Exception {
        createTestData("testPrefetchSuperRel");

        SelectQuery q = new SelectQuery(Employee.class);
        q.setResolvingInherited(true);
        q.addPrefetch("addresses");

        QueryCounter delegate = new QueryCounter();
        DataContext context = createDataContext();
        context.setDelegate(delegate);
        context.performQuery(q);

        assertEquals(2, delegate.count);
    }

    /**
     * Testing that given a superclass ObjectId, a correct subclass is returned by
     * DataObjectUtils, when an object is already cached....
     */
    public void testObjectForSuperPK() throws Exception {
        createTestData("testObjectForSuperPK");

        DataContext context = createDataContext();
        
        // first read the object...
        DataObjectUtils.objectForPK(context, new ObjectId(
                Manager.class,
                Employee.PERSON_ID_PK_COLUMN,
                3));

        // now try to retrieve it again, see that it is found in cache...
        
        QueryCounter delegate = new QueryCounter();
        context.setDelegate(delegate);
        
        ObjectId employeeID = new ObjectId(
                Employee.class,
                Employee.PERSON_ID_PK_COLUMN,
                3);

        DataObject object = DataObjectUtils.objectForPK(context, employeeID);
        
        // make sure the object came from cache
        assertEquals(0, delegate.count);
        
        assertTrue(object instanceof Manager);

        // also test that the ObjectID is that of manager
        assertSame(Manager.class, object.getObjectId().getObjClass());

        // ... and that it is used by the ObjectStore
        assertNull(context.getObjectStore().getObject(employeeID));
        assertSame(object, context.getObjectStore().getObject(object.getObjectId()));
    }

    final class QueryCounter extends DefaultDataContextDelegate {

        int count;

        public GenericSelectQuery willPerformSelect(
                DataContext context,
                GenericSelectQuery query) {
            count++;
            return query;
        }
    }

}
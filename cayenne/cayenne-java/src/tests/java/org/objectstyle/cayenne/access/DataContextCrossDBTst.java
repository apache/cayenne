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
package org.objectstyle.cayenne.access;

import org.objectstyle.cayenne.testdo.db1.CrossdbM1E1;
import org.objectstyle.cayenne.testdo.db2.CrossdbM2E1;
import org.objectstyle.cayenne.testdo.db2.CrossdbM2E2;
import org.objectstyle.cayenne.unit.MultiNodeTestCase;

public class DataContextCrossDBTst extends MultiNodeTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testMultiDBUpdate() {

        // for now testing that no exceptions are thrown... wouldn't hurt to check the
        // data as well???

        DataContext context = createDataContext();

        // insert
        CrossdbM1E1 o1 = (CrossdbM1E1) context
                .createAndRegisterNewObject(CrossdbM1E1.class);
        o1.setName("o1");

        CrossdbM2E1 o2 = (CrossdbM2E1) context
                .createAndRegisterNewObject(CrossdbM2E1.class);
        o2.setName("o2");

        CrossdbM2E2 o3 = (CrossdbM2E2) context
                .createAndRegisterNewObject(CrossdbM2E2.class);
        o3.setName("o3");

        o3.setToM1E1(o1);
        o3.setToM2E1(o2);
        context.commitChanges();

        // update
        CrossdbM1E1 o11 = (CrossdbM1E1) context
                .createAndRegisterNewObject(CrossdbM1E1.class);
        o11.setName("o11");
        o3.setToM1E1(o11);
        context.commitChanges();

        // update with existing

        o3.setToM1E1(o1);
        context.commitChanges();
    }
}

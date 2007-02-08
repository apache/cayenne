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

import java.sql.Types;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * @author Andrei Adamchik
 */
public class EOModelPrototypesTst extends BasicTestCase {

    public void testSkipPrototypes() throws Exception {
        DataMap map = new EOModelProcessor().loadEOModel("prototypes.eomodeld");

        assertNotNull(map.getObjEntity("Document"));
        assertNull(map.getObjEntity("EOPrototypes"));
        assertNull(map.getObjEntity("EOXYZPrototypes"));
    }

    public void testDbAttributeType() throws Exception {
        DataMap map = new EOModelProcessor().loadEOModel("prototypes.eomodeld");

        DbEntity dbe = map.getDbEntity("DOCUMENT");
        assertNotNull(dbe);

        // test that an attribute that has ObjAttribute has its type configured
        DbAttribute dba1 = (DbAttribute) dbe.getAttribute("DOCUMENT_TYPE");
        assertEquals(Types.VARCHAR, dba1.getType());

        // test that a numeric attribute has its type configured
        DbAttribute dba2 = (DbAttribute) dbe.getAttribute("TEST_NUMERIC");
        assertEquals(Types.INTEGER, dba2.getType());

        // test that an attribute that has no ObjAttribute has its type configured
        DbAttribute dba3 = (DbAttribute) dbe.getAttribute("DOCUMENT_ID");
        assertEquals(Types.INTEGER, dba3.getType());
    }

    // TODO: move this test to EOModelProcessorInheritanceTst. The original problem had
    // nothing
    // to do with prototypes...
    public void testSameColumnMapping() throws Exception {
        DataMap map = new EOModelProcessor().loadEOModel("prototypes.eomodeld");

        ObjEntity estimateOE = map.getObjEntity("Estimate");
        ObjEntity invoiceOE = map.getObjEntity("Invoice");
        ObjEntity vendorOE = map.getObjEntity("VendorPO");

        assertNotNull(estimateOE);
        assertNotNull(invoiceOE);
        assertNotNull(vendorOE);

        ObjAttribute en = (ObjAttribute) estimateOE.getAttribute("estimateNumber");
        assertEquals("DOCUMENT_NUMBER", en.getDbAttributePath());

        ObjAttribute in = (ObjAttribute) invoiceOE.getAttribute("invoiceNumber");
        assertEquals("DOCUMENT_NUMBER", in.getDbAttributePath());

        ObjAttribute vn = (ObjAttribute) vendorOE.getAttribute("purchaseOrderNumber");
        assertEquals("DOCUMENT_NUMBER", vn.getDbAttributePath());
    }

    // TODO: move this test to EOModelProcessorInheritanceTst. The original problem had
    // nothing to do with prototypes...
    public void testOverridingAttributes() throws Exception {
        DataMap map = new EOModelProcessor().loadEOModel("prototypes.eomodeld");

        ObjEntity documentOE = map.getObjEntity("Document");
        ObjEntity estimateOE = map.getObjEntity("Estimate");

        assertSame(documentOE, estimateOE.getAttribute("created").getEntity());
        assertSame(estimateOE, estimateOE.getAttribute("estimateNumber").getEntity());
    }
}
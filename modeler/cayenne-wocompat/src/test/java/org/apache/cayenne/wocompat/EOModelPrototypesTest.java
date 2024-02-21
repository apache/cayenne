/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.wocompat;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class EOModelPrototypesTest {

    private URL url;

    @Before
    public void setUp() throws Exception {
        url = getClass().getClassLoader().getResource("prototypes.eomodeld/");
        assertNotNull(url);
    }

    @Test
    public void testSkipPrototypes() throws Exception {
        DataMap map = new EOModelProcessor().loadEOModel(url);

        assertNotNull(map.getObjEntity("Document"));
        assertNull(map.getObjEntity("EOPrototypes"));
        assertNull(map.getObjEntity("EOXYZPrototypes"));
    }

    @Test
    public void testDbAttributeType() throws Exception {
        DataMap map = new EOModelProcessor().loadEOModel(url);

        DbEntity dbe = map.getDbEntity("DOCUMENT");
        assertNotNull(dbe);

        // test that an attribute that has ObjAttribute has its type configured
        DbAttribute dba1 = dbe.getAttribute("DOCUMENT_TYPE");
        assertEquals(Types.VARCHAR, dba1.getType());

        // test that a numeric attribute has its type configured
        DbAttribute dba2 = dbe.getAttribute("TEST_NUMERIC");
        assertEquals(Types.INTEGER, dba2.getType());

        // test that an attribute that has no ObjAttribute has its type configured
        DbAttribute dba3 = dbe.getAttribute("DOCUMENT_ID");
        assertEquals(Types.INTEGER, dba3.getType());
    }

    // TODO: move this test to EOModelProcessorInheritanceTst. The original problem had
    // nothing
    // to do with prototypes...
    @Test
    public void testSameColumnMapping() throws Exception {
        DataMap map = new EOModelProcessor().loadEOModel(url);

        ObjEntity estimateOE = map.getObjEntity("Estimate");
        ObjEntity invoiceOE = map.getObjEntity("Invoice");
        ObjEntity vendorOE = map.getObjEntity("VendorPO");

        assertNotNull(estimateOE);
        assertNotNull(invoiceOE);
        assertNotNull(vendorOE);

        ObjAttribute en = (ObjAttribute) estimateOE.getAttribute("estimateNumber");
        assertEquals("DOCUMENT_NUMBER", en.getDbAttributePath().value());

        ObjAttribute in = (ObjAttribute) invoiceOE.getAttribute("invoiceNumber");
        assertEquals("DOCUMENT_NUMBER", in.getDbAttributePath().value());

        ObjAttribute vn = (ObjAttribute) vendorOE.getAttribute("purchaseOrderNumber");
        assertEquals("DOCUMENT_NUMBER", vn.getDbAttributePath().value());
    }

    // TODO: move this test to EOModelProcessorInheritanceTst. The original problem had
    // nothing to do with prototypes...
    @Test
    public void testOverridingAttributes() throws Exception {
        DataMap map = new EOModelProcessor().loadEOModel(url);

        ObjEntity estimateOE = map.getObjEntity("Estimate");

        assertSame(estimateOE, estimateOE.getAttribute("created").getEntity());
        assertSame(estimateOE, estimateOE.getAttribute("estimateNumber").getEntity());
    }
}

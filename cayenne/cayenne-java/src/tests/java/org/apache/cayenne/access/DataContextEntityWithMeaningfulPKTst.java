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

package org.apache.cayenne.access;

import java.util.List;

import org.apache.art.MeaningfulPKDep;
import org.apache.art.MeaningfulPKTest1;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextEntityWithMeaningfulPKTst extends CayenneTestCase {
    protected DataContext ctxt;

    protected void setUp() throws Exception {
        super.setUp();
        
        deleteTestData();
        ctxt = getDomain().createDataContext();
    }

    public void testInsertWithMeaningfulPK() throws Exception {
        MeaningfulPKTest1 obj =
            (MeaningfulPKTest1) ctxt.createAndRegisterNewObject("MeaningfulPKTest1");
        obj.setPkAttribute(new Integer(1000));
        obj.setDescr("aaa-aaa");
        ctxt.commitChanges();
    }

    public void testChangeKey() throws Exception {
        MeaningfulPKTest1 obj =
            (MeaningfulPKTest1) ctxt.createAndRegisterNewObject("MeaningfulPKTest1");
        obj.setPkAttribute(new Integer(1000));
        obj.setDescr("aaa-aaa");
        ctxt.commitChanges();

        obj.setPkAttribute(new Integer(2000));
        ctxt.commitChanges();

        // assert that object id got fixed
        ObjectId id = obj.getObjectId();
        assertEquals(new Integer(2000), id.getIdSnapshot().get("PK_ATTRIBUTE"));
    }

    public void testToManyRelationshipWithMeaningfulPK1() throws Exception {
        MeaningfulPKTest1 obj =
            (MeaningfulPKTest1) ctxt.createAndRegisterNewObject("MeaningfulPKTest1");
        obj.setPkAttribute(new Integer(1000));
        obj.setDescr("aaa-aaa");
        ctxt.commitChanges();

        // must be able to resolve to-many relationship
        ctxt = createDataContext();
        List objects = ctxt.performQuery(new SelectQuery(MeaningfulPKTest1.class));
        assertEquals(1, objects.size());
        obj = (MeaningfulPKTest1) objects.get(0);
        assertEquals(0, obj.getMeaningfulPKDepArray().size());
    }

    public void testToManyRelationshipWithMeaningfulPK2() throws Exception {
        MeaningfulPKTest1 obj =
            (MeaningfulPKTest1) ctxt.createAndRegisterNewObject("MeaningfulPKTest1");
        obj.setPkAttribute(new Integer(1000));
        obj.setDescr("aaa-aaa");
        ctxt.commitChanges();

        // must be able to set reverse relationship
        MeaningfulPKDep dep =
            (MeaningfulPKDep) ctxt.createAndRegisterNewObject("MeaningfulPKDep");
        dep.setToMeaningfulPK(obj);
        ctxt.commitChanges();
    }

}

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
package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks deriving a to-one target id from the FK columns of a source snapshot, for a single-column FK, a compound FK
 * with and without a nullable part, and the relationships that don't determine a target id.
 */
public class ObjectSnapshotsTest {

    private ObjEntity order;
    private ObjEntity customer;

    @BeforeEach
    public void buildModel() {

        DataMap map = new DataMap("M");

        DbEntity addressTable = addDbEntity(map, "ADDRESS", "ID");
        DbEntity customerTable = addDbEntity(map, "CUSTOMER", "ID");
        customerTable.addAttribute(new DbAttribute("NAME"));
        customerTable.addAttribute(new DbAttribute("ADDRESS_ID"));
        DbEntity regionTable = addDbEntity(map, "REGION", "COUNTRY", "CODE");
        DbEntity orderTable = addDbEntity(map, "ORDER", "ID");
        orderTable.addAttribute(new DbAttribute("CUSTOMER_ID"));
        orderTable.addAttribute(new DbAttribute("CUSTOMER_NAME"));
        DbAttribute country = new DbAttribute("COUNTRY");
        country.setMandatory(true);
        orderTable.addAttribute(country);
        DbAttribute regionCode = new DbAttribute("REGION_CODE");
        regionCode.setMandatory(true);
        orderTable.addAttribute(regionCode);

        // a compound FK with a nullable part
        orderTable.addAttribute(new DbAttribute("ALT_COUNTRY"));
        orderTable.addAttribute(new DbAttribute("ALT_REGION_CODE"));

        DbRelationship toCustomer = addDbRelationship(orderTable, "toCustomer", "CUSTOMER", false);
        toCustomer.addJoin(new DbJoin(toCustomer, "CUSTOMER_ID", "ID"));
        toCustomer.setForeignKey(true);

        DbRelationship toRegion = addDbRelationship(orderTable, "toRegion", "REGION", false);
        toRegion.addJoin(new DbJoin(toRegion, "COUNTRY", "COUNTRY"));
        toRegion.addJoin(new DbJoin(toRegion, "REGION_CODE", "CODE"));
        toRegion.setForeignKey(true);

        DbRelationship toAltRegion = addDbRelationship(orderTable, "toAltRegion", "REGION", false);
        toAltRegion.addJoin(new DbJoin(toAltRegion, "ALT_COUNTRY", "COUNTRY"));
        toAltRegion.addJoin(new DbJoin(toAltRegion, "ALT_REGION_CODE", "CODE"));
        toAltRegion.setForeignKey(true);

        // an FK to a unique non-PK column
        DbRelationship toCustomerByName = addDbRelationship(orderTable, "toCustomerByName", "CUSTOMER", false);
        toCustomerByName.addJoin(new DbJoin(toCustomerByName, "CUSTOMER_NAME", "NAME"));
        toCustomerByName.setForeignKey(true);

        DbRelationship toAddress = addDbRelationship(customerTable, "toAddress", "ADDRESS", false);
        toAddress.addJoin(new DbJoin(toAddress, "ADDRESS_ID", "ID"));
        toAddress.setForeignKey(true);

        DbRelationship orders = addDbRelationship(customerTable, "orders", "ORDER", true);
        orders.addJoin(new DbJoin(orders, "ID", "CUSTOMER_ID"));

        customer = addObjEntity(map, "Customer", "CUSTOMER");
        addObjEntity(map, "Region", "REGION");
        addObjEntity(map, "Address", "ADDRESS");
        order = addObjEntity(map, "Order", "ORDER");
        addObjRelationship(order, "customer", "Customer", "toCustomer");
        addObjRelationship(order, "customerByName", "Customer", "toCustomerByName");
        addObjRelationship(order, "customerAddress", "Address", "toCustomer.toAddress");
        addObjRelationship(order, "region", "Region", "toRegion");
        addObjRelationship(order, "altRegion", "Region", "toAltRegion");
        addObjRelationship(customer, "orders", "Order", "orders");

        new EntityResolver(List.of(map));
    }

    @Test
    public void singleColumnFK() {
        Map<String, Object> row = new HashMap<>();
        row.put("ID", 1);
        row.put("CUSTOMER_ID", 7);

        ObjectId id = ObjectSnapshots.targetId(order.getRelationship("customer"), row);
        assertEquals(ObjectId.of("Customer", "ID", 7), id);
    }

    @Test
    public void singleColumnFK_Null() {
        Map<String, Object> row = new HashMap<>();
        row.put("ID", 1);
        row.put("CUSTOMER_ID", null);

        assertNull(ObjectSnapshots.targetId(order.getRelationship("customer"), row));

        // a partial snapshot without the FK column reads as a null FK
        assertNull(ObjectSnapshots.targetId(order.getRelationship("customer"), Map.of("ID", 1)));
    }

    @Test
    public void compoundFK() {
        Map<String, Object> row = new HashMap<>();
        row.put("ID", 1);
        row.put("COUNTRY", "US");
        row.put("REGION_CODE", "CA");

        ObjectId id = ObjectSnapshots.targetId(order.getRelationship("region"), row);
        assertEquals(ObjectId.of("Region", Map.of("COUNTRY", "US", "CODE", "CA")), id);
    }

    @Test
    public void compoundFK_AllNull() {
        Map<String, Object> row = new HashMap<>();
        row.put("ID", 1);
        row.put("COUNTRY", null);
        row.put("REGION_CODE", null);

        assertNull(ObjectSnapshots.targetId(order.getRelationship("region"), row));
    }

    @Test
    public void compoundFK_PartiallyNull() {
        Map<String, Object> row = new HashMap<>();
        row.put("ID", 1);
        row.put("COUNTRY", "US");
        row.put("REGION_CODE", null);

        // a null in a mandatory FK column is an inconsistent snapshot
        assertThrows(CayenneRuntimeException.class,
                () -> ObjectSnapshots.targetId(order.getRelationship("region"), row));

        // a null in a nullable FK column is a null FK (CAY-284)
        row.put("ALT_COUNTRY", "US");
        row.put("ALT_REGION_CODE", null);
        assertNull(ObjectSnapshots.targetId(order.getRelationship("altRegion"), row));
    }

    @Test
    public void notToPrimaryKey() {
        Map<String, Object> row = new HashMap<>();
        row.put("ID", 1);
        row.put("CUSTOMER_ID", 7);
        row.put("CUSTOMER_NAME", "Acme");

        // to-many, FK to a non-PK column, flattened
        assertThrows(CayenneRuntimeException.class,
                () -> ObjectSnapshots.targetId(customer.getRelationship("orders"), row));
        assertThrows(CayenneRuntimeException.class,
                () -> ObjectSnapshots.targetId(order.getRelationship("customerByName"), row));
        assertThrows(CayenneRuntimeException.class,
                () -> ObjectSnapshots.targetId(order.getRelationship("customerAddress"), row));
    }

    private static DbEntity addDbEntity(DataMap map, String name, String... pkNames) {
        DbEntity entity = new DbEntity(name);
        for (String pkName : pkNames) {
            DbAttribute pk = new DbAttribute(pkName);
            pk.setPrimaryKey(true);
            pk.setMandatory(true);
            entity.addAttribute(pk);
        }
        map.addDbEntity(entity);
        return entity;
    }

    private static DbRelationship addDbRelationship(DbEntity source, String name, String target, boolean toMany) {
        DbRelationship relationship = new DbRelationship(name);
        source.addRelationship(relationship);
        relationship.setTargetEntityName(target);
        relationship.setToMany(toMany);
        return relationship;
    }

    private static ObjEntity addObjEntity(DataMap map, String name, String dbEntityName) {
        ObjEntity entity = new ObjEntity(name);
        entity.setDbEntityName(dbEntityName);
        map.addObjEntity(entity);
        return entity;
    }

    private static void addObjRelationship(ObjEntity source, String name, String target, String dbPath) {
        ObjRelationship relationship = new ObjRelationship(name);
        source.addRelationship(relationship);
        relationship.setTargetEntityName(target);
        relationship.setDbRelationshipPath(dbPath);
    }
}

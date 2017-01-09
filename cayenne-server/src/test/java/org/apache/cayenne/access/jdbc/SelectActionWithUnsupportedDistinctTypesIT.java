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

package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.unsupported_distinct_types.Customer;
import org.apache.cayenne.testdo.unsupported_distinct_types.Product;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.UNSUPPORTED_DISTINCT_TYPES_PROJECT)
public class SelectActionWithUnsupportedDistinctTypesIT extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    private TableHelper tProduct;
    private TableHelper tComposition;
    private TableHelper tCustomer;
    private TableHelper tOrders;

    @Before
    public void setUp() throws Exception {
        tProduct = new TableHelper(dbHelper, "PRODUCT");
        tProduct.setColumns("ID", "LONGVARCHAR_COL");

        tCustomer = new TableHelper(dbHelper, "CUSTOMER");
        tCustomer.setColumns("ID", "LONGVARCHAR_COL");

        tComposition = new TableHelper(dbHelper, "COMPOSITION");
        tComposition.setColumns("BASE_ID", "CONTAINED_ID");

        tOrders = new TableHelper(dbHelper, "ORDERS");
        tOrders.setColumns("CUSTOMER_ID", "PRODUCT_ID");
    }

    private void createCompositionManyToManyDataSet() throws SQLException {
        tProduct.insert(1, "product1");
        tProduct.insert(2, "product2");
        tProduct.insert(3, "product3");
        tProduct.insert(4, "product4");

        tComposition.insert(2, 1);
        tComposition.insert(3, 1);
        tComposition.insert(3, 2);
        tComposition.insert(4, 1);
        tComposition.insert(4, 2);
        tComposition.insert(4, 3);
    }

    private void createOrdersManyToManyDataSet() throws SQLException {
        tProduct.insert(1, "product1");
        tProduct.insert(2, "product2");
        tProduct.insert(3, "product3");

        tCustomer.insert(1, "customer1");
        tCustomer.insert(2, "customer2");
        tCustomer.insert(3, "customer3");

        tOrders.insert(1, 1);
        tOrders.insert(2, 1);
        tOrders.insert(2, 2);
        tOrders.insert(3, 1);
        tOrders.insert(3, 2);
        tOrders.insert(3, 3);
    }

    @Test
    public void testCompositionSelectManyToManyQuery() throws SQLException {
        createCompositionManyToManyDataSet();

        SelectQuery query = new SelectQuery(Product.class);
        query.addPrefetch("contained");
        query.addPrefetch("base");

        List<Product> result = context.performQuery(query);
        assertNotNull(result);

        for (Product product : result) {
            List<Product> productsContained = product.getContained();
            assertNotNull(productsContained);

            List<Product> productsBase = product.getBase();
            assertNotNull(productsBase);

            assertEquals(3, productsContained.size() + productsBase.size());
        }
    }

    @Test
    public void testOrdersSelectManyToManyQuery() throws SQLException {
        createOrdersManyToManyDataSet();
        List assertSizes = new ArrayList(3);
        assertSizes.addAll(Arrays.asList(1, 2, 3));

        SelectQuery productQuery = new SelectQuery(Product.class);
        productQuery.addPrefetch("orderBy");

        List<Product> productResult = context.performQuery(productQuery);
        assertNotNull(productResult);

        List orderBySizes = new ArrayList(3);
        for (Product product : productResult) {
            List<Customer> orderBy = product.getOrderBy();
            assertNotNull(orderBy);
            orderBySizes.add(orderBy.size());
        }
        assertTrue(assertSizes.containsAll(orderBySizes));


        SelectQuery customerQuery = new SelectQuery(Customer.class);
        customerQuery.addPrefetch("order");

        List<Customer> customerResult = context.performQuery(customerQuery);
        assertNotNull(customerResult);

        List orderSizes = new ArrayList(3);
        for (Customer customer : customerResult) {
            List<Product> orders = customer.getOrder();
            assertNotNull(orders);
            orderSizes.add(orders.size());
        }
        assertTrue(assertSizes.containsAll(orderSizes));
    }

}

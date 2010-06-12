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
package org.apache.cayenne.unit.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * A non-persistent Java Bean. Useful for testing Cayenne operations that rely on
 * introspection.
 */
public class TestBean {

    protected Integer integer;
    protected String string;
    protected String property1;
    protected int property2;
    protected Date dateProperty;
    protected Collection<?> collection;
    protected TestBean relatedBean;

    public static TestBean testFixtureWithCollection(
            String rootBaseName,
            String childBaseName) {
        TestBean root = new TestBean(rootBaseName, 0);

        Collection<TestBean> collection = new ArrayList<TestBean>(10);
        for (int i = 0; i < 10; i++) {
            collection.add(new TestBean(childBaseName + i, i));
        }

        root.setCollection(collection);
        return root;
    }

    public TestBean() {

    }

    public TestBean(String string, int intValue) {
        this.string = string;
        this.integer = new Integer(intValue);
    }

    public TestBean(int intValue) {
        integer = new Integer(intValue);
    }

    public Integer getInteger() {
        return integer;
    }

    public void setInteger(Integer integer) {
        this.integer = integer;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public String getProperty1() {
        return property1;
    }

    public void setProperty1(String property1) {
        this.property1 = property1;
    }

    public int getProperty2() {
        return property2;
    }

    public void setProperty2(int property2) {
        this.property2 = property2;
    }

    public Collection<?> getCollection() {
        return collection;
    }

    public void setCollection(Collection<?> collection) {
        this.collection = collection;
    }

    public Date getDateProperty() {
        return dateProperty;
    }

    public void setDateProperty(Date dateProperty) {
        this.dateProperty = dateProperty;
    }

    public TestBean getRelatedBean() {
        return relatedBean;
    }

    public void setRelatedBean(TestBean relatedBean) {
        this.relatedBean = relatedBean;
    }
}

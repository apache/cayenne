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

package org.apache.cayenne.xml;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.util.ToStringBuilder;
import org.apache.cayenne.util.Util;

/**
 */
public class TestObject extends CayenneDataObject {

    protected String name = "";
    protected int age;
    protected boolean open;
    protected List<TestObject> children = new ArrayList<TestObject>();
    protected TestObject parent = null;

    public TestObject() {
        this("", 0, false);
    }

    public TestObject(String name, int age, boolean open) {
        this.name = name;
        this.age = age;
        this.open = open;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public TestObject getParent() {
        return parent;
    }

    public void setParent(TestObject parent) {
        this.parent = parent;
    }

    public void setChildren(List<TestObject> children) {
        this.children = children;
    }

    public void addChild(TestObject child) {
        children.add(child);
    }

    public void removeChild(TestObject child) {
        children.remove(child);
    }

    public List<TestObject> getChildren() {
        return children;
    }

    @Override
    public boolean equals(Object o) {
        if (null == o || !(o instanceof TestObject)) {
            return false;
        }

        TestObject test = (TestObject) o;

        if (!Util.nullSafeEquals(name, test.getName())) {
            return false;
        }

        if (!Util.nullSafeEquals(parent, test.getParent())) {
            return false;
        }

        return ((test.getAge() == age) && (test.isOpen() == open));
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.setRoot("Test", this.getClass().getName());

        // "parent" must come first to fully test 1-to-1 relationships, per CAY-597.
        encoder.encodeProperty("parent", parent);
        encoder.encodeProperty("name", name);
        encoder.encodeProperty("age", new Integer(age));
        encoder.encodeProperty("open", Boolean.valueOf(open));
        encoder.encodeProperty("children", children);
    }

    @Override
    public void decodeFromXML(XMLDecoder decoder) {

        if (null != decoder.decodeObject("parent")) {
            parent = (TestObject) decoder.decodeObject("parent");
        }

        if (null != decoder.decodeInteger("age")) {
            age = decoder.decodeInteger("age").intValue();
        }

        if (null != decoder.decodeBoolean("open")) {
            open = decoder.decodeBoolean("open").booleanValue();
        }

        name = decoder.decodeString("name");
        children = (List<TestObject>) decoder.decodeObject("children");
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("parent", parent)
                .append("name", name)
                .append("age", age)
                .append("open", open)
                .append("children#", children != null ? children.size() : -1)
                .toString();
    }
}

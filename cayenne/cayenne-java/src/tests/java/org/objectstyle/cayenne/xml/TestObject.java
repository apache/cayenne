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
package org.objectstyle.cayenne.xml;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrei Adamchik
 */
public class TestObject extends CayenneDataObject {

    protected String name = "";
    protected int age;
    protected boolean open;
    protected List children = new ArrayList();
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

    public void setChildren(List children) {
        this.children = children;
    }

    public void addChild(TestObject child) {
        children.add(child);
    }

    public void removeChild(TestObject child) {
        children.remove(child);
    }

    public List getChildren() {
        return children;
    }

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

    public void encodeAsXML(XMLEncoder encoder) {
        encoder.setRoot("Test", this.getClass().getName());
        
        // "parent" must come first to fully test 1-to-1 relationships, per CAY-597.
        encoder.encodeProperty("parent", parent);
        encoder.encodeProperty("name", name);
        encoder.encodeProperty("age", new Integer(age));
        encoder.encodeProperty("open", new Boolean(open));
        encoder.encodeProperty("children", children);
    }

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
        children = (List) decoder.decodeObject("children");
    }

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

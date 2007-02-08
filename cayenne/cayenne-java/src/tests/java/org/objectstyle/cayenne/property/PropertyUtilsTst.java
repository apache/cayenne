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
package org.objectstyle.cayenne.property;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class PropertyUtilsTst extends TestCase {

    public void testGetProperty() {
        TestJavaBean o1 = createBean();

        assertSame(o1.getByteArrayField(), PropertyUtils
                .getProperty(o1, "byteArrayField"));
        assertSame(o1.getIntegerField(), PropertyUtils.getProperty(o1, "integerField"));
        assertEquals(new Integer(o1.getIntField()), PropertyUtils.getProperty(
                o1,
                "intField"));
        assertSame(o1.getNumberField(), PropertyUtils.getProperty(o1, "numberField"));
        assertSame(o1.getObjectField(), PropertyUtils.getProperty(o1, "objectField"));
        assertSame(o1.getStringField(), PropertyUtils.getProperty(o1, "stringField"));
        assertEquals(new Boolean(o1.isBooleanField()), PropertyUtils.getProperty(
                o1,
                "booleanField"));
    }

    public void testSetProperty() {
        TestJavaBean o1 = createBean();
        TestJavaBean o2 = new TestJavaBean();

        PropertyUtils.setProperty(o2, "byteArrayField", o1.getByteArrayField());
        PropertyUtils.setProperty(o2, "integerField", o1.getIntegerField());
        PropertyUtils.setProperty(o2, "intField", new Integer(o1.getIntField()));
        PropertyUtils.setProperty(o2, "numberField", o1.getNumberField());
        PropertyUtils.setProperty(o2, "objectField", o1.getObjectField());
        PropertyUtils.setProperty(o2, "stringField", o1.getStringField());
        PropertyUtils.setProperty(o2, "booleanField", new Boolean(o1.isBooleanField()));
    }

    public void testGetPropertyMap() {
        Map o1 = createMap();

        assertSame(o1.get("byteArrayField"), PropertyUtils.getProperty(o1, "byteArrayField"));
        assertSame(o1.get("integerField"), PropertyUtils.getProperty(o1, "integerField"));
        assertEquals(o1.get("intField"), PropertyUtils.getProperty(o1, "intField"));
        assertSame(o1.get("numberField"), PropertyUtils.getProperty(o1, "numberField"));
        assertSame(o1.get("objectField"), PropertyUtils.getProperty(o1, "objectField"));
        assertSame(o1.get("stringField"), PropertyUtils.getProperty(o1, "stringField"));
        assertEquals(o1.get("booleanField"), PropertyUtils.getProperty(o1, "booleanField"));
    }
    
    public void testSetPropertyMap() {
        Map o1 = createMap();
        Map o2 = new HashMap();

        PropertyUtils.setProperty(o2, "byteArrayField", o1.get("byteArrayField"));
        PropertyUtils.setProperty(o2, "integerField", o1.get("integerField"));
        PropertyUtils.setProperty(o2, "intField", o1.get("intField"));
        PropertyUtils.setProperty(o2, "numberField", o1.get("numberField"));
        PropertyUtils.setProperty(o2, "objectField", o1.get("objectField"));
        PropertyUtils.setProperty(o2, "stringField", o1.get("stringField"));
        PropertyUtils.setProperty(o2, "booleanField", o1.get("booleanField"));
        
        assertEquals(o1, o2);
    }


    public void testSetConverted() {
        TestJavaBean o1 = new TestJavaBean();

        // Object -> String
        Object object = new Object();
        PropertyUtils.setProperty(o1, "stringField", object);
        assertEquals(object.toString(), o1.getStringField());

        // String to number
        PropertyUtils.setProperty(o1, "integerField", "25");
        assertEquals(new Integer(25), o1.getIntegerField());

        // string to primitive
        PropertyUtils.setProperty(o1, "intField", "28");
        assertEquals(28, o1.getIntField());
    }
    
    

    public void testSetNull() {
        TestJavaBean o1 = new TestJavaBean();

        o1.setStringField("xyz");
        PropertyUtils.setProperty(o1, "stringField", null);
        assertNull(o1.getStringField());

        o1.setIntField(99);
        PropertyUtils.setProperty(o1, "intField", null);
        assertEquals(0, o1.getIntField());
    }

    protected TestJavaBean createBean() {
        TestJavaBean o1 = new TestJavaBean();
        o1.setByteArrayField(new byte[] {
                1, 2, 3
        });
        o1.setIntegerField(new Integer(33));
        o1.setIntField(-44);
        o1.setNumberField(new BigDecimal("11111"));
        o1.setObjectField(new Object());
        o1.setStringField("aaaaa");
        o1.setBooleanField(true);

        return o1;
    }

    protected Map createMap() {
        Map o1 = new HashMap();
        o1.put("byteArrayField", new byte[] {
                1, 2, 3
        });
        o1.put("integerField", new Integer(33));
        o1.put("intField", new Integer(-44));
        o1.put("numberField", new BigDecimal("11111"));
        o1.put("objectField", new Object());
        o1.put("stringField", "aaaaa");
        o1.put("booleanField", Boolean.TRUE);

        return o1;
    }
}

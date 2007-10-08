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
package org.objectstyle.cayenne.conf;

import java.sql.Connection;
import java.util.Properties;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.objectstyle.cayenne.ConfigurationException;

import junit.framework.TestCase;

/**
 * @author Andrei Adamchik
 */
public class DBCPDataSourceFactoryTst extends TestCase {

    public void testStringProperty() {
        DBCPDataSourceFactory factory = new DBCPDataSourceFactory();
        Properties props = new Properties();
        props.put("a", "X");
        props.put("cayenne.dbcp.c", "Y");

        assertNull(factory.stringProperty(props, "a"));
        assertNull(factory.stringProperty(props, "b"));
        assertEquals("Y", factory.stringProperty(props, "c"));
    }

    public void testIntProperty() {
        DBCPDataSourceFactory factory = new DBCPDataSourceFactory();
        Properties props = new Properties();
        props.put("a", "10");
        props.put("cayenne.dbcp.b", "11");
        props.put("cayenne.dbcp.d", "**");

        assertEquals(11, factory.intProperty(props, "b", -1));
        assertEquals(-1, factory.intProperty(props, "a", -1));
        assertEquals(-1, factory.intProperty(props, "c", -1));
        assertEquals(-2, factory.intProperty(props, "d", -2));
    }

    public void testWhenExhaustedAction() throws Exception {
        DBCPDataSourceFactory factory = new DBCPDataSourceFactory();
        Properties props = new Properties();
        props.put("cayenne.dbcp.a", "1");
        props.put("cayenne.dbcp.b", "WHEN_EXHAUSTED_BLOCK");
        props.put("cayenne.dbcp.c", "WHEN_EXHAUSTED_GROW");
        props.put("cayenne.dbcp.d", "WHEN_EXHAUSTED_FAIL");
        props.put("cayenne.dbcp.e", "garbage");

        assertEquals(1, factory.whenExhaustedAction(props, "a", (byte) 100));
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_BLOCK, factory.whenExhaustedAction(
                props,
                "b",
                (byte) 100));
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_GROW, factory.whenExhaustedAction(
                props,
                "c",
                (byte) 100));
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_FAIL, factory.whenExhaustedAction(
                props,
                "d",
                (byte) 100));

        try {
            factory.whenExhaustedAction(props, "e", (byte) 100);
            fail("must throw on invalid key");
        }
        catch (ConfigurationException ex) {
            // expected
        }

        assertEquals(100, factory.whenExhaustedAction(props, "f", (byte) 100));
    }

    public void testTransactionIsolation() throws Exception {
        DBCPDataSourceFactory factory = new DBCPDataSourceFactory();
        Properties props = new Properties();
        props.put("cayenne.dbcp.a", "1");
        props.put("cayenne.dbcp.b", "TRANSACTION_NONE");
        props.put("cayenne.dbcp.c", "TRANSACTION_READ_UNCOMMITTED");
        props.put("cayenne.dbcp.d", "TRANSACTION_SERIALIZABLE");
        props.put("cayenne.dbcp.e", "garbage");

        assertEquals(1, factory.defaultTransactionIsolation(props, "a", (byte) 100));
        assertEquals(Connection.TRANSACTION_NONE, factory.defaultTransactionIsolation(
                props,
                "b",
                (byte) 100));
        assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, factory
                .defaultTransactionIsolation(props, "c", (byte) 100));
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, factory
                .defaultTransactionIsolation(props, "d", (byte) 100));

        try {
            factory.defaultTransactionIsolation(props, "e", (byte) 100);
            fail("must throw on invalid key");
        }
        catch (ConfigurationException ex) {
            // expected
        }

        assertEquals(100, factory.defaultTransactionIsolation(props, "f", (byte) 100));
    }
}
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

import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.collections.ExtendedProperties;
import org.objectstyle.cayenne.conn.DataSourceInfo;

/**
 * @author Andrei Adamchik
 */
public class ConnectionPropertiesTst extends TestCase {

    /**
     * Constructor for ConnectionPropertiesTst.
     * @param arg0
     */
    public ConnectionPropertiesTst(String arg0) {
        super(arg0);
    }

    public void testBuildDataSourceInfo() throws Exception {
        ConnectionProperties ps = new ConnectionProperties(new ExtendedProperties());
        
        ExtendedProperties props = new ExtendedProperties();
        props.setProperty(ConnectionProperties.ADAPTER_KEY, "1");
        props.setProperty(ConnectionProperties.DRIVER_KEY, "2");
        props.setProperty(ConnectionProperties.PASSWORD_KEY, "3");
        props.setProperty(ConnectionProperties.URL_KEY, "4");
        props.setProperty(ConnectionProperties.USER_NAME_KEY, "5");

        DataSourceInfo dsi = ps.buildDataSourceInfo(props);   
        
        assertEquals("1", dsi.getAdapterClassName());     
        assertEquals("2", dsi.getJdbcDriver());     
        assertEquals("3", dsi.getPassword());     
        assertEquals("4", dsi.getDataSourceUrl());     
        assertEquals("5", dsi.getUserName());     
    }

    public void testExtractNames() throws Exception {
        ConnectionProperties ps = new ConnectionProperties(new ExtendedProperties());

        ExtendedProperties props = new ExtendedProperties();
        props.setProperty("a.1", "a");
        props.setProperty("a.2", "a");
        props.setProperty("b.3", "a");
        props.setProperty("c.4", "a");

        List names = ps.extractNames(props);
        assertNotNull(names);
        assertEquals(3, names.size());
        assertTrue(names.contains("a"));
        assertTrue(names.contains("b"));
        assertTrue(names.contains("c"));
    }
}

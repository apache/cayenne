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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrus Adamchik
 */
public class ConfigLoaderTst extends CayenneTestCase {

    public void testCase1() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf
                .append("<?xml version='1.0' encoding='utf-8'?>")
                .append("\n<domains>")
                .append("\n   <domain name='domain1'>")
                .append("\n   </domain>")
                .append("\n </domains>");

        ConfigLoaderCase aCase = new ConfigLoaderCase();
        aCase.setConfigInfo(buf.toString());
        aCase.setTotalDomains(1);
        runCase(aCase);
    }

    public void testCase2() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf
                .append("<?xml version='1.0' encoding='utf-8'?>")
                .append("\n<domains>")
                .append("\n   <domain name='domain1'>")
                .append("\n   <map name='m1' location='aaa'/>")
                .append("\n   </domain>")
                .append("\n </domains>");

        ConfigLoaderCase aCase = new ConfigLoaderCase();
        aCase.setConfigInfo(buf.toString());
        aCase.setTotalDomains(1);
        aCase.setFailedMaps(1);
        runCase(aCase);
    }

    public void testCase3() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf
                .append("<?xml version='1.0' encoding='utf-8'?>")
                .append("\n<domains>")
                .append("\n   <domain name='domain1'>")
                .append("\n   <map name='m1' location='testmap.map.xml'/>")
                .append("\n   </domain>")
                .append("\n </domains>");

        ConfigLoaderCase aCase = new ConfigLoaderCase();
        aCase.setConfigInfo(buf.toString());
        aCase.setTotalDomains(1);
        runCase(aCase);
    }

    public void testCase4() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf
                .append("<?xml version='1.0' encoding='utf-8'?>")
                .append("\n<domains>")
                .append("\n   <domain name='domain1'>")
                .append(
                        "\n        <map name='m1' location='testmap.map.xml'/>")
                .append("\n        <node name='db1' datasource='node.xml'")
                .append(
                        "\n              factory='org.objectstyle.cayenne.conf.DriverDataSourceFactory'")
                .append(
                        "\n              adapter='org.objectstyle.cayenne.dba.mysql.MySQLAdapter'>")
                .append("\n        </node>")
                .append("\n   </domain>")
                .append("\n </domains>");

        ConfigLoaderCase aCase = new ConfigLoaderCase();
        aCase.setConfigInfo(buf.toString());
        aCase.setFailedDataSources(1);
        aCase.setTotalDomains(1);
        runCase(aCase);
    }

    public void testCase5() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf
                .append("<?xml version='1.0' encoding='utf-8'?>")
                .append("\n<domains>")
                .append("\n   <domain name='domain1'>")
                .append(
                        "\n        <map name='m1' location='testmap.map.xml'/>")
                .append("\n        <node name='db1' datasource='node.xml'")
                .append(
                        "\n              factory='org.objectstyle.cayenne.conf.DriverDataSourceFactory'")
                .append(
                        "\n              adapter='org.objectstyle.cayenne.dba.mysql.MySQLAdapter'>")
                .append("\n              <map-ref name='m1'/>")
                .append("\n        </node>")
                .append("\n   </domain>")
                .append("\n </domains>");

        ConfigLoaderCase aCase = new ConfigLoaderCase();
        aCase.setConfigInfo(buf.toString());
        aCase.setFailedDataSources(1);
        aCase.setTotalDomains(1);
        runCase(aCase);
    }

    public void testCase6() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf
                .append("<?xml version='1.0' encoding='utf-8'?>")
                .append("\n<domains>")
                .append("\n   <domain name='domain1'>")
                .append("\n   <map name='m1' location='testmap.map.xml'/>")
                .append("\n   </domain>")
                .append(
                        "\n   <view name='v1' location='testview1.view.xml'/>")
                .append(
                        "\n   <view name='v2' location='testview2.view.xml'/>")
                .append("\n </domains>");

        ConfigLoaderCase aCase = new ConfigLoaderCase();
        aCase.setConfigInfo(buf.toString());
        aCase.setTotalDomains(1);
        runCase(aCase);
    }

    public void testCase7() throws Exception {
        StringBuffer buf = new StringBuffer();
        buf
                .append("<?xml version='1.0' encoding='utf-8'?>")
                .append("\n<domains>")
                .append("\n<domain name='d1'>")
                .append("\n<node name='n1' datasource='node.xml'")
                .append("\nfactory='org.objectstyle.cayenne.conf.MockDataSourceFactory'")
                .append("\nadapter='org.objectstyle.cayenne.dba.mysql.MySQLAdapter'/>")
                .append("\n</domain>")
                .append("\n<domain name='d2'>")
                .append("\n<node name='n2' datasource='node.xml'")
                .append("\nfactory='org.objectstyle.cayenne.conf.MockDataSourceFactory1'")
                .append("\nadapter='org.objectstyle.cayenne.dba.mysql.MySQLAdapter'/>")
                .append("\n</domain>")
                .append("\n</domains>");

        final List nodes = new ArrayList();

        MockConfigLoaderDelegate delegate = new MockConfigLoaderDelegate() {

            public void shouldLoadDataNode(
                    String domainName,
                    String nodeName,
                    String dataSource,
                    String adapter,
                    String factory) {

                NodeLoadState state = new NodeLoadState();
                state.domainName = domainName;
                state.nodeName = nodeName;
                state.factory = factory;
                nodes.add(state);
            }
        };
        ConfigLoader helper = new ConfigLoader(delegate);
        helper.loadDomains(new ByteArrayInputStream(buf.toString().getBytes()));

        assertEquals(2, nodes.size());
        
        NodeLoadState s1 = (NodeLoadState) nodes.get(0);
        assertEquals("d1", s1.domainName);
        assertEquals("n1", s1.nodeName);
        assertEquals("org.objectstyle.cayenne.conf.MockDataSourceFactory", s1.factory);
        
        NodeLoadState s2 = (NodeLoadState) nodes.get(1);
        assertEquals("d2", s2.domainName);
        assertEquals("n2", s2.nodeName);
        assertEquals("org.objectstyle.cayenne.conf.MockDataSourceFactory1", s2.factory);
    }

    private void runCase(ConfigLoaderCase aCase) throws Exception {
        Configuration conf = new EmptyConfiguration();
        ConfigLoaderDelegate delegate = conf.getLoaderDelegate();
        ConfigLoader helper = new ConfigLoader(delegate);
        aCase.test(helper);
    }

    class NodeLoadState {

        String domainName;
        String nodeName;
        String factory;
    }
}

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

package org.apache.cayenne.conf;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 */
public class ConfigLoaderTest extends TestCase {

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
                .append("\n        <map name='m1' location='testmap.map.xml'/>")
                .append("\n        <node name='db1' datasource='node.xml'")
                .append(
                        "\n              factory='org.apache.cayenne.conf.DriverDataSourceFactory'")
                .append(
                        "\n              adapter='org.apache.cayenne.dba.mysql.MySQLAdapter'>")
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
                .append("\n        <map name='m1' location='testmap.map.xml'/>")
                .append("\n        <node name='db1' datasource='node.xml'")
                .append(
                        "\n              factory='org.apache.cayenne.conf.DriverDataSourceFactory'")
                .append(
                        "\n              adapter='org.apache.cayenne.dba.mysql.MySQLAdapter'>")
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
                .append("\n   <view name='v1' location='testview1.view.xml'/>")
                .append("\n   <view name='v2' location='testview2.view.xml'/>")
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
                .append("\nfactory='org.apache.cayenne.conf.MockDataSourceFactory'")
                .append("\nadapter='org.apache.cayenne.dba.mysql.MySQLAdapter'/>")
                .append("\n</domain>")
                .append("\n<domain name='d2'>")
                .append("\n<node name='n2' datasource='node.xml'")
                .append("\nfactory='org.apache.cayenne.conf.MockDataSourceFactory1'")
                .append(
                        "\nschema-update-strategy='org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy'")
                .append("\nadapter='org.apache.cayenne.dba.mysql.MySQLAdapter'/>")
                .append("\n</domain>")
                .append("\n</domains>");

        final List nodes = new ArrayList();

        MockConfigLoaderDelegate delegate = new MockConfigLoaderDelegate() {

            @Override
            public void shouldLoadDataNode(
                    String domainName,
                    String nodeName,
                    String dataSource,
                    String adapter,
                    String factory,
                    String schemaUpdateStrategy) {

                NodeLoadState state = new NodeLoadState();
                state.domainName = domainName;
                state.nodeName = nodeName;
                state.factory = factory;
                state.schemaUpdateStrategy = schemaUpdateStrategy;
                nodes.add(state);
            }
        };
        ConfigLoader helper = new ConfigLoader(delegate);
        helper.loadDomains(new ByteArrayInputStream(buf.toString().getBytes()));

        assertEquals(2, nodes.size());

        NodeLoadState s1 = (NodeLoadState) nodes.get(0);
        assertEquals("d1", s1.domainName);
        assertEquals("n1", s1.nodeName);
        assertEquals("org.apache.cayenne.conf.MockDataSourceFactory", s1.factory);
        assertEquals(
                "org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy",
                s1.schemaUpdateStrategy);

        NodeLoadState s2 = (NodeLoadState) nodes.get(1);
        assertEquals("d2", s2.domainName);
        assertEquals("n2", s2.nodeName);
        assertEquals("org.apache.cayenne.conf.MockDataSourceFactory1", s2.factory);
        assertEquals(
                "org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy",
                s2.schemaUpdateStrategy);
    }

    private void runCase(ConfigLoaderCase aCase) throws Exception {
        Configuration conf = new EmptyConfiguration();
        ConfigLoaderDelegate delegate = conf.getLoaderDelegate();
        ConfigLoader helper = new ConfigLoader(delegate);
        aCase.test(helper);
    }

    class NodeLoadState {

        String schemaUpdateStrategy;
        String domainName;
        String nodeName;
        String factory;
    }
}

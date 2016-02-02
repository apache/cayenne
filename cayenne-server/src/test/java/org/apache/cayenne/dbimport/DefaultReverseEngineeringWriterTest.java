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


package org.apache.cayenne.dbimport;

import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.custommonkey.xmlunit.Diff;
import org.junit.Test;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import static org.junit.Assert.assertTrue;

public class DefaultReverseEngineeringWriterTest {

    @Test
    public void testWriteReverseEngineering() throws Exception {
        ReverseEngineeringWriter engineering = new DefaultReverseEngineeringWriter();
        ReverseEngineering reverseEngineering = new ReverseEngineering();

        Catalog catalog1 = new Catalog("catalog1");
        Catalog catalog2 = new Catalog("catalog2");
        Catalog catalog3 = new Catalog("catalog3");

        catalog1.addSchema(new Schema("schema1"));
        catalog1.addSchema(new Schema("schema2"));

        catalog1.addExcludeColumn(new ExcludeColumn("excludedColumn1"));
        catalog1.addExcludeColumn(new ExcludeColumn("excludedColumn2"));
        catalog1.addIncludeColumn(new IncludeColumn("includedColumn1"));
        catalog1.addIncludeColumn(new IncludeColumn("includedColumn2"));

        catalog1.addExcludeProcedure(new ExcludeProcedure("excludedProcedure1"));
        catalog1.addExcludeProcedure(new ExcludeProcedure("excludedProcedure2"));
        catalog1.addIncludeProcedure(new IncludeProcedure("includedProcedure1"));
        catalog1.addIncludeProcedure(new IncludeProcedure("includedProcedure2"));

        catalog1.addExcludeTable(new ExcludeTable("excludedTable1"));
        catalog1.addExcludeTable(new ExcludeTable("excludedTable2"));
        catalog1.addIncludeTable(new IncludeTable("includedTable1"));
        catalog1.addIncludeTable(new IncludeTable("includedTable2"));

        reverseEngineering.addCatalog(catalog1);
        reverseEngineering.addCatalog(catalog2);
        reverseEngineering.addCatalog(catalog3);

        reverseEngineering.addSchema(new Schema("schema3"));

        URL url = getClass().getResource("reverseEngineering.xml");
        String decodedURL = URLDecoder.decode(url.getPath(), "UTF-8");
        Writer printWriter = new PrintWriter(decodedURL);

        reverseEngineering.setConfigurationSource(new URLResource(url));
        Resource reverseEngineeringResource = engineering.write(reverseEngineering, printWriter);
        assertReverseEngineering(reverseEngineeringResource);
    }

    public void assertReverseEngineering(Resource resource) throws Exception {
        URL url1 = resource.getURL();
        URL url2 = getResource("reverseEngineering-expected.xml").getURL();

        FileReader writedXML;
        FileReader expectedXML;
        writedXML = new FileReader(URLDecoder.decode(url1.getPath(), "UTF-8"));
        expectedXML = new FileReader(URLDecoder.decode(url2.getPath(), "UTF-8"));
        Diff diff = new Diff(writedXML, expectedXML);
        assertTrue(diff.identical());
    }

    protected URLResource getResource(String file) throws MalformedURLException {
        return new URLResource(getClass().getResource(file));
    }
}

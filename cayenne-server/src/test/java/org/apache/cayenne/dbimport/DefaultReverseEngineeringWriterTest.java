/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.dbimport;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.junit.Test;
import org.xml.sax.SAXException;
import sun.security.util.Resources;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertTrue;

/*
 * @since 4.0
 */

public class DefaultReverseEngineeringWriterTest {

    @Test
    public void testWriteReverseEngineering() throws Exception {
        ReverseEngineeringWriter engineering = new DefaultReverseEngineeringWriter();
        ReverseEngineering reverseEngineering = new ReverseEngineering();
        Resource resource = getResource("reverseEngineering.xml");
        reverseEngineering.setConfigurationSource(resource);

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

        File file = new File(resource.getURL().getPath());
        PrintWriter printWriter = new PrintWriter(new FileWriter(file));

        assertReverseEngineering(engineering.write(reverseEngineering, printWriter));
    }

    public void assertReverseEngineering(Resource resource) throws Exception {
        URL url1 = resource.getURL();
        URL url2 = getResource("reverseEngineering-expected.xml").getURL();

        FileReader writedXML;
        FileReader expectedXML;
        writedXML = new FileReader(url1.getPath());
        expectedXML = new FileReader(url2.getPath());
        Diff diff = new Diff(writedXML, expectedXML);
        assertTrue(diff.identical());
    }

    protected URLResource getResource(String file) throws MalformedURLException {
        return new URLResource(getClass().getResource(file));
    }
}

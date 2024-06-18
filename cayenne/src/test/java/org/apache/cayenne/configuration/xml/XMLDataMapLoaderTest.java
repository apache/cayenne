/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.resource.URLResource;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.XMLReader;

import java.net.URL;

import static org.junit.Assert.*;

public class XMLDataMapLoaderTest {

    private Injector injector;

    private DataMapLoader loader;

    @Before
    public void setUp() throws Exception {
        Module testModule = binder -> {
            binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
            binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            binder.bind(HandlerFactory.class).to(DefaultHandlerFactory.class);
            binder.bind(DataChannelMetaData.class).to(NoopDataChannelMetaData.class);
            binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();
        };

        injector = DIBootstrap.createInjector(testModule);
        loader = injector.getInstance(DataMapLoader.class);
    }

    @Test(expected = CayenneRuntimeException.class)
    public void loadMissingConfig() throws Exception {
        loader.load(new URLResource(new URL("file:/no_such_file_for_map_xml")));
    }

    @Test(expected = CayenneRuntimeException.class)
    public void loadWrongVersionConfig() throws Exception {
        URL url = getClass().getResource("testConfigMap5.map.xml");
        loader.load(new URLResource(url));
    }

    @Test
    public void loadEmptyConfig() throws Exception {
        URL url = getClass().getResource("testConfigMap2.map.xml");
        DataMap map = loader.load(new URLResource(url));

        assertNotNull(map);
        assertEquals("testConfigMap2", map.getName());
        assertTrue(map.getDbEntities().isEmpty());
        assertTrue(map.getObjEntities().isEmpty());
        assertTrue(map.getProcedures().isEmpty());
        assertTrue(map.getQueryDescriptors().isEmpty());
        assertTrue(map.getEmbeddables().isEmpty());
        assertNull(map.getDefaultCatalog());
        assertNull(map.getDefaultSchema());
        assertNull(map.getDefaultPackage());
    }

    @Test
    public void loadFullDataMap() {
        URL url = getClass().getResource("testConfigMap4.map.xml");
        DataMap map = loader.load(new URLResource(url));

        assertNotNull(map);
        assertEquals("testConfigMap4", map.getName());

        // check general state
        assertEquals(12, map.getDbEntities().size());
        assertEquals(17, map.getObjEntities().size());
        assertEquals(4, map.getProcedures().size());
        assertEquals(14, map.getQueryDescriptors().size());
        assertEquals(1, map.getEmbeddables().size());
        assertEquals("TEST_CATALOG", map.getDefaultCatalog());
        assertNull(map.getDefaultSchema());
        assertEquals("org.apache.cayenne.testdo.testmap", map.getDefaultPackage());

        // check some loaded content
        assertEquals("org.apache.cayenne.testdo.testmap.Artist",
                map.getObjEntity("Artist").getClassName());
        assertEquals(5,
                map.getObjEntity("CompoundPainting").getAttributes().size());
        assertEquals(3,
                map.getObjEntity("Artist").getRelationships().size());
        assertEquals(7,
                map.getObjEntity("ArtistCallback").getCallbackMethods().size());

        assertEquals("name = \"test\"",
                map.getDbEntity("ARTGROUP").getQualifier().toString());
        assertEquals(4,
                map.getDbEntity("EXHIBIT").getAttributes().size());
        assertEquals(3,
                map.getDbEntity("PAINTING").getRelationships().size());
        assertEquals("gallery_seq",
                map.getDbEntity("GALLERY").getPrimaryKeyGenerator().getGeneratorName());

        DbAttribute pk1 = map.getDbEntity("EXHIBIT").getAttribute("EXHIBIT_ID");
        assertFalse(pk1.isGenerated());
        assertTrue(pk1.isPrimaryKey());

        DbAttribute pk2 = map.getDbEntity("GENERATED_COLUMN").getAttribute("GENERATED_COLUMN");
        assertTrue(pk2.isGenerated());
        assertTrue(pk2.isPrimaryKey());

        assertEquals(true,
                map.getProcedure("cayenne_tst_out_proc").isReturningValue());
        assertEquals(1,
                map.getProcedure("cayenne_tst_out_proc").getCallOutParameters().size());
        assertEquals(2,
                map.getProcedure("cayenne_tst_out_proc").getCallParameters().size());

        assertEquals("true",
                map.getQueryDescriptor("EjbqlQueryTest")
                        .getProperty("cayenne.GenericSelectQuery.fetchingDataRows"));

        SQLTemplateDescriptor descriptor = (SQLTemplateDescriptor)map.getQueryDescriptor("NonSelectingQuery");
        assertEquals("INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ESTIMATED_PRICE) " +
                        "VALUES (512, 'No Painting Like This', 12.5)",
                descriptor.getAdapterSql().get("org.apache.cayenne.dba.db2.DB2Adapter"));

        assertEquals("TEST",
                map.getEmbeddable("org.apache.cayenne.testdo.Embeddable")
                        .getAttribute("test").getDbAttributeName());
    }

}

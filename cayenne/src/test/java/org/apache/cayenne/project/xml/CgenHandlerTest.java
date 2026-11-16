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

package org.apache.cayenne.project.xml;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.ProjectLoadingMode;
import org.apache.cayenne.project.cgen.CgenDescriptor;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CgenHandlerTest extends BaseHandlerTest {

    private static final String CGEN = """
            <dataMap>
                <cgen>
                    <name>Custom</name>
                    <excludeEntities>Painting,Artist</excludeEntities>
                    <excludeEmbeddables>org.example.Phone</excludeEmbeddables>
                    <destDir>../java</destDir>
                    <mode>all</mode>
                    <template>subclass &amp; more</template>
                    <superTemplate>superclass.vm</superTemplate>
                    <embeddableTemplate>embeddable-subclass.vm</embeddableTemplate>
                    <embeddableSuperTemplate>embeddable-superclass.vm</embeddableSuperTemplate>
                    <dataMapTemplate><![CDATA[
                        datamap subclass
                    ]]></dataMapTemplate>
                    <dataMapSuperTemplate>datamap-superclass.vm</dataMapSuperTemplate>
                    <outputPattern>*.groovy</outputPattern>
                    <makePairs>false</makePairs>
                    <skipRelationshipsLoading>true</skipRelationshipsLoading>
                    <usePkgPath>false</usePkgPath>
                    <overwrite>true</overwrite>
                    <createPropertyNames>true</createPropertyNames>
                    <superPkg>org.example.auto</superPkg>
                    <createPKProperties>false</createPKProperties>
                    <externalToolConfig>tools.properties</externalToolConfig>
                </cgen>
                <cgen>
                    <name>Second</name>
                </cgen>
                <dbEntity name="ARTIST">
                    <dbAttribute name="ARTIST_ID" type="INTEGER"/>
                </dbEntity>
            </dataMap>
            """;

    @Test
    public void loadCgen() throws Exception {
        DataMap map = parseDataMap(CGEN, ProjectLoadingMode.FULL);

        assertEquals(2, map.getCgenDescriptors().size());
        CgenDescriptor descriptor = map.getCgenDescriptors().get(0);
        assertEquals("Custom", descriptor.getName());
        assertEquals(List.of("Painting", "Artist"), List.copyOf(descriptor.getExcludedEntities()));
        assertEquals(List.of("org.example.Phone"), List.copyOf(descriptor.getExcludedEmbeddables()));
        assertEquals("../java", descriptor.getDestDir());
        assertEquals("all", descriptor.getMode());
        assertEquals("subclass & more", descriptor.getTemplate());
        assertEquals("superclass.vm", descriptor.getSuperTemplate());
        assertEquals("embeddable-subclass.vm", descriptor.getEmbeddableTemplate());
        assertEquals("embeddable-superclass.vm", descriptor.getEmbeddableSuperTemplate());
        // not trimmed
        assertEquals("\n            datamap subclass\n        ", descriptor.getDataMapTemplate());
        assertEquals("datamap-superclass.vm", descriptor.getDataMapSuperTemplate());
        assertEquals("*.groovy", descriptor.getOutputPattern());
        assertFalse(descriptor.isMakePairs());
        assertFalse(descriptor.isUsePkgPath());
        assertTrue(descriptor.isOverwrite());
        assertTrue(descriptor.isCreatePropertyNames());
        assertEquals("org.example.auto", descriptor.getSuperPkg());
        assertFalse(descriptor.isCreatePKProperties());
        assertEquals("tools.properties", descriptor.getExternalToolConfig());

        CgenDescriptor defaults = new CgenDescriptor();
        defaults.setName("Second");
        assertEquals(encode(defaults), encode(map.getCgenDescriptors().get(1)));

        assertNotNull(map.getDbEntity("ARTIST"));
    }

    @Test
    public void skipCgen() throws Exception {
        DataMap map = parseDataMap(CGEN, ProjectLoadingMode.RUNTIME);

        assertTrue(map.getCgenDescriptors().isEmpty());
        assertNotNull(map.getDbEntity("ARTIST"));
    }

    @Test
    public void skipCgenOfOtherNamespace() throws Exception {
        // the pre-v15 "cgen" extension namespace, not converted in a current version DataMap
        DataMap map = parseDataMap("""
                <dataMap>
                    <cgen xmlns="http://cayenne.apache.org/schema/15/cgen">
                        <name>Custom</name>
                    </cgen>
                </dataMap>
                """, ProjectLoadingMode.FULL);

        assertTrue(map.getCgenDescriptors().isEmpty());
    }

    @Test
    public void loadCgenBlankValues() throws Exception {
        DataMap map = parseDataMap("""
                <dataMap>
                    <cgen>
                        <name> </name>
                        <excludeEntities></excludeEntities>
                        <destDir> </destDir>
                        <mode> </mode>
                        <template>
                        </template>
                        <outputPattern></outputPattern>
                        <makePairs> </makePairs>
                        <createPKProperties></createPKProperties>
                        <superPkg> </superPkg>
                    </cgen>
                </dataMap>
                """, ProjectLoadingMode.FULL);

        CgenDescriptor descriptor = map.getCgenDescriptors().get(0);
        CgenDescriptor defaults = new CgenDescriptor();
        assertEquals(defaults.getName(), descriptor.getName());
        assertTrue(descriptor.getExcludedEntities().isEmpty());
        assertNull(descriptor.getDestDir());
        assertEquals(defaults.getMode(), descriptor.getMode());
        assertNull(descriptor.getTemplate());
        assertEquals(defaults.getOutputPattern(), descriptor.getOutputPattern());
        assertTrue(descriptor.isMakePairs());
        assertTrue(descriptor.isCreatePKProperties());
        assertNull(descriptor.getSuperPkg());
    }

    @Test
    public void loadCgenOtherValues() throws Exception {
        DataMap map = parseDataMap("""
                <dataMap>
                    <cgen>
                        <mode>Entity</mode>
                        <usePkgPath>TRUE</usePkgPath>
                        <overwrite>yes</overwrite>
                    </cgen>
                    <cgen>
                        <mode>datamap</mode>
                    </cgen>
                </dataMap>
                """, ProjectLoadingMode.FULL);

        CgenDescriptor descriptor = map.getCgenDescriptors().get(0);
        assertEquals("entity", descriptor.getMode());
        assertFalse(descriptor.isUsePkgPath());
        assertFalse(descriptor.isOverwrite());
        assertEquals("all", map.getCgenDescriptors().get(1).getMode());
    }

    /**
     * The output directory is checked by class generation only, so that the other tools can load a DataMap made on
     * another OS. Only Windows has invalid path characters that XML can hold.
     */
    @Test
    public void loadCgenInvalidDestDir() throws Exception {
        DataMap map = parseDataMap("""
                <dataMap>
                    <cgen>
                        <destDir>out*put</destDir>
                    </cgen>
                </dataMap>
                """, ProjectLoadingMode.FULL);

        assertEquals("out*put", map.getCgenDescriptors().get(0).getDestDir());
    }

    private static String encode(CgenDescriptor descriptor) {
        StringWriter out = new StringWriter();
        try (PrintWriter writer = new PrintWriter(out)) {
            descriptor.encodeAsXML(new XMLEncoder(writer, "\t"), new DataMap("test"));
        }
        return out.toString();
    }
}

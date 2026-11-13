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

package org.apache.cayenne.project.cgen;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CgenDescriptorTest {

    @Test
    public void defaults() {
        CgenDescriptor descriptor = new CgenDescriptor();

        assertEquals("Default", descriptor.getName());
        assertNull(descriptor.getDestDir());
        assertEquals("entity", descriptor.getMode());
        assertTrue(descriptor.isMakePairs());
        assertTrue(descriptor.isUsePkgPath());
        assertFalse(descriptor.isOverwrite());
        assertFalse(descriptor.isCreatePropertyNames());
        assertTrue(descriptor.isCreatePKProperties());
        assertEquals("*.java", descriptor.getOutputPattern());
        assertNull(descriptor.getSuperPkg());
        assertNull(descriptor.getExternalToolConfig());
        assertNull(descriptor.getTemplate());
        assertNull(descriptor.getSuperTemplate());
        assertNull(descriptor.getEmbeddableTemplate());
        assertNull(descriptor.getEmbeddableSuperTemplate());
        assertNull(descriptor.getDataMapTemplate());
        assertNull(descriptor.getDataMapSuperTemplate());
        assertTrue(descriptor.getExcludedEntities().isEmpty());
        assertTrue(descriptor.getExcludedEmbeddables().isEmpty());
    }

    @Test
    public void copy() {
        CgenDescriptor original = new CgenDescriptor();
        original.setName("Custom");
        original.setDestDir("../java");
        original.setMode("all");
        original.setMakePairs(false);
        original.setUsePkgPath(false);
        original.setOverwrite(true);
        original.setCreatePropertyNames(true);
        original.setCreatePKProperties(false);
        original.setOutputPattern("*.groovy");
        original.setSuperPkg("org.example.auto");
        original.setExternalToolConfig("tools.properties");
        original.setTemplate("subclass");
        original.setSuperTemplate("superclass");
        original.setEmbeddableTemplate("embeddable subclass");
        original.setEmbeddableSuperTemplate("embeddable superclass");
        original.setDataMapTemplate("datamap subclass");
        original.setDataMapSuperTemplate("datamap superclass");
        original.setExcludedEntities(List.of("Artist"));
        original.setExcludedEmbeddables(List.of("org.example.Address"));

        CgenDescriptor copy = new CgenDescriptor(original);

        assertEquals("Custom", copy.getName());
        assertEquals("../java", copy.getDestDir());
        assertEquals("all", copy.getMode());
        assertFalse(copy.isMakePairs());
        assertFalse(copy.isUsePkgPath());
        assertTrue(copy.isOverwrite());
        assertTrue(copy.isCreatePropertyNames());
        assertFalse(copy.isCreatePKProperties());
        assertEquals("*.groovy", copy.getOutputPattern());
        assertEquals("org.example.auto", copy.getSuperPkg());
        assertEquals("tools.properties", copy.getExternalToolConfig());
        assertEquals("subclass", copy.getTemplate());
        assertEquals("superclass", copy.getSuperTemplate());
        assertEquals("embeddable subclass", copy.getEmbeddableTemplate());
        assertEquals("embeddable superclass", copy.getEmbeddableSuperTemplate());
        assertEquals("datamap subclass", copy.getDataMapTemplate());
        assertEquals("datamap superclass", copy.getDataMapSuperTemplate());
        assertEquals(Set.of("Artist"), copy.getExcludedEntities());
        assertEquals(Set.of("org.example.Address"), copy.getExcludedEmbeddables());

        copy.getExcludedEntities().add("Painting");
        copy.getExcludedEmbeddables().clear();
        assertEquals(Set.of("Artist"), original.getExcludedEntities());
        assertEquals(Set.of("org.example.Address"), original.getExcludedEmbeddables());
    }

    @Test
    public void setMode() {
        CgenDescriptor descriptor = new CgenDescriptor();

        descriptor.setMode("ALL");
        assertEquals("all", descriptor.getMode());

        descriptor.setMode("Entity");
        assertEquals("entity", descriptor.getMode());
    }

    @Test
    public void setMode_Unknown() {
        CgenDescriptor descriptor = new CgenDescriptor();

        descriptor.setMode("bogus");
        assertEquals("all", descriptor.getMode());

        descriptor.setMode(null);
        assertEquals("all", descriptor.getMode());
    }

    @Test
    public void setExcludedEntities() {
        CgenDescriptor descriptor = new CgenDescriptor();
        descriptor.setExcludedEntities(List.of("Painting", "Artist", "Painting"));

        assertEquals(List.of("Painting", "Artist"), List.copyOf(descriptor.getExcludedEntities()));
    }

    @Test
    public void setExcludedEntities_Copies() {
        CgenDescriptor descriptor = new CgenDescriptor();
        List<String> names = new ArrayList<>(List.of("Painting", "Artist"));
        descriptor.setExcludedEntities(names);

        names.clear();
        assertEquals(List.of("Painting", "Artist"), List.copyOf(descriptor.getExcludedEntities()));
    }

    @Test
    public void setExcludedEntities_Null() {
        CgenDescriptor descriptor = new CgenDescriptor();
        descriptor.setExcludedEntities(List.of("Painting"));

        descriptor.setExcludedEntities(null);
        assertTrue(descriptor.getExcludedEntities().isEmpty());
    }

    @Test
    public void setExcludedEmbeddables() {
        CgenDescriptor descriptor = new CgenDescriptor();
        descriptor.setExcludedEmbeddables(List.of("org.example.Phone", "org.example.Address", "org.example.Phone"));

        assertEquals(List.of("org.example.Phone", "org.example.Address"),
                List.copyOf(descriptor.getExcludedEmbeddables()));
    }

    @Test
    public void setExcludedEmbeddables_Null() {
        CgenDescriptor descriptor = new CgenDescriptor();
        descriptor.setExcludedEmbeddables(List.of("org.example.Phone"));

        descriptor.setExcludedEmbeddables(null);
        assertTrue(descriptor.getExcludedEmbeddables().isEmpty());
    }

    @Test
    public void parseExcludedNames() {
        assertEquals(List.of("Painting", "Artist"), CgenDescriptor.parseExcludedNames("Painting,Artist"));
        assertEquals(List.of("Artist"), CgenDescriptor.parseExcludedNames("Artist"));
    }

    @Test
    public void encodeAsXML() {
        CgenDescriptor descriptor = new CgenDescriptor();
        descriptor.setName("Custom");
        descriptor.setDestDir("../java");
        descriptor.setMode("all");
        descriptor.setMakePairs(false);
        descriptor.setUsePkgPath(false);
        descriptor.setOverwrite(true);
        descriptor.setCreatePropertyNames(true);
        descriptor.setCreatePKProperties(false);
        descriptor.setOutputPattern("*.groovy");
        descriptor.setSuperPkg("org.example.auto");
        descriptor.setExternalToolConfig("tools.properties");
        descriptor.setTemplate("custom/subclass.vm");
        descriptor.setSuperTemplate("custom/superclass.vm");
        descriptor.setEmbeddableTemplate("embeddable subclass");
        descriptor.setEmbeddableSuperTemplate("embeddable superclass");
        descriptor.setDataMapTemplate("datamap subclass");
        descriptor.setDataMapSuperTemplate("datamap superclass");
        descriptor.setExcludedEntities(List.of("Painting", "Artist"));
        descriptor.setExcludedEmbeddables(List.of("org.example.Phone"));

        assertEquals("""
                <cgen>
                \t<name>Custom</name>
                \t<excludeEntities>Artist,Painting</excludeEntities>
                \t<excludeEmbeddables>org.example.Phone</excludeEmbeddables>
                \t<destDir>../java</destDir>
                \t<mode>all</mode>
                \t<template>custom/subclass.vm</template>
                \t<superTemplate>custom/superclass.vm</superTemplate>
                \t<embeddableTemplate>embeddable subclass</embeddableTemplate>
                \t<embeddableSuperTemplate>embeddable superclass</embeddableSuperTemplate>
                \t<dataMapTemplate>datamap subclass</dataMapTemplate>
                \t<dataMapSuperTemplate>datamap superclass</dataMapSuperTemplate>
                \t<outputPattern>*.groovy</outputPattern>
                \t<makePairs>false</makePairs>
                \t<usePkgPath>false</usePkgPath>
                \t<overwrite>true</overwrite>
                \t<createPropertyNames>true</createPropertyNames>
                \t<superPkg>org.example.auto</superPkg>
                \t<createPKProperties>false</createPKProperties>
                \t<externalToolConfig>tools.properties</externalToolConfig>
                </cgen>
                """, encode(descriptor, owner()));
    }

    /**
     * Unset templates are written as the default paths, and an unset output directory as the DataMap directory.
     */
    @Test
    public void encodeAsXML_Defaults() {
        assertEquals("""
                <cgen>
                \t<name>Default</name>
                \t<destDir>.</destDir>
                \t<mode>entity</mode>
                \t<template>templates/v4_1/subclass.vm</template>
                \t<superTemplate>templates/v4_1/superclass.vm</superTemplate>
                \t<embeddableTemplate>templates/v4_1/embeddable-subclass.vm</embeddableTemplate>
                \t<embeddableSuperTemplate>templates/v4_1/embeddable-superclass.vm</embeddableSuperTemplate>
                \t<dataMapTemplate>templates/v4_1/datamap-subclass.vm</dataMapTemplate>
                \t<dataMapSuperTemplate>templates/v4_1/datamap-superclass.vm</dataMapSuperTemplate>
                \t<outputPattern>*.java</outputPattern>
                \t<makePairs>true</makePairs>
                \t<usePkgPath>true</usePkgPath>
                \t<overwrite>false</overwrite>
                \t<createPropertyNames>false</createPropertyNames>
                \t<createPKProperties>true</createPKProperties>
                </cgen>
                """, encode(new CgenDescriptor(), owner()));
    }

    @Test
    public void encodeAsXML_ExcludedNamesMissingFromTheDataMap() {
        CgenDescriptor descriptor = new CgenDescriptor();
        descriptor.setExcludedEntities(List.of("Removed", "Painting"));
        descriptor.setExcludedEmbeddables(List.of("org.example.Removed"));

        String xml = encode(descriptor, owner());
        assertTrue(xml.contains("\t<excludeEntities>Painting</excludeEntities>\n"), xml);
        assertFalse(xml.contains("excludeEmbeddables"), xml);
    }

    @Test
    public void encodeAsXML_Escaped() {
        CgenDescriptor descriptor = new CgenDescriptor();
        descriptor.setName("<Custom> & more");
        descriptor.setDestDir("..\\java");
        descriptor.setSuperPkg("org\\example");
        descriptor.setTemplate("#if($a < $b && $c > $d)");

        String xml = encode(descriptor, owner());
        assertTrue(xml.contains("\t<name>&lt;Custom&gt; &amp; more</name>\n"), xml);
        assertTrue(xml.contains("\t<destDir>../java</destDir>\n"), xml);
        assertTrue(xml.contains("\t<superPkg>org/example</superPkg>\n"), xml);
        assertTrue(xml.contains("\t<template>#if($a &lt; $b &amp;&amp; $c &gt; $d)</template>\n"), xml);
    }

    @Test
    public void encodeAsXML_MultiLineTemplate() {
        CgenDescriptor descriptor = new CgenDescriptor();
        descriptor.setTemplate("line 1\nline 2");
        descriptor.setSuperTemplate("a[i]]>b\r]]>");

        String xml = encode(descriptor, owner());
        assertTrue(xml.contains("\t<template><![CDATA[line 1\nline 2]]></template>\n"), xml);
        assertTrue(xml.contains(
                "\t<superTemplate><![CDATA[a[i]]]]><![CDATA[>b\r]]]]><![CDATA[>]]></superTemplate>\n"), xml);
    }

    /**
     * The single class template is a default of the subclass template as well.
     */
    @Test
    public void encodeAsXML_DefaultTemplates() {
        CgenDescriptor descriptor = new CgenDescriptor();
        descriptor.setTemplate(CgenDescriptor.DEFAULT_SINGLE_CLASS_TEMPLATE);
        descriptor.setSuperTemplate(CgenDescriptor.DEFAULT_SINGLE_CLASS_TEMPLATE);
        descriptor.setEmbeddableTemplate(CgenDescriptor.DEFAULT_SUPER_TEMPLATE);
        descriptor.setDataMapTemplate(CgenDescriptor.DEFAULT_DATA_MAP_SINGLE_CLASS_TEMPLATE);

        String xml = encode(descriptor, owner());
        assertTrue(xml.contains("\t<template>templates/v4_1/singleclass.vm</template>\n"), xml);
        assertTrue(xml.contains("\t<superTemplate>templates/v4_1/superclass.vm</superTemplate>\n"), xml);
        assertTrue(xml.contains("\t<embeddableTemplate>templates/v4_1/embeddable-subclass.vm</embeddableTemplate>\n"),
                xml);
        assertTrue(xml.contains("\t<dataMapTemplate>templates/v4_1/datamap-singleclass.vm</dataMapTemplate>\n"), xml);
    }

    private static DataMap owner() {
        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(new ObjEntity("Artist"));
        dataMap.addObjEntity(new ObjEntity("Painting"));
        dataMap.addEmbeddable(new Embeddable("org.example.Phone"));
        return dataMap;
    }

    private static String encode(CgenDescriptor descriptor, DataMap owner) {
        StringWriter out = new StringWriter();
        try (PrintWriter writer = new PrintWriter(out)) {
            descriptor.encodeAsXML(new XMLEncoder(writer, "\t"), owner);
        }
        return out.toString().replace(System.lineSeparator(), "\n");
    }
}

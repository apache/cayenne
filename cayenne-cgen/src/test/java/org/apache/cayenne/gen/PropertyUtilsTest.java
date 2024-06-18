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

package org.apache.cayenne.gen;

import org.apache.cayenne.access.types.TimestampType;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.exp.property.DateProperty;
import org.apache.cayenne.exp.property.EmbeddableProperty;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.MapProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SetProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.gen.mock.CustomProperty;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.2
 */
public class PropertyUtilsTest {

    PropertyUtils propertyUtils;
    ImportUtils importUtils;
    Logger logger = LoggerFactory.getLogger(PropertyUtilsTest.class);

    @Before
    public void setup() {
        importUtils = new ImportUtils();

        DefaultScope testScope = new DefaultScope();
        propertyUtils = DIBootstrap.createInjector(
                        new CgenCaseModule(testScope),
                        new CgenModule(),
                        binder -> CoreModule.extend(binder).addUserExtendedType(new TimestampType()))
                .getInstance(ToolsUtilsFactory.class)
                .createPropertyUtils(logger, importUtils);
    }

    @Test
    public void testImportAttribute() throws Exception {
        ObjAttribute attribute = new ObjAttribute();
        attribute.setName("test");
        attribute.setType(java.util.Date.class.getName());

        Map<String, String> importTypesMap = importUtils.importTypesMap;
        assertEquals(0, importTypesMap.size());

        propertyUtils.addImport(attribute);

        assertEquals(3, importTypesMap.size());
        assertEquals(java.util.Date.class.getName(), importTypesMap.get("Date"));
        assertEquals(DateProperty.class.getName(), importTypesMap.get("DateProperty"));
        assertEquals(PropertyFactory.class.getName(), importTypesMap.get("PropertyFactory"));
    }

    @Test
    public void testImportRelationship() {
        String typeName = "org.example.model.TargetEntity";
        ObjEntity entity = mock(ObjEntity.class);
        when(entity.getClassName()).thenReturn(typeName);
        when(entity.getName()).thenReturn("target");

        ObjRelationship relationship = mock(ObjRelationship.class);
        when(relationship.isToMany()).thenReturn(true);
        when(relationship.getCollectionType()).thenReturn("java.util.List");
        when(relationship.getTargetEntityName()).thenReturn("target");
        when(relationship.getName()).thenReturn("list_rel");
        when(relationship.getTargetEntity()).thenReturn(entity);

        Map<String, String> importTypesMap = importUtils.importTypesMap;
        assertEquals(0, importTypesMap.size());

        propertyUtils.addImport(relationship);

        assertEquals(4, importTypesMap.size());
        assertEquals(typeName, importTypesMap.get("TargetEntity"));
        assertEquals(List.class.getName(), importTypesMap.get("List"));
        assertEquals(ListProperty.class.getName(), importTypesMap.get("ListProperty"));
        assertEquals(PropertyFactory.class.getName(), importTypesMap.get("PropertyFactory"));
    }

    @Test
    public void simpleNumericDefinition() throws Exception {
        importUtils.addType(NumericProperty.class.getName());

        ObjEntity entity = new ObjEntity("test");
        ObjAttribute attribute = new ObjAttribute();
        attribute.setName("test");
        attribute.setType("int");
        entity.addAttribute(attribute);

        String definition = propertyUtils.propertyDefinition(attribute);
        assertEquals("public static final NumericProperty<Integer> TEST = PropertyFactory.createNumeric(\"test\", Integer.class);",
                definition);
    }

    @Test
    public void simpleStringDefinition() throws Exception {
        importUtils.addType(StringProperty.class.getName());

        ObjEntity entity = new ObjEntity("test");
        ObjAttribute attribute = new ObjAttribute();
        attribute.setName("test");
        attribute.setType("java.lang.String");
        entity.addAttribute(attribute);

        String definition = propertyUtils.propertyDefinition(attribute);
        assertEquals("public static final StringProperty<String> TEST = PropertyFactory.createString(\"test\", String.class);",
                definition);
    }

    @Test
    public void uppercaseNameDefinition() throws Exception {
        importUtils.addType(StringProperty.class.getName());

        ObjEntity entity = new ObjEntity("test");
        ObjAttribute attribute = new ObjAttribute();
        attribute.setName("URL");
        attribute.setType("java.lang.String");
        entity.addAttribute(attribute);

        String definition = propertyUtils.propertyDefinition(attribute);
        assertEquals("public static final StringProperty<String> URL_ = PropertyFactory.createString(\"URL\", String.class);",
                definition);
    }

    @Test
    public void customPropertyDefinition() throws ClassNotFoundException {
        importUtils.addType(CustomProperty.class.getName());
        importUtils.addType(TimestampType.class.getName());

        ObjEntity entity = new ObjEntity("test");
        ObjAttribute attribute = new ObjAttribute();
        attribute.setName("test");
        attribute.setType("org.apache.cayenne.access.types.TimestampType");
        entity.addAttribute(attribute);

        String definition = propertyUtils.propertyDefinition(attribute);
        assertEquals("public static final CustomProperty<TimestampType> TEST = new CustomProperty(\"test\", TimestampType.class);",
                definition);
    }

    @Test
    public void simpleStringPropertyEmbDefinition() throws ClassNotFoundException {
        importUtils.addType(StringProperty.class.getName());

        DataMap dataMap = new DataMap();

        ObjEntity entity = new ObjEntity();
        entity.setDataMap(dataMap);

        EmbeddedAttribute embeddedAttribute = new EmbeddedAttribute();
        embeddedAttribute.setName("a");
        embeddedAttribute.setType("test");
        embeddedAttribute.addAttributeOverride("testEmbAttr", "testPath");
        embeddedAttribute.setEntity(entity);

        EmbeddableAttribute attribute = new EmbeddableAttribute();
        attribute.setName("testEmbAttr");
        attribute.setType("java.lang.String");

        Embeddable embeddable = new Embeddable();
        embeddable.setClassName("test");
        embeddable.addAttribute(attribute);

        dataMap.addEmbeddable(embeddable);

        String definition = propertyUtils.propertyDefinition(attribute);
        assertEquals("public static final StringProperty<String> TEST_EMB_ATTR = PropertyFactory.createString(\"testEmbAttr\", String.class);",
                definition);
    }

    @Test
    public void simpleNumericPropertyEmbDefinition() throws ClassNotFoundException {
        importUtils.addType(EmbeddableProperty.class.getName());

        DataMap dataMap = new DataMap();

        ObjEntity entity = new ObjEntity();
        entity.setDataMap(dataMap);

        EmbeddedAttribute embeddedAttribute = new EmbeddedAttribute();
        embeddedAttribute.setName("a");
        embeddedAttribute.setType("test");
        embeddedAttribute.addAttributeOverride("testEmbAttr", "testPath");
        embeddedAttribute.setEntity(entity);

        EmbeddableAttribute attribute = new EmbeddableAttribute();
        attribute.setName("testEmbAttr");
        attribute.setType("int");

        Embeddable embeddable = new Embeddable();
        embeddable.setClassName("test");
        embeddable.addAttribute(attribute);

        dataMap.addEmbeddable(embeddable);

        String definition = propertyUtils.propertyDefinition(embeddedAttribute);
        assertEquals("public static final EmbeddableProperty<test> A = PropertyFactory.createEmbeddable(\"a\", test.class);",
                definition);
    }

    @Test
    public void customPropertyEmbDefinition() throws ClassNotFoundException {
        importUtils.addType(CustomProperty.class.getName());
        importUtils.addType(TimestampType.class.getName());

        DataMap dataMap = new DataMap();

        ObjEntity entity = new ObjEntity();
        entity.setDataMap(dataMap);

        EmbeddedAttribute embeddedAttribute = new EmbeddedAttribute();
        embeddedAttribute.setName("a");
        embeddedAttribute.setType("test");
        embeddedAttribute.addAttributeOverride("testEmbAttr", "testPath");
        embeddedAttribute.setEntity(entity);

        EmbeddableAttribute attribute = new EmbeddableAttribute();
        attribute.setName("testEmbAttr");
        attribute.setType("org.apache.cayenne.access.types.TimestampType");

        Embeddable embeddable = new Embeddable();
        embeddable.setClassName("test");
        embeddable.addAttribute(attribute);

        dataMap.addEmbeddable(embeddable);

        String definition = propertyUtils.propertyDefinition(attribute);
        assertEquals("public static final CustomProperty<TimestampType> TEST_EMB_ATTR = new CustomProperty(\"testEmbAttr\", TimestampType.class);",
                definition);
    }

    @Test
    public void toOneRelationshipDefinition() {

        String typeName = "org.example.model.TargetEntity";
        importUtils.addType(EntityProperty.class.getName());
        importUtils.addType(typeName);

        ObjEntity entity = mock(ObjEntity.class);
        when(entity.getClassName()).thenReturn(typeName);
        when(entity.getName()).thenReturn("target");

        ObjRelationship relationship = mock(ObjRelationship.class);
        when(relationship.isToMany()).thenReturn(false);
        when(relationship.getTargetEntityName()).thenReturn("target");
        when(relationship.getName()).thenReturn("to_one_rel");
        when(relationship.getTargetEntity()).thenReturn(entity);

        String definition = propertyUtils.propertyDefinition(relationship);
        assertEquals("public static final EntityProperty<TargetEntity> TO_ONE_REL = PropertyFactory.createEntity(\"to_one_rel\", TargetEntity.class);",
                definition);
    }

    @Test
    public void listRelationshipDefinition() {

        String typeName = "org.example.model.TargetEntity";
        importUtils.addType(ListProperty.class.getName());
        importUtils.addType(typeName);

        ObjEntity entity = mock(ObjEntity.class);
        when(entity.getClassName()).thenReturn(typeName);
        when(entity.getName()).thenReturn("target");

        ObjRelationship relationship = mock(ObjRelationship.class);
        when(relationship.isToMany()).thenReturn(true);
        when(relationship.getCollectionType()).thenReturn("java.util.List");
        when(relationship.getTargetEntityName()).thenReturn("target");
        when(relationship.getName()).thenReturn("list_rel");
        when(relationship.getTargetEntity()).thenReturn(entity);

        String definition = propertyUtils.propertyDefinition(relationship);
        assertEquals("public static final ListProperty<TargetEntity> LIST_REL = PropertyFactory.createList(\"list_rel\", TargetEntity.class);",
                definition);
    }

    @Test
    public void setRelationshipDefinition() {

        String typeName = "org.example.model.TargetEntity";
        importUtils.addType(SetProperty.class.getName());
        importUtils.addType(typeName);

        ObjEntity entity = mock(ObjEntity.class);
        when(entity.getClassName()).thenReturn(typeName);
        when(entity.getName()).thenReturn("target");

        ObjRelationship relationship = mock(ObjRelationship.class);
        when(relationship.isToMany()).thenReturn(true);
        when(relationship.getCollectionType()).thenReturn("java.util.Set");
        when(relationship.getTargetEntityName()).thenReturn("target");
        when(relationship.getName()).thenReturn("set_rel");
        when(relationship.getTargetEntity()).thenReturn(entity);

        String definition = propertyUtils.propertyDefinition(relationship);
        assertEquals("public static final SetProperty<TargetEntity> SET_REL = PropertyFactory.createSet(\"set_rel\", TargetEntity.class);",
                definition);
    }

    @Test
    public void mapRelationshipDefinition() {

        String typeName = "org.example.model.TargetEntity";
        importUtils.addType(MapProperty.class.getName());
        importUtils.addType(typeName);

        ObjAttribute attribute = mock(ObjAttribute.class);
        when(attribute.getType()).thenReturn(String.class.getName());

        ObjEntity entity = mock(ObjEntity.class);
        when(entity.getClassName()).thenReturn(typeName);
        when(entity.getName()).thenReturn("target");
        when(entity.getAttribute(anyString())).thenReturn(attribute);

        ObjRelationship relationship = mock(ObjRelationship.class);
        when(relationship.isToMany()).thenReturn(true);
        when(relationship.getCollectionType()).thenReturn("java.util.Map");
        when(relationship.getMapKey()).thenReturn("key");
        when(relationship.getTargetEntityName()).thenReturn("target");
        when(relationship.getName()).thenReturn("map_rel");
        when(relationship.getTargetEntity()).thenReturn(entity);

        String definition = propertyUtils.propertyDefinition(relationship);
        assertEquals("public static final MapProperty<String, TargetEntity> MAP_REL = PropertyFactory.createMap(\"map_rel\", String.class, TargetEntity.class);",
                definition);
    }

}
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
import org.apache.cayenne.CoreModule;
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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PropertyUtilsTest {

    PropertyUtils propertyUtils;
    ImportUtils importUtils;
    Logger logger = LoggerFactory.getLogger(PropertyUtilsTest.class);

    @BeforeEach
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
    public void importAttribute() throws Exception {
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
    public void importRelationship() {
        String typeName = "org.example.model.TargetEntity";
        ObjRelationship relationship = toMany("list_rel", typeName, "java.util.List");

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

        ObjRelationship relationship = toOne("to_one_rel", typeName);

        String definition = propertyUtils.propertyDefinition(relationship);
        assertEquals("public static final EntityProperty<TargetEntity> TO_ONE_REL = PropertyFactory.createEntity(\"to_one_rel\", TargetEntity.class);",
                definition);
    }

    @Test
    public void listRelationshipDefinition() {

        String typeName = "org.example.model.TargetEntity";
        importUtils.addType(ListProperty.class.getName());
        importUtils.addType(typeName);

        ObjRelationship relationship = toMany("list_rel", typeName, "java.util.List");

        String definition = propertyUtils.propertyDefinition(relationship);
        assertEquals("public static final ListProperty<TargetEntity> LIST_REL = PropertyFactory.createList(\"list_rel\", TargetEntity.class);",
                definition);
    }

    @Test
    public void setRelationshipDefinition() {

        String typeName = "org.example.model.TargetEntity";
        importUtils.addType(SetProperty.class.getName());
        importUtils.addType(typeName);

        ObjRelationship relationship = toMany("set_rel", typeName, "java.util.Set");

        String definition = propertyUtils.propertyDefinition(relationship);
        assertEquals("public static final SetProperty<TargetEntity> SET_REL = PropertyFactory.createSet(\"set_rel\", TargetEntity.class);",
                definition);
    }

    @Test
    public void mapRelationshipDefinition() {

        String typeName = "org.example.model.TargetEntity";
        importUtils.addType(MapProperty.class.getName());
        importUtils.addType(typeName);

        ObjRelationship relationship = toMany("map_rel", typeName, "java.util.Map");
        relationship.setMapKey("key");

        String definition = propertyUtils.propertyDefinition(relationship);
        assertEquals("public static final MapProperty<String, TargetEntity> MAP_REL = PropertyFactory.createMap(\"map_rel\", String.class, TargetEntity.class);",
                definition);
    }

    private static ObjRelationship toOne(String name, String targetClass) {
        return relationship(name, targetClass, false, null);
    }

    private static ObjRelationship toMany(String name, String targetClass, String collectionType) {
        return relationship(name, targetClass, true, collectionType);
    }

    /**
     * Creates a relationship from a "source" entity to a "target" entity of the given class, that has a String "key"
     * attribute.
     */
    private static ObjRelationship relationship(
            String name, String targetClass, boolean toMany, String collectionType) {
        DbEntity sourceTable = new DbEntity("SOURCE");
        sourceTable.addAttribute(pk("ID"));
        sourceTable.addAttribute(new DbAttribute("TARGET_ID"));

        DbEntity targetTable = new DbEntity("TARGET");
        targetTable.addAttribute(pk("ID"));
        targetTable.addAttribute(new DbAttribute("SOURCE_ID"));

        DataMap map = new DataMap("M");
        map.addDbEntity(sourceTable);
        map.addDbEntity(targetTable);

        DbRelationship dbRelationship = new DbRelationship("db_rel");
        sourceTable.addRelationship(dbRelationship);
        dbRelationship.setTargetEntityName("TARGET");
        dbRelationship.setToMany(toMany);
        dbRelationship.addJoin(toMany
                ? new DbJoin(dbRelationship, "ID", "SOURCE_ID")
                : new DbJoin(dbRelationship, "TARGET_ID", "ID"));

        ObjEntity source = new ObjEntity("source");
        source.setDbEntityName("SOURCE");
        map.addObjEntity(source);

        ObjEntity target = new ObjEntity("target");
        target.setDbEntityName("TARGET");
        target.setClassName(targetClass);
        target.addAttribute(new ObjAttribute("key", String.class.getName(), target));
        map.addObjEntity(target);

        ObjRelationship relationship = new ObjRelationship(name);
        source.addRelationship(relationship);
        relationship.setTargetEntityName("target");
        relationship.setDbRelationshipPath("db_rel");
        relationship.setCollectionType(collectionType);
        return relationship;
    }

    private static DbAttribute pk(String name) {
        DbAttribute attribute = new DbAttribute(name);
        attribute.setPrimaryKey(true);
        return attribute;
    }
}

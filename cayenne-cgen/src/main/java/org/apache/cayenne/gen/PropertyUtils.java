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

package org.apache.cayenne.gen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.DateProperty;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.MapProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SetProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;

/**
 * @since 4.2
 */
public class PropertyUtils {

    private static final String PK_PROPERTY_SUFFIX = "_PK_PROPERTY";
    private static final Map<String, String> FACTORY_METHODS = new HashMap<>();

    static {
        FACTORY_METHODS.put(BaseProperty.class.getName(), "createBase");
        FACTORY_METHODS.put(NumericProperty.class.getName(), "createNumeric");
        FACTORY_METHODS.put(StringProperty.class.getName(), "createString");
        FACTORY_METHODS.put(DateProperty.class.getName(), "createDate");
        FACTORY_METHODS.put(ListProperty.class.getName(), "createList");
        FACTORY_METHODS.put(SetProperty.class.getName(), "createSet");
        FACTORY_METHODS.put(MapProperty.class.getName(), "createMap");
    }

    private static final List<Class<?>> JAVA_DATE_TYPES = Arrays.asList(
            java.util.Date.class,
            java.time.LocalDate.class,
            java.time.LocalTime.class,
            java.time.LocalDateTime.class,
            java.sql.Date.class,
            java.sql.Time.class,
            java.sql.Timestamp.class
    );

    private final ImportUtils importUtils;

    public PropertyUtils(ImportUtils importUtils) {
        this.importUtils = importUtils;
    }

    public void addImportForPK(EntityUtils entityUtils) throws ClassNotFoundException {
        DbEntity entity = entityUtils.objEntity.getDbEntity();
        boolean needToCreatePK = false;

        for(DbAttribute attribute : entity.getPrimaryKeys()) {
            if(!entityUtils.declaresDbAttribute(attribute)) {
                importUtils.addType(TypesMapping.getJavaBySqlType(attribute.getType()));
                importUtils.addType(getPropertyTypeForAttribute(attribute));
                needToCreatePK = true;
            }
        }

        if(needToCreatePK) {
            importUtils.addType(PropertyFactory.class.getName());
            importUtils.addType(ExpressionFactory.class.getName());
        }
    }

    public void addImport(ObjAttribute attribute) {
        importUtils.addType(PropertyFactory.class.getName());
        importUtils.addType(attribute.getType());
        importUtils.addType(getPropertyTypeForAttribute(attribute));
    }

    public void addImport(ObjRelationship relationship) {
        importUtils.addType(PropertyFactory.class.getName());
        if (relationship.getTargetEntityName() != null && relationship.getTargetEntity() != null) {
            importUtils.addType(relationship.getTargetEntity().getClassName());
        } else {
            importUtils.addType(Persistent.class.getName());
        }
        importUtils.addType(getPropertyTypeForJavaClass(relationship));
        if (relationship.isToMany()) {
            importUtils.addType(relationship.getCollectionType());
        }
    }

    public String propertyDefinition(DbAttribute attribute) throws ClassNotFoundException {
        StringUtils utils = StringUtils.getInstance();

        String attributeType = TypesMapping.getJavaBySqlType(attribute.getType());
        String propertyType = getPropertyTypeForAttribute(attribute);
        String propertyFactoryMethod = factoryMethodForPropertyType(propertyType);
        attributeType = importUtils.formatJavaType(attributeType, false);

        return String.format("public static final %s<%s> %s = PropertyFactory.%s(ExpressionFactory.dbPathExp(\"%s\"), %s.class);",
                importUtils.formatJavaType(propertyType),
                attributeType,
                utils.capitalizedAsConstant(attribute.getName() + PK_PROPERTY_SUFFIX),
                propertyFactoryMethod,
                attribute.getName(),
                attributeType
        );
    }

    private String getPropertyTypeForAttribute(DbAttribute attribute) throws ClassNotFoundException {
        String attributeType = TypesMapping.getJavaBySqlType(attribute.getType());
        if(TypesMapping.JAVA_BYTES.equals(attributeType)) {
            return BaseProperty.class.getName();
        }

        Class<?> javaClass = Class.forName(attributeType);
        if (Number.class.isAssignableFrom(javaClass)) {
            return NumericProperty.class.getName();
        }

        if (CharSequence.class.isAssignableFrom(javaClass)) {
            return StringProperty.class.getName();
        }

        if (JAVA_DATE_TYPES.contains(javaClass)) {
            return DateProperty.class.getName();
        }

        return BaseProperty.class.getName();
    }

    public String propertyDefinition(ObjAttribute attribute) {
        StringUtils utils = StringUtils.getInstance();

        String propertyType = getPropertyTypeForAttribute(attribute);
        String propertyFactoryMethod = factoryMethodForPropertyType(propertyType);
        String attributeType = utils.stripGeneric(importUtils.formatJavaType(attribute.getType(), false));

        return String.format("public static final %s<%s> %s = PropertyFactory.%s(\"%s\", %s.class);",
                importUtils.formatJavaType(propertyType),
                attributeType,
                utils.capitalizedAsConstant(attribute.getName()),
                propertyFactoryMethod,
                attribute.getName(),
                attributeType
        );
    }

    public String propertyDefinition(ObjRelationship relationship) {
        if (relationship.isToMany()) {
            return toManyRelationshipDefinition(relationship);
        } else {
            return toOneRelationshipDefinition(relationship);
        }
    }

    private String toManyRelationshipDefinition(ObjRelationship relationship) {
        if (Map.class.getName().equals(relationship.getCollectionType())) {
            return mapRelationshipDefinition(relationship);
        } else {
            return collectionRelationshipDefinition(relationship);
        }
    }

    private String mapRelationshipDefinition(ObjRelationship relationship) {
        StringUtils utils = StringUtils.getInstance();

        String propertyType = getPropertyTypeForJavaClass(relationship);
        String propertyFactoryMethod = factoryMethodForPropertyType(propertyType);
        String mapKeyType = EntityUtils.getMapKeyTypeInternal(relationship);
        String attributeType = relationship.getTargetEntity() == null
                ? Persistent.class.getSimpleName()
                : relationship.getTargetEntity().getClassName();

        return String.format("public static final %s<%s, %s> %s = PropertyFactory.%s(\"%s\", %s.class, %s.class);",
                importUtils.formatJavaType(propertyType),
                importUtils.formatJavaType(mapKeyType),
                importUtils.formatJavaType(attributeType),
                utils.capitalizedAsConstant(relationship.getName()),
                propertyFactoryMethod,
                relationship.getName(),
                importUtils.formatJavaType(mapKeyType),
                importUtils.formatJavaType(attributeType)
        );
    }

    private String collectionRelationshipDefinition(ObjRelationship relationship) {
        StringUtils utils = StringUtils.getInstance();

        String propertyType = getPropertyTypeForJavaClass(relationship);
        String propertyFactoryMethod = factoryMethodForPropertyType(propertyType);
        String entityType = importUtils.formatJavaType(relationship.getTargetEntity() == null
                ? Persistent.class.getSimpleName()
                : relationship.getTargetEntity().getClassName());

        return String.format("public static final %s<%s> %s = PropertyFactory.%s(\"%s\", %s.class);",
                importUtils.formatJavaType(propertyType),
                entityType,
                utils.capitalizedAsConstant(relationship.getName()),
                propertyFactoryMethod,
                relationship.getName(),
                entityType
        );
    }

    private String toOneRelationshipDefinition(ObjRelationship relationship) {
        StringUtils utils = StringUtils.getInstance();

        String propertyType = EntityProperty.class.getName();
        String propertyFactoryMethod = "createEntity";
        String attributeType = (relationship.getTargetEntityName() == null || relationship.getTargetEntity() == null)
                ? Persistent.class.getSimpleName()
                : importUtils.formatJavaType(relationship.getTargetEntity().getClassName());

        return String.format("public static final %s<%s> %s = PropertyFactory.%s(\"%s\", %s.class);",
                importUtils.formatJavaType(propertyType),
                attributeType,
                utils.capitalizedAsConstant(relationship.getName()),
                propertyFactoryMethod,
                relationship.getName(),
                attributeType
        );
    }

    private String factoryMethodForPropertyType(String propertyType) {
        return FACTORY_METHODS.get(propertyType);
    }

    private String getPropertyTypeForJavaClass(ObjRelationship relationship) {
        if (relationship.isToMany()) {
            String collectionType = relationship.getCollectionType();
            if (java.util.Map.class.getName().equals(collectionType)) {
                return MapProperty.class.getName();
            }

            if (java.util.List.class.getName().equals(collectionType)) {
                return ListProperty.class.getName();
            }

            return SetProperty.class.getName();
        }

        return EntityProperty.class.getName();
    }

    private String getPropertyTypeForAttribute(ObjAttribute attribute) {
        String attributeType = attribute.getType();

        if (importUtils.isNumericPrimitive(attributeType)) {
            return NumericProperty.class.getName();
        }

        Class<?> javaClass;
        if (importUtils.isPrimitive(attributeType) || attributeType.startsWith("java.")) {
            javaClass = attribute.getJavaClass();
        } else {
            return BaseProperty.class.getName();
        }

        if (Number.class.isAssignableFrom(javaClass)) {
            return NumericProperty.class.getName();
        }

        if (CharSequence.class.isAssignableFrom(javaClass)) {
            return StringProperty.class.getName();
        }

        if (JAVA_DATE_TYPES.contains(javaClass)) {
            return DateProperty.class.getName();
        }

        return BaseProperty.class.getName();
    }
}

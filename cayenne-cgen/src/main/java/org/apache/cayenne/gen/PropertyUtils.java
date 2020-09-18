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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.EmbeddableObject;
import org.apache.cayenne.Fault;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.BaseIdProperty;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.DateProperty;
import org.apache.cayenne.exp.property.EmbeddableProperty;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.ListProperty;
import org.apache.cayenne.exp.property.MapProperty;
import org.apache.cayenne.exp.property.NumericIdProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.SetProperty;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.gen.property.PropertyDescriptorCreator;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.slf4j.Logger;

/**
 * @since 4.2
 */
public class PropertyUtils {

    private static final String PK_PROPERTY_SUFFIX = "_PK_PROPERTY";
    private static final char DUPLICATE_NAME_SUFFIX = '_';
    private static final Map<String, String> FACTORY_METHODS = new HashMap<>();

    static {
        FACTORY_METHODS.put(BaseProperty.class.getName(), "createBase");
        FACTORY_METHODS.put(NumericProperty.class.getName(), "createNumeric");
        FACTORY_METHODS.put(StringProperty.class.getName(), "createString");
        FACTORY_METHODS.put(DateProperty.class.getName(), "createDate");
        FACTORY_METHODS.put(ListProperty.class.getName(), "createList");
        FACTORY_METHODS.put(SetProperty.class.getName(), "createSet");
        FACTORY_METHODS.put(MapProperty.class.getName(), "createMap");
        FACTORY_METHODS.put(EmbeddableProperty.class.getName(), "createEmbeddable");
        FACTORY_METHODS.put(NumericIdProperty.class.getName(), "createNumericId");
        FACTORY_METHODS.put(BaseIdProperty.class.getName(), "createBaseId");
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

    private List<PropertyDescriptorCreator> propertyList;
    private AdhocObjectFactory adhocObjectFactory;

    private Logger logger;

    public PropertyUtils(ImportUtils importUtils) {
        this.importUtils = importUtils;
        this.propertyList = new ArrayList<>();

    }

    public PropertyUtils(ImportUtils importUtils,
                         AdhocObjectFactory adhocObjectFactory,
                         List<PropertyDescriptorCreator> propertyList,
                         Logger logger) {
        this.importUtils = importUtils;
        this.adhocObjectFactory = adhocObjectFactory;
        this.propertyList = propertyList;
        this.logger = logger;
    }

    public void addImportForPK(EntityUtils entityUtils) throws ClassNotFoundException {
        DbEntity entity = entityUtils.objEntity.getDbEntity();
        boolean needToCreatePK = false;

        for(DbAttribute attribute : entity.getPrimaryKeys()) {
            if(!entityUtils.declaresDbAttribute(attribute)) {
                String javaBySqlType = TypesMapping.getJavaBySqlType(attribute.getType());
                importUtils.addType(javaBySqlType);
                importUtils.addType(getPkPropertyTypeForType(javaBySqlType));
                needToCreatePK = true;
            }
        }

        if(needToCreatePK) {
            importUtils.addType(PropertyFactory.class.getName());
        }
    }

    public void addImport(ObjAttribute attribute) throws ClassNotFoundException {
        importUtils.addType(PropertyFactory.class.getName());
        importUtils.addType(attribute.getType());
        importUtils.addType(getPropertyDescriptor(attribute.getType()).getPropertyType());
        if(attribute.isLazy()) {
            importUtils.addType(Fault.class.getName());
        }
    }

    public void addImport(EmbeddedAttribute attribute) throws ClassNotFoundException {
        importUtils.addType(PropertyFactory.class.getName());
        importUtils.addType(attribute.getType());
        importUtils.addType(getPropertyDescriptor(EmbeddableObject.class.getName()).getPropertyType());
    }

    public void addImport(EmbeddableAttribute attribute) throws ClassNotFoundException {
        importUtils.addType(PropertyFactory.class.getName());
        importUtils.addType(attribute.getType());
        importUtils.addType(getPropertyDescriptor(attribute.getType()).getPropertyType());
    }

    public void addImport(ObjRelationship relationship) {
        addImport(relationship, false);
    }

    public void addImport(ObjRelationship relationship, boolean client) {
        importUtils.addType(PropertyFactory.class.getName());
        if (relationship.getTargetEntity() != null) {
            importUtils.addType(client
                    ? relationship.getTargetEntity().getClientClassName()
                    : relationship.getTargetEntity().getClassName())
            ;
        } else {
            importUtils.addType(Persistent.class.getName());
        }
        importUtils.addType(getPropertyTypeForJavaClass(relationship));
        if (relationship.isToMany()) {
            importUtils.addType(relationship.getCollectionType());
        }
    }

    public String propertyDefinition(ObjEntity entity, DbAttribute attribute) throws ClassNotFoundException {
        StringUtils utils = StringUtils.getInstance();

        String attributeType = TypesMapping.getJavaBySqlType(attribute.getType());
        String propertyType = getPkPropertyTypeForType(TypesMapping.getJavaBySqlType(attribute.getType()));
        String propertyFactoryMethod = factoryMethodForPropertyType(propertyType);
        attributeType = importUtils.formatJavaType(attributeType, false);

        return String.format("public static final %s<%s> %s = PropertyFactory.%s(\"%s\", \"%s\", %s.class);",
                importUtils.formatJavaType(propertyType),
                attributeType,
                utils.capitalizedAsConstant(attribute.getName()) + PK_PROPERTY_SUFFIX,
                propertyFactoryMethod,
                attribute.getName(),
                entity.getName(),
                attributeType
        );
    }

    public String propertyDefinition(ObjAttribute attribute, boolean client) throws ClassNotFoundException {
        StringUtils utils = StringUtils.getInstance();
        String attributeType = utils.stripGeneric(importUtils.formatJavaType(attribute.getType(), false));
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(attribute.getType());
        return String.format("public static final %s<%s> %s = %s(\"%s\", %s.class);",
                importUtils.formatJavaType(propertyDescriptor.getPropertyType()),
                attributeType,
                generatePropertyName(attribute),
                propertyDescriptor.getPropertyFactoryMethod(),
                attribute.getName(),
                attributeType
        );
    }

    protected String generatePropertyName(ObjAttribute attribute) {
        StringUtils utils = StringUtils.getInstance();
        ObjEntity entity = attribute.getEntity();
        String name = utils.capitalizedAsConstant(attribute.getName());
        // ensure that final name is unique
        while(entity.getAttribute(name) != null) {
            name = name + DUPLICATE_NAME_SUFFIX;
        }
        return name;
    }

    public String propertyDefinition(ObjAttribute attribute) throws ClassNotFoundException {
        return propertyDefinition(attribute, false);
    }

    public String propertyDefinition(EmbeddedAttribute attribute) throws ClassNotFoundException {
        StringUtils utils = StringUtils.getInstance();
        String attributeType = utils.stripGeneric(importUtils.formatJavaType(attribute.getType(), false));
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(EmbeddableObject.class.getName());
        return String.format("public static final %s<%s> %s = %s(\"%s\", %s.class);",
                importUtils.formatJavaType(propertyDescriptor.getPropertyType()),
                attributeType,
                generatePropertyName(attribute),
                propertyDescriptor.getPropertyFactoryMethod(),
                attribute.getName(),
                attributeType
        );
    }

    public String propertyDefinition(EmbeddableAttribute attribute) throws ClassNotFoundException {
        StringUtils utils = StringUtils.getInstance();
        String attributeType = utils.stripGeneric(importUtils.formatJavaType(attribute.getType(), false));
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(attribute.getType());
        return String.format("public static final %s<%s> %s = %s(\"%s\", %s.class);",
                importUtils.formatJavaType(propertyDescriptor.getPropertyType()),
                attributeType,
                generatePropertyName(attribute),
                propertyDescriptor.getPropertyFactoryMethod(),
                attribute.getName(),
                attributeType
        );
    }

    protected String generatePropertyName(EmbeddableAttribute attribute) {
        StringUtils utils = StringUtils.getInstance();
        Embeddable embeddable = attribute.getEmbeddable();
        String name = utils.capitalizedAsConstant(attribute.getName());
        // ensure that final name is unique
        while(embeddable.getAttribute(name) != null) {
            name = name + DUPLICATE_NAME_SUFFIX;
        }
        return name;
    }

    public String propertyDefinition(ObjRelationship relationship, boolean client) {
        if (relationship.isToMany()) {
            return toManyRelationshipDefinition(relationship, client);
        } else {
            return toOneRelationshipDefinition(relationship, client);
        }
    }

    public String propertyDefinition(ObjRelationship relationship) {
        return propertyDefinition(relationship, false);
    }

    private String toManyRelationshipDefinition(ObjRelationship relationship, boolean client) {
        if (Map.class.getName().equals(relationship.getCollectionType())) {
            return mapRelationshipDefinition(relationship, client);
        } else {
            return collectionRelationshipDefinition(relationship, client);
        }
    }

    private String mapRelationshipDefinition(ObjRelationship relationship, boolean client) {
        StringUtils utils = StringUtils.getInstance();

        String propertyType = getPropertyTypeForJavaClass(relationship);
        String propertyFactoryMethod = factoryMethodForPropertyType(propertyType);
        String mapKeyType = importUtils.formatJavaType(EntityUtils.getMapKeyTypeInternal(relationship));
        String attributeType = getRelatedTypeName(relationship, client);

        return String.format("public static final %s<%s, %s> %s = PropertyFactory.%s(\"%s\", %s.class, %s.class);",
                importUtils.formatJavaType(propertyType),
                mapKeyType,
                attributeType,
                utils.capitalizedAsConstant(relationship.getName()),
                propertyFactoryMethod,
                relationship.getName(),
                mapKeyType,
                attributeType
        );
    }

    private String collectionRelationshipDefinition(ObjRelationship relationship, boolean client) {
        StringUtils utils = StringUtils.getInstance();

        String propertyType = getPropertyTypeForJavaClass(relationship);
        String propertyFactoryMethod = factoryMethodForPropertyType(propertyType);
        String entityType = getRelatedTypeName(relationship, client);

        return String.format("public static final %s<%s> %s = PropertyFactory.%s(\"%s\", %s.class);",
                importUtils.formatJavaType(propertyType),
                entityType,
                utils.capitalizedAsConstant(relationship.getName()),
                propertyFactoryMethod,
                relationship.getName(),
                entityType
        );
    }

    private String getRelatedTypeName(ObjRelationship relationship, boolean client) {
        if(relationship.getTargetEntity() == null) {
            return Persistent.class.getSimpleName();
        }

        return importUtils.formatJavaType(client
                ? relationship.getTargetEntity().getClientClassName()
                : relationship.getTargetEntity().getClassName());
    }

    private String toOneRelationshipDefinition(ObjRelationship relationship, boolean client) {
        StringUtils utils = StringUtils.getInstance();

        String propertyType = EntityProperty.class.getName();
        String propertyFactoryMethod = "createEntity";
        String attributeType = getRelatedTypeName(relationship, client);

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

    private String getPkPropertyTypeForType(String attributeType) throws ClassNotFoundException {
        Class<?> javaClass = Class.forName(attributeType);
        if (Number.class.isAssignableFrom(javaClass)) {
            return NumericIdProperty.class.getName();
        }
        return BaseIdProperty.class.getName();
    }

    private String getPropertyTypeForJavaClass(ObjRelationship relationship) {
        if (relationship.isToMany()) {
            String collectionType = relationship.getCollectionType();
            if (java.util.Map.class.getName().equals(collectionType)) {
                return MapProperty.class.getName();
            }

            if (java.util.List.class.getName().equals(collectionType) || java.util.Collection.class.getName().equals(collectionType)) {
                return ListProperty.class.getName();
            }

            return SetProperty.class.getName();
        }

        return EntityProperty.class.getName();
    }

    public PropertyDescriptor getPropertyDescriptor(String attrType) {
        try {
            Class<?> type = adhocObjectFactory.getJavaClass(attrType);
            for(PropertyDescriptorCreator creator : propertyList) {
                Optional<PropertyDescriptor> optionalPropertyDescriptor = creator.apply(type);
                if(optionalPropertyDescriptor.isPresent()) {
                    return optionalPropertyDescriptor.get();
                }
            }
        } catch (DIRuntimeException ex) {
            if(logger != null) {
                logger.warn("WARN: Class not found: " + attrType + ". Will use default PropertyDescriptor.");
            }
            return PropertyDescriptor.defaultDescriptor();
        }
        return PropertyDescriptor.defaultDescriptor();
    }
}

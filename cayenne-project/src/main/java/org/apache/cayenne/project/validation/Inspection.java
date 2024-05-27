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
package org.apache.cayenne.project.validation;

public enum Inspection {
    DATA_CHANNEL_NO_NAME("Unnamed data domain", "Inspection description"),

    DATA_NODE_NO_NAME("Unnamed data node", "Inspection description"),
    DATA_NODE_NAME_DUPLICATE("Duplicate of a data node name", "Inspection description"),
    DATA_NODE_CONNECTION_PARAMS("Empty params of a data node connection", "Inspection description"),

    DATA_MAP_NO_NAME("Empty data map name", "Inspection description"),
    DATA_MAP_NAME_DUPLICATE("Duplicate of a data map name", "Inspection description"),
    DATA_MAP_NODE_LINKAGE("Data map is not linked to a data node", "Inspection description"),
    DATA_MAP_JAVA_PACKAGE("Invalid java package of a data map", "Inspection description"),

    OBJ_ENTITY_NO_NAME("Empty obj entity name", "Inspection description"),
    OBJ_ENTITY_NAME_DUPLICATE("Duplicate of obj entity name", "Inspection description"),
    OBJ_ENTITY_NO_DB_ENTITY("Obj entity has no db entity mapping", "Inspection description"),
    OBJ_ENTITY_INVALID_CLASS("Obj entity has invalid Java class", "Inspection description"),
    OBJ_ENTITY_INVALID_SUPER_CLASS("Obj entity has invalid Java super class", "Inspection description"),

    OBJ_ATTRIBUTE_NO_NAME("Empty obj attribute name", "Inspection description"),
    OBJ_ATTRIBUTE_INVALID_NAME("Invalid obj entity name", "Inspection description"),
    OBJ_ATTRIBUTE_NO_TYPE("Empty obj attribute type", "Inspection description"),
    OBJ_ATTRIBUTE_NO_EMBEDDABLE("Embeddable obj attribute has no embeddable", "Inspection description"),
    OBJ_ATTRIBUTE_INVALID_MAPPING("Obj attribute has invalid mapping to a db attribute", "Inspection description"),
    OBJ_ATTRIBUTE_PATH_DUPLICATE("Duplicate of db path of obj attribute", "Inspection description"),
    OBJ_ATTRIBUTE_SUPER_NAME_DUPLICATE("Duplicate of an obj attribute name in a super entity", "Inspection description"),

    OBJ_RELATIONSHIP_NO_NAME("Empty obj relationship name", "Inspection description"),
    OBJ_RELATIONSHIP_NAME_DUPLICATE("Duplicate of an obj relationship name", "Inspection description"),
    OBJ_RELATIONSHIP_INVALID_NAME("Invalid obj relationship name", "Inspection description"),
    OBJ_RELATIONSHIP_NO_TARGET("No obj relationship target", "Inspection description"),
    OBJ_RELATIONSHIP_TARGET_NOT_PK("Obj relationship target attribute is not a primary key", "Inspection description"),
    OBJ_RELATIONSHIP_INVALID_REVERSED("Invalid reversed obj relationship", "Inspection description"),
    OBJ_RELATIONSHIP_SEMANTIC_DUPLICATE("Obj relationships with same source and target entities", "Inspection description"),
    OBJ_RELATIONSHIP_INVALID_MAPPING("Obj relationship has invalid mapping to a db relationship", "Inspection description"),
    OBJ_RELATIONSHIP_NULLIFY_NOT_NULL("Nullify delete rule with a mandatory foreign key", "Inspection description"),
    OBJ_RELATIONSHIP_DUPLICATE_IN_ENTITY("Duplicate of an obj relationship in the same obj entity", "Inspection description"),

    DB_ENTITY_NO_NAME("Empty db entity name", "Inspection description"),
    DB_ENTITY_NAME_DUPLICATE("Duplicate of a db entity name", "Inspection description"),
    DB_ENTITY_NO_ATTRIBUTES("Db entity has no attributes", "Inspection description"),
    DB_ENTITY_NO_PK("Db entity has no primary key", "Inspection description"),

    DB_ATTRIBUTE_NO_NAME("Empty db attribute name", "Inspection description"),
    DB_ATTRIBUTE_INVALID_NAME("Invalid db attribute name", "Inspection description"),
    DB_ATTRIBUTE_NO_TYPE("Empty db attribute type", "Inspection description"),
    DB_ATTRIBUTE_NO_LENGTH("String db attribute has no length", "Inspection description"),

    DB_RELATIONSHIP_NO_NAME("Empty db relationship name", "Inspection description"),
    DB_RELATIONSHIP_NAME_DUPLICATE("Duplicate of a db relationship name", "Inspection description"),
    DB_RELATIONSHIP_INVALID_NAME("Invalid db relationship name", "Inspection description"),
    DB_RELATIONSHIP_PATH_DUPLICATE("Duplicate of a db relationship path", "Inspection description"),
    DB_RELATIONSHIP_NO_TARGET("No db relationship target", "Inspection description"),
    DB_RELATIONSHIP_TARGET_NOT_PK("Db relationship target attribute is not a primary key", "Inspection description"),
    DB_RELATIONSHIP_NO_JOINS("No db relationship joins", "Inspection description"),
    DB_RELATIONSHIP_INVALID_JOIN("Invalid db relationship join", "Inspection description"),
    DB_RELATIONSHIP_BOTH_TO_MANY("Both db relationship and the reversed one are to-many", "Inspection description"),
    DB_RELATIONSHIP_DIFFERENT_TYPES("Source and target db relationship attributes are of different types", "Inspection description"),
    DB_RELATIONSHIP_GENERATED_WITH_DEPENDENT_PK("Db relationship target is a dependent generated primary key", "Inspection description"),

    EMBEDDABLE_NO_NAME("Empty embeddable name", "Inspection description"),
    EMBEDDABLE_NAME_DUPLICATE("Duplicate of an embeddable name", "Inspection description"),

    EMBEDDABLE_ATTRIBUTE_NO_NAME("Empty embeddable attribute name", "Inspection description"),
    EMBEDDABLE_ATTRIBUTE_NO_TYPE("Empty embeddable attribute type", "Inspection description"),

    PROCEDURE_NO_NAME("Empty procedure name", "Inspection description"),
    PROCEDURE_NAME_DUPLICATE("Duplicate of procedure name", "Inspection description"),
    PROCEDURE_NO_PARAMS("Procedure returns a value but has no params", "Inspection description"),

    PROCEDURE_PARAMETER_NO_NAME("Empty procedure parameter name", "Inspection description"),
    PROCEDURE_PARAMETER_NO_TYPE("Empty procedure parameter type", "Inspection description"),
    PROCEDURE_PARAMETER_NO_LENGTH("String procedure parameter has no length", "Inspection description"),
    PROCEDURE_PARAMETER_NO_DIRECTION("Procedure parameter has no direction", "Inspection description"),

    QUERY_NO_NAME("Empty query name", "Inspection description"),
    QUERY_NAME_DUPLICATE("Duplicate of a query name", "Inspection description"),
    QUERY_MULTI_CACHE_GROUP("Query has several cache groups", "Inspection description"),

    SELECT_QUERY_NO_ROOT("Empty select query root", "Inspection description"),
    SELECT_QUERY_INVALID_QUALIFIER("Invalid select query qualifier", "Not implemented"),
    SELECT_QUERY_INVALID_ORDERING_PATH("Invalid select query ordering path", "Inspection description"),
    SELECT_QUERY_INVALID_PREFETCH_PATH("Invalid select query prefetch path", "Not implemented"),

    PROCEDURE_QUERY_NO_ROOT("Empty procedure query root", "Inspection description"),
    PROCEDURE_QUERY_INVALID_ROOT("Invalid procedure query root", "Inspection description"),

    EJBQL_QUERY_INVALID_SYNTAX("Invalid syntax of an EJBQL query", "Inspection description"),

    SQL_TEMPLATE_NO_ROOT("Empty SQL template query root", "Inspection description"),
    SQL_TEMPLATE_NO_DEFAULT_SQL("SQL template query has no default SQL template", "Inspection description");

    private final String readableName;
    private final String description;

    Inspection(String name, String description) {
        this.readableName = name;
        this.description = description;
    }

    public String readableName() {
        return readableName;
    }

    public String description() {
        return description;
    }
}

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

/**
 * @since 5.0
 */
public enum Inspection {
    DATA_CHANNEL_NO_NAME(Group.DATA_CHANNEL, "Empty data domain name", "Inspection description"),

    DATA_NODE_NO_NAME(Group.DATA_NODE, "Empty data node name", "Inspection description"),
    DATA_NODE_NAME_DUPLICATE(Group.DATA_NODE, "Duplicate of a data node name", "Inspection description"),
    DATA_NODE_CONNECTION_PARAMS(Group.DATA_NODE, "Empty params of a data node connection", "Inspection description"),

    DATA_MAP_NO_NAME(Group.DATA_MAP, "Empty data map name", "Inspection description"),
    DATA_MAP_NAME_DUPLICATE(Group.DATA_MAP, "Duplicate of a data map name", "Inspection description"),
    DATA_MAP_NODE_LINKAGE(Group.DATA_MAP, "Data map is not linked to a data node", "Inspection description"),
    DATA_MAP_JAVA_PACKAGE(Group.DATA_MAP, "Invalid java package of a data map", "Inspection description"),

    OBJ_ENTITY_NO_NAME(Group.OBJ_ENTITY, "Empty obj entity name", "Inspection description"),
    OBJ_ENTITY_NAME_DUPLICATE(Group.OBJ_ENTITY, "Duplicate of obj entity name", "Inspection description"),
    OBJ_ENTITY_NO_DB_ENTITY(Group.OBJ_ENTITY, "Obj entity has no db entity mapping", "Inspection description"),
    OBJ_ENTITY_INVALID_CLASS(Group.OBJ_ENTITY, "Obj entity has invalid Java class", "Inspection description"),
    OBJ_ENTITY_INVALID_SUPER_CLASS(Group.OBJ_ENTITY, "Obj entity has invalid Java super class", "Inspection description"),

    OBJ_ATTRIBUTE_NO_NAME(Group.OBJ_ATTRIBUTE, "Empty obj attribute name", "Inspection description"),
    OBJ_ATTRIBUTE_INVALID_NAME(Group.OBJ_ATTRIBUTE, "Invalid obj entity name", "Inspection description"),
    OBJ_ATTRIBUTE_NO_TYPE(Group.OBJ_ATTRIBUTE, "Empty obj attribute type", "Inspection description"),
    OBJ_ATTRIBUTE_NO_EMBEDDABLE(Group.OBJ_ATTRIBUTE, "Embeddable obj attribute has no embeddable", "Inspection description"),
    OBJ_ATTRIBUTE_INVALID_MAPPING(Group.OBJ_ATTRIBUTE, "Obj attribute has invalid mapping to a db attribute", "Inspection description"),
    OBJ_ATTRIBUTE_PATH_DUPLICATE(Group.OBJ_ATTRIBUTE, "Duplicate of db path of obj attribute", "Inspection description"),
    OBJ_ATTRIBUTE_SUPER_NAME_DUPLICATE(Group.OBJ_ATTRIBUTE, "Duplicate of an obj attribute name in a super entity", "Inspection description"),

    OBJ_RELATIONSHIP_NO_NAME(Group.OBJ_RELATIONSHIP, "Empty obj relationship name", "Inspection description"),
    OBJ_RELATIONSHIP_NAME_DUPLICATE(Group.OBJ_RELATIONSHIP, "Duplicate of an obj relationship name", "Inspection description"),
    OBJ_RELATIONSHIP_INVALID_NAME(Group.OBJ_RELATIONSHIP, "Invalid obj relationship name", "Inspection description"),
    OBJ_RELATIONSHIP_NO_TARGET(Group.OBJ_RELATIONSHIP, "No obj relationship target", "Inspection description"),
    OBJ_RELATIONSHIP_TARGET_NOT_PK(Group.OBJ_RELATIONSHIP, "Obj relationship target attribute is not a primary key", "Inspection description"),
    OBJ_RELATIONSHIP_INVALID_REVERSED(Group.OBJ_RELATIONSHIP, "Invalid reversed obj relationship", "Inspection description"),
    OBJ_RELATIONSHIP_SEMANTIC_DUPLICATE(Group.OBJ_RELATIONSHIP, "Obj relationships with same source and target entities", "Inspection description"),
    OBJ_RELATIONSHIP_INVALID_MAPPING(Group.OBJ_RELATIONSHIP, "Obj relationship has invalid mapping to a db relationship", "Inspection description"),
    OBJ_RELATIONSHIP_NULLIFY_NOT_NULL(Group.OBJ_RELATIONSHIP, "Nullify delete rule with a mandatory foreign key", "Inspection description"),
    OBJ_RELATIONSHIP_DUPLICATE_IN_ENTITY(Group.OBJ_RELATIONSHIP, "Duplicate of an obj relationship in the same obj entity", "Inspection description"),

    DB_ENTITY_NO_NAME(Group.DB_ENTITY, "Empty db entity name", "Inspection description"),
    DB_ENTITY_NAME_DUPLICATE(Group.DB_ENTITY, "Duplicate of a db entity name", "Inspection description"),
    DB_ENTITY_NO_ATTRIBUTES(Group.DB_ENTITY, "Db entity has no attributes", "Inspection description"),
    DB_ENTITY_NO_PK(Group.DB_ENTITY, "Db entity has no primary key", "Inspection description"),

    DB_ATTRIBUTE_NO_NAME(Group.DB_ATTRIBUTE, "Empty db attribute name", "Inspection description"),
    DB_ATTRIBUTE_INVALID_NAME(Group.DB_ATTRIBUTE, "Invalid db attribute name", "Inspection description"),
    DB_ATTRIBUTE_NO_TYPE(Group.DB_ATTRIBUTE, "Empty db attribute type", "Inspection description"),
    DB_ATTRIBUTE_NO_LENGTH(Group.DB_ATTRIBUTE, "String db attribute has no length", "Inspection description"),

    DB_RELATIONSHIP_NO_NAME(Group.DB_RELATIONSHIP, "Empty db relationship name", "Inspection description"),
    DB_RELATIONSHIP_NAME_DUPLICATE(Group.DB_RELATIONSHIP, "Duplicate of a db relationship name", "Inspection description"),
    DB_RELATIONSHIP_INVALID_NAME(Group.DB_RELATIONSHIP, "Invalid db relationship name", "Inspection description"),
    DB_RELATIONSHIP_PATH_DUPLICATE(Group.DB_RELATIONSHIP, "Duplicate of a db relationship path", "Inspection description"),
    DB_RELATIONSHIP_NO_TARGET(Group.DB_RELATIONSHIP, "No db relationship target", "Inspection description"),
    DB_RELATIONSHIP_TARGET_NOT_PK(Group.DB_RELATIONSHIP, "Db relationship target attribute is not a primary key", "Inspection description"),
    DB_RELATIONSHIP_NO_JOINS(Group.DB_RELATIONSHIP, "No db relationship joins", "Inspection description"),
    DB_RELATIONSHIP_INVALID_JOIN(Group.DB_RELATIONSHIP, "Invalid db relationship join", "Inspection description"),
    DB_RELATIONSHIP_BOTH_TO_MANY(Group.DB_RELATIONSHIP, "Both db relationship and the reversed one are to-many", "Inspection description"),
    DB_RELATIONSHIP_DIFFERENT_TYPES(Group.DB_RELATIONSHIP, "Source and target db relationship attributes are of different types", "Inspection description"),
    DB_RELATIONSHIP_GENERATED_WITH_DEPENDENT_PK(Group.DB_RELATIONSHIP, "Db relationship target is a dependent generated primary key", "Inspection description"),

    EMBEDDABLE_NO_NAME(Group.EMBEDDABLE, "Empty embeddable name", "Inspection description"),
    EMBEDDABLE_NAME_DUPLICATE(Group.EMBEDDABLE, "Duplicate of an embeddable name", "Inspection description"),

    EMBEDDABLE_ATTRIBUTE_NO_NAME(Group.EMBEDDABLE_ATTRIBUTE, "Empty embeddable attribute name", "Inspection description"),
    EMBEDDABLE_ATTRIBUTE_NO_TYPE(Group.EMBEDDABLE_ATTRIBUTE, "Empty embeddable attribute type", "Inspection description"),

    PROCEDURE_NO_NAME(Group.PROCEDURE, "Empty procedure name", "Inspection description"),
    PROCEDURE_NAME_DUPLICATE(Group.PROCEDURE, "Duplicate of procedure name", "Inspection description"),
    PROCEDURE_NO_PARAMS(Group.PROCEDURE, "Procedure returns a value but has no params", "Inspection description"),

    PROCEDURE_PARAMETER_NO_NAME(Group.PROCEDURE_PARAMETER, "Empty procedure parameter name", "Inspection description"),
    PROCEDURE_PARAMETER_NO_TYPE(Group.PROCEDURE_PARAMETER, "Empty procedure parameter type", "Inspection description"),
    PROCEDURE_PARAMETER_NO_LENGTH(Group.PROCEDURE_PARAMETER, "String procedure parameter has no length", "Inspection description"),
    PROCEDURE_PARAMETER_NO_DIRECTION(Group.PROCEDURE_PARAMETER, "Procedure parameter has no direction", "Inspection description"),

    QUERY_NO_NAME(Group.QUERY, "Empty query name", "Inspection description"),
    QUERY_NAME_DUPLICATE(Group.QUERY, "Duplicate of a query name", "Inspection description"),
    QUERY_MULTI_CACHE_GROUP(Group.QUERY, "Query has several cache groups", "Inspection description"),

    SELECT_QUERY_NO_ROOT(Group.SELECT_QUERY, "Empty select query root", "Inspection description"),
    SELECT_QUERY_INVALID_QUALIFIER(Group.SELECT_QUERY, "Invalid select query qualifier", "Not implemented"),
    SELECT_QUERY_INVALID_ORDERING_PATH(Group.SELECT_QUERY, "Invalid select query ordering path", "Inspection description"),
    SELECT_QUERY_INVALID_PREFETCH_PATH(Group.SELECT_QUERY, "Invalid select query prefetch path", "Not implemented"),

    PROCEDURE_QUERY_NO_ROOT(Group.PROCEDURE_QUERY, "Empty procedure query root", "Inspection description"),
    PROCEDURE_QUERY_INVALID_ROOT(Group.PROCEDURE_QUERY, "Invalid procedure query root", "Inspection description"),

    EJBQL_QUERY_INVALID_SYNTAX(Group.EJBQL_QUERY, "Invalid syntax of an EJBQL query", "Inspection description"),

    SQL_TEMPLATE_NO_ROOT(Group.SQL_TEMPLATE, "Empty SQL template query root", "Inspection description"),
    SQL_TEMPLATE_NO_DEFAULT_SQL(Group.SQL_TEMPLATE, "SQL template query has no default SQL template", "Inspection description");

    private final Group group;
    private final String readableName;
    private final String description;

    Inspection(Group group, String name, String description) {
        this.group = group;
        this.readableName = name;
        this.description = description;
    }

    public Group group() {
        return group;
    }

    public String readableName() {
        return readableName;
    }

    public String description() {
        return description;
    }

    @Override
    public String toString() {
        return readableName();
    }

    /**
     * @since 5.0
     */
    public enum Group {
        DATA_CHANNEL("Data domain"),
        DATA_NODE("Data node"),
        DATA_MAP("Data map"),
        OBJ_ENTITY("Obj entity"),
        OBJ_ATTRIBUTE("Obj attribute"),
        OBJ_RELATIONSHIP("Obj relationship"),
        DB_ENTITY("Db entity"),
        DB_ATTRIBUTE("Db attribute"),
        DB_RELATIONSHIP("Db relationship"),
        EMBEDDABLE("Embeddable"),
        EMBEDDABLE_ATTRIBUTE("Embeddable attribute"),
        PROCEDURE("Procedure"),
        PROCEDURE_PARAMETER("Procedure parameter"),
        QUERY("Query"),
        SELECT_QUERY("Select query"),
        PROCEDURE_QUERY("Procedure query"),
        EJBQL_QUERY("EJBQL query"),
        SQL_TEMPLATE("SQL template");

        private final String readableName;

        Group(String readableName) {
            this.readableName = readableName;
        }

        public String readableName() {
            return readableName;
        }

        @Override
        public String toString() {
            return readableName();
        }
    }
}

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

package org.apache.cayenne.modeler.pref;

import org.apache.cayenne.pref.RenamedPreferences;

import java.util.prefs.Preferences;

public class ProjectStatePreferences extends RenamedPreferences {

    private String event;
    private String domain;
    private String node;
    private String dataMap;
    private String objEntity;
    private String dbEntity;
    private String embeddable;
    private String embAttrs;
    private String objAttrs;
    private String dbAttrs;
    private String objRels;
    private String dbRels;
    private String procedure;
    private String procedureParams;
    private String query;
    private String multipleObjects;
    private String parentObject;

    public static final String EVENT_PROPERTY = "event";
    public static final String DOMAIN_PROPERTY = "domain";
    public static final String NODE_PROPERTY = "node";
    public static final String DATA_MAP_PROPERTY = "dataMap";
    public static final String OBJ_ENTITY_PROPERTY = "objEntity";
    public static final String DB_ENTITY_PROPERTY = "dbEntity";
    public static final String EMBEDDABLE_PROPERTY = "embeddable";
    public static final String EMBEDDABLE_ATTRS_PROPERTY = "embAttrs";
    public static final String OBJ_ATTRS_PROPERTY = "objAttrs";
    public static final String DB_ATTRS_PROPERTY = "dbAttrs";
    public static final String OBJ_RELS_PROPERTY = "objRels";
    public static final String DB_RELS_PROPERTY = "dbRels";
    public static final String PROCEDURE_PROPERTY = "procedure";
    public static final String PROCEDURE_PARAMS_PROPERTY = "procedureParams";
    public static final String QUERY_PROPERTY = "query";
    public static final String MULTIPLE_OBJECTS_PROPERTY = "multipleObjects";
    public static final String PARENT_OBJECT_PROPERTY = "parentObject";

    public ProjectStatePreferences(Preferences pref) {
        super(pref);
    }

    public String getEvent() {
        if (event == null) {
            event = getCurrentPreference().get(EVENT_PROPERTY, "");
        }

        return event;
    }

    public void setEvent(String event) {
        this.event = event;
        if (event != null) {
            getCurrentPreference().put(EVENT_PROPERTY, event);
        }
    }

    public String getDomain() {
        if (domain == null) {
            domain = getCurrentPreference().get(DOMAIN_PROPERTY, "");
        }

        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
        if (domain != null) {
            getCurrentPreference().put(DOMAIN_PROPERTY, domain);
        }
    }

    public String getNode() {
        if (node == null) {
            node = getCurrentPreference().get(NODE_PROPERTY, "");
        }

        return node;
    }

    public void setNode(String node) {
        this.node = node;
        if (node != null) {
            getCurrentPreference().put(NODE_PROPERTY, node);
        }
    }

    public String getDataMap() {
        if (dataMap == null) {
            dataMap = getCurrentPreference().get(DATA_MAP_PROPERTY, "");
        }

        return dataMap;
    }

    public void setDataMap(String dataMap) {
        this.dataMap = dataMap;
        if (dataMap != null) {
            getCurrentPreference().put(DATA_MAP_PROPERTY, dataMap);
        }
    }

    public String getObjEntity() {
        if (objEntity == null) {
            objEntity = getCurrentPreference().get(OBJ_ENTITY_PROPERTY, "");
        }

        return objEntity;
    }

    public void setObjEntity(String objEntity) {
        this.objEntity = objEntity;
        if (objEntity != null) {
            getCurrentPreference().put(OBJ_ENTITY_PROPERTY, objEntity);
        }
    }

    public String getDbEntity() {
        if (dbEntity == null) {
            dbEntity = getCurrentPreference().get(DB_ENTITY_PROPERTY, "");
        }

        return dbEntity;
    }

    public void setDbEntity(String dbEntity) {
        this.dbEntity = dbEntity;
        if (dbEntity != null) {
            getCurrentPreference().put(DB_ENTITY_PROPERTY, dbEntity);
        }
    }

    public String getEmbeddable() {
        if (embeddable == null) {
            embeddable = getCurrentPreference().get(EMBEDDABLE_PROPERTY, "");
        }

        return embeddable;
    }

    public void setEmbeddable(String embeddable) {
        this.embeddable = embeddable;
        if (embeddable != null) {
            getCurrentPreference().put(EMBEDDABLE_PROPERTY, embeddable);
        }
    }

    public String getEmbAttrs() {
        if (embAttrs == null) {
            embAttrs = getCurrentPreference().get(EMBEDDABLE_ATTRS_PROPERTY, "");
        }

        return embAttrs;
    }

    public void setEmbAttrs(String embAttrs) {
        this.embAttrs = embAttrs;
        if (embAttrs != null) {
            getCurrentPreference().put(EMBEDDABLE_ATTRS_PROPERTY, embAttrs);
        }
    }

    public String getObjAttrs() {
        if (objAttrs == null) {
            objAttrs = getCurrentPreference().get(OBJ_ATTRS_PROPERTY, "");
        }

        return objAttrs;
    }

    public void setObjAttrs(String objAttrs) {
        this.objAttrs = objAttrs;
        if (objAttrs != null) {
            getCurrentPreference().put(OBJ_ATTRS_PROPERTY, objAttrs);
        }
    }

    public String getDbAttrs() {
        if (dbAttrs == null) {
            dbAttrs = getCurrentPreference().get(DB_ATTRS_PROPERTY, "");
        }

        return dbAttrs;
    }

    public void setDbAttrs(String dbAttrs) {
        this.dbAttrs = dbAttrs;
        if (dbAttrs != null) {
            getCurrentPreference().put(DB_ATTRS_PROPERTY, dbAttrs);
        }
    }

    public String getObjRels() {
        if (objRels == null) {
            objRels = getCurrentPreference().get(OBJ_RELS_PROPERTY, "");
        }

        return objRels;
    }

    public void setObjRels(String objRels) {
        this.objRels = objRels;
        if (objRels != null) {
            getCurrentPreference().put(OBJ_RELS_PROPERTY, objRels);
        }
    }

    public String getDbRels() {
        if (dbRels == null) {
            dbRels = getCurrentPreference().get(DB_RELS_PROPERTY, "");
        }

        return dbRels;
    }

    public void setDbRels(String dbRels) {
        this.dbRels = dbRels;
        if (dbRels != null) {
            getCurrentPreference().put(DB_RELS_PROPERTY, dbRels);
        }
    }

    public String getProcedure() {
        if (procedure == null) {
            procedure = getCurrentPreference().get(PROCEDURE_PROPERTY, "");
        }

        return procedure;
    }

    public void setProcedure(String procedure) {
        this.procedure = procedure;
        if (procedure != null) {
            getCurrentPreference().put(PROCEDURE_PROPERTY, procedure);
        }
    }

    public String getProcedureParams() {
        if (procedureParams == null) {
            procedureParams = getCurrentPreference().get(PROCEDURE_PARAMS_PROPERTY, "");
        }

        return procedureParams;
    }

    public void setProcedureParams(String procedureParams) {
        this.procedureParams = procedureParams;
        if (procedureParams != null) {
            getCurrentPreference().put(PROCEDURE_PARAMS_PROPERTY, procedureParams);
        }
    }

    public String getQuery() {
        if (query == null) {
            query = getCurrentPreference().get(QUERY_PROPERTY, "");
        }

        return query;
    }

    public void setQuery(String query) {
        this.query = query;
        if (query != null) {
            getCurrentPreference().put(QUERY_PROPERTY, query);
        }
    }

    public String getMultipleObjects() {
        if (multipleObjects == null) {
            multipleObjects = getCurrentPreference().get(MULTIPLE_OBJECTS_PROPERTY, "");
        }

        return multipleObjects;
    }

    public void setMultipleObjects(String multipleObjects) {
        this.multipleObjects = multipleObjects;
        if (multipleObjects != null) {
            getCurrentPreference().put(MULTIPLE_OBJECTS_PROPERTY, multipleObjects);
        }
    }

    public String getParentObject() {
        if (parentObject == null) {
            parentObject = getCurrentPreference().get(PARENT_OBJECT_PROPERTY, "");
        }

        return parentObject;
    }

    public void setParentObject(String parentObject) {
        this.parentObject = parentObject;
        if (parentObject != null) {
            getCurrentPreference().put(PARENT_OBJECT_PROPERTY, parentObject);
        }
    }

}

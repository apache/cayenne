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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.util.Util;
import org.apache.velocity.VelocityContext;

import java.util.Collection;
import java.util.LinkedList;

/**
 * {@link Artifact} facade for a DataMap.
 * 
 * @since 3.0
 */
public class DataMapArtifact implements Artifact {

    public static final String DATAMAP_UTILS_KEY = "dataMapUtils";

    protected DataMap dataMap;
    protected Collection<QueryDescriptor> selectQueries;
    protected Collection<QueryDescriptor> sqlTemplateQueries;
    protected Collection<QueryDescriptor> procedureQueries;
    protected Collection<QueryDescriptor> ejbqlQueries;
    protected Collection<String> queryNames;

    public DataMapArtifact(DataMap dataMap, Collection<QueryDescriptor> queries) {
        this.dataMap = dataMap;
        selectQueries = new LinkedList<>();
        sqlTemplateQueries = new LinkedList<>();
        procedureQueries = new LinkedList<>();
        ejbqlQueries = new LinkedList<>();
        queryNames = new LinkedList<>();
        addQueries(queries);
    }

    public String getQualifiedBaseClassName() {
        return Object.class.getName();
    }

    public String getQualifiedClassName() {
        return dataMap.getNameWithDefaultPackage(Util.underscoredToJava(dataMap.getName(), true));
    }

    public Object getObject() {
        return this;
    }

    public void postInitContext(VelocityContext context) {
        DataMapUtils dataMapUtils = new DataMapUtils();
        context.put(DATAMAP_UTILS_KEY, dataMapUtils);
    }

    public TemplateType[] getTemplateTypes(ArtifactGenerationMode mode) {
        switch (mode) {
            case SINGLE_CLASS:
                return new TemplateType[] {
                    TemplateType.DATAMAP_SINGLE_CLASS
                };
            case GENERATION_GAP:
                return new TemplateType[] {
                        TemplateType.DATAMAP_SUPERCLASS, TemplateType.DATAMAP_SUBCLASS
                };
            default:
                return new TemplateType[0];
        }
    }

    private void addQueries(Collection<QueryDescriptor> queries) {
        if (queries != null) {
            for (QueryDescriptor query : queries) {
                addQuery(query);
            }
        }
    }

    private void addQuery(QueryDescriptor query) {

        switch (query.getType()) {
            case QueryDescriptor.SELECT_QUERY:
                selectQueries.add(query);
                break;
            case QueryDescriptor.PROCEDURE_QUERY:
                procedureQueries.add(query);
                break;
            case QueryDescriptor.SQL_TEMPLATE:
                sqlTemplateQueries.add(query);
                break;
            case QueryDescriptor.EJBQL_QUERY:
                ejbqlQueries.add(query);
                break;
        }

        if (query.getName() != null && !"".equals(query.getName())) {
            queryNames.add(query.getName());
        }
    }

    public Collection<QueryDescriptor> getSelectQueries() {
        return selectQueries;
    }

    public boolean hasSelectQueries() {
        return selectQueries.size() > 0;
    }

    public boolean hasQueryNames() {
        return !queryNames.isEmpty();
    }

    public Collection<String> getQueryNames() {
        return queryNames;
    }

    public DataMap getDataMap() {
    	return dataMap;
    }
}

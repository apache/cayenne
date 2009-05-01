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

import java.util.Collection;
import java.util.LinkedList;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.NameConverter;
import org.apache.velocity.VelocityContext;

/**
 * {@link Artifact} facade for a DataMap.
 * 
 * @since 3.0
 */
public class DataMapArtifact implements Artifact {

    public static final String DATAMAP_UTILS_KEY = "dataMapUtils";

    protected DataMap dataMap;
    protected Collection<SelectQuery> selectQueries;
    protected Collection<SQLTemplate> sqlTemplateQueries;
    protected Collection<ProcedureQuery> procedureQueries;
    protected Collection<EJBQLQuery> ejbqlQueries;
    protected Collection<String> queryNames;

    public DataMapArtifact(DataMap dataMap, Collection<Query> queries) {
        this.dataMap = dataMap;
        selectQueries = new LinkedList<SelectQuery>();
        sqlTemplateQueries = new LinkedList<SQLTemplate>();
        procedureQueries = new LinkedList<ProcedureQuery>();
        ejbqlQueries = new LinkedList<EJBQLQuery>();
        queryNames = new LinkedList<String>();
        addQueries(queries);
    }

    public String getQualifiedBaseClassName() {
        return Object.class.getName();
    }

    public String getQualifiedClassName() {
        String pkg = dataMap.getDefaultPackage();
        if (pkg == null) {
            pkg = "";
        }
        else {
            pkg = pkg + '.';
        }

        return pkg
                + NameConverter.underscoredToJava(NameConverter
                        .specialCharsToJava(dataMap.getName()), true);
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

    private void addQueries(Collection<Query> queries) {
        if (queries != null) {
            for (Query query : queries) {
                addQuery(query);
            }
        }
    }

    private void addQuery(Query query) {
        if (query instanceof SelectQuery) {
            selectQueries.add((SelectQuery) query);
        } else if (query instanceof ProcedureQuery) {
            procedureQueries.add((ProcedureQuery) query);
        } else if (query instanceof SQLTemplate) {
            sqlTemplateQueries.add((SQLTemplate) query);
        } else if (query instanceof EJBQLQuery) {
            ejbqlQueries.add((EJBQLQuery) query);
        }
        if (query.getName() != null && !"".equals(query.getName())) {
            queryNames.add(query.getName());
        }
    }

    public Collection<SelectQuery> getSelectQueries() {
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
}

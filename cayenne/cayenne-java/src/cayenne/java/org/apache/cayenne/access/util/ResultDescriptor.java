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

package org.apache.cayenne.access.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;

/**
 * Contains information about the ResultSet used to process fetched rows. ResultDescriptor
 * is initialized by calling various "add*" methods, after that it must be indexed by
 * calling "index".
 * 
 * @author Andrei Adamchik
 * @deprecated Since 1.2 replaced with RowDescriptor that provides clean and
 *             straightforward creation options instead of ResultDescriptor's obscure ways
 *             to index Cayenne attributes data.
 */
public class ResultDescriptor {

    // indexed data
    protected String[] names;
    protected int[] jdbcTypes;
    protected ExtendedType[] converters;
    protected int[] idIndexes;
    protected int[] outParamIndexes;

    // unindexed data
    protected List dbAttributes = new ArrayList();
    protected List javaTypes = new ArrayList();
    protected ExtendedTypeMap typesMapping;
    protected ObjEntity rootEntity;
    protected boolean indexedIds;

    /**
     * Creates and returns a ResultDescritor based on ResultSet metadata.
     */
    public static ResultDescriptor createDescriptor(
            ResultSet resultSet,
            ExtendedTypeMap typeConverters) {
        ResultDescriptor descriptor = new ResultDescriptor(typeConverters);
        try {
            ResultSetMetaData md = resultSet.getMetaData();
            int len = md.getColumnCount();
            if (len == 0) {
                throw new CayenneRuntimeException("No columns in ResultSet.");
            }

            for (int i = 0; i < len; i++) {

                // figure out column name
                int pos = i + 1;
                String name = md.getColumnLabel(pos);
                int sqlType = md.getColumnType(pos);
                int precision = md.getScale(pos);
                int length = md.getColumnDisplaySize(pos);
                if (name == null || name.length() == 0) {
                    name = md.getColumnName(i + 1);

                    if (name == null || name.length() == 0) {
                        name = "column_" + (i + 1);
                    }
                }

                DbAttribute desc = new DbAttribute();
                desc.setName(name);
                desc.setType(md.getColumnType(i + 1));

                descriptor.addDbAttribute(desc);
                descriptor.addJavaType(TypesMapping.getJavaBySqlType(
                        sqlType,
                        length,
                        precision));
            }
        }
        catch (SQLException sqex) {
            throw new CayenneRuntimeException("Error reading metadata.", sqex);
        }

        descriptor.index();
        return descriptor;
    }

    /**
     * Creates and returns a ResultDescriptor for an array of ColumnDescriptors.
     * 
     * @since 1.1
     */
    public static ResultDescriptor createDescriptor(
            ColumnDescriptor[] columns,
            ExtendedTypeMap typeConverters) {

        ResultDescriptor descriptor = new ResultDescriptor(typeConverters);

        int len = columns.length;
        descriptor.names = new String[len];
        descriptor.jdbcTypes = new int[len];
        descriptor.converters = new ExtendedType[len];
        int idWidth = 0;

        for (int i = 0; i < len; i++) {
            descriptor.names[i] = columns[i].getName();
            descriptor.jdbcTypes[i] = columns[i].getJdbcType();
            descriptor.converters[i] = typeConverters.getRegisteredType(columns[i]
                    .getJavaClass());
            if (columns[i].isPrimaryKey()) {
                idWidth++;
            }
        }

        return descriptor;
    }

    /**
     * Creates and returns a ResultDescriptor for the stored procedure parameters.
     */
    public static ResultDescriptor createDescriptor(
            Procedure procedure,
            ExtendedTypeMap typeConverters) {
        ResultDescriptor descriptor = new ResultDescriptor(typeConverters);
        Iterator it = procedure.getCallParameters().iterator();
        while (it.hasNext()) {
            descriptor.addDbAttribute(new ProcedureParameterWrapper(
                    (ProcedureParameter) it.next()));
        }

        descriptor.index();
        return descriptor;
    }

    public ResultDescriptor(ExtendedTypeMap typesMapping) {
        this(typesMapping, null);
    }

    public ResultDescriptor(ExtendedTypeMap typesMapping, ObjEntity rootEntity) {
        this.typesMapping = typesMapping;
        this.rootEntity = rootEntity;
    }

    public void addColumns(Collection dbAttributes) {
        this.dbAttributes.addAll(dbAttributes);
    }

    public void addDbAttribute(DbAttribute attr) {
        this.dbAttributes.add(attr);
    }

    public void addJavaTypes(Collection javaTypes) {
        this.javaTypes.addAll(javaTypes);
    }

    public void addJavaType(String javaType) {
        this.javaTypes.add(javaType);
    }

    /**
     * Reindexes primary key based on DbEntity.
     * 
     * @since 1.1
     */
    protected void indexIds(DbEntity entity) {
        // TODO: maybe check if the entity has changed,
        // and reindex again... since now we assume that once indexing
        // is done, the entity is always the same...

        if (!indexedIds) {
            synchronized (this) {
                if (!indexedIds) {
                    idIndexes = new int[0];

                    if (entity == null) {
                        return;
                    }

                    indexedIds = true;

                    int resultWidth = names.length;
                    int[] tmp = new int[resultWidth];
                    int j = 0;
                    for (int i = 0; i < resultWidth; i++) {
                        DbAttribute attribute = (DbAttribute) entity
                                .getAttribute(names[i]);
                        if (attribute != null && attribute.isPrimaryKey()) {
                            tmp[j++] = i;
                        }
                    }

                    if (j > 0) {
                        this.idIndexes = new int[j];
                        System.arraycopy(tmp, 0, idIndexes, 0, j);
                    }

                }
            }
        }
    }

    public void index() {

        // assert validity
        if (javaTypes.size() > 0 && javaTypes.size() != dbAttributes.size()) {
            throw new IllegalArgumentException(
                    "DbAttributes and Java type arrays must have the same size.");
        }

        // init various things
        int resultWidth = dbAttributes.size();
        int outWidth = 0;
        this.names = new String[resultWidth];
        this.jdbcTypes = new int[resultWidth];
        for (int i = 0; i < resultWidth; i++) {
            DbAttribute attr = (DbAttribute) dbAttributes.get(i);

            // set type
            jdbcTypes[i] = attr.getType();

            // check if this is a stored procedure OUT parameter
            if (attr instanceof ProcedureParameterWrapper) {
                if (((ProcedureParameterWrapper) attr).getParameter().isOutParam()) {
                    outWidth++;
                }
            }

            // figure out name
            String name = null;
            if (rootEntity != null) {
                ObjAttribute objAttr = rootEntity.getAttributeForDbAttribute(attr);
                if (objAttr != null) {
                    name = objAttr.getDbAttributePath();
                }
            }

            if (name == null) {
                name = attr.getName();
            }

            names[i] = name;
        }

        if (outWidth == 0) {
            this.outParamIndexes = new int[0];
        }
        else {
            this.outParamIndexes = new int[outWidth];
            for (int i = 0, j = 0; i < resultWidth; i++) {
                DbAttribute attr = (DbAttribute) dbAttributes.get(i);
                jdbcTypes[i] = attr.getType();

                if (attr instanceof ProcedureParameterWrapper) {
                    if (((ProcedureParameterWrapper) attr).getParameter().isOutParam()) {
                        outParamIndexes[j++] = i;
                    }
                }
            }
        }

        // initialize type converters, must do after everything else,
        // since this may depend on some of the indexed data
        if (javaTypes.size() > 0) {
            initConvertersFromJavaTypes();
        }
        else if (rootEntity != null) {
            initConvertersFromMapping();
        }
        else {
            initDefaultConverters();
        }
    }

    protected void initConvertersFromJavaTypes() {
        int resultWidth = dbAttributes.size();
        this.converters = new ExtendedType[resultWidth];

        for (int i = 0; i < resultWidth; i++) {
            converters[i] = typesMapping.getRegisteredType((String) javaTypes.get(i));
        }
    }

    protected void initDefaultConverters() {
        int resultWidth = dbAttributes.size();
        this.converters = new ExtendedType[resultWidth];

        for (int i = 0; i < resultWidth; i++) {
            String javaType = TypesMapping.getJavaBySqlType(jdbcTypes[i]);
            converters[i] = typesMapping.getRegisteredType(javaType);
        }
    }

    protected void initConvertersFromMapping() {

        // assert that we have all the data
        if (dbAttributes.size() == 0) {
            throw new IllegalArgumentException("DbAttributes list is empty.");
        }

        if (rootEntity == null) {
            throw new IllegalArgumentException("Root ObjEntity is null.");
        }

        int resultWidth = dbAttributes.size();
        this.converters = new ExtendedType[resultWidth];

        for (int i = 0; i < resultWidth; i++) {
            String javaType = null;
            DbAttribute attr = (DbAttribute) dbAttributes.get(i);
            ObjAttribute objAttr = rootEntity.getAttributeForDbAttribute(attr);

            // TODO: [See CAY-207 for details] This setup doesn't allow to correctly
            // determine the Java class of an attribute if it is defined in a sub-entity
            // of the query root entity... Hence all inherited attributes will be fetched
            // as generic types, ignoring any possible custom type.
            if (objAttr != null) {
                javaType = objAttr.getType();
            }
            else {
                javaType = TypesMapping.getJavaBySqlType(attr.getType());
            }

            converters[i] = typesMapping.getRegisteredType(javaType);
        }
    }

    public ExtendedType[] getConverters() {
        return converters;
    }

    /**
     * @since 1.1
     */
    public int[] getIdIndexes(DbEntity entity) {
        indexIds(entity);
        return idIndexes;
    }

    public int[] getJdbcTypes() {
        return jdbcTypes;
    }

    public String[] getNames() {
        return names;
    }

    /**
     * Returns a count of columns in the result.
     */
    public int getResultWidth() {
        return dbAttributes.size();
    }

    public int[] getOutParamIndexes() {
        return outParamIndexes;
    }

    // [UGLY HACK AHEAD] wrapper to make a ProcedureParameter
    // look like a DbAttribute. A better implementation would
    // probably be a common interface for both.
    static class ProcedureParameterWrapper extends DbAttribute {

        ProcedureParameter parameter;

        ProcedureParameterWrapper(ProcedureParameter parameter) {
            this.parameter = parameter;
        }

        public int getMaxLength() {
            return parameter.getMaxLength();
        }

        public int getPrecision() {
            return parameter.getPrecision();
        }

        public int getType() {
            return parameter.getType();
        }

        public String getName() {
            return parameter.getName();
        }

        public Object getParent() {
            return parameter.getProcedure();
        }

        public ProcedureParameter getParameter() {
            return parameter;
        }

    }
}

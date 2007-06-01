/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.jdbc.ColumnDescriptor;
import org.objectstyle.cayenne.access.types.ExtendedType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParameter;

/**
 * Contains information about the ResultSet used to process fetched rows. 
 * ResultDescriptor is initialized by calling various "add*" methods, after that
 * it must be indexed by calling "index".
 * 
 * @author Andrei Adamchik
 */
public class ResultDescriptor {

    private static final int[] emptyInt = new int[0];

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
                descriptor.addJavaType(
                    TypesMapping.getJavaBySqlType(sqlType, length, precision));
            }
        }
        catch (SQLException sqex) {
            throw new CayenneRuntimeException("Error reading metadata.", sqex);
        }

        descriptor.index();
        return descriptor;
    }

    /**
     * Creates and returns a ResultDescriptor for an array of DataColumnDescriptors.
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
            descriptor.converters[i] =
                typeConverters.getRegisteredType(columns[i].getJavaClass());
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
            descriptor.addDbAttribute(
                new ProcedureParameterWrapper((ProcedureParameter) it.next()));
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
                    idIndexes = emptyInt;

                    if (entity == null) {
                        return;
                    }

                    indexedIds = true;

                    int resultWidth = names.length;
                    int[] tmp = new int[resultWidth];
                    int j = 0;
                    for (int i = 0; i < resultWidth; i++) {
                        DbAttribute attribute =
                            (DbAttribute) entity.getAttribute(names[i]);
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
            throw new IllegalArgumentException("DbAttributes and Java type arrays must have the same size.");
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
            this.outParamIndexes = emptyInt;
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
     * @deprecated Since 1.1 use {@link #getIdIndexes(DbEntity)}.
     */
    public int[] getIdIndexes() {
        return getIdIndexes(null);
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
            return parameter.getParent();
        }

        public ProcedureParameter getParameter() {
            return parameter;
        }

    }
}

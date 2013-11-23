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

package org.apache.cayenne.dba.openbase;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.types.ByteType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.resource.ResourceLocator;

/**
 * DbAdapter implementation for <a href="http://www.openbase.com">OpenBase</a>. Sample
 * connection settings to use with OpenBase are shown below:
 * 
 * <pre>
 * openbase.jdbc.username = test
 * openbase.jdbc.password = secret
 * openbase.jdbc.url = jdbc:openbase://serverhostname/cayenne
 * openbase.jdbc.driver = com.openbase.jdbc.ObDriver
 * </pre>
 * 
 * @since 1.1
 */
public class OpenBaseAdapter extends JdbcAdapter {

    public OpenBaseAdapter(
            @Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject ResourceLocator resourceLocator) {
        super(
                runtimeProperties,
                defaultExtendedTypes,
                userExtendedTypes,
                extendedTypeFactories,
                resourceLocator);

        // init defaults
        this.setSupportsUniqueConstraints(false);
    }

    /**
     * Uses special action builder to create the right action.
     * 
     * @since 1.2
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new OpenBaseActionBuilder(this, node
                .getEntityResolver()));
    }

    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // Byte handling doesn't work on read...
        // need special converter
        map.registerType(new OpenBaseByteType());

        map.registerType(new OpenBaseCharType());
    }

    @Override
    public DbAttribute buildAttribute(
            String name,
            String typeName,
            int type,
            int size,
            int scale,
            boolean allowNulls) {

        // OpenBase makes no distinction between CHAR and VARCHAR
        // so lets use VARCHAR, since it seems more generic
        if (type == Types.CHAR) {
            type = Types.VARCHAR;
        }

        return super.buildAttribute(name, typeName, type, size, scale, allowNulls);
    }

    /**
     * Returns word "go".
     */
    @Override
    public String getBatchTerminator() {
        return "go";
    }

    /**
     * Returns null, since views are not yet supported in openbase.
     */
    @Override
    public String tableTypeForView() {
        // TODO: according to OpenBase docs views *ARE* supported.
        return null;
    }

    /**
     * Returns OpenBase-specific translator for queries.
     */
    @Override
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new OpenBaseQualifierTranslator(queryAssembler);
    }

    /**
     * Creates and returns a primary key generator. Overrides superclass implementation to
     * return an instance of OpenBasePkGenerator that uses built-in multi-server primary
     * key generation.
     */
    @Override
    protected PkGenerator createPkGenerator() {
        return new OpenBasePkGenerator(this);
    }

    /**
     * Returns a SQL string that can be used to create database table corresponding to
     * <code>ent</code> parameter.
     */
    @Override
    public String createTable(DbEntity ent) {

        StringBuilder buf = new StringBuilder();

        buf.append("CREATE TABLE ");
        buf.append(quotingStrategy.quotedFullyQualifiedName(ent));
        buf.append(" (");

        // columns
        Iterator<DbAttribute> it = ent.getAttributes().iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (first) {
                first = false;
            }
            else {
                buf.append(", ");
            }

            DbAttribute at = it.next();

            // attribute may not be fully valid, do a simple check
            if (at.getType() == TypesMapping.NOT_DEFINED) {
                throw new CayenneRuntimeException("Undefined type for attribute '"
                        + ent.getFullyQualifiedName()
                        + "."
                        + at.getName()
                        + "'.");
            }

            String[] types = externalTypesForJdbcType(at.getType());
            if (types == null || types.length == 0) {
                throw new CayenneRuntimeException("Undefined type for attribute '"
                        + ent.getFullyQualifiedName()
                        + "."
                        + at.getName()
                        + "': "
                        + at.getType());
            }

            String type = types[0];
            buf.append(quotingStrategy.quotedName(at)).append(' ').append(type);

            // append size and precision (if applicable)
            if (TypesMapping.supportsLength(at.getType())) {
                int len = at.getMaxLength();
                int scale = TypesMapping.isDecimal(at.getType()) ? at.getScale() : -1;

                // sanity check
                if (scale > len) {
                    scale = -1;
                }

                if (len > 0) {
                    buf.append('(').append(len);

                    if (scale >= 0) {
                        buf.append(", ").append(scale);
                    }

                    buf.append(')');
                }
            }

            if (at.isMandatory()) {
                buf.append(" NOT NULL");
            }
            else {
                buf.append(" NULL");
            }
        }

        buf.append(')');
        return buf.toString();
    }

    /**
     * Returns a SQL string that can be used to create a foreign key constraint for the
     * relationship.
     */
    @Override
    public String createFkConstraint(DbRelationship rel) {
        StringBuilder buf = new StringBuilder();

        // OpendBase Specifics is that we need to create a constraint going
        // from destination to source for this to work...

        DbEntity sourceEntity = (DbEntity) rel.getSourceEntity();
        DbEntity targetEntity = (DbEntity) rel.getTargetEntity();
        String toMany = (!rel.isToMany()) ? "'1'" : "'0'";

        // TODO: doesn't seem like OpenBase supports compound joins...
        // need to doublecheck that

        int joinsLen = rel.getJoins().size();
        if (joinsLen == 0) {
            throw new CayenneRuntimeException("Relationship has no joins: "
                    + rel.getName());
        }
        else if (joinsLen > 1) {
            // ignore extra joins
        }

        DbJoin join = rel.getJoins().get(0);

        buf
                .append("INSERT INTO _SYS_RELATIONSHIP (")
                .append("dest_table, dest_column, source_table, source_column, ")
                .append(
                        "block_delete, cascade_delete, one_to_many, operator, relationshipName")
                .append(") VALUES ('")
                .append(sourceEntity.getFullyQualifiedName())
                .append("', '")
                .append(join.getSourceName())
                .append("', '")
                .append(targetEntity.getFullyQualifiedName())
                .append("', '")
                .append(join.getTargetName())
                .append("', 0, 0, ")
                .append(toMany)
                .append(", '=', '")
                .append(rel.getName())
                .append("')");

        return buf.toString();
    }

    // OpenBase JDBC driver has trouble reading "integer" as byte
    // this converter addresses such problem
    static class OpenBaseByteType extends ByteType {

        OpenBaseByteType() {
            super(true);
        }

        @Override
        public Object materializeObject(ResultSet rs, int index, int type)
                throws Exception {

            // read value as int, and then narrow it down
            int val = rs.getInt(index);
            return (rs.wasNull()) ? null : Byte.valueOf((byte) val);
        }

        @Override
        public Object materializeObject(CallableStatement rs, int index, int type)
                throws Exception {

            // read value as int, and then narrow it down
            int val = rs.getInt(index);
            return (rs.wasNull()) ? null : Byte.valueOf((byte) val);
        }
    }

    static class OpenBaseCharType extends CharType {

        OpenBaseCharType() {
            super(false, true);
        }

        @Override
        public void setJdbcObject(
                PreparedStatement st,
                Object val,
                int pos,
                int type,
                int precision) throws Exception {

            // These to types map to "text"; and when setting "text" as object
            // OB assumes that the object is the actual CLOB... weird
            if (type == Types.CLOB || type == Types.LONGVARCHAR) {
                st.setString(pos, (String) val);
            }
            else {
                super.setJdbcObject(st, val, pos, type, precision);
            }
        }
    }

    @Override
    public MergerFactory mergerFactory() {
        return new OpenBaseMergerFactory();
    }

}

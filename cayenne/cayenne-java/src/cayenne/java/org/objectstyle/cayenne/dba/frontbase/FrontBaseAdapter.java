/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.dba.frontbase;

import java.sql.Types;
import java.util.Iterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLAction;

/**
 * DbAdapter implementation for <a href="http://www.frontbase.com/">FrontBase RDBMS</a>.
 * Sample <a target="_top"
 * href="../../../../../../../developerguide/unit-tests.html">connection settings</a> to
 * use with FrontBase are shown below:
 * 
 * <pre>
 *          fb.cayenne.adapter = org.objectstyle.cayenne.dba.frontbase.FrontBaseAdapter
 *          fb.jdbc.username = _system
 *          fb.jdbc.password = secret
 *          fb.jdbc.url = jdbc:FrontBase://localhost/cayenne/
 *          fb.jdbc.driver = jdbc.FrontBase.FBJDriver
 * </pre>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO, Andrus 11/8/2005:
// Limitations (also see FrontBaseStackAdapter in unit tests):
//
// 1. Case insensitive ordering (i.e. UPPER in the ORDER BY clause) is supported by
// FrontBase, however aliases don't work ( ORDER BY UPPER(t0.ARTIST_NAME)) ... not sure
// what to do about it.
public class FrontBaseAdapter extends JdbcAdapter {

    public FrontBaseAdapter() {
        setSupportsBatchUpdates(true);
    }

    /**
     * Uses special action builder to create the right action.
     */
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new FrontBaseActionBuilder(this, node
                .getEntityResolver()));
    }

    public String tableTypeForTable() {
        return "BASE TABLE";
    }

    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        map.registerType(new FrontBaseByteArrayType());
        map.registerType(new FrontBaseBooleanType());
        map.registerType(new FrontBaseCharType());
    }

    /**
     * Customizes table creating procedure for FrontBase.
     */
    public String createTable(DbEntity ent) {

        // later we may support view creation
        // for derived DbEntities
        if (ent instanceof DerivedDbEntity) {
            throw new CayenneRuntimeException("Can't create table for derived DbEntity '"
                    + ent.getName()
                    + "'.");
        }

        StringBuffer buf = new StringBuffer();
        buf.append("CREATE TABLE ").append(ent.getFullyQualifiedName()).append(" (");

        // columns
        Iterator it = ent.getAttributes().iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (first) {
                first = false;
            }
            else {
                buf.append(", ");
            }

            DbAttribute at = (DbAttribute) it.next();

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
            buf.append(at.getName()).append(' ').append(type);

            // Mapping LONGVARCHAR without length creates a column with lenght "1" which
            // is defintely not what we want...so just use something very large (1Gb seems
            // to be the limit for FB)
            if (at.getType() == Types.LONGVARCHAR) {

                int len = at.getMaxLength() > 0 ? at.getMaxLength() : 1073741824;
                buf.append("(" + len + ")");
            }
            else if (at.getType() == Types.VARBINARY || at.getType() == Types.BINARY) {

                // use a BIT column with size * 8
                int len = at.getMaxLength() > 0 ? at.getMaxLength() : 1073741824;
                len *= 8;
                buf.append("(" + len + ")");
            }
            else if (TypesMapping.supportsLength(at.getType())) {
                int len = at.getMaxLength();
                int prec = TypesMapping.isDecimal(at.getType()) ? at.getPrecision() : -1;

                // sanity check
                if (prec > len) {
                    prec = -1;
                }

                if (len > 0) {
                    buf.append('(').append(len);

                    if (prec >= 0) {
                        buf.append(", ").append(prec);
                    }

                    buf.append(')');
                }
            }

            if (at.isMandatory()) {
                buf.append(" NOT NULL");
            }
            // else: don't appen NULL for FrontBase:
        }

        // primary key clause
        Iterator pkit = ent.getPrimaryKey().iterator();
        if (pkit.hasNext()) {
            if (first)
                first = false;
            else
                buf.append(", ");

            buf.append("PRIMARY KEY (");
            boolean firstPk = true;
            while (pkit.hasNext()) {
                if (firstPk)
                    firstPk = false;
                else
                    buf.append(", ");

                DbAttribute at = (DbAttribute) pkit.next();
                buf.append(at.getName());
            }
            buf.append(')');
        }
        buf.append(')');
        return buf.toString();
    }

    /**
     * Adds the CASCADE option to the DROP TABLE clause.
     */
    public String dropTable(DbEntity ent) {
        return super.dropTable(ent) + " CASCADE";
    }

    protected PkGenerator createPkGenerator() {
        return new FrontBasePkGenerator();
    }
}

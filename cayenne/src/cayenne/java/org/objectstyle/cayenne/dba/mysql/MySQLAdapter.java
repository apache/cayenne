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
package org.objectstyle.cayenne.dba.mysql;

import java.sql.Types;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbRelationship;

/**
 * DbAdapter implementation for <a href="http://www.mysql.com">MySQL RDBMS</a>.
 * Sample <a target="_top" href="../../../../../../../developerguide/unit-tests.html">connection 
 * settings</a> to use with MySQL are shown below:
 * 
<pre>
test-mysql.cayenne.adapter = org.objectstyle.cayenne.dba.mysql.MySQLAdapter
test-mysql.jdbc.username = test
test-mysql.jdbc.password = secret
test-mysql.jdbc.url = jdbc:mysql://serverhostname/cayenne
test-mysql.jdbc.driver = com.mysql.jdbc.Driver
</pre>
 * 
 * @author Andrei Adamchik
 */
public class MySQLAdapter extends JdbcAdapter {

    public MySQLAdapter() {
        // init defaults
        this.setSupportsFkConstraints(false);
        this.setSupportsUniqueConstraints(true);
    }

    /**
     * Installs appropriate ExtendedTypes used as converters for passing values
     * between JDBC and Java layers.
     */
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // must handle CLOBs as strings, otherwise there
        // are problems with NULL clobs that are treated
        // as empty strings... somehow this doesn't happen
        //  for BLOBs (ConnectorJ v. 3.0.9)
        map.registerType(new CharType(false, false));
    }

    public DbAttribute buildAttribute(
        String name,
        String typeName,
        int type,
        int size,
        int precision,
        boolean allowNulls) {

        // all LOB types are returned by the driver as OTHER... must remap them manually
        // (at least on MySQL 3.23)
        if (type == Types.OTHER) {
            if ("longblob".equalsIgnoreCase(typeName)) {
                type = Types.BLOB;
            }
            else if ("mediumblob".equalsIgnoreCase(typeName)) {
                type = Types.BLOB;
            }
            else if ("blob".equalsIgnoreCase(typeName)) {
                type = Types.BLOB;
            }
            else if ("tinyblob".equalsIgnoreCase(typeName)) {
                type = Types.VARBINARY;
            }
            else if ("longtext".equalsIgnoreCase(typeName)) {
                type = Types.CLOB;
            }
            else if ("mediumtext".equalsIgnoreCase(typeName)) {
                type = Types.CLOB;
            }
            else if ("text".equalsIgnoreCase(typeName)) {
                type = Types.CLOB;
            }
            else if ("tinytext".equalsIgnoreCase(typeName)) {
                type = Types.VARCHAR;
            }
        }

        return super.buildAttribute(name, typeName, type, size, precision, allowNulls);
    }

    /** Throws an exception, since FK constraints are not supported by MySQL. */
    public String createFkConstraint(DbRelationship rel) {
        throw new CayenneRuntimeException("FK constraints are not supported.");
    }

    /** 
     * Returns null, since views are not yet supported in MySQL. Views
     * support is promised in MySQL 4.1.
     */
    public String tableTypeForView() {
        return null;
    }

    /**
      * Creates and returns a primary key generator. Overrides superclass 
      * implementation to return an
      * instance of MySQLPkGenerator that does the correct table locking.
      */
    protected PkGenerator createPkGenerator() {
        return new MySQLPkGenerator();
    }
}

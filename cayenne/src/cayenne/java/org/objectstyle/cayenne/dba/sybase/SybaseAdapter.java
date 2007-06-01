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
package org.objectstyle.cayenne.dba.sybase;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.objectstyle.cayenne.access.types.ByteArrayType;
import org.objectstyle.cayenne.access.types.ByteType;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.access.types.ShortType;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;

/** 
 * DbAdapter implementation for <a href="http://www.sybase.com">Sybase RDBMS</a>.
 *
 * @author Andrei Adamchik
 */
public class SybaseAdapter extends JdbcAdapter {

    /**
     * Returns word "go".
     * 
     * @since 1.0.4
     */
    public String getBatchTerminator() {
        return "go";
    }
    
    /**
     * Installs appropriate ExtendedTypes as converters for passing values
     * between JDBC and Java layers.
     */
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        map.registerType(new CharType(true, false));

        // create specially configured ByteArrayType handler
        map.registerType(new ByteArrayType(true, false));

        // address Sybase driver inability to handle java.lang.Short and java.lang.Byte
        map.registerType(new ShortType(true));
        map.registerType(new ByteType(true));
    }

    /** 
     * Creates and returns a primary key generator. 
     * Overrides superclass implementation to return an
     * instance of SybasePkGenerator.
     */
    protected PkGenerator createPkGenerator() {
        return new SybasePkGenerator();
    }

    public void bindParameter(
        PreparedStatement statement,
        Object object,
        int pos,
        int sqlType,
        int precision)
        throws SQLException, Exception {

        // Sybase driver doesn't like CLOBs and BLOBs as parameters
        if (object == null) {
            if (sqlType == Types.CLOB) {
                sqlType = Types.VARCHAR;
            }
            else if (sqlType == Types.BLOB) {
                sqlType = Types.VARBINARY;
            }
        }

        super.bindParameter(statement, object, pos, sqlType, precision);
    }
}
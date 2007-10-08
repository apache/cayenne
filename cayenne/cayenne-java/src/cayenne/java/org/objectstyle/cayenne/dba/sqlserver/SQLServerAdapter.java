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
package org.objectstyle.cayenne.dba.sqlserver;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.trans.QualifierTranslator;
import org.objectstyle.cayenne.access.trans.QueryAssembler;
import org.objectstyle.cayenne.access.trans.TrimmingQualifierTranslator;
import org.objectstyle.cayenne.dba.sybase.SybaseAdapter;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLAction;

/**
 * Cayenne DbAdapter implementation for <a
 * href="http://www.microsoft.com/sql/default.asp"Microsoft SQL Server </a> engine.
 * </p>
 * <h3>Microsoft Driver Settings</h3>
 * <p>
 * Sample <a target="_top"
 * href="../../../../../../../developerguide/unit-tests.html">connection settings </a> to
 * use with MS SQL Server are shown below:
 * 
 * <pre>
 * 
 *  
 *   
 *       sqlserver.cayenne.adapter = org.objectstyle.cayenne.dba.sqlserver.SQLServerAdapter
 *       sqlserver.jdbc.username = test
 *       sqlserver.jdbc.password = secret
 *       sqlserver.jdbc.url = jdbc:microsoft:sqlserver://192.168.0.65;databaseName=cayenne;SelectMethod=cursor
 *       sqlserver.jdbc.driver = com.microsoft.jdbc.sqlserver.SQLServerDriver
 * </pre>
 * 
 * <p>
 * <i>Note on case-sensitive LIKE: if your application requires case-sensitive LIKE
 * support, ask your DBA to configure the database to use a case-senstitive collation (one
 * with "CS" in symbolic collation name instead of "CI", e.g.
 * "SQL_Latin1_general_CP1_CS_AS"). </i>
 * </p>
 * <h3>jTDS Driver Settings</h3>
 * <p>
 * jTDS is an open source driver that can be downloaded from <a href=
 * "http://jtds.sourceforge.net">http://jtds.sourceforge.net </a>. It supports both
 * SQLServer and Sybase. Sample SQLServer settings are the following:
 * </p>
 * 
 * <pre>
 * 
 *  
 *   
 *       sqlserver.cayenne.adapter = org.objectstyle.cayenne.dba.sqlserver.SQLServerAdapter
 *       sqlserver.jdbc.username = test
 *       sqlserver.jdbc.password = secret
 *       sqlserver.jdbc.url = jdbc:jtds:sqlserver://192.168.0.65/cayenne
 *       sqlserver.jdbc.driver = net.sourceforge.jtds.jdbc.Driver
 *    
 *   
 *  
 * </pre>
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
public class SQLServerAdapter extends SybaseAdapter {

    public static final String TRIM_FUNCTION = "RTRIM";

    public SQLServerAdapter() {
        // TODO: i wonder if Sybase supports generated keys... 
        // in this case we need to move this to the super.
        this.setSupportsGeneratedKeys(true);
    }
    

    /**
     * Uses SQLServerActionBuilder to create the right action.
     * 
     * @since 1.2
     */
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new SQLServerActionBuilder(this, node.getEntityResolver()));
    }

    
    /**
     * Returns a trimming translator.
     */
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new TrimmingQualifierTranslator(
                queryAssembler,
                SQLServerAdapter.TRIM_FUNCTION);
    }

    
    /**
     * Overrides super implementation to correctly set up identity columns.
     * 
     * @since 1.2
     */
    protected void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        super.createTableAppendColumn(sqlBuffer, column);
        
        if(column.isGenerated()) {
            // current limitation - we don't allow to set identity parameters...
            sqlBuffer.append(" IDENTITY (1, 1)");
        }
    }
}
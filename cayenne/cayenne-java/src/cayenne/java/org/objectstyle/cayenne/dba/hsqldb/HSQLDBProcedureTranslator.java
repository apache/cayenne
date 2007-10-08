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
package org.objectstyle.cayenne.dba.hsqldb;

import org.objectstyle.cayenne.access.trans.ProcedureTranslator;
import org.objectstyle.cayenne.map.Procedure;

/**
 * Works around HSQLDB's pickiness about stored procedure syntax.
 * 
 * @since 1.2
 * @author Cristopher Daniluk
 */
public class HSQLDBProcedureTranslator extends ProcedureTranslator {

    /**
     * Creates HSQLDB-compliant SQL to execute a stored procedure.
     */
    protected String createSqlString() {
        Procedure procedure = getProcedure();

        StringBuffer buf = new StringBuffer();

        int totalParams = callParams.size();

        // check if procedure returns values
        if (procedure.isReturningValue()) {
            totalParams--;

            // HSQL won't accept "? =". The parser only recognizes "?="

            // TODO: Andrus, 12/12/2005 - this is kind of how it is in the
            // CallableStatement javadocs, so we may need to make "?=" a default ... this
            // requires testing on Oracle/PostgreSQL/Sybase/SQLServer.
            buf.append("{?= call ");
        }
        else {
            buf.append("{call ");
        }

        // HSQLDB requires that procedures with periods (referring to Java packages)
        // be enclosed in quotes. It is not clear that quotes can always be used, though
        if (procedure.getFullyQualifiedName().indexOf('.') > -1) {
            buf.append("\"").append(procedure.getFullyQualifiedName()).append("\"");
        }
        else {
            buf.append(procedure.getFullyQualifiedName());
        }

        if (totalParams > 0) {
            // unroll the loop
            buf.append("(?");

            for (int i = 1; i < totalParams; i++) {
                buf.append(", ?");
            }

            buf.append(")");
        }

        buf.append("}");
        return buf.toString();
    }
}

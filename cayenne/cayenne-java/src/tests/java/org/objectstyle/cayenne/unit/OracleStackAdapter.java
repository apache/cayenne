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

package org.objectstyle.cayenne.unit;

import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.access.DataContextProcedureQueryTst;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.oracle.OracleAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.map.ProcedureParameter;

/**
 * @author Andrei Adamchik
 */
public class OracleStackAdapter extends AccessStackAdapter {

    /**
     * Constructor for OracleDelegate.
     * 
     * @param adapter
     */
    public OracleStackAdapter(DbAdapter adapter) {
        super(adapter);
    }

    public boolean supportsStoredProcedures() {
        return true;
    }

    public void willDropTables(Connection conn, DataMap map, Collection tablesToDrop)
            throws Exception {
        // avoid dropping constraints...
    }

    /**
     * Oracle 8i does not support more then 1 "LONG xx" column per table PAINTING_INFO
     * need to be fixed.
     */
    public void willCreateTables(Connection con, DataMap map) {
        DbEntity paintingInfo = map.getDbEntity("PAINTING_INFO");

        if (paintingInfo != null) {
            DbAttribute textReview = (DbAttribute) paintingInfo
                    .getAttribute("TEXT_REVIEW");
            textReview.setType(Types.VARCHAR);
            textReview.setMaxLength(255);
        }
    }

    public void createdTables(Connection con, DataMap map) throws Exception {
        if (map.getProcedureMap().containsKey("cayenne_tst_select_proc")) {
            executeDDL(con, "oracle", "create-types-pkg.sql");
            executeDDL(con, "oracle", "create-select-sp.sql");
            executeDDL(con, "oracle", "create-update-sp.sql");
            executeDDL(con, "oracle", "create-out-sp.sql");
        }
    }

    public boolean supportsLobs() {
        return true;
    }

    public void tweakProcedure(Procedure proc) {
        if (DataContextProcedureQueryTst.SELECT_STORED_PROCEDURE.equals(proc.getName())
                && proc.getCallParameters().size() == 2) {
            List params = new ArrayList(proc.getCallParameters());

            proc.clearCallParameters();
            proc.addCallParameter(new ProcedureParameter("result", OracleAdapter
                    .getOracleCursorType(), ProcedureParameter.OUT_PARAMETER));
            Iterator it = params.iterator();
            while (it.hasNext()) {
                ProcedureParameter param = (ProcedureParameter) it.next();
                proc.addCallParameter(param);
            }

            proc.setReturningValue(true);
        }
    }
}

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

package org.apache.cayenne.access.trans;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;

/** 
 * Translates query ordering to SQL. 
 * 
 * @author Andrus Adamchik
 * @author Craig Miskell
 */
public class OrderingTranslator extends QueryAssemblerHelper {

    protected List<String> orderByColumnList = new ArrayList<String>();
    
    public OrderingTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler);
    }

    /** Translates query Ordering list to SQL ORDER BY clause. 
     *  Ordering list is obtained from <code>queryAssembler</code>'s query object. 
     *  In a process of building of ORDER BY clause, <code>queryAssembler</code> 
     *  is notified when a join needs to be added. */
    public String doTranslation() {
        Query q = queryAssembler.getQuery();

        // only select queries can have ordering...
        if (q == null || !(q instanceof SelectQuery))
            return null;

        StringBuffer buf = new StringBuffer();

        for (Ordering ord : ((SelectQuery) q).getOrderings()) {
            if (buf.length() > 0)
                buf.append(", ");

            StringBuffer ordComp = new StringBuffer();
            
            //UPPER is (I think) part of the SQL99 standard, and I'm not convinced it's universally available
            // - should the syntax used here be defined by the Db specific adaptor perhaps, or at least
            // possibly specified by the db adaptor (a DB specific OrderingTranslator hook)?
            if (ord.isCaseInsensitive()) {
                ordComp.append("UPPER(");
            }

            Expression exp = ord.getSortSpec();

            if (exp.getType() == Expression.OBJ_PATH) {
                appendObjPath(ordComp, exp);
            } else if (exp.getType() == Expression.DB_PATH) {
                appendDbPath(ordComp, exp);
            } else {
                throw new CayenneRuntimeException(
                    "Unsupported ordering expression: " + exp);
            }

            //Close UPPER() modifier
            if (ord.isCaseInsensitive()) {
                ordComp.append(")");
            }

            orderByColumnList.add(ordComp.toString());
            
            buf.append(ordComp.toString());

            // "ASC" is a noop, omit it from the query 
            if (!ord.isAscending()) {
                buf.append(" DESC");
            }
        }

        return buf.length() > 0 ? buf.toString() : null;
    }

    /**
     * Returns the column expressions (not Expressions) used in
     * the order by clause.  E.g., in the case of an case-insensitive 
     * order by, an element of the list would be 
     * <code>UPPER(&lt;column reference&gt;)</code>
     */    
    public List<String> getOrderByColumnList() {
        return orderByColumnList;
    }
}

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

package org.apache.cayenne.jpa.cspi;

import javax.persistence.Query;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.SQLTemplate;

public class CjpaNativeQuery extends CjpaQuery {

    private static final String POSITIONAL_PARAM_PREFIX = "positional_";

    public CjpaNativeQuery(ObjectContext context, String sqlString, Class resultClass) {
        super(context);
        setQuery(new SQLTemplate(resultClass, processSQLString(sqlString)));
    }

    public CjpaNativeQuery(ObjectContext context, String sqlString, String dataMapName) {
        super(context);
        DataMap map = context.getEntityResolver().getDataMap(dataMapName);
        setQuery(new SQLTemplate(map, processSQLString(sqlString)));
    }

    protected String processSQLString(String sqlString) {
        // named parameters are like ":parametername" and positional parameters
        // are like "?123". SQLTemplate support "$parametername"

        // TODO: improve convert as ':' could be used in sql. e.x. in
        // non-parametrized parameter or postgresql cast.

        sqlString = sqlString.replace(':', '$');

        // handle positional parameters like named
        if (sqlString.indexOf('?') >= 0) {
            // convert "?123" to "$positional_123"
            sqlString = sqlString.replaceAll("\\?([0-9]+)", "\\$"
                    + POSITIONAL_PARAM_PREFIX
                    + "$1");
        }

        return sqlString;
    }

    /**
     * Bind an argument to a positional parameter.
     * 
     * @param position
     * @param value
     * @return the same query instance
     * @throws IllegalArgumentException if position does not correspond to positional
     *             parameter of query or argument is of incorrect type
     */
    public Query setParameter(int position, Object value) {

        // Positional parameters are designated by the question
        // mark(?) prefix followed by an integer. For example: ?1.
        String name = POSITIONAL_PARAM_PREFIX + Integer.toString(position);
        try {
            return setParameter(name, value);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid positional parameter: "
                    + position, e);
        }
    }

}

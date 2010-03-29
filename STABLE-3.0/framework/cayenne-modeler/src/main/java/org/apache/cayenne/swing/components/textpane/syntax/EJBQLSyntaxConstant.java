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
package org.apache.cayenne.swing.components.textpane.syntax;

public class EJBQLSyntaxConstant implements SyntaxConstant{
	private static String[] KEYWORDS = { "AS", "ABS", "ASC", "AVG", "BETWEEN",
			"BOTH", "BIT_LENGTH", "CHARACTER_LENGTH", "CHAR_LENGTH", "COUNT",
			"CONCAT", "CURRENT_TIME", "CURRENT_DATE", "CURRENT_TIMESTAMP",
			"DELETE", "DESC", "DISTINCT", "EMPTY", "ESCAPE", "FALSE", "FETCH",
			"FROM", "GROUP", "HAVING", "IS", "INNER", "LOCATE", "LOWER",
			"LEADING", "LEFT", "LENGTH", "MAX", "MEMBER", "MIN", "MOD", "NEW",
			"NULL", "OBJECT", "OF", "ORDER", "POSITION", "SELECT", "SOME",
			"SUM", "SIZE", "SQRT", "SUBSTR", "TRAILING", "TRUE", "TRIM",
			"UNKNOWN", "UPDATE", "UPPER", "USER", "WHERE", "JOIN" };

	private static String[] KEYWORDS2 = {};

	private static String[] TYPES = {};

	private static String[] OPERATORS = { "ALL", "AND", "ANY", "BETWEEN", "BY",
			"EXISTS", "IN", "LIKE", "NOT", "NULL", "OR" };

    
    public String[] getKEYWORDS() {
        return KEYWORDS;
    }

    
    public String[] getKEYWORDS2() {
        return KEYWORDS2;
    }

    
    public String[] getTYPES() {
        return TYPES;
    }

    
    public String[] getOPERATORS() {
        return OPERATORS;
    }
    
    public String getContentType() {
        return "text/ejbql";
    }

}

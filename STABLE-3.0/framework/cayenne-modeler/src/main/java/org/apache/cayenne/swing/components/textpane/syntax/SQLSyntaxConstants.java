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


public class SQLSyntaxConstants implements SyntaxConstant{

	private static String[] KEYWORDS = {

	"ABORT", "ACCESS", "ADD", "ALTER", "ARRAY", "ARRAY_LEN", "AS", "ASC",
			"ASSERT", "ASSIGN", "AT", "AUDIT", "AUTHORIZATION",
			"AUTHORIZATION", "AVG", "BASE_TABLE", "BEGIN", "BODY", "CASE",
			"CHAR", "CHAR_BASE", "CHECK", "CLOSE", "CLUSTER", "CLUSTERS",
			"COLAUTH", "COLAUTH", "COLUMN", "COMMIT", "COMPRESS", "CONSTANT",
			"CONSTRAINT", "CONSTRAINT", "COUNT", "CREATE", "CURRENT",
			"CURRVAL", "CURSOR", "DATABASE", "DATA_BASE", "DATE", "DBA",
			"DEBUGOFF", "DEBUGON", "DECLARE", "DEFAULT", "DEFINITION", "DELAY",
			"DELETE", "DESC", "DIGITS", "DISPOSE", "DISTINCT", "DO", "DROP",
			"DUMP", "ELSE", "ELSIF", "END", "ENTRY", "EXCEPTION",
			"EXCEPTION_INIT", "EXCLUSIVE", "EXIT", "FALSE", "FETCH", "FILE",
			"FOR", "FORM", "FROM", "FUNCTION", "GENERIC", "GOTO", "GRANT",
			"GREATEST", "GROUP", "HAVING", "IDENTIFIED", "IDENTITYCOL", "IF",
			"IMMEDIATE", "INCREMENT", "INDEX", "INDEXES", "INDICATOR",
			"INITIAL", "INSERT", "INTERFACE", "INTO", "IS", "LEAST", "LEVEL",
			"LIMITED", "LOCK", "LONG", "LOOP", "MAX", "MAXEXTENTS", "MIN",
			"MINUS", "MLSLABEL", "MOD", "MORE", "NEW", "NEXTVAL", "NOAUDIT",
			"NOCOMPRESS", "NOWAIT", "NULL", "NUMBER_BASE", "OF", "OFFLINE",
			"ON", "OFF", "ONLINE", "OPEN", "OPTION", "ORDER", "OTHERS", "OUT",
			"PACKAGE", "PARTITION", "PCTFREE", "PRAGMA", "PRIVATE",
			"PRIVILEGES", "PROCEDURE", "PUBLIC", "QUOTED_IDENTIFIER", "RAISE",
			"RANGE", "RECORD", "REF", "RELEASE", "REMR", "RENAME", "RESOURCE",
			"RETURN", "REVERSE", "REVOKE", "ROLLBACK", "ROW", "ROWLABEL",
			"ROWNUM", "ROWS", "ROWTYPE", "RUN", "SAVEPOINT", "SCHEMA",
			"SELECT", "SEPERATE", "SESSION", "SET", "SHARE", "SPACE", "SQL",
			"SQLCODE", "SQLERRM", "STATEMENT", "STDDEV", "SUBTYPE",
			"SUCCESSFULL", "SUM", "SYNONYM", "SYSDATE", "TABAUTH", "TABLE",
			"TABLES", "TASK", "TERMINATE", "THEN", "TO", "TRIGGER", "TRUE",
			"TYPE", "UID", "UNION", "UNIQUE", "UPDATE", "UPDATETEXT", "USE",
			"USER", "USING", "VALIDATE", "VALUES", "VARIANCE", "VIEW", "VIEWS",
			"WHEN", "WHENEVER", "WHERE", "WHILE", "WITH", "WORK", "WRITE",
			"XOR",

			"UPPER", "VERIFY", "SERVEROUTPUT", "PAGESIZE", "LINESIZE",
			"ARRAYSIZE", "DBMS_OUTPUT", "PUT_LINE", "ENABLE",

			"FIRST", "LIMIT", "OFFSET", "TOP"

	};

	private static String[] KEYWORDS2 = { "ABS", "ACOS", "ADD_MONTHS", "ASCII",
			"ASIN", "ATAN", "ATAN2", "CEIL", "CHARTOROWID", "CHR", "CONCAT",
			"CONVERT", "COS", "COSH", "DECODE", "DEFINE", "FLOOR", "HEXTORAW",
			"INITCAP", "INSTR", "INSTRB", "LAST_DAY", "LENGTH", "LENGTHB",
			"LN", "LOG", "LOWER", "LPAD", "LTRIM", "MOD", "MONTHS_BETWEEN",
			"NEW_TIME", "NEXT_DAY", "NLSSORT", "NSL_INITCAP", "NLS_LOWER",
			"NLS_UPPER", "NVL", "POWER", "RAWTOHEX", "REPLACE", "ROUND",
			"ROWIDTOCHAR", "RPAD", "RTRIM", "SIGN", "SOUNDEX", "SIN", "SINH",
			"SQRT", "SUBSTR", "SUBSTRB", "TAN", "TANH", "TO_CHAR", "TO_DATE",
			"TO_MULTIBYTE", "TO_NUMBER", "TO_SINGLE_BYTE", "TRANSLATE",
			"TRUNC", 
			"#bind", "#bindEqual", "#bindNotEqual", "#bindObjectEqual",
			"#bindObjectNotEqual", "#chain", "#chunk", "#end", "#result"

	};

	private static String[] TYPES = { "binary", "bit", "blob", "boolean",
			"char", "character", "DATE", "datetime", "DEC", "decimal",
			"DOUBLE PRECISION", "float", "image", "int", "integer", "money",
			"name", "NATURAL", "NATURALN", "NUMBER", "numeric", "nchar",
			"nvarchar", "ntext", "pls_integer", "POSITIVE", "POSITIVEN", "RAW",
			"real", "ROWID", "SIGNTYPE", "smalldatetime", "smallint",
			"smallmoney", "text", "timestamp", "tinyint", "uniqueidentifier",
			"UROWID", "varbinary", "varchar", "varchar2" };

	private static String[] OPERATORS = { "ALL", "AND", "ANY", "BETWEEN", "BY",
			"CONNECT", "EXISTS", "IN", "INTERSECT", "LIKE", "NOT", "NULL",
			"OR", "START", "UNION", "WITH" };
	
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
        return "text/sql";
    }
}

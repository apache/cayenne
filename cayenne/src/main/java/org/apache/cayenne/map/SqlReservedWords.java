/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.map;

import java.util.Set;

/**
 * A set of SQL reserved words used to avoid generating unquoted table aliases that would collide with keywords. This
 * is a broad union of the SQL standard and common reserved words across the databases Cayenne supports; it does not
 * need to be exhaustive, only good enough that mnemonic aliases stay safe.
 */
final class SqlReservedWords {

    private static final Set<String> WORDS = Set.of(
            "ABSOLUTE", "ACTION", "ADD", "ALL", "ALLOCATE", "ALTER", "AND", "ANY", "ARE", "AS",
            "ASC", "ASSERTION", "AT", "AUTHORIZATION", "AVG",
            "BEGIN", "BETWEEN", "BIGINT", "BINARY", "BIT", "BOOLEAN", "BOTH", "BY",
            "CALL", "CASCADE", "CASCADED", "CASE", "CAST", "CATALOG", "CHAR", "CHARACTER", "CHECK",
            "CLOSE", "COALESCE", "COLLATE", "COLLATION", "COLUMN", "COMMIT", "CONNECT", "CONNECTION",
            "CONSTRAINT", "CONTINUE", "CONVERT", "CORRESPONDING", "COUNT", "CREATE", "CROSS",
            "CURRENT", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR",
            "DATE", "DAY", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DEFERRABLE",
            "DEFERRED", "DELETE", "DESC", "DESCRIBE", "DIAGNOSTICS", "DISCONNECT", "DISTINCT",
            "DOMAIN", "DOUBLE", "DROP",
            "ELSE", "END", "ESCAPE", "EXCEPT", "EXCEPTION", "EXEC", "EXECUTE", "EXISTS", "EXTERNAL",
            "EXTRACT",
            "FALSE", "FETCH", "FIRST", "FLOAT", "FOR", "FOREIGN", "FOUND", "FROM", "FULL", "FUNCTION",
            "GET", "GLOBAL", "GO", "GOTO", "GRANT", "GROUP",
            "HAVING", "HOUR",
            "IDENTITY", "IF", "IMMEDIATE", "IN", "INDEX", "INDICATOR", "INITIALLY", "INNER", "INPUT",
            "INSENSITIVE", "INSERT", "INT", "INTEGER", "INTERSECT", "INTERVAL", "INTO", "IS",
            "ISOLATION",
            "JOIN",
            "KEY",
            "LANGUAGE", "LAST", "LEADING", "LEFT", "LEVEL", "LIKE", "LIMIT", "LOCAL", "LOWER",
            "MATCH", "MAX", "MIN", "MINUTE", "MODULE", "MONTH",
            "NAMES", "NATIONAL", "NATURAL", "NCHAR", "NEXT", "NO", "NOT", "NULL", "NULLIF", "NUMERIC",
            "OCTET_LENGTH", "OF", "ON", "ONLY", "OPEN", "OPTION", "OR", "ORDER", "OUTER", "OUTPUT",
            "OVER", "OVERLAPS",
            "PAD", "PARTIAL", "PARTITION", "POSITION", "PRECISION", "PREPARE", "PRESERVE", "PRIMARY",
            "PRIOR", "PRIVILEGES", "PROCEDURE", "PUBLIC",
            "READ", "REAL", "REFERENCES", "RELATIVE", "RESTRICT", "REVOKE", "RIGHT", "ROLLBACK",
            "ROW", "ROWS",
            "SCHEMA", "SCROLL", "SECOND", "SECTION", "SELECT", "SESSION", "SESSION_USER", "SET",
            "SIZE", "SMALLINT", "SOME", "SPACE", "SQL", "SQLCODE", "SQLERROR", "SQLSTATE", "START",
            "SUBSTRING", "SUM", "SYSTEM_USER",
            "TABLE", "TEMPORARY", "THEN", "TIME", "TIMESTAMP", "TIMEZONE_HOUR", "TIMEZONE_MINUTE",
            "TO", "TOP", "TRAILING", "TRANSACTION", "TRANSLATE", "TRANSLATION", "TRIGGER", "TRIM",
            "TRUE",
            "UNION", "UNIQUE", "UNKNOWN", "UPDATE", "UPPER", "USAGE", "USER", "USING",
            "VALUE", "VALUES", "VARCHAR", "VARYING", "VIEW",
            "WHEN", "WHENEVER", "WHERE", "WHILE", "WITH", "WORK", "WRITE",
            "YEAR",
            "ZONE");

    static boolean isReserved(String word) {
        return WORDS.contains(word.toUpperCase());
    }

    private SqlReservedWords() {
    }
}

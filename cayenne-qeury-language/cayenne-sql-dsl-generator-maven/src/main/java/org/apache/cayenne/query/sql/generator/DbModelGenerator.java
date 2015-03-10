/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cayenne.query.sql.generator;

import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;

public class DbModelGenerator {

    public void generate(File path, String fullQualifiedClassName, DataMap map) {
        int lastPointIndex = fullQualifiedClassName.lastIndexOf('.');
        String classPackage = fullQualifiedClassName.substring(0, lastPointIndex).replace('.', File.pathSeparatorChar);
        String className = fullQualifiedClassName.substring(lastPointIndex);

        FileWriter out = null;
        try {
            File dirPath = new File(path, classPackage);
            FileUtils.forceMkdir(dirPath);

            out = new FileWriter(new File(dirPath, className + ".java"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            generate(out, map, classPackage, className);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void generate(Appendable out, DataMap map, String classPackage, String className) throws IOException {
        out.append("package ").append(classPackage).append(";\n");
        out.append("\n");
        out.append("import ").append(Field.class.getName()).append(";\n");
        out.append("import ").append(BaseTable.class.getName()).append(";\n");
        out.append("import ").append(BigDecimal.class.getName()).append(";\n");
        out.append("import ").append(Date.class.getName()).append(";\n");
        out.append("import ").append(Timestamp.class.getName()).append(";\n");
        out.append("\n\n");
        out.append("public interface ").append(className).append(" {\n");

        generateTables(out, map);

        out.append("}\n");
    }

    private void generateTables(Appendable out, DataMap map) throws IOException {
        for (DbEntity table : map.getDbEntities()) {

            out.append(String.format(
                    "    %1$s %1$s = new %1$s();\n" +
                    "    final class %1$s extends BaseTable<%1$s> {\n", table.getName()));

            generateColumns(out, table);

            out.append("         { initFields(); }\n");
            out.append("    }\n");
        }
    }

    private void generateColumns(Appendable out, DbEntity table) throws IOException {
        for (DbAttribute column : table.getAttributes()) {
            out.append(String.format("        public final Field %s = %s;\n", column.getName(), getColumnCreationMethod(column)));
        }

        for (DbRelationship column : table.getRelationships()) {
            out.append(String.format("        public final Field %s = %s;\n", column.getName(), getColumnCreationMethod(column)));
        }
    }

    private String getColumnCreationMethod(DbAttribute column) {
        String columnCreationMethod = getJavaType(column.getType());
        if (column.isPrimaryKey()) {
            columnCreationMethod += ".primaryKey()";
        }
        return columnCreationMethod;
    }

    private String getColumnCreationMethod(DbRelationship column) {
        Collection<DbAttribute> pkColumns;
        if (column.getJoins().isEmpty()) {
            pkColumns = column.getTargetEntity().getPrimaryKeys();
        } else {
            pkColumns = column.getTargetAttributes();
        }
        return createForeignKeyColumn(pkColumns);

        // TODO
        //if (column.isPrimaryKey()) {
        //    columnCreationMethod += ".primaryKey()";
        //}
    }

    private String createForeignKeyColumn(Collection<DbAttribute> pkColumn) {
        DbAttribute attr = pkColumn.iterator().next();

        return "foreignKey(" + attr.getEntity().getName() + "." + attr.getName() + ")";
    }

    private static String getJavaType(int columnType) {
        switch (columnType) {
            case Types.VARCHAR:
            case Types.CHAR:
                return "string()";
            case Types.SMALLINT:
            case Types.NUMERIC:
            case Types.DECIMAL:
                return "numeric()";
            case Types.INTEGER:
                return "integer()";
            case Types.DATE:
            case Types.TIME:
                return "date()";
            case Types.TIMESTAMP:
                return "timestamp()";
            case Types.BIT:
                return "bool()";
            default:
                return String.format("field(%s.class)", "Object");
        }
    }
}
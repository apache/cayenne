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
package org.apache.cayenne.exp.parser;

import org.apache.cayenne.util.Util;

class ParserUtil {

    static Object makeEnum(String enumPath) throws ParseException {

        if (enumPath == null) {
            throw new ParseException("Null 'enumPath'");
        }

        int dot = enumPath.lastIndexOf('.');
        if (dot <= 0 || dot == enumPath.length() - 1) {
            throw new ParseException("Invalid enum path: " + enumPath);
        }

        String className = enumPath.substring(0, dot);
        String enumName = enumPath.substring(dot + 1);

        Class enumClass;
        try {
            enumClass = Util.getJavaClass(className);
        }
        catch (ClassNotFoundException e) {
            throw new ParseException("Enum class not found: " + className);
        }

        if (!enumClass.isEnum()) {
            throw new ParseException("Specified class is not an enum: " + className);
        }

        try {
            return Enum.valueOf(enumClass, enumName);
        }
        catch (IllegalArgumentException e) {
            throw new ParseException("Invalid enum path: " + enumPath);
        }
    }
}

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
package org.apache.cayenne.dbsync.merge.builders;

import org.apache.cayenne.map.DataMap;

/**
 * Factory for test data see pattern definition:
 * http://martinfowler.com/bliki/ObjectMother.html
 *
 * @since 4.0.
 */
public class ObjectMother {

    public static DataMapBuilder dataMap() {
        return new DataMapBuilder();
    }

    public static DataMapBuilder dataMap(DataMap dataMap) {
        return new DataMapBuilder(dataMap);
    }

    public static DbEntityBuilder dbEntity() {
        return new DbEntityBuilder();
    }

    public static DbEntityBuilder dbEntity(String name) {
        return new DbEntityBuilder().name(name);
    }

    public static ObjEntityBuilder objEntity() {
        return new ObjEntityBuilder();
    }

    public static ObjEntityBuilder objEntity(String packageName, String className, String table) {
        return new ObjEntityBuilder()
                .name(className)
                .clazz(packageName + "." + className)
                .dbEntity(table);
    }

    public static ObjAttributeBuilder objAttr(String name) {
        return new ObjAttributeBuilder().name(name);
    }

    public static DbAttributeBuilder dbAttr(String name) {
        return dbAttr().name(name);
    }

    public static DbAttributeBuilder dbAttr() {
        return new DbAttributeBuilder();
    }

    public static ProcedureBuilder procedure(String name) {
        return procedure().name(name);
    }

    public static ProcedureBuilder procedure() {
        return new ProcedureBuilder();
    }

}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.modeler.dialog.db.model;

import java.util.LinkedList;
import java.util.List;

/**
 * @since 4.0
 */
public abstract class DBElement {
    protected String name;
    protected List<DBElement> dbElements;

    public DBElement(String name) {
        this.name = name;
        dbElements = new LinkedList<>();
    }

    public List<DBElement> getDbElements() {
        return dbElements;
    }

    public abstract void addElement(DBElement dbElement);

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

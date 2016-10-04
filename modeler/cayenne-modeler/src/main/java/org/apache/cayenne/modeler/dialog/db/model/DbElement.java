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
public abstract class DbElement {

    protected String name;
    protected List<DbElement> elements;

    public DbElement(String name) {
        this.name = name;
        this.elements = new LinkedList<>();
    }

    public List<DbElement> getElements() {
        return elements;
    }

    public DbElement getExistingElement(String name) {
        for (DbElement dbElement : elements) {
            if (dbElement.name.equals(name)) {
                return dbElement;
            }
        }
        return null;
    }

    public void addElement(DbElement dbElement) {
        elements.add(dbElement);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

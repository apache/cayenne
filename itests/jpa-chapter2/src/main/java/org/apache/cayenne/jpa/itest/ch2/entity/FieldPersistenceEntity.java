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
package org.apache.cayenne.jpa.itest.ch2.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class FieldPersistenceEntity {
    
    public static final String INITIAL_VALUE = "Init Value";

    @Id
    protected int id;

    protected String property1 = INITIAL_VALUE;

    public int getProperty1() {
        throw new RuntimeException(
                "getter is not supposed to be called in case of field based persistence");
    }

    public void setProperty1(int value) {
        throw new RuntimeException(
                "setter is not supposed to be called in case of field based persistence");
    }
    
    public int idField() {
        return id;
    }
}

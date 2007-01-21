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
public class PropertyPersistenceEntity {

    protected int idx;

    protected String property1x;
    protected boolean property2x;

    @Id
    public int getId() {
        return idx;
    }

    public void setId(int id) {
        this.idx = id;
    }

    public String getProperty1() {
        return property1x;
    }

    public void setProperty1(String property1) {
        this.property1x = property1;
    }

    public boolean isProperty2() {
        return property2x;
    }

    public void setProperty2(boolean property2x) {
        this.property2x = property2x;
    }
}

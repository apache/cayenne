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
package org.apache.cayenne.jpa.itest.ch9.entity;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

@Entity
public class BasicEntity {

    @Id
    protected int id;

    @Basic
    protected String basicDefault;

    @Basic
    protected int basicDefaultInt;

    @Basic(fetch = FetchType.EAGER)
    protected String basicEager;

    @Basic(fetch = FetchType.LAZY)
    protected String basicLazy;

    public String getBasicDefaultX() {
        return basicDefault;
    }

    public void setBasicDefaultX(String basicDefault) {
        this.basicDefault = basicDefault;
    }
    
    public int getBasicDefaultIntX() {
        return basicDefaultInt;
    }

    public void setBasicDefaultIntX(int basicDefault) {
        this.basicDefaultInt = basicDefault;
    }

    public String getBasicEagerX() {
        return basicEager;
    }

    public void setBasicEagerX(String basicEager) {
        this.basicEager = basicEager;
    }

    public String getBasicLazy() {
        return basicLazy;
    }

    public void setBasicLazyX(String basicLazy) {
        this.basicLazy = basicLazy;
    }

    public int getIdX() {
        return id;
    }

    public void setIdX(int id) {
        this.id = id;
    }
}

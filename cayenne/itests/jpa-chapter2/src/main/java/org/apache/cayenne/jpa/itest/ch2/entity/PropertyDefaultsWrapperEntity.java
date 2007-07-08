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
public class PropertyDefaultsWrapperEntity {

    @Id
    protected int id;

    protected Boolean booleanWrapper;
    protected Byte byteWrapper;
    protected Short shortWrapper;
    protected Integer intWrapper;
    protected Character charWrapper;
    protected Long longWrapper;
    protected Float floatWrapper;
    protected Double doubleWrapper;

    public void setBooleanWrapper(Boolean booleanWrapper) {
        this.booleanWrapper = booleanWrapper;
    }

    public void setByteWrapper(Byte byteWrapper) {
        this.byteWrapper = byteWrapper;
    }

    public void setCharWrapper(Character charWrapper) {
        this.charWrapper = charWrapper;
    }

    public void setDoubleWrapper(Double doubleWrapper) {
        this.doubleWrapper = doubleWrapper;
    }

    public void setFloatWrapper(Float floatWrapper) {
        this.floatWrapper = floatWrapper;
    }

    public void setIntWrapper(Integer intWrapper) {
        this.intWrapper = intWrapper;
    }

    public void setLongWrapper(Long longWrapper) {
        this.longWrapper = longWrapper;
    }

    public void setShortWrapper(Short shortWrapper) {
        this.shortWrapper = shortWrapper;
    }
}

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


package org.apache.cayenne.jpa.map;

import javax.persistence.NamedNativeQuery;

public class JpaNamedNativeQuery extends JpaNamedQuery {

    protected String resultClassName;
    protected String resultSetMapping;

    public JpaNamedNativeQuery() {
        
    }
    
    public JpaNamedNativeQuery(NamedNativeQuery annotation) {
        name = annotation.name();
        query = annotation.query();

        getHints();
        for (int i = 0; i < annotation.hints().length; i++) {
            hints.add(new JpaQueryHint(annotation.hints()[i]));
        }
        
        resultClassName = annotation.resultClass().getName();
        resultSetMapping = annotation.resultSetMapping();
    }
    
    public String getResultSetMapping() {
        return resultSetMapping;
    }

    public void setResultSetMapping(String resultSetMapping) {
        this.resultSetMapping = resultSetMapping;
    }

    public String getResultClassName() {
        return resultClassName;
    }

    public void setResultClassName(String resultClassName) {
        this.resultClassName = resultClassName;
    }
}

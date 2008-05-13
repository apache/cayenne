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


package org.apache.cayenne.jpa.conf;

import java.util.Collection;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class MockAnnotatedBean3 {
    
    @Id
    protected int pk;

    @Basic
    protected String attribute1;

    // no annotation here should result in a conflict.
    protected MockAnnotatedBean1 attribute2;

    @ManyToOne
    protected MockAnnotatedBean1 toBean2;

    @OneToMany
    protected Collection<MockAnnotatedBean1> toBean2s1;

    @OneToMany
    // no collection type - must result in a failure
    protected Collection<?> toBean2s2;
    
    // date w/o Temporal annotation must resolve to TIMESTAMP
    protected Date date;
}

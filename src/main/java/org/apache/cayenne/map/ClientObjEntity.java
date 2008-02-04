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

package org.apache.cayenne.map;

import java.util.Collection;
import java.util.Collections;

/**
 * A client version of ObjEntity that delegates some of its method calls to
 * its corresponding server entity.
 *
 * @since 3.0
 * @author Kevin Menard
 */
public class ClientObjEntity extends ObjEntity {

    private Collection<String> primaryKeyNames;

    public ClientObjEntity() {
        super();
    }

    public ClientObjEntity(final String name) {
        super(name);
    }

    @Override
    public Collection<String> getPrimaryKeyNames() {
        return Collections.unmodifiableCollection(primaryKeyNames);
    }

    public void setPrimaryKeyNames(final Collection<String> primaryKeyNames) {
        this.primaryKeyNames = primaryKeyNames;
    }
}

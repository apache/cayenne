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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * A client version of ObjEntity that overrides server entity algorithms for accessing
 * some pieces of information
 * 
 * @since 3.0
 */
class ClientObjEntity extends ObjEntity {

    private Collection<ObjAttribute> primaryKeys;

    ClientObjEntity(String name) {
        super(name);
        this.primaryKeys = Collections.emptyList();
    }

    @Override
    public Collection<String> getPrimaryKeyNames() {
        Collection<String> names = new ArrayList<String>(primaryKeys.size());
        for (ObjAttribute attribute : primaryKeys) {
            names.add(attribute.getDbAttributePath());
        }
        return Collections.unmodifiableCollection(names);
    }

    @Override
    public Collection<ObjAttribute> getPrimaryKeys() {
        return Collections.unmodifiableCollection(primaryKeys);
    }

    void setPrimaryKeys(Collection<ObjAttribute> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }
}

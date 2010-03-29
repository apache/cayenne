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

package org.apache.cayenne.gen;

import java.util.Collection;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.NameConverter;
import org.apache.cayenne.util.Util;

public class ClientDataMapArtifact extends DataMapArtifact {

    public ClientDataMapArtifact(DataMap dataMap, Collection<Query> queries) {
        super(dataMap, queries);

    }

    @Override
    public String getQualifiedBaseClassName() {

        return dataMap.getDefaultClientSuperclass();
    }

    @Override
    public String getQualifiedClassName() {
        String clientPrefix = "";

        if (Util.nullSafeEquals(dataMap.getDefaultClientPackage(), dataMap
                .getDefaultPackage())) {
            clientPrefix = "Client_";
        }

        String pkg = dataMap.getDefaultClientPackage();
        if (pkg == null) {
            pkg = "";
        }
        else {
            pkg = pkg + '.';
        }

        return pkg
                + NameConverter.underscoredToJava(clientPrefix
                        + NameConverter.specialCharsToJava(dataMap.getName()), true);
    }
}
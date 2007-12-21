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

package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.Procedure;

class ProcedureQueryMetadata extends BaseQueryMetadata {

    transient Procedure procedure;

    public Procedure getProcedure() {
        return procedure;
    }

    void copyFromInfo(QueryMetadata info) {
        procedure = null;
        super.copyFromInfo(info);
    }

    void resolve(
            Object root,
            Object resultRoot,
            EntityResolver resolver,
            ProcedureQuery query) {
        if (super.resolve(resultRoot, resolver, null)) {
            procedure = null;

            if (root != null) {
                if (root instanceof String) {
                    this.procedure = resolver.lookupProcedure((String) root);
                }
                else if (root instanceof Procedure) {
                    this.procedure = (Procedure) root;
                }

                // theoretically procedure can be in one DataMap, while the Java Class
                // - in another.
                if (this.procedure != null && this.dataMap == null) {
                    this.dataMap = procedure.getDataMap();
                }
            }

            // generate unique cache key...
            if (QueryMetadata.NO_CACHE.equals(getCachePolicy())) {

            }
            else if (query.getName() != null) {
                this.cacheKey = query.getName();
            }
            else {

                // create a unique key based on procedure and parameters

                StringBuilder key = new StringBuilder("proc:");
                if (procedure != null) {
                    key.append(procedure.getName());
                }

                Map<String, ?> parameters = query.getParameters();
                if (!parameters.isEmpty()) {

                    List<String> keys = new ArrayList<String>(parameters.keySet());
                    Collections.sort(keys);

                    for(String parameterKey : keys) {
                        key.append('/').append(parameterKey).append('=').append(
                                parameters.get(parameterKey));
                    }
                }

                this.cacheKey = key.toString();
            }

        }
    }
}

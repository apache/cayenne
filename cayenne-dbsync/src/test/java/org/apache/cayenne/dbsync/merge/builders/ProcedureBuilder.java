/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dbsync.merge.builders;

import java.util.Arrays;

import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.util.Util;

public class ProcedureBuilder extends DefaultBuilder<Procedure> {

    public ProcedureBuilder() {
        super(new Procedure());
    }

    public ProcedureBuilder name() {
        return name(getRandomJavaName());
    }

    public ProcedureBuilder name(String name) {
        obj.setName(name);
        return this;
    }

    public ProcedureBuilder callParameters(ProcedureParameter... procedureParameters) {
        obj.setCallParameters(Arrays.asList(procedureParameters));

        return this;
    }

    public Procedure build() {
        if(obj.getName() == null) {
            obj.setName(Util.capitalized(getRandomJavaName()));
        }

        return obj;
    }
}

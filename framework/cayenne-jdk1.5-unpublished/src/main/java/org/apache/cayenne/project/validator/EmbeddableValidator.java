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
package org.apache.cayenne.project.validator;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;

public class EmbeddableValidator extends TreeNodeValidator {

    public EmbeddableValidator() {
        super();
    }

    @Override
    public void validateObject(ProjectPath path, Validator validator) {
        Embeddable emb = (Embeddable) path.getObject();
        validateName(emb, path,validator);
    }

    protected void validateName(Embeddable emb, ProjectPath path, Validator validator) {
        String name = emb.getClassName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed Embeddable.", path);
            return;
        }

        DataMap map = (DataMap) path.getObjectParent();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (Embeddable otherEmb : map.getEmbeddables()) {
            if (otherEmb == emb) {
                continue;
            }

            if (name.equals(otherEmb.getClassName())) {
                validator.registerError("Duplicate Embeddable name: " + name + ".", path);
                break;
            }
        }

        // check for dupliucates in other DataMaps
        DataDomain domain = path.firstInstanceOf(DataDomain.class);
        if (domain != null) {
            for (DataMap nextMap : domain.getDataMaps()) {
                if (nextMap == map) {
                    continue;
                }

                Embeddable conflictingEmbeddable = nextMap.getEmbeddable(name);
                if (conflictingEmbeddable != null) {

                        validator.registerWarning(
                                "Duplicate Embeddable name in another DataMap: "
                                        + name
                                        + ".",
                                path);
                        break;
                }
            }
        }
    }

}

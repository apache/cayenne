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

import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;


public class EmbeddableAttributeValidator extends TreeNodeValidator {
    
    public EmbeddableAttributeValidator() {
        super();
    }

    @Override
    public void validateObject(ProjectPath path, Validator validator) {
        EmbeddableAttribute emAttribute = (EmbeddableAttribute) path.getObject();
        
        // Must have name
        if (Util.isEmptyString(emAttribute.getName())) {
            validator.registerError("Unnamed ObjAttribute.", path);
        }
        
        // skip validation of inherited attributes
        if (path.getObjectParent() != null
                && path.getObjectParent() != emAttribute.getEmbeddable()) {
            return;
        }
        
        // all attributes must have type
        if (Util.isEmptyString(emAttribute.getType())) {
            validator.registerWarning("EmbeddableAttribute has no type.", path);
        }
    }
}

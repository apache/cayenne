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
package org.apache.cayenne.project2.validate;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.project.validator.MappingNamesHelper;
import org.apache.cayenne.util.Util;

class ObjAttributeValidator {

    void validate(Object object, ConfigurationValidationVisitor validator) {
        ObjAttribute attribute = (ObjAttribute) object;

        // Must have name
        if (Util.isEmptyString(attribute.getName())) {
            validator.registerError("Unnamed ObjAttribute.", object);
        }
        else {
            MappingNamesHelper helper = MappingNamesHelper.getInstance();
            String invalidChars = helper.invalidCharsInObjPathComponent(attribute
                    .getName());

            if (invalidChars != null) {
                validator.registerWarning(
                        "ObjAttribute name contains invalid characters: " + invalidChars,
                        object);
            }
            else if (helper.invalidDataObjectProperty(attribute.getName())) {
                validator.registerWarning("ObjAttribute name is invalid: "
                        + attribute.getName(), object);
            }
        }

        // all attributes must have type
        if (Util.isEmptyString(attribute.getType())) {
            validator.registerWarning("ObjAttribute has no type.", object);
        }

        if (attribute.getEntity() instanceof ObjEntity
                && ((ObjEntity) attribute.getEntity()).isAbstract()) {
            // nothing, abstract entity does not have to define a dbAttribute
        }
        else if (attribute instanceof EmbeddedAttribute) {
            Map<String, String> attrOverrides = ((EmbeddedAttribute) attribute)
                    .getAttributeOverrides();
            Embeddable emb = ((EmbeddedAttribute) attribute).getEmbeddable();
            if (emb == null && ((EmbeddedAttribute) attribute).getType() != null) {
                validator.registerWarning(
                        "EmbeddedAttribute has incorrect Embeddable.",
                        object);
            }
            else if (emb == null && ((EmbeddedAttribute) attribute).getType() == null) {
                validator.registerWarning("EmbeddedAttribute has no Embeddable.", object);
            }

            if (emb != null) {
                Collection<EmbeddableAttribute> embAttributes = emb.getAttributes();

                Iterator<EmbeddableAttribute> it = embAttributes.iterator();
                while (it.hasNext()) {
                    EmbeddableAttribute embAttr = (EmbeddableAttribute) it.next();
                    String dbAttributeName;
                    if (attrOverrides.size() > 0
                            && attrOverrides.containsKey(embAttr.getName())) {
                        dbAttributeName = attrOverrides.get(embAttr.getName());
                    }
                    else {
                        dbAttributeName = embAttr.getDbAttributeName();
                    }

                    if (dbAttributeName == "" || dbAttributeName == null) {
                        validator.registerWarning(
                                "EmbeddedAttribute has no DbAttribute mapping.",
                                object);
                    }
                    else if (((ObjEntity) attribute.getEntity())
                            .getDbEntity()
                            .getAttribute(dbAttributeName) == null) {
                        validator.registerWarning(
                                "EmbeddedAttribute has incorrect DbAttribute mapping.",
                                object);
                    }
                }
            }

        }
        else if (attribute.getDbAttribute() == null) {
            validator.registerWarning("ObjAttribute has no DbAttribute mapping.", object);
        }
        // can't support generated meaningful attributes for now; besides they don't make
        // sense.
        else if (attribute.getDbAttribute().isPrimaryKey()
                && attribute.getDbAttribute().isGenerated()) {
            validator.registerWarning("ObjAttribute is mapped to a generated PK: "
                    + attribute.getDbAttributeName(), object);
        }
    }
}

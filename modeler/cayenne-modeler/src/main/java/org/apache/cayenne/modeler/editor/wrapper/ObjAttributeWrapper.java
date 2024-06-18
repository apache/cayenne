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
package org.apache.cayenne.modeler.editor.wrapper;

import java.util.List;

import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.editor.validation.ObjAttributeWrapperValidator;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

/**
 *  A wrapper for a ObjAttribute instance. Allows to add failures, that connected
 *  with attribute.
 */
public class ObjAttributeWrapper implements Wrapper<ObjAttribute> {

    private final ObjAttribute objAttribute;
    private final ValidationResult validationResult;

    private final ObjAttributeWrapperValidator validator = new ObjAttributeWrapperValidator();

    // TODO: for now name is only wrapped attribute we validating but this
    //       can be extended to other ObjAttribute fields as well
    private String name;

    public ObjAttributeWrapper(ObjAttribute objAttribute) {
        if (objAttribute == null) {
            throw new IllegalArgumentException("Attribute cannot be null.");
        }
        this.objAttribute = objAttribute;
        validationResult = new ValidationResult();
    }

    @Override
    public ObjAttribute getValue() {
        return objAttribute;
    }

    @Override
    public boolean isValid() {
    	return !validationResult.hasFailures();
    }

    /**
     * @param column Index of column.
     * @return String failure description, null if there is no such failure.
     */
    public String getFailureDescription(int column) {
        String result = null;
        if (validationResult.hasFailures(column)) {
            List<ValidationFailure> failures= validationResult.getFailures(column);
            
            // there is only one failure for each column
            result = failures.get(0).getDescription();
        }
        return result;
    }

    @Override
    public void commitEdits() {
        if (isValid()) {
            ProjectUtil.setAttributeName(this.getValue(), name);
        }
    }

    /**
     * Correct ObjAttributeWrapper failures.
     */
    @Override
    public void resetEdits() {
        this.name = null;
        validationResult.clear();
    }

    public ObjEntity getEntity() {
        return objAttribute.getEntity();
    }

    public void setEntity(ObjEntity entity) {
        objAttribute.setEntity(entity);
    }

    public String getName() {
        return name == null ? objAttribute.getName() : name;
    }

    public void setName(String name) {
        this.name = name;
        validator.validate(this, validationResult);
    }

    public Object getParent() {
        return objAttribute.getEntity();
    }

    public void setParent(Object parent) {
        objAttribute.setParent(parent);
    }

    public Class<?> getObjAttributeClass() {
        return objAttribute.getJavaClass();
    }

    public String getType() {
        return objAttribute.getType();
    }

    public void setType(String type) {
        objAttribute.setType(type);
    }

    public boolean isUsedForLocking() {
        return objAttribute.isUsedForLocking();
    }

    public void setUsedForLocking(boolean usedForLocking) {
        objAttribute.setUsedForLocking(usedForLocking);
    }

    public boolean isLazy() {
        return objAttribute.isLazy();
    }

    public void setLazy(boolean lazy) {
        objAttribute.setLazy(lazy);
    }

    public DbAttribute getDbAttribute() {
        try {
            return objAttribute.getDbAttribute();
        } catch (ExpressionException e) {
            return null;
        }
    }

    public boolean isInherited() {
        return objAttribute.isInherited();
    }

    public String getDbAttributeName() {
        return objAttribute.getDbAttributeName();
    }

    public void setDbAttributePath(String dbAttributePath) {
        objAttribute.setDbAttributePath(dbAttributePath);
    }

    public String getDbAttributePath() {
        return objAttribute.getDbAttributePath().value();
    }

}

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
package org.apache.cayenne.testdo.testmap;

import org.apache.cayenne.testdo.testmap.annotations.Tag1;
import org.apache.cayenne.testdo.testmap.auto._Artist;
import org.apache.cayenne.unit.util.ValidationDelegate;
import org.apache.cayenne.validation.ValidationResult;

@Tag1
public class Artist extends _Artist {
    
    private static final long serialVersionUID = 1L; 

    protected transient ValidationDelegate validationDelegate;
    protected boolean validateForSaveCalled;
    protected boolean postAdded;
    protected boolean prePersisted;
    protected boolean preRemoved;
    protected boolean preUpdated;
    protected boolean postUpdated;
    protected boolean postRemoved;
    protected boolean postPersisted;
    protected int postLoaded;
    protected transient int propertyWrittenDirectly;

    protected String someOtherProperty;
    protected Object someOtherObjectProperty;

    public boolean isValidateForSaveCalled() {
        return validateForSaveCalled;
    }

    public void resetValidationFlags() {
        validateForSaveCalled = false;
    }

    public void setValidationDelegate(ValidationDelegate validationDelegate) {
        this.validationDelegate = validationDelegate;
    }

    public void resetCallbackFlags() {
        postAdded = false;
        prePersisted = false;
        preRemoved = false;
        preUpdated = false;
        postUpdated = false;
        postRemoved = false;
        postPersisted = false;
        postLoaded = 0;
    }

    @Override
    public void validateForSave(ValidationResult validationResult) {
        validateForSaveCalled = true;
        if (validationDelegate != null) {
            validationDelegate.validateForSave(this, validationResult);
        }
        super.validateForSave(validationResult);
    }

    public void postAddCallback() {
        postAdded = true;
    }
    
    public void prePersistCallback() {
        prePersisted = true;
    }

    public void preRemoveCallback() {
        preRemoved = true;
    }

    public void preUpdateCallback() {
        preUpdated = true;
    }

    public void postUpdateCallback() {
        postUpdated = true;
    }

    public void postPersistCallback() {
        postPersisted = true;
    }

    public void postRemoveCallback() {
        postRemoved = true;
    }

    public void postLoadCallback() {
        postLoaded++;
    }

    public boolean isPostAdded() {
        return postAdded;
    }
    
    public boolean isPrePersisted() {
        return prePersisted;
    }

    public boolean isPreRemoved() {
        return preRemoved;
    }

    public boolean isPreUpdated() {
        return preUpdated;
    }

    public boolean isPostUpdated() {
        return postUpdated;
    }

    public boolean isPostRemoved() {
        return postRemoved;
    }

    public boolean isPostPersisted() {
        return postPersisted;
    }

    public int getPostLoaded() {
        return postLoaded;
    }

    public String getSomeOtherProperty() {
        return someOtherProperty;
    }

    public void setSomeOtherProperty(String string) {
        someOtherProperty = string;
    }

    public Object getSomeOtherObjectProperty() {
        return someOtherObjectProperty;
    }

    public void setSomeOtherObjectProperty(Object object) {
        someOtherObjectProperty = object;
    }
    
    @Override
    public void writePropertyDirectly(String propName, Object val) {
        propertyWrittenDirectly++;
        super.writePropertyDirectly(propName, val);
    }
    
    public int getPropertyWrittenDirectly() {
        return propertyWrittenDirectly;
    }

}

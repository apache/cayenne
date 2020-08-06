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

import org.apache.cayenne.validation.ValidationResult;

public class Painting extends org.apache.cayenne.testdo.testmap.auto._Painting {
    protected boolean validateForSaveCalled;

    protected boolean postAdded;
    protected boolean preRemoved;
    protected boolean preUpdated;

    public void resetCallbackFlags() {
        postAdded = false;
        preRemoved = false;
        preUpdated = false;
    }

    public void postAddCallback() {
        postAdded = true;
    }

    public void preRemoveCallback() {
        preRemoved = true;
    }

    public void preUpdateCallback() {
        preUpdated = true;
    }

    public boolean isPostAdded() {
        return postAdded;
    }

    public boolean isPreRemoved() {
        return preRemoved;
    }

    public boolean isPreUpdated() {
        return preUpdated;
    }

    public boolean isValidateForSaveCalled() {
        return validateForSaveCalled;
    }

    public void resetValidationFlags() {
        validateForSaveCalled = false;
    }

    @Override
    public void validateForSave(ValidationResult validationResult) {
        validateForSaveCalled = true;
        super.validateForSave(validationResult);
    }
}

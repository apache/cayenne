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
package org.apache.cayenne.testdo.mt;

import java.util.List;

import org.apache.cayenne.Validating;
import org.apache.cayenne.testdo.mt.auto._ClientMtTable1;
import org.apache.cayenne.validation.SimpleValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

public class ClientMtTable1 extends _ClientMtTable1 implements Validating {
    
    protected boolean validatedForDelete;
    protected boolean validatedForInsert;
    protected boolean validatedForUpdate;
    protected boolean blow;

    // provide direct access to persistent properties for testing..

    public String getGlobalAttribute1Direct() {
        return globalAttribute1;
    }

    public String getServerAttribute1Direct() {
        return serverAttribute1;
    }

    public List getTable2ArrayDirect() {
        return table2Array;
    }
    
    public void resetValidation(boolean blow) {
        this.blow = blow;
        this.validatedForDelete = false;
        this.validatedForInsert = false;
        this.validatedForUpdate = false;
    }

    public void validateForDelete(ValidationResult validationResult) {
        validatedForDelete = true;
        
        if(blow) {
            validationResult.addFailure(new SimpleValidationFailure(this, "test error"));
        }
    }

    public void validateForInsert(ValidationResult validationResult) {
        validatedForInsert = true;
        
        if(blow) {
            validationResult.addFailure(new SimpleValidationFailure(this, "test error"));
        }
    }

    public void validateForUpdate(ValidationResult validationResult) {
        validatedForUpdate = true;
        
        if(blow) {
            validationResult.addFailure(new SimpleValidationFailure(this, "test error"));
        }
    }

    
    public boolean isValidatedForDelete() {
        return validatedForDelete;
    }

    
    public boolean isValidatedForInsert() {
        return validatedForInsert;
    }

    
    public boolean isValidatedForUpdate() {
        return validatedForUpdate;
    }
}

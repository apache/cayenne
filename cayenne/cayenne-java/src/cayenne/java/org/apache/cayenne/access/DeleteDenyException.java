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


package org.apache.cayenne.access;

import org.apache.cayenne.Persistent;

/**
 * An exception thrown during an attempt to delete an object that has a relationship to a
 * non-null related object, that has a DENY delete rule.
 * 
 * @author Craig Miskell
 * @deprecated since 1.2 moved to org.apache.cayenne package.
 */
public class DeleteDenyException extends org.apache.cayenne.DeleteDenyException {

    public DeleteDenyException() {
        super();
    }

    public DeleteDenyException(Persistent object, String relationship, String reason) {
        super(object, relationship, reason);
    }

    public DeleteDenyException(String message) {
        super(message);
    }
}

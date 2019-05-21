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

package org.apache.cayenne;

/**
 * An exception thrown during an attempt to delete an object that has a relationship to a
 * non-null related object, that has a DENY delete rule.
 * 
 * @since 1.2
 */
public class DeleteDenyException extends CayenneRuntimeException {

    protected Persistent object;
    protected String relationship;

    public DeleteDenyException() {
    }

    public DeleteDenyException(String message) {
        super(message);
    }

    /**
     * @since 1.2
     */
    public DeleteDenyException(Persistent object, String relationship, String reason) {
        super(reason);

        this.object = object;
        this.relationship = relationship;
    }

    /**
     * @since 1.2
     */
    public Persistent getObject() {
        return object;
    }

    /**
     * @since 1.2
     */
    public String getRelationship() {
        return relationship;
    }

    @Override
    public String getMessage() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Can't delete object");

        if (object != null && object.getObjectId() != null) {
            buffer.append(" with OID ").append(object.getObjectId());
        }

        if (relationship != null) {
            buffer.append(". Reason: relationship '").append(relationship).append(
                    "' has 'deny' delete rule");
        }

        String message = super.getUnlabeledMessage();
        if (message != null) {
            buffer.append(". Details: ").append(message);
        }

        return buffer.toString();
    }
}

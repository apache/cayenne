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

package org.apache.cayenne;

/**
 * A runtime exception thrown when <code>DataObject.resolveFault()</code> finds that no
 * matching row exists in the database for an <code>ObjectId</code>.
 * 
 */
public class FaultFailureException extends CayenneRuntimeException {

    /**
     * Creates new FaultFailureException without detail message.
     */
    public FaultFailureException() {
        super();
    }

    /**
     * Constructs an FaultFailureException with the specified detail message.
     * 
     * @param msg the detail message.
     */
    public FaultFailureException(String msg) {
        super(msg);
    }

    /**
     * Constructs an FaultFailureException that wraps a <code>Throwable</code> thrown
     * elsewhere.
     */
    public FaultFailureException(Throwable th) {
        super(th);
    }

    public FaultFailureException(String msg, Throwable th) {
        super(msg, th);
    }
}

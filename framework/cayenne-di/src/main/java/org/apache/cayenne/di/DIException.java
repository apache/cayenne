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
package org.apache.cayenne.di;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * @since 3.1
 */
public class DIException extends CayenneRuntimeException {

    public DIException() {
        super();
    }

    public DIException(String messageFormat, Object... messageArgs) {
        super(messageFormat, messageArgs);
    }

    public DIException(String messageFormat, Throwable cause, Object... messageArgs) {
        super(messageFormat, cause, messageArgs);
    }

    public DIException(Throwable cause) {
        super(cause);
    }

}

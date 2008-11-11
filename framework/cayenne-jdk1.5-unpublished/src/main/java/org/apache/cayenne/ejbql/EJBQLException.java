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
package org.apache.cayenne.ejbql;

import org.apache.cayenne.exp.ExpressionException;

/**
 * An exception thrown on errors during parsing EJBQL statements.
 * 
 * @since 3.0
 */
public class EJBQLException extends ExpressionException {

    public EJBQLException() {

    }

    public EJBQLException(String msg) {
        super(msg);
    }

    public EJBQLException(Throwable th) {
        super(th);
    }

    public EJBQLException(String msg, Throwable th) {
        super(msg, th);
    }

    public EJBQLException(String msg, String expressionString, Throwable th) {
        super(msg, expressionString, th);
    }
}

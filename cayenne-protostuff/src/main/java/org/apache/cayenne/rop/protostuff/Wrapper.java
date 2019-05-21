/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.rop.protostuff;

import java.io.Serializable;

/**
 * As Protostuff has limitation that nested messages should not contain references to the root message, so we provide
 * a simple wrapper for the root message.
 *
 * <a href="http://www.protostuff.io/documentation/object-graphs/">
 *
 * @since 4.0
 */
public class Wrapper implements Serializable {

    public Object data;

    public Wrapper(Object data) {
        this.data = data;
    }

}

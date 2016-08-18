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
package org.apache.cayenne.crypto.transformer.value;

import java.nio.charset.Charset;

/**
 * @since 4.0
 */
public final class Utf8StringConverter implements BytesConverter<String> {

    static final String DEFAULT_CHARSET = "UTF-8";

    public static final BytesConverter<String> INSTANCE = new Utf8StringConverter();

    private Charset utf8;

    Utf8StringConverter() {
        this.utf8 = Charset.forName(DEFAULT_CHARSET);
    }

    @Override
    public byte[] toBytes(String value) {
        return value.getBytes(utf8);
    }
    
    @Override
    public String fromBytes(byte[] bytes) {
        return new String(bytes, utf8);
    }

}

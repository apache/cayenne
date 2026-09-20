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
package org.apache.cayenne.docs;

import org.apache.cayenne.crypto.CryptoModule;
import org.apache.cayenne.docs.crypto.MyClass;
import org.apache.cayenne.docs.crypto.MyClassBytesConverter;
import org.apache.cayenne.docs.crypto.MyColumnMapper;
import org.apache.cayenne.runtime.CayenneRuntime;

/**
 * Examples that are only compiled, but not run, as there is no keystore or encrypted columns in the docs test project.
 */
public class CryptoExamples {

    public void columnMapper() {
        // tag::columnMapper[]
        CayenneRuntime runtime = CayenneRuntime.of()
                .addModule(binder -> CryptoModule.extend(binder)
                        .columnMapper(MyColumnMapper.class))
                .build();
        // end::columnMapper[]
    }

    public void keyStore() {
        // tag::keyStore[]
        CayenneRuntime runtime = CayenneRuntime.of()
                .addModule(binder -> CryptoModule.extend(binder)
                        .keyStore(
                                this.getClass().getResource("keystore.jcek"),
                                "my-password".toCharArray(),
                                "my-key-alias"))
                .build();
        // end::keyStore[]
    }

    public void compressAndHMAC() {
        // tag::compressAndHMAC[]
        CayenneRuntime runtime = CayenneRuntime.of()
                .addModule(binder -> CryptoModule.extend(binder)
                        .compress()
                        .useHMAC())
                .build();
        // end::compressAndHMAC[]
    }

    public void bytesConverter() {
        // tag::bytesConverter[]
        CayenneRuntime runtime = CayenneRuntime.of()
                .addModule(binder -> CryptoModule.extend(binder)
                        .objectToBytesConverter(MyClass.class, new MyClassBytesConverter()))
                .build();
        // end::bytesConverter[]
    }
}

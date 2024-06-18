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
package org.apache.cayenne.crypto;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.crypto.db.Table1;
import org.apache.cayenne.crypto.db.Table4;
import org.apache.cayenne.crypto.key.KeySource;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.security.Key;

import static org.junit.Assert.*;

public class Runtime_LazyInit_IT extends Runtime_AES128_Base {


    protected static boolean UNLOCKED;

    @Before
    public void before() throws Exception {
        setUp(false, false);
        UNLOCKED = false;
    }

    @Override
    protected CayenneRuntime createRuntime(final Module crypto) {
        Module cryptoWrapper = binder -> {
            crypto.configure(binder);
            binder.decorate(KeySource.class).after(LockingKeySourceDecorator.class);
        };

        return super.createRuntime(cryptoWrapper);
    }

    @Test
    public void testCryptoLocked() {

        assertFalse(UNLOCKED);

        Table4 t4 = runtime.newContext().newObject(Table4.class);
        t4.setPlainInt(56);
        t4.setPlainString("XX");
        t4.getObjectContext().commitChanges();

        assertEquals(t4.getObjectId(), ObjectSelect.query(Table4.class).selectOne(runtime.newContext()).getObjectId());
    }

    @Test
    public void testCryptoLocked_Unlocked() {


        assertFalse(UNLOCKED);

        try {
            Table1 t1 = runtime.newContext().newObject(Table1.class);
            t1.setPlainInt(56);
            t1.setCryptoInt(77);
            t1.setPlainString("XX");
            t1.setCryptoString("YY");
            t1.getObjectContext().commitChanges();

            fail("Must have thrown on crypto access");
        } catch (CayenneRuntimeException e) {
            // expected
        }

        UNLOCKED = true;

        Table1 t1 = runtime.newContext().newObject(Table1.class);
        t1.setPlainInt(56);
        t1.setCryptoInt(77);
        t1.setPlainString("XX");
        t1.setCryptoString("YY");
        t1.getObjectContext().commitChanges();

        assertEquals(t1.getObjectId(), ObjectSelect.query(Table1.class).selectOne(runtime.newContext()).getObjectId());

    }


    public static class LockingKeySourceDecorator implements KeySource {

        private KeySource keySource;

        public LockingKeySourceDecorator(@Inject KeySource keySource) {
            this.keySource = keySource;
        }

        @Override
        public Key getKey(String alias) {
            return ensureKeySource().getKey(alias);
        }

        @Override
        public String getDefaultKeyAlias() {
            return ensureKeySource().getDefaultKeyAlias();
        }

        private KeySource ensureKeySource() {

            if (!UNLOCKED) {
                throw new IllegalStateException("Crypto is locked");
            }

            return keySource;
        }
    }

}

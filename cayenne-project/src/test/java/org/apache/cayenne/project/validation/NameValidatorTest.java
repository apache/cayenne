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
package org.apache.cayenne.project.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NameValidatorTest {

    @Test
    public void invalidJavaClassComponents_Valid() {
        assertFalse(NameValidator.invalidJavaClassComponents("Artist"));
        assertFalse(NameValidator.invalidJavaClassComponents("org.example.model.Artist"));
        assertFalse(NameValidator.invalidJavaClassComponents("org.example.model"));
    }

    @Test
    public void invalidJavaClassComponents_ReservedWords() {
        assertTrue(NameValidator.invalidJavaClassComponents("class"));
        assertTrue(NameValidator.invalidJavaClassComponents("com.default.Foo"));
        assertTrue(NameValidator.invalidJavaClassComponents("org.example.switch"));
    }

    @Test
    public void invalidJavaClassComponents_EmptyComponents() {
        assertTrue(NameValidator.invalidJavaClassComponents(""));
        assertTrue(NameValidator.invalidJavaClassComponents("com..Foo"));
        assertTrue(NameValidator.invalidJavaClassComponents(".Foo"));
        assertTrue(NameValidator.invalidJavaClassComponents("Foo."));
        assertTrue(NameValidator.invalidJavaClassComponents(null));
    }

    @Test
    public void invalidCharsInJavaClassName() {
        assertNull(NameValidator.invalidCharsInJavaClassName("org.example.Artist"));
        assertEquals("-", NameValidator.invalidCharsInJavaClassName("org.example.My-Class"));
        assertEquals(" ", NameValidator.invalidCharsInJavaClassName("My Class"));
    }

    @Test
    public void invalidPersistentProperty() {
        assertTrue(NameValidator.invalidPersistentProperty("class"));
        assertTrue(NameValidator.invalidPersistentProperty("objectId"));
        assertTrue(NameValidator.invalidPersistentProperty("objectContext"));
        assertTrue(NameValidator.invalidPersistentProperty("persistenceState"));
        assertTrue(NameValidator.invalidPersistentProperty("snapshotVersion"));
        assertTrue(NameValidator.invalidPersistentProperty(null));

        assertFalse(NameValidator.invalidPersistentProperty("name"));
        assertFalse(NameValidator.invalidPersistentProperty("classRoom"));
    }
}

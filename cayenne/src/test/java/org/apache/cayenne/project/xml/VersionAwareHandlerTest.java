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

package org.apache.cayenne.project.xml;

import org.apache.cayenne.project.upgrade.UpgradeHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VersionAwareHandlerTest {

    VersionAwareHandler handler;

    @BeforeEach
    public void createHandler() {
        handler = new VersionAwareHandler(new LoaderContext(null, null), "test"){
        };
    }

    private Attributes legacyVersion(String version) {
        AttributesImpl attributes = new AttributesImpl();
        if (version != null) {
            attributes.addAttribute("", "project-version", "project-version", "", version);
        }
        return attributes;
    }

    private String namespace(String version) {
        return "http://cayenne.apache.org/schema/" + version + "/test";
    }

    @Test
    public void validateCurrentVersion() {
        handler.validateVersion(namespace(UpgradeHandler.CURRENT_VERSION), legacyVersion(null));
    }

    @Test
    public void validateOlderVersion() {
        UnsupportedVersionException e = assertThrows(UnsupportedVersionException.class,
                () -> handler.validateVersion(namespace("10"), legacyVersion(null)));
        assertEquals("10", e.getVersion());
    }

    @Test
    public void validateOlderVersion_NoNamespace() {
        UnsupportedVersionException e = assertThrows(UnsupportedVersionException.class,
                () -> handler.validateVersion("", legacyVersion("8")));
        assertEquals("8", e.getVersion());
    }

    @Test
    public void validateOlderVersion_UnversionedNamespace() {
        UnsupportedVersionException e = assertThrows(UnsupportedVersionException.class,
                () -> handler.validateVersion("http://cayenne.apache.org/schema/3.0/modelMap", legacyVersion("6")));
        assertEquals("6", e.getVersion());
    }

    @Test
    public void validateNewerVersion() {
        UnsupportedVersionException e = assertThrows(UnsupportedVersionException.class,
                () -> handler.validateVersion(namespace("15"), legacyVersion(null)));
        assertEquals("15", e.getVersion());
    }

    @Test
    public void validateMissingVersion() {
        UnsupportedVersionException e = assertThrows(UnsupportedVersionException.class,
                () -> handler.validateVersion("", legacyVersion(null)));
        assertEquals(UpgradeHandler.UNKNOWN_VERSION, e.getVersion());
    }
}

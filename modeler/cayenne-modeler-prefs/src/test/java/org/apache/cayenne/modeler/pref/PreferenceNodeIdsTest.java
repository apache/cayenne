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

package org.apache.cayenne.modeler.pref;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PreferenceNodeIdsTest {

    @Test
    public void idForPathIsStable() {
        String id1 = PreferenceNodeIds.idForPath("/home/user/project/myapp.xml");
        String id2 = PreferenceNodeIds.idForPath("/home/user/project/myapp.xml");
        assertEquals(id1, id2);
    }

    @Test
    public void idForPathStaysUnderMaxLen() {
        String longPath = "/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/very-long-name.xml";
        String id = PreferenceNodeIds.idForPath(longPath);
        assertTrue(id.length() <= PreferenceNodeIds.MAX_LEN,
                "id length " + id.length() + " exceeds MAX_LEN " + PreferenceNodeIds.MAX_LEN);
    }

    @Test
    public void idForPathStartsWithHash() {
        String id = PreferenceNodeIds.idForPath("/foo/bar.xml");
        assertEquals(PreferenceNodeIds.HASH_LEN, id.indexOf('-'),
                "id should start with exactly HASH_LEN hex chars before the dash");
    }

    @Test
    public void idForPathDifferentPathsProduceDifferentIds() {
        String id1 = PreferenceNodeIds.idForPath("/project/a.xml");
        String id2 = PreferenceNodeIds.idForPath("/project/b.xml");
        assertTrue(!id1.equals(id2), "different paths must produce different ids");
    }

    @Test
    public void idForPathXmlSuffixStripped() {
        String withXml = PreferenceNodeIds.idForPath("/foo/bar.xml");
        String withoutXml = PreferenceNodeIds.idForPath("/foo/bar");
        assertEquals(withXml, withoutXml, ".xml suffix must be stripped before hashing");
    }

    @Test
    public void idForPathNullReturnsHashOnly() {
        String id = PreferenceNodeIds.idForPath(null);
        assertEquals(PreferenceNodeIds.HASH_LEN, id.length());
    }

    @Test
    public void canonicalizeNullReturnsEmpty() {
        assertEquals("", PreferenceNodeIds.canonicalize(null));
    }

    @Test
    public void canonicalizeStripsXmlSuffix() {
        String result = PreferenceNodeIds.canonicalize("/foo/bar.xml");
        assertTrue(result.endsWith("/foo/bar"), "expected .xml stripped, got: " + result);
    }

    @Test
    public void canonicalizeConvertsBackslashes() {
        String result = PreferenceNodeIds.canonicalize("C:\\Users\\test\\project.xml");
        assertTrue(!result.contains("\\"), "backslashes must be converted to forward slashes");
    }

    @Test
    public void basenameExtractsLastSegment() {
        assertEquals("myproject", PreferenceNodeIds.basename("/home/user/myproject"));
    }

    @Test
    public void basenameNoSlashReturnsInput() {
        assertEquals("standalone", PreferenceNodeIds.basename("standalone"));
    }

    @Test
    public void sanitizeAllowsAlphanumAndSafePunctuation() {
        assertEquals("Abc-123_.", PreferenceNodeIds.sanitize("Abc-123_."));
    }

    @Test
    public void sanitizeReplacesSpacesAndSpecialChars() {
        String result = PreferenceNodeIds.sanitize("my project (v2)");
        assertEquals("my_project__v2_", result);
    }

    @Test
    public void sanitizeEmptyInputReturnsEmpty() {
        assertEquals("", PreferenceNodeIds.sanitize(""));
    }

    @Test
    public void sha256HexReturnsSixtyFourChars() {
        assertEquals(64, PreferenceNodeIds.sha256Hex("hello").length());
    }

    @Test
    public void sha256HexIsStable() {
        assertEquals(PreferenceNodeIds.sha256Hex("test"), PreferenceNodeIds.sha256Hex("test"));
    }
}

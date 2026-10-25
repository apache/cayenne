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
package org.apache.cayenne.dba;

import org.apache.cayenne.CayenneRuntimeException;
import org.junit.jupiter.api.Test;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class DbAdapterDetectorTest {

    @Test
    public void detect_LastDetectorWins() {
        DbAdapter a1 = mock(DbAdapter.class);
        DbAdapter a2 = mock(DbAdapter.class);
        DbAdapterDetector d1 = md -> a1;
        DbAdapterDetector d2 = md -> a2;

        // detectors are tried in reverse order, so that custom detectors added last take precedence
        assertSame(a2, DbAdapterDetector.detect(mock(DatabaseMetaData.class), List.of(d1, d2)));
    }

    @Test
    public void detect_SkipsUnmatchedDetectors() {
        DbAdapter a1 = mock(DbAdapter.class);
        DbAdapterDetector d1 = md -> a1;
        DbAdapterDetector d2 = md -> null;

        assertSame(a1, DbAdapterDetector.detect(mock(DatabaseMetaData.class), List.of(d1, d2)));
    }

    @Test
    public void detect_NothingMatches() {
        DbAdapterDetector d1 = md -> null;
        DatabaseMetaData md = mock(DatabaseMetaData.class);

        assertThrows(CayenneRuntimeException.class, () -> DbAdapterDetector.detect(md, List.of(d1)));
        assertThrows(CayenneRuntimeException.class, () -> DbAdapterDetector.detect(md, List.of()));
    }

    @Test
    public void detect_MetadataError() {
        DbAdapterDetector d1 = md -> {
            throw new SQLException("bad metadata");
        };

        assertThrows(CayenneRuntimeException.class,
                () -> DbAdapterDetector.detect(mock(DatabaseMetaData.class), List.of(d1)));
    }
}

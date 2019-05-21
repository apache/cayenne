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

package org.apache.cayenne.dbsync.merge.token.model;

import java.util.List;

import org.apache.cayenne.dbsync.merge.MergeCase;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 4.0
 */
public class SetGeneratedFlagToModelIT extends MergeCase {

    @Test
    public void test() throws Exception {
        if(!accessStackAdapter.supportsGeneratedKeysAdd()) {
            // nothing to do here
            return;
        }

        DbEntity dbEntity = map.getDbEntity("PAINTING");
        assertNotNull(dbEntity);

        DbAttribute attribute = dbEntity.getAttribute("PAINTING_ID");
        assertNotNull(attribute);
        assertFalse(attribute.isGenerated());

        attribute.setGenerated(true);

        List<MergerToken> tokens = createMergeTokens();
        assertEquals(1, tokens.size());
        MergerToken token = tokens.get(0);
        if (token.getDirection().isToDb()) {
            token = token.createReverse(mergerFactory());
        }
        assertTrue(token instanceof SetGeneratedFlagToModel);

        execute(token);

        assertFalse(attribute.isGenerated());

        assertTokensAndExecute(0, 0);
    }

}

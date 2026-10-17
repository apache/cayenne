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

package org.apache.cayenne.access;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.things.Bag;
import org.apache.cayenne.testdo.things.Box;
import org.apache.cayenne.testdo.things.BoxInfo;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Merging a fresh snapshot into a MODIFIED object: a to-one that is not defined by an FK on the object's own row
 * (here the inverse side of a one-to-one, Box.boxInfo, with the FK in BOX_INFO) can't be read from the snapshot. It
 * is left as it is, like a to-many, until a prefetch or an invalidation refreshes it, and a local change to it is
 * never discarded by the merge.
 */
public class ModifiedObjectToOneMergeIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.THINGS_PROJECT);

    private ObjectContext context;
    private ObjectContext peer;

    private Box box;
    private BoxInfo info1;
    private BoxInfo info2;

    @BeforeEach
    public void setUp() {
        context = env.context();
        peer = env.runtime().newContext();

        Bag bag = context.newObject(Bag.class);
        bag.setName("bag");

        box = context.newObject(Box.class);
        box.setBag(bag);
        box.setName("box");

        Box otherBox = context.newObject(Box.class);
        otherBox.setBag(bag);
        otherBox.setName("other box");

        info1 = context.newObject(BoxInfo.class);
        info1.setColor("red");
        info1.setBox(box);

        info2 = context.newObject(BoxInfo.class);
        info2.setColor("blue");
        info2.setBox(otherBox);

        context.commitChanges();

        // resolve the inverse to-one before the merge, so that a stale value can be observed
        assertSame(info1, box.getBoxInfo());
    }

    @Test
    public void inverseToOneKeptOnMergeIntoModifiedObject() {
        // a local change to an attribute, not to the arc
        box.setName("box modified");

        // a peer replaces the box's info row: the one-to-one FK now points from a new row
        peer.deleteObject(peer.localObject(info1));
        peer.commitChanges();
        BoxInfo peerInfo3 = peer.newObject(BoxInfo.class);
        peerInfo3.setColor("green");
        peerInfo3.setBox(peer.localObject(box));
        peer.commitChanges();

        // the BOX row did not change, so only a re-fetch merges a snapshot into the MODIFIED box
        ObjectSelect.query(Box.class).select(context);

        assertEquals(PersistenceState.MODIFIED, box.getPersistenceState());
        assertEquals("box modified", box.getName(), "the local change must survive the merge");

        // the snapshot of BOX carries no information about the arc, so the resolved target is kept as it is
        assertSame(info1, box.getBoxInfo());
    }

    @Test
    public void inverseToOneChangedLocallyIsKeptOnMerge() {
        // the user repoints the arc without committing
        box.setBoxInfo(info2);

        // a peer changes the BOX row, which merges the new snapshot into the MODIFIED box right away
        Box peerBox = peer.localObject(box);
        peerBox.setName("box renamed by peer");
        peer.commitChanges();

        // ... and so does a re-fetch
        ObjectSelect.query(Box.class).select(context);

        assertEquals(PersistenceState.MODIFIED, box.getPersistenceState());
        assertEquals("box renamed by peer", box.getName(), "an unmodified attribute takes the snapshot value");
        assertSame(info2, box.getBoxInfo(), "a locally changed arc must not be invalidated by the merge");
    }
}

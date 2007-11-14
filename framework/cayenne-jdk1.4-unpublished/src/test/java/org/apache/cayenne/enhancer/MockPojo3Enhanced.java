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
package org.apache.cayenne.enhancer;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;

public class MockPojo3Enhanced implements Persistent {

    protected ObjectId $cay_objectId;
    protected int $cay_persistenceState = PersistenceState.TRANSIENT;
    protected transient ObjectContext $cay_objectContext;

    protected List<MockPojo2> toMany;

    public int getPersistenceState() {
        return $cay_persistenceState;
    }

    public void setPersistenceState(int persistenceState) {
        this.$cay_persistenceState = persistenceState;
    }

    public ObjectContext getObjectContext() {
        return $cay_objectContext;
    }

    public void setObjectContext(ObjectContext objectContext) {
        this.$cay_objectContext = objectContext;
    }

    public ObjectId getObjectId() {
        return $cay_objectId;
    }

    public void setObjectId(ObjectId objectId) {
        this.$cay_objectId = objectId;
    }

    public List<MockPojo2> getToMany() {
        if ($cay_objectContext != null) {
            $cay_objectContext.prepareForAccess(this, "toMany", true);
        }
        
        return toMany;
    }

    public void setToMany(List<MockPojo2> toMany) {
        this.toMany = toMany;
    }
}

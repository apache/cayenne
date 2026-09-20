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
package org.apache.cayenne.docs.lifecycle;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.annotation.PostRemove;
import org.apache.cayenne.annotation.PostUpdate;

public class AnnotationExamples {

    // tag::multipleEntities[]
    // this callback will be invoked on PostRemove event of any object
    // belonging to MyEntity1, MyEntity2 or their subclasses
    @PostRemove({MyEntity1.class, MyEntity2.class})
    void postRemove(Persistent object) {
        // ...
    }
    // end::multipleEntities[]

    // tag::multipleAnnotations[]
    // similar example with multiple annotations on a single method
    // each matching just one entity
    @PostPersist(MyEntity1.class)
    @PostRemove(MyEntity1.class)
    @PostUpdate(MyEntity1.class)
    void postCommit(MyEntity1 object) {
        // ...
    }
    // end::multipleAnnotations[]
}

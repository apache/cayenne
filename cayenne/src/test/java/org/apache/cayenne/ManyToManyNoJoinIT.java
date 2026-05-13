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
package org.apache.cayenne;

import org.apache.cayenne.testdo.relationships_activity.Activity;
import org.apache.cayenne.testdo.relationships_activity.ActivityResult;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Date;

public class ManyToManyNoJoinIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.RELATIONSHIPS_ACTIVITY_PROJECT);

    @Test
    public void validateForSave1() throws Exception {
        ActivityResult result = env.context().newObject(ActivityResult.class);
        result.setAppointDate(new Date(System.currentTimeMillis()));
        result.setAppointNo(1);
        result.setField("xx");

        Activity activity = env.context().newObject(Activity.class);
        activity.setAppointmentDate(result.getAppointDate());
        activity.setAppointmentNo(result.getAppointNo());

        activity.addToResults(result);
        env.context().commitChanges();
    }

}

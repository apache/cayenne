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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.meaningful_pk.MeaningfulPKDep;
import org.apache.cayenne.testdo.meaningful_pk.MeaningfulPKTest1;
import org.apache.cayenne.testdo.meaningful_pk.MeaningfulPk;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.ExtraModules;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

@UseCayenneRuntime(CayenneProjects.MEANINGFUL_PK_PROJECT)
@ExtraModules(GraphSorterModule.class)
public class DataContextEntityWithMeaningfulPKAndCustomDbRowOpSorterIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Test
    public void testInsertDelete() {
        MeaningfulPk pkObj = context.newObject(MeaningfulPk.class);
        pkObj.setPk("123");
        context.commitChanges();

        context.deleteObject(pkObj);

        MeaningfulPk pkObj2 = context.newObject(MeaningfulPk.class);
        pkObj2.setPk("123");
        context.commitChanges();
    }

    @Test
    public void test_MeaningfulPkInsertDeleteCascade() {
        // setup
        MeaningfulPKTest1 obj = context.newObject(MeaningfulPKTest1.class);
        obj.setPkAttribute(1000);
        obj.setDescr("aaa");
        context.commitChanges();

        // must be able to set reverse relationship
        MeaningfulPKDep dep = context.newObject(MeaningfulPKDep.class);
        dep.setToMeaningfulPK(obj);
        dep.setPk(10);
        context.commitChanges();

        // test
        context.deleteObject(obj);

        MeaningfulPKTest1 obj2 = context.newObject(MeaningfulPKTest1.class);
        obj2.setPkAttribute(1000);
        obj2.setDescr("bbb");

        MeaningfulPKDep dep2 = context.newObject(MeaningfulPKDep.class);
        dep2.setToMeaningfulPK(obj2);
        dep2.setPk(10);
        context.commitChanges();
    }

}

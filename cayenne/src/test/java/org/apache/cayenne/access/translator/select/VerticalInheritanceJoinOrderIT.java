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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.inheritance_vertical.IvBase;
import org.apache.cayenne.testdo.inheritance_vertical.IvOther;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 5.0
 */
@UseCayenneRuntime(CayenneProjects.INHERITANCE_VERTICAL_PROJECT)
public class VerticalInheritanceJoinOrderIT extends RuntimeCase {

    @Inject
    private DataNode dataNode;

    /**
     * Tests that INNER joins from WHERE clause relationships are added before
     * LEFT OUTER joins from vertical inheritance child tables in the generated SQL.
     */
    @Test
    public void testQualifierJoinsBeforeDependentTableLeftJoins() {
        ObjectSelect<IvBase> q = ObjectSelect.query(IvBase.class)
                .where(IvBase.OTHERS.dot(IvOther.NAME).eq("test"))
                .and(IvBase.OTHERS.dot(IvOther.BASE).dot(IvBase.NAME).eq("test2"));

        String sql = translateToSql(q);

        assertTrue("Expected INNER JOIN to IV_OTHER, got: " + sql, sql.contains("JOIN IV_OTHER"));
        assertTrue("Expected LEFT JOIN to IV_IMPL, got: " + sql, sql.contains("LEFT JOIN IV_IMPL"));

        int iLeftJoin = sql.indexOf("LEFT JOIN IV_IMPL");
        int iInnerJoinOther = sql.indexOf("JOIN IV_OTHER");
        assertTrue("INNER JOIN to IV_OTHER should precede LEFT JOIN, got: " + sql,
                iInnerJoinOther < iLeftJoin);

        int iInnerJoinBase = sql.indexOf("JOIN IV_BASE", iInnerJoinOther);
        if (iInnerJoinBase > 0) {
            assertTrue("INNER JOIN to IV_BASE should precede LEFT JOIN, got: " + sql,
                    iInnerJoinBase < iLeftJoin);
        }
    }

    private String translateToSql(ObjectSelect<?> query) {
        DbAdapter adapter = dataNode.getAdapter();
        EntityResolver resolver = dataNode.getEntityResolver();
        return new DefaultSelectTranslator(query, adapter, resolver).getSql();
    }
}

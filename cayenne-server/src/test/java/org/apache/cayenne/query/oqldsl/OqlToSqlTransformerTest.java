package org.apache.cayenne.query.oqldsl; /*****************************************************************
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

import de.jexp.jequel.Sql92Format;
import de.jexp.jequel.sql.Sql;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.oqldsl.model.Select;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * TODO Db connection not needed here
 * */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class OqlToSqlTransformerTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Test
    public void testBasicEntityQuery() {
        ObjEntity objEntity = context.getEntityResolver().getObjEntity(Artist.class);

        Select select = new OqlQuery(objEntity).getSelect();
        Sql sql = new OqlToSqlTransformer().transform(select);
        assertEquals("select a.* from ARTIST as a", sql.accept(new Sql92Format()));
    }

    @Test
    public void testEntityWithScalarQuery() {
        ObjEntity objEntity = context.getEntityResolver().getObjEntity(Artist.class);

        Select select = OqlQuery.select(objEntity, objEntity.getAttribute("name")).getSelect();
        Sql sql = new OqlToSqlTransformer().transform(select);
        assertEquals("select a.*, a.name from ARTIST as a", sql.accept(new Sql92Format()));
    }
}
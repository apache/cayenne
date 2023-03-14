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

import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.log.Slf4jJdbcEventLogger;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ExtraModules;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.Util;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
@ExtraModules(CAY2723IT.CustomLogModule.class)
public class CAY2723IT extends ServerCase {
    @Inject
    private DataContext context;

    @Inject
    private DataChannelInterceptor queryInterceptor;

    /**
     * need to run this to ensure that PK generation doesn't affect main test
     */
    @Before
    public void warmup() {
        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle("test_warmup");
        context.commitChanges();
    }

    @Test
    public void phantomToDepPKUpdate() {
        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle("test_p_123");

        PaintingInfo paintingInfo = context.newObject(PaintingInfo.class);
        paintingInfo.setTextReview("test_a_123");

        painting.setToPaintingInfo(paintingInfo);
        painting.setToPaintingInfo(null);

        context.deleteObject(paintingInfo);

        // here should be only single insert of the painting object
        int queryCounter = queryInterceptor.runWithQueryCounter(() -> context.commitChanges());
        assertEquals(1, queryCounter);
    }

    public static class CustomLogger extends Slf4jJdbcEventLogger {

        private static final Logger logger = LoggerFactory.getLogger(JdbcEventLogger.class);

        public CustomLogger(@Inject RuntimeProperties runtimeProperties) {
            super(runtimeProperties);
        }

        @Override
        public void log(String message) {
            if (message != null) {
                logger.error("\t>>>>>\t" + message);
            }
        }

        @Override
        public void logQuery(String sql, ParameterBinding[] bindings) {
            StringBuilder buffer = new StringBuilder("\t>>>>>\t")
                    .append(sql)
                    .append(" ");
            appendParameters(buffer, "bind", bindings);
            if (buffer.length() > 0) {
                logger.error(buffer.toString());
            }
        }

        @Override
        public void logQueryError(Throwable th) {
            if (th != null) {
                th = Util.unwindException(th);
            }

            logger.error("*** error.", th);

            if (th instanceof SQLException) {
                SQLException sqlException = ((SQLException) th).getNextException();
                while (sqlException != null) {
                    logger.error("*** nested SQL error.", sqlException);
                    sqlException = sqlException.getNextException();
                }
            }
        }

        @Override
        public boolean isLoggable() {
            return true;
        }
    }

    public static class CustomLogModule implements Module {
        @Override
        public void configure(Binder binder) {
            binder.bind(JdbcEventLogger.class).to(CustomLogger.class);
        }
    }
}

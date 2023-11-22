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

package org.apache.cayenne.template;

import java.io.ByteArrayInputStream;

import org.apache.cayenne.template.parser.SQLTemplateParser;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
public class TemplateParserPoolTest {

    TemplateParserPool parserPool;

    @Before
    public void createPool() {
        parserPool = new TemplateParserPool();
    }

    @Test
    public void get() throws Exception {
        for(int i=0; i<TemplateParserPool.MAX_POOL_SIZE + 10; i++) {
            SQLTemplateParser parser = parserPool.get();
            assertNotNull(parser);
        }
    }

    @Test
    public void put() throws Exception {
        SQLTemplateParser parser = new SQLTemplateParser(new ByteArrayInputStream("".getBytes()));

        parserPool.put(parser);

        for(int i=0; i<TemplateParserPool.INITIAL_POOL_SIZE; i++) {
            SQLTemplateParser parser1 = parserPool.get();
            assertNotNull(parser1);
            assertNotSame(parser, parser1);
        }

        SQLTemplateParser parser1 = parserPool.get();
        assertSame(parser, parser1);
    }

    @Test
    public void createNewParser() throws Exception {
        SQLTemplateParser parser = parserPool.createNewParser();
        assertNotNull(parser);
    }

}
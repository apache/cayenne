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
package org.apache.cayenne.access.translator.ejbql;

import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLParser;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EJBQLVerticalInheritanceTranslatorIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.INHERITANCE_VERTICAL_PROJECT);

    private static final Pattern TABLE_DECLARATION = Pattern.compile("(?:FROM|JOIN)\\s+([A-Z_0-9]+)\\s+(t\\d+)");

    private SQLTemplate translateSelect(String ejbql) {
        EJBQLParser parser = EJBQLParserFactory.getParser();
        EJBQLCompiledExpression select = parser.compile(ejbql, env.runtime().getDataDomain().getEntityResolver());
        EJBQLQuery query = new EJBQLQuery(ejbql);

        EJBQLTranslationContext context = new EJBQLTranslationContext(
                env.runtime().getDataDomain().getEntityResolver(), query, select, new JdbcEJBQLTranslator(),
                env.dataNode().getAdapter(), QuotingStrategy.NONE);
        select.getExpression().visit(new EJBQLSelectTranslator(context));
        return context.getQuery();
    }

    private List<String> tableAliases(String sql) {
        List<String> aliases = new ArrayList<>();
        Matcher matcher = TABLE_DECLARATION.matcher(sql);
        while (matcher.find()) {
            aliases.add(matcher.group(2));
        }
        return aliases;
    }

    @Test
    public void noDuplicateTableAliasesForInheritanceRoot() {
        String sql = translateSelect("select a from IvRoot a").getDefaultTemplate();

        List<String> aliases = tableAliases(sql);
        Set<String> unique = new HashSet<>(aliases);

        assertEquals(aliases.size(), unique.size());
    }

    @Test
    public void noDuplicateTableAliasesForInheritanceSubEntity() {
        String sql = translateSelect("select a from IvSub1 a").getDefaultTemplate();

        List<String> aliases = tableAliases(sql);
        Set<String> unique = new HashSet<>(aliases);

        assertEquals(aliases.size(), unique.size());
    }

    @Test
    public void flattenedJoinChainKeepsOuterSemantics() {
        String sql = translateSelect("select a from IvRoot a").getDefaultTemplate();

        assertTrue(sql.contains("LEFT OUTER JOIN IV_SUB1_SUB1"));
    }
}

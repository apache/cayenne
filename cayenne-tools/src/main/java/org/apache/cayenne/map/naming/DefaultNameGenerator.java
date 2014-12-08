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
package org.apache.cayenne.map.naming;

import java.util.Locale;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.jvnet.inflector.Noun;

/**
 * A strategy for generating names of entities, attributes etc.
 * 
 * @since 4.0
 */
public class DefaultNameGenerator implements ObjectNameGenerator {

	@Override
	public String createDbRelationshipName(ExportedKey key, boolean toMany) {

		String name;

		if (toMany) {
			try {
				/**
				 * by default we use english language rules here. uppercase is
				 * required for NameConverter to work properly
				 */
				name = Noun.pluralOf(key.getFKTableName().toLowerCase(), Locale.ENGLISH).toUpperCase();
			} catch (Exception inflectorError) {
				/**
				 * seems that Inflector cannot be trusted. For instance, it
				 * throws an exception when invoked for word "ADDRESS" (although
				 * lower case works fine). To feel safe, we use superclass'
				 * behavior if something's gone wrong
				 */
				return key.getFKTableName().toLowerCase();
			}
		} else {
			String fkColName = key.getFKColumnName();

			// trim "ID" in the end
			if (fkColName == null) {
				name = key.getPKTableName();
			} else if (fkColName.toUpperCase().endsWith("_ID") && fkColName.length() > 3) {
				name = fkColName.substring(0, fkColName.length() - 3);
			} else if (fkColName.toUpperCase().endsWith("ID") && fkColName.length() > 2) {
				name = fkColName.substring(0, fkColName.length() - 2);
			} else {
				/**
				 * We don't want relationship to conflict with attribute, so
				 * we'd better return superior value with 'to'
				 */
				name = key.getPKTableName();
			}
		}

		return NameConverter.underscoredToJava(name, false);
	}

	@Override
	public String createObjEntityName(DbEntity dbEntity) {
		return NameConverter.underscoredToJava(dbEntity.getName(), true);
	}

	@Override
	public String createObjAttributeName(DbAttribute attr) {
		return NameConverter.underscoredToJava(attr.getName(), false);
	}

	@Override
	public String createObjRelationshipName(DbRelationship dbRel) {
		return NameConverter.underscoredToJava(dbRel.getName(), false);
	}
}

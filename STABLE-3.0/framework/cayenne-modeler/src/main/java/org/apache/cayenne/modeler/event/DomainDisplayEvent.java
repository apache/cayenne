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
package org.apache.cayenne.modeler.event;

import org.apache.cayenne.access.DataDomain;

/**
 * Represents a display event of a DataDomain.
 * 
 */
public class DomainDisplayEvent extends DisplayEvent {
	protected DataDomain domain;
	protected boolean domainChanged = true;

	public DomainDisplayEvent(Object src, DataDomain domain) {
		super(src);
		this.domain = domain;
	}

	/** Get domain for this data map. */
	public DataDomain getDomain() {
		return domain;
	}
	
	/**
	 * Sets the domain.
	 * @param domain The domain to set
	 */
	public void setDomain(DataDomain domain) {
		this.domain = domain;
	}
	
	/**
	 * Returns the domainChanged.
	 * @return boolean
	 */
	public boolean isDomainChanged() {
		return domainChanged;
	}


	/**
	 * Sets the domainChanged.
	 * @param domainChanged The domainChanged to set
	 */
	public void setDomainChanged(boolean domainChanged) {
		this.domainChanged = domainChanged;
	}
}

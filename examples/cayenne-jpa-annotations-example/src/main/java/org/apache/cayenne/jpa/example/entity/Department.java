/*
 *  Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.cayenne.jpa.example.entity;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;

import org.apache.cayenne.jpa.bridge.QueryHints;

@Entity
@NamedQuery(name = "DepartmentWithName", query = "delete from department", hints = {
        @QueryHint(name = QueryHints.QUERY_TYPE_HINT, value = QueryHints.SELECT_QUERY),
        @QueryHint(name = QueryHints.QUALIFIER_HINT, value = "name likeIgnoreCase $name")
})
public class Department {

    @Id
    protected int department_id;

    protected String name;
    protected String description;

    @OneToMany(mappedBy = "department")
    protected List<Person> employees;

    @OneToMany(mappedBy = "department")
    protected List<Project> projects;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Person> getEmployees() {
        return employees;
    }

    public List<Project> getProjects() {
        return projects;
    }
}

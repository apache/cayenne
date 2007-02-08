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
package org.apache.cayenne.jpa.example;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;

import org.apache.cayenne.jpa.example.entity.Department;
import org.apache.cayenne.jpa.example.entity.Person;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An example main class.
 * 
 * @author Andrus Adamchik
 */
public class Main {

    EntityManager entityManager;
    Log logger;

    public static void main(String[] args) {
        try {
            new Main().execute();
        }
        catch (Throwable th) {
            th.printStackTrace();
        }
    }

    Main() {

        DerbySetupHelper schemaHelper = new DerbySetupHelper();

        // setup Derby properties first
        schemaHelper.prepareDerby();

        // create JPA EntityManager
        this.entityManager = Persistence
                .createEntityManagerFactory("HRPersistenceUnit")
                .createEntityManager(PersistenceContextType.EXTENDED);

        // now that the mapping is loaded, create test DB
        schemaHelper.setupDatabase();

        this.logger = LogFactory.getLog(getClass());
    }

    void execute() {

        logger.info("*** 1. Setup data: ");

        // batch a few queries in a single call...

        Query delete1 = entityManager.createNamedQuery("DeletePerson");
        logger.info("   deleted = " + delete1.executeUpdate());

        Query delete2 = entityManager.createNamedQuery("DeleteDepartment");
        logger.info("   deleted = " + delete2.executeUpdate());

        Query insert1 = entityManager.createNamedQuery("CreateData");
        logger.info("   inserted = " + insert1.executeUpdate());

        logger.info("=======================================\n\n ");
        logger.info("*** 2. Select: ");

        Query select1 = entityManager.createNamedQuery("DepartmentWithName");
        List results = select1.getResultList();
        logger.info("   select results: " + results);

        Department department = (Department) results.get(0);
        department.setDescription(department.getDescription() + "_");

        logger.info("=======================================\n\n ");
        logger.info("*** 3. Commit modified: ");
        entityManager.flush();

        logger.info("   department: " + department);

        logger.info("=======================================\n\n ");
        logger.info("*** 4. Commit New Object: ");
        Person person = new Person();
        person.setBaseSalary(new Double(23000.00));
        person.setDateHired(new Date());
        person.setFullName("Test Person");

        entityManager.persist(person);

        entityManager.flush();

        logger.info("   person: " + person);

        logger.info("=======================================\n\n ");
        logger.info("*** 5. Setup relationship: ");

        person.setDepartment(department);

        Person anotherPerson = new Person();
        anotherPerson.setBaseSalary(new Double(88000.00));
        anotherPerson.setDateHired(new Date());
        anotherPerson.setFullName("Another Test Person");
        entityManager.persist(anotherPerson);

        department.getEmployees().add(anotherPerson);

        entityManager.flush();

        Person yetAnotherPerson = new Person();
        yetAnotherPerson.setBaseSalary(new Double(1000000.00));
        yetAnotherPerson.setDateHired(new Date());
        yetAnotherPerson.setFullName("Yet Another Test Person");
        entityManager.persist(yetAnotherPerson);
        department.getEmployees().add(yetAnotherPerson);

        entityManager.flush();

        logger.info("=======================================\n\n ");
        logger.info("*** 6. Delete relationship: ");

        department.getEmployees().remove(anotherPerson);
        yetAnotherPerson.setDepartment(null);
        entityManager.flush();
        logger.info(" employees: " + department.getEmployees());
    }
}
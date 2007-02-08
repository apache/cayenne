package org.objectstyle.petstore.dao.impl;

import java.util.HashMap;
import java.util.Map;

import org.objectstyle.petstore.dao.AccountDao;
import org.objectstyle.petstore.domain.Account;

public class AccountCayenneDao extends CayenneDao implements AccountDao {

    public Account getAccount(String username) {
        Map parameters = new HashMap();
        parameters.put("userName", username);
        return (Account) findObject("getAccount", parameters);
    }

    public Account getAccount(String username, String password) {
        Map parameters = new HashMap();
        parameters.put("userName", username);
        parameters.put("password", password);
        return (Account) findObject("getAccount", parameters);
    }
}

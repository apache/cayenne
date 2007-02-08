package org.objectstyle.petstore.dao;

import org.objectstyle.petstore.domain.Account;

public interface AccountDao {

    Account getAccount(String username);

    Account getAccount(String username, String password);
}

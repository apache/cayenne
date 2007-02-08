package org.objectstyle.petstore.service;

import org.objectstyle.petstore.dao.AccountDao;
import org.objectstyle.petstore.dao.DaoManager;
import org.objectstyle.petstore.dao.PersistenceManager;
import org.objectstyle.petstore.domain.Account;

public class AccountService {

    private AccountDao accountDao;
    private PersistenceManager persistenceManager;

    public AccountService() {
        DaoManager daoManager = DaoManager.getManager();
        this.accountDao = (AccountDao) daoManager.getDao(AccountDao.class);
        this.persistenceManager = (PersistenceManager) daoManager
                .getDao(PersistenceManager.class);
    }

    public AccountService(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    public Account getAccount(String username) {
        return accountDao.getAccount(username);
    }

    public Account getAccount(String username, String password) {
        return accountDao.getAccount(username, password);
    }

    public void insertAccount(Account account) {
        persistenceManager.persistObject(account);
    }

    public void commitChanges() {
        persistenceManager.commitChanges();
    }
}

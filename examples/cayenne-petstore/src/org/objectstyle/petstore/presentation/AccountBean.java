package org.objectstyle.petstore.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.struts.beanaction.BeanActionException;
import org.objectstyle.petstore.domain.Account;
import org.objectstyle.petstore.domain.Category;
import org.objectstyle.petstore.service.AccountService;
import org.objectstyle.petstore.service.CatalogService;

import com.ibatis.common.util.PaginatedList;

public class AccountBean extends AbstractBean {

    private static final List LANGUAGE_LIST;
    private static final List CATEGORY_LIST;

    private AccountService accountService;
    private CatalogService catalogService;

    private Account account;
    private String password;
    private String languagePreference;
    private boolean listOption;
    private boolean bannerOption;
    private Object favouriteCategoryId;

    private String repeatedPassword;
    private String pageDirection;
    private String validation;
    private PaginatedList myList;
    private boolean authenticated;

    static {
        List langList = new ArrayList();
        langList.add("english");
        langList.add("japanese");
        LANGUAGE_LIST = Collections.unmodifiableList(langList);

        List catList = new ArrayList();
        catList.add("FISH");
        catList.add("DOGS");
        catList.add("REPTILES");
        catList.add("CATS");
        catList.add("BIRDS");
        CATEGORY_LIST = Collections.unmodifiableList(catList);
    }

    public AccountBean() {
        this.account = new Account();
        this.accountService = new AccountService();
        this.catalogService = new CatalogService();
    }

    public String getUsername() {
        return account.getUserName();
    }

    public void setUsername(String username) {
        account.setUserName(username);
    }

    public PaginatedList getMyList() {
        return myList;
    }

    public void setMyList(PaginatedList myList) {
        this.myList = myList;
    }

    public String getRepeatedPassword() {
        return repeatedPassword;
    }

    public void setRepeatedPassword(String repeatedPassword) {
        this.repeatedPassword = repeatedPassword;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public List getLanguages() {
        return LANGUAGE_LIST;
    }

    public List getCategories() {
        return CATEGORY_LIST;
    }

    public String getPageDirection() {
        return pageDirection;
    }

    public void setPageDirection(String pageDirection) {
        this.pageDirection = pageDirection;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public boolean isBannerOption() {
        return bannerOption;
    }

    public void setBannerOption(boolean bannerOption) {
        this.bannerOption = bannerOption;
    }

    public String getLanguagePreference() {
        return languagePreference;
    }

    public void setLanguagePreference(String languagePrefrence) {
        this.languagePreference = languagePrefrence;
    }

    public Object getFavouriteCategoryId() {
        return favouriteCategoryId;
    }

    public void setFavouriteCategoryId(Object favouriteCategoryId) {
        this.favouriteCategoryId = favouriteCategoryId;
    }

    public boolean isListOption() {
        return listOption;
    }

    public void setListOption(boolean listOption) {
        this.listOption = listOption;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String editAccountForm() {
        try {
            this.account = accountService.getAccount(account.getUserName());
            initFormForAccount();

            return SUCCESS;
        }
        catch (Exception e) {
            throw new BeanActionException(
                    "There was a problem retrieving your Account Information. Cause: "
                            + e,
                    e);
        }
    }

    public String newAccount() {
        try {
            accountService.insertAccount(account);
            initAccountFromForm();
            accountService.commitChanges();

            myList = catalogService.getProductListByCategory(account
                    .getProfile()
                    .getCategory());
            authenticated = true;
            repeatedPassword = null;
            return SUCCESS;
        }
        catch (Exception e) {
            throw new BeanActionException(
                    "There was a problem creating your Account Information.  Cause: " + e,
                    e);
        }
    }

    public String editAccount() {
        try {
            account = accountService.getAccount(getUsername());
            initAccountFromForm();
            accountService.commitChanges();

            myList = catalogService.getProductListByCategory(account
                    .getProfile()
                    .getCategory());
            return SUCCESS;
        }
        catch (Exception e) {
            throw new BeanActionException(
                    "There was a problem updating your Account Information. Cause: " + e,
                    e);
        }
    }

    public String switchMyListPage() {
        if ("next".equals(pageDirection)) {
            myList.nextPage();
        }
        else if ("previous".equals(pageDirection)) {
            myList.previousPage();
        }
        return SUCCESS;
    }

    public String signon() {

        account = accountService.getAccount(getUsername(), getPassword());

        if (account == null) {
            String value = "Invalid username or password.  Signon failed.";
            setMessage(value);
            clear();
            return FAILURE;
        }
        else {
            initFormForAccount();
            setPassword(null);

            myList = catalogService.getProductListByCategory(account
                    .getProfile()
                    .getCategory());

            authenticated = true;
            return SUCCESS;
        }
    }

    public String signoff() {
        clear();
        return SUCCESS;
    }

    public boolean isAuthenticated() {
        return authenticated && account != null && account.getUserName() != null;
    }

    public void reset() {
        bannerOption = false;
        listOption = false;
    }

    public void clear() {
        account = new Account();
        repeatedPassword = null;
        pageDirection = null;
        myList = null;
        authenticated = false;

        password = null;
        bannerOption = false;
        listOption = false;
        languagePreference = null;
        favouriteCategoryId = null;
    }

    private void initFormForAccount() {
        if (account != null && account.getProfile() != null) {
            this.password = account.getUser().getPassword();
            this.bannerOption = account.getProfile().getBannerOption().booleanValue();
            this.listOption = account.getProfile().getListOption().booleanValue();
            this.languagePreference = account.getProfile().getLanguagePreference();

            Category category = account.getProfile().getCategory();
            this.favouriteCategoryId = category != null ? category.getCategoryId() : null;
        }
        else {
            this.password = null;
            this.bannerOption = false;
            this.listOption = false;
            this.languagePreference = null;
            this.favouriteCategoryId = null;
        }
    }

    private void initAccountFromForm() {
        account.getProfile().setBannerOption(Boolean.valueOf(bannerOption));
        account.getProfile().setListOption(Boolean.valueOf(listOption));
        account.getProfile().setLanguagePreference(languagePreference);
        account.getProfile().setCategory(catalogService.getCategory(favouriteCategoryId));
        account.getUser().setPassword(password);
    }
}
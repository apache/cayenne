package org.objectstyle.petstore.service;

import java.util.List;

import org.objectstyle.petstore.dao.DaoManager;
import org.objectstyle.petstore.dao.ItemDao;
import org.objectstyle.petstore.dao.PersistenceManager;
import org.objectstyle.petstore.dao.ProductDao;
import org.objectstyle.petstore.domain.Category;
import org.objectstyle.petstore.domain.Item;
import org.objectstyle.petstore.domain.Product;

import com.ibatis.common.util.PaginatedList;

public class CatalogService {

    private PersistenceManager persistenceManager;
    private ProductDao productDao;
    private ItemDao itemDao;

    public CatalogService() {
        DaoManager daoManager = DaoManager.getManager();

        productDao = (ProductDao) daoManager.getDao(ProductDao.class);
        itemDao = (ItemDao) daoManager.getDao(ItemDao.class);
        persistenceManager = (PersistenceManager) daoManager
                .getDao(PersistenceManager.class);
    }

    public CatalogService(ItemDao itemDao, ProductDao productDao) {
        this.productDao = productDao;
        this.itemDao = itemDao;
    }

    public List getCategoryList() {
        return persistenceManager.getAllObjects(Category.class);
    }

    public Category getCategory(Object categoryId) {
        return (Category) persistenceManager.findObject(Category.class, categoryId);
    }

    public PaginatedList searchProductList(String keywords) {
        return productDao.searchProductList(keywords);
    }

    public PaginatedList getProductListByCategory(Category category) {
        return productDao.getProductListByCategory(category);
    }

    public Product getProduct(String productId) {
        return (Product) persistenceManager.findObject(Product.class, productId);
    }

    public PaginatedList getItemListByProduct(Product product) {
        return itemDao.getItemListByProduct(product);
    }

    public boolean isItemInStock(String itemId) {
        return itemDao.isItemInStock(itemId);
    }

    public Item getItem(String itemId) {
        return (Item) persistenceManager.findObject(Item.class, itemId);
    }
}
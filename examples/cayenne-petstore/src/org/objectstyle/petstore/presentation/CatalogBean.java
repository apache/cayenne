package org.objectstyle.petstore.presentation;

import org.objectstyle.petstore.domain.Category;
import org.objectstyle.petstore.domain.Item;
import org.objectstyle.petstore.domain.Product;
import org.objectstyle.petstore.service.CatalogService;

import com.ibatis.common.util.PaginatedList;

public class CatalogBean extends AbstractBean {

    private CatalogService catalogService;

    private String keyword;
    private String pageDirection;

    private String categoryId;
    private Category category;
    private PaginatedList categoryList;

    private String productId;
    private Product product;
    private PaginatedList productList;

    private String itemId;
    private Item item;
    private PaginatedList itemList;

    public CatalogBean() {
        this(new CatalogService());
    }

    public CatalogBean(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getPageDirection() {
        return pageDirection;
    }

    public void setPageDirection(String pageDirection) {
        this.pageDirection = pageDirection;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public PaginatedList getCategoryList() {
        return categoryList;
    }

    public void setCategoryList(PaginatedList categoryList) {
        this.categoryList = categoryList;
    }

    public PaginatedList getProductList() {
        return productList;
    }

    public void setProductList(PaginatedList productList) {
        this.productList = productList;
    }

    public PaginatedList getItemList() {
        return itemList;
    }

    public void setItemList(PaginatedList itemList) {
        this.itemList = itemList;
    }

    public String viewCategory() {
        if (categoryId != null) {
            category = catalogService.getCategory(categoryId);
            productList = catalogService.getProductListByCategory(category);
        }
        return SUCCESS;
    }

    public String viewProduct() {
        if (productId != null) {
            product = catalogService.getProduct(productId);
            itemList = catalogService.getItemListByProduct(product);
        }
        return SUCCESS;
    }

    public String viewItem() {
        item = catalogService.getItem(itemId);
        product = item.getProduct();
        return SUCCESS;
    }

    public String searchProducts() {
        if (keyword == null || keyword.length() < 1) {
            setMessage("Please enter a keyword to search for, then press the search button.");
            return FAILURE;
        }
        else {
            productList = catalogService.searchProductList(keyword.toLowerCase());
            return SUCCESS;
        }
    }

    public String switchProductListPage() {
        if ("next".equals(pageDirection)) {
            productList.nextPage();
        }
        else if ("previous".equals(pageDirection)) {
            productList.previousPage();
        }
        return SUCCESS;
    }

    public String switchItemListPage() {
        if ("next".equals(pageDirection)) {
            itemList.nextPage();
        }
        else if ("previous".equals(pageDirection)) {
            itemList.previousPage();
        }
        return SUCCESS;
    }

    public void clear() {
        keyword = null;
        pageDirection = null;

        categoryId = null;
        category = null;
        categoryList = null;

        productId = null;
        product = null;
        productList = null;

        itemId = null;
        item = null;
        itemList = null;
    }

}

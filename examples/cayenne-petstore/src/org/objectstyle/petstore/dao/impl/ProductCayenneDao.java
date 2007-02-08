package org.objectstyle.petstore.dao.impl;

import java.util.List;
import java.util.StringTokenizer;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.petstore.dao.ProductDao;
import org.objectstyle.petstore.domain.Category;
import org.objectstyle.petstore.domain.Product;

import com.ibatis.common.util.PaginatedArrayList;
import com.ibatis.common.util.PaginatedList;

public class ProductCayenneDao extends CayenneDao implements ProductDao {

    public PaginatedList searchProductList(String keywords) {

        SelectQuery query = new SelectQuery(Product.class, buildSearchQualifier(keywords));
        query.setPageSize(PAGE_SIZE);
        List result = getDataContext().performQuery(query);
        return new PaginatedArrayList(result, PAGE_SIZE);
    }

    public PaginatedList getProductListByCategory(Category category) {
        // using query instead of a relationship to enable pagination
        Expression qualifier = ExpressionFactory.matchExp(
                Product.CATEGORY_PROPERTY,
                category);
        SelectQuery query = new SelectQuery(Product.class, qualifier);
        query.setPageSize(PAGE_SIZE);
        List result = getDataContext().performQuery(query);
        return new PaginatedArrayList(result, PAGE_SIZE);
    }

    protected Expression buildSearchQualifier(String keywords) {
        if (Util.isEmptyString(keywords)) {
            return null;
        }

        return matchKeywords(Product.NAME_PROPERTY, keywords).orExp(
                matchKeywords(Product.CATEGORY_NAME_PROPERTY, keywords)).orExp(
                matchKeywords(Product.DESCRIPTION_PROPERTY, keywords));
    }

    /**
     * Creates an expression that matches a String of space-separated keywords against a
     * given property name.
     */
    protected Expression matchKeywords(String key, String keywords) {

        StringTokenizer toks = new StringTokenizer(keywords);

        if (!toks.hasMoreTokens()) {
            throw new IllegalArgumentException("'keywords' must be a non-empty string");
        }

        Expression e = ExpressionFactory.likeIgnoreCaseExp(key, "%"
                + toks.nextToken()
                + "%");

        while (toks.hasMoreTokens()) {
            e = e.orExp(ExpressionFactory.likeIgnoreCaseExp(key, "%"
                    + toks.nextToken()
                    + "%"));
        }

        return e;
    }
}

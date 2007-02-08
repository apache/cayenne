<%@ include file="../common/IncludeTop.jsp" %>

<bean:define id="product" name="catalogBean" property="product"/>
<bean:define id="itemList" name="catalogBean" property="itemList"/>

<div id="BackLink">

  <html:link paramId="categoryId" paramName="product" paramProperty="categoryId" page="/shop/viewCategory.shtml">
    Return to <bean:write name="product" property="categoryId"/></html:link>

</div>

<div id="Catalog">

  <h2><bean:write name="product" property="name"/></h2>

  <table>
    <tr><th>Item ID</th>  <th>Product ID</th>  <th>Description</th>  <th>List
      Price</th>  <th>&nbsp;</th></tr>
    <logic:iterate id="item" name="itemList">
      <tr>
        <td>
          <html:link paramId="itemId" paramName="item" paramProperty="itemId" page="/shop/viewItem.shtml">
            <bean:write name="item" property="itemId"/></html:link></td>
        <td><bean:write name="item" property="productId"/></td>
        <td>
          <bean:write name="item" property="attribute1"/>
          <bean:write name="item" property="attribute2"/>
          <bean:write name="item" property="attribute3"/>
          <bean:write name="item" property="attribute4"/>
          <bean:write name="item" property="attribute5"/>
          <bean:write name="product" property="name"/>
        </td>
        <td><bean:write name="item" property="listPrice" format="$#,##0.00"/></td>
        <td><html:link styleClass="Button" paramId="workingItemId" paramName="item" paramProperty="itemId" page="/shop/addItemToCart.shtml">
          Add to Cart</html:link></td>
      </tr>
    </logic:iterate>
    <tr><td>
      <logic:notEqual name="itemList" property="firstPage" value="true">
        <a class="Button" href="switchItemListPage.shtml?pageDirection=previous">&lt;&lt; Prev</a>
      </logic:notEqual>
      <logic:notEqual name="itemList" property="lastPage" value="true">
        <a class="Button" href="switchItemListPage.shtml?pageDirection=next">Next &gt;&gt;</a>
      </logic:notEqual>
    </td></tr>
  </table>

</div>

<%@ include file="../common/IncludeBottom.jsp" %>






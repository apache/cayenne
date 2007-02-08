<%@ include file="../common/IncludeTop.jsp" %>

<bean:define id="category" name="catalogBean" property="category"/>
<bean:define id="productList" name="catalogBean" property="productList"/>

<div id="BackLink">

  <html:link page="/shop/index.shtml">Return to Main Menu</html:link>

</div>

<div id="Catalog">

  <h2><bean:write name="category" property="name"/></h2>

  <table>
    <tr><th>Product ID</th>  <th>Name</th></tr>
    <logic:iterate id="product" name="productList">
      <tr>
        <td><html:link paramId="productId" paramName="product" paramProperty="productId" page="/shop/viewProduct.shtml">
          <bean:write name="product" property="productId"/></html:link></td>
        <td><bean:write name="product" property="name"/></td>
      </tr>
    </logic:iterate>
    <tr><td colspan="2">
      <logic:notEqual name="productList" property="firstPage" value="true">
        <a class="Button" href="switchProductListPage.shtml?pageDirection=previous">&lt;&lt; Prev</a>
      </logic:notEqual>
      <logic:notEqual name="productList" property="lastPage" value="true">
        <a class="Button" href="switchProductListPage.shtml?pageDirection=next">Next &gt;&gt;</a>
      </logic:notEqual>
    </td></tr>
  </table>

</div>

<%@ include file="../common/IncludeBottom.jsp" %>



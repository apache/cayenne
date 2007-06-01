<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-nested.tld" prefix="nested" %>

<html:html locale="true">

<head>

<title><bean:message key="index.title"/></title>

<html:base/>

<LINK REL="stylesheet" TYPE="text/css" href="styles.css" TITLE="default">

</head>

<body bgcolor="white">
<jsp:include page="navi.html" flush="true"/>

<br><br>

<span class="titleTextStrong">Gallery Browse Page</span><br><br>



<table width="100%" class="standardTable" border="0" cellspacing="0">
<logic:iterate id="aGallery" name="galleries">
<tr>
	<td bgcolor="silver" colspan="2"><b>Gallery Details:</b></td>
</tr>
<tr>
	<td width="250"><bean:write name="aGallery" property="galleryName"/></td>
	<td width="390"><b>Current Displays:</b></td>
</tr>
<tr>
	<td></td>
	<td>
	  	<nested:root name="aGallery">
        <nested:iterate property="paintingArray">
            &nbsp;&nbsp;&nbsp;<nested:write property="paintingTitle" />, $<nested:write property="estimatedPrice" /> <i>by <nested:write property="toArtist.artistName" /> <a href="removePaintingFromGallery.do?title=<nested:write property="paintingTitle" />&galleryName=<bean:write name="aGallery" property="galleryName"/>">remove painting from gallery</a></i><br>
        </nested:iterate>
    	</nested:root><br>
	</td>
</tr>
</logic:iterate>
<tr>
	<td colspan="2" align="center"><a href="addGallery.do">Add Gallery</a></td>
</tr>
</table>


</body>

</html:html>
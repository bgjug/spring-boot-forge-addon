<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<!DOCTYPE html>
<html>
<head>
<title>${entityName}</title>
</head>
<body>
	<h1>${entityName}</h1>
	<form:form modelAttribute="${entity}" method="post" action="/${entity}/${entity}-save" >
		<p><form:errors /></p>

		${formData}
		
		<button type="submit">Save</button>	
	</form:form>
</body>

</html>
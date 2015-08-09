<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<!DOCTYPE html>
<html>
<head>
<title>User</title>
</head>
<body>
	<h1>User</h1>
	<form:form modelAttribute="user" method="post" action="/user/user-save" >
		<p><form:errors /></p>

		<dl>
			<dt>
				<label for="id">Id</label>
			</dt>
			<dd>
				<form:input path="id" readonly="true" />
			</dd>
		</dl>
		<dl>
			<dt>
				<label for="firstName">First Name</label>
			</dt>
			<dd>
				<form:input path="firstName" />
			</dd>
		</dl>
		<dl>
			<dt>
				<label for="lastName">Last Name</label>
			</dt>
			<dd>
				<form:input path="lastName" />
			</dd>
		</dl>
		<button type="submit">Save</button>	
	</form:form>
</body>

</html>
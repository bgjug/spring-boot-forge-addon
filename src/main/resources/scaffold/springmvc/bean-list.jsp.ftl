<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html>
<head>
<title>User list</title>
</head>

<body>
	<h1>User list</h1>
	<span><a href="/user/user-add"> Add user </a></span>
	<table>
		<thead>
			<tr>
				<th><i>Id</i></th>
				<th><i>FirstName</i></th>
				<th><i>LastName</i></th>
				<th><i>Operations</i></th>
			</tr>
		</thead>
		<tbody>
			<c:forEach var="user" items="${r"${users}"}">
				<tr>
					<td>${r"${user.id}"}</td>
					<td>${r"${user.firstName}"}</td>
					<td>${r"${user.lastName}"}</td>
					<td>
						<span><a href="/user/user-edit/${r"${user.id}"}">Edit</a></span> &nbsp;

						<span><a href="/user/user-remove/${r"${user.id}"}"> Remove </a></span>
					</td>
				</tr>
			</c:forEach>
			</tbody>
	</table>
	<c:if test="${r"${hasPrevious}"}">
		<span><a href="/user/user-list?page=${r"${currentPage-1}"}">previous</a></span>
	</c:if>
	<c:if test="${r"${hasNext}"}">
		<span><a href="/user/user-list?page=${r"${currentPage+1}"}">next</a></span>
	</c:if>
</body>

</html>
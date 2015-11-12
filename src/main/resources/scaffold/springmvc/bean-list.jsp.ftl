<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html>
<head>
<title>${entityName} list</title>
</head>

<body>
	<h1>${entityName} list</h1>
	<span><a href="/${entity}/${entity}-add"> Add ${entityName} </a></span>
	<table>
		<thead>
			<tr>
				${columnHeader}
				<th><i>Operations</i></th>
			</tr>
		</thead>
		<tbody>
			<c:forEach var="item" items="${r"${items}"}">
				<tr>
					${columnData}
					<td>
						<span><a href="/${entity}/${entity}-edit/${r"${item.id}"}">Edit</a></span> &nbsp;

						<span><a href="/${entity}/${entity}-remove/${r"${item.id}"}"> Remove </a></span>
					</td>
				</tr>
			</c:forEach>
			</tbody>
	</table>
	<c:if test="${r"${hasPrevious}"}">
		<span><a href="/${entity}/${entity}-list?page=${r"${currentPage-1}"}">previous</a></span>
	</c:if>
	<c:if test="${r"${hasNext}"}">
		<span><a href="/${entity}/${entity}-list?page=${r"${currentPage+1}"}">next</a></span>
	</c:if>
</body>

</html>
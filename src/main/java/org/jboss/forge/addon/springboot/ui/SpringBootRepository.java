package org.jboss.forge.addon.springboot.ui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLClassLoader;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import org.jboss.forge.addon.facets.FacetNotFoundException;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.javaee.jpa.PersistenceOperations;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.parser.java.ui.AbstractJavaSourceCommand;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.ClassLoaderFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.springboot.facet.SpringBootRestFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UISelection;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
 
@FacetConstraint(SpringBootRestFacet.class)
public class SpringBootRepository extends
		AbstractJavaSourceCommand<JavaInterfaceSource> {

	public static final String DEFAULT_REPOSITORY_PACKAGE = ".repository";

	@Inject
	@WithAttributes(label = "Repository for entity", required = true)
	private UISelectOne<JavaResource> repositoryEntity;

	@Inject
	@WithAttributes(label = "Repository type", required = true)
	private UISelectOne<SpringBootRepositoryType> repositoryType;

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(SpringBootRepository.class)
				.name("Spring Boot: Repository")
				.category(Categories.create("Spring", "Boot"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		UIContext context = builder.getUIContext();
		Project project = getSelectedProject(context);
		JavaSourceFacet javaSourceFacet = project
				.getFacet(JavaSourceFacet.class);

		setupEntities(context);

		String entityName = null;

		if (repositoryEntity.getValue() != null) {
			JavaClassSource entityClass = ((JavaResource) repositoryEntity
					.getValue()).getJavaType();

			entityName = entityClass.getName();
		}

		builder.add(repositoryEntity);
		super.initializeUI(builder);
		builder.add(repositoryType);
		repositoryType
				.setDefaultValue(SpringBootRepositoryType.PAGING_AND_SORTING);

		if (entityName != null) {
			getNamed().setDefaultValue(entityName + "Repository");
		}

		getTargetPackage().setDescription("Repository package name");

		getTargetPackage().setDefaultValue(
				javaSourceFacet.getBasePackage() + DEFAULT_REPOSITORY_PACKAGE);
	}

	@Inject
	private PersistenceOperations persistenceOperations;

	private void setupEntities(UIContext context) {
		UISelection<FileResource<?>> selection = context.getInitialSelection();
		Project project = getSelectedProject(context);
		final List<JavaResource> entities = persistenceOperations
				.getProjectEntities(project);
		repositoryEntity.setValueChoices(entities);
		int idx = -1;
		if (!selection.isEmpty()) {
			idx = entities.indexOf(selection.get());
		}
		if (idx != -1) {
			repositoryEntity.setDefaultValue(entities.get(idx));
		}
	}

	private String detectEntityPrimaryKeyType(Project project,
			JavaClassSource entityClass) throws FacetNotFoundException,
			IOException, ClassNotFoundException {
		entityPkQualifiedName = null;
		simplePkName = null;
		if (entityClass.hasAnnotation(IdClass.class)) {
			AnnotationSource<JavaClassSource> annotation = entityClass
					.getAnnotation(IdClass.class);

			entityPkQualifiedName = annotation.getLiteralValue();

			String path = entityPkQualifiedName.replaceAll("\\.", "/");
			JavaSourceFacet sourceFacet = project
					.getFacet(JavaSourceFacet.class);
			String typeName = entityPkQualifiedName;
			while (path != null) {
				JavaResource jresource = sourceFacet.getJavaResource(path);
				if (jresource != null) {
					JavaClassSource csj = jresource.getJavaType();
					if (csj.hasInterface(Serializable.class)) {
						return null;
					}

					path = csj.getSuperType().replaceAll("\\.", "/");
				} else {
					path = null;
					try (URLClassLoader cl = project.getFacet(
							ClassLoaderFacet.class).getClassLoader()) {
						Class<?> typeClas = cl.loadClass(typeName);
						if (checkForSerializable(typeClas)) {
							return null;
						}
					}
				}
			}
			return "Primary key type must implement Serializable!";
		} else {
			for (FieldSource<JavaClassSource> field : entityClass.getFields()) {
				if (field.hasAnnotation(Id.class)
						|| field.hasAnnotation(EmbeddedId.class)) {
					Type<JavaClassSource> fieldType = field.getType();

					if (fieldType.isPrimitive()) {
						return "Primitive type for primary key is not supported!";
					}

					entityPkQualifiedName = fieldType.getQualifiedName();
					simplePkName = fieldType.getSimpleName();
					
					if (fieldType.isType(Serializable.class)) {
						return null;
					}

					try (URLClassLoader cl = project.getFacet(
							ClassLoaderFacet.class).getClassLoader()) {
						if (checkForSerializable(cl
								.loadClass(entityPkQualifiedName))) {
							return null;
						}
					}

					return "Primary key type must implement Serializable!";
				}
			}
		}

		return null;
	}

	private boolean checkForSerializable(Class<?> pkClass)
			throws ClassNotFoundException {
		while (pkClass != null) {
			for (Class<?> intf : pkClass.getInterfaces()) {
				if (intf.isAssignableFrom(Serializable.class)) {
					return true;
				}
			}
			pkClass = pkClass.getSuperclass();
		}

		return false;
	}

	@Override
	public void validate(UIValidationContext validator) {
		super.validate(validator);
		try {
			if (!repositoryEntity.getValue().reify(JavaResource.class)
					.getJavaType().hasAnnotation(Entity.class)) {
				validator.addValidationError(repositoryEntity,
						"The selected class must be JPA entity");

				return;
			}

			JavaClassSource entityClass = ((JavaResource) repositoryEntity
					.getValue()).getJavaType();
			String result = null;
			try {
				result = detectEntityPrimaryKeyType(
						getSelectedProject(validator), entityClass);
			} catch (Exception e) {
				validator.addValidationError(repositoryEntity,
						"Error while validating primary key field type");
			}

			if (result != null) {
				validator.addValidationError(repositoryEntity, result);
			}
		} catch (FileNotFoundException e) {
			validator
					.addValidationError(repositoryEntity,
							"You must select existing JPA entity to create repository for");
		}
	}

	private String entityPkQualifiedName = null;
	private String simplePkName = null;

//	@Override
//	public Result execute(UIExecutionContext context) throws Exception {
//		if (entityPkQualifiedName == null) {
//			Results.fail("Unable to detect entity primary key class type!");
//		}
//
//		return super.execute(context);
//	}

	@Override
	public JavaInterfaceSource decorateSource(UIExecutionContext context,
			Project project, JavaInterfaceSource source) throws Exception {

		JavaClassSource entityClass = ((JavaResource) repositoryEntity
				.getValue()).getJavaType();

		switch (repositoryType.getValue()) {
		case CRUD:
			source.addImport("org.springframework.data.repository.CrudRepository");
			source.addInterface("CrudRepository<" + entityClass.getName() + ","
					+ simplePkName + ">");
			break;
		case PAGING_AND_SORTING:
			source.addImport("org.springframework.data.repository.PagingAndSortingRepository");
			source.addInterface("PagingAndSortingRepository<"
					+ entityClass.getName() + "," + simplePkName + ">");
			break;
		}

		if (!entityPkQualifiedName.equals(simplePkName)) {
			// Don't import int, long, double and other simple types
			source.addImport(entityPkQualifiedName);
		}
		source.addImport(entityClass.getQualifiedName());

		return source;
	}

	@Inject
	private ProjectFactory projectFactory;

	@Override
	protected ProjectFactory getProjectFactory() {
		return projectFactory;
	}

	@Override
	protected boolean isProjectRequired() {
		return true;
	}

	@Override
	protected String getType() {
		return "Interface";
	}

	@Override
	protected Class<JavaInterfaceSource> getSourceType() {
		return JavaInterfaceSource.class;
	}
}
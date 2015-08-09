package org.jboss.forge.addon.springboot.ui;

import java.io.FileNotFoundException;

import javax.inject.Inject;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.parser.java.ui.AbstractJavaSourceCommand;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.springboot.facet.SpringBootJPAFacetImpl;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.facets.HintsFacet;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

@FacetConstraint(SpringBootJPAFacetImpl.class)
public class SpringBootRepository extends
		AbstractJavaSourceCommand<JavaInterfaceSource> {

	@Inject
	@WithAttributes(label = "Repository for entity", required = true)
	private UIInput<JavaResource> repositoryEntity;

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

		repositoryEntity.getFacet(HintsFacet.class).setInputType(
				InputType.JAVA_CLASS_PICKER);
		Object selection = builder.getUIContext().getInitialSelection().get();

		String entityName = null;

		if (selection instanceof JavaResource) {
			repositoryEntity.setDefaultValue((JavaResource) selection);
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
				javaSourceFacet.getBasePackage() + ".repository");
	}

	@Override
	public void validate(UIValidationContext validator) {
		super.validate(validator);
		try {
			if (!repositoryEntity.getValue().reify(JavaResource.class)
					.getJavaType().hasAnnotation(Entity.class)) {
				validator.addValidationError(repositoryEntity,
						"The selected class must be JPA entity");
			}
		} catch (FileNotFoundException e) {
			validator.addValidationError(repositoryEntity,
					"You must select existing JPA entity to audit");
		}
	}

	private String entytPkQualifiedName = null;
	private String simplePkName = null;

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
		JavaClassSource entityClass = ((JavaResource) repositoryEntity
				.getValue()).getJavaType();

		entytPkQualifiedName = null;
		simplePkName = null;
		if (entityClass.hasAnnotation(IdClass.class)) {
			// AnnotationSource<JavaClassSource> idClassAnotation = entityClass
			// .getAnnotation(IdClass.class);
			// String valueClassName = idClassAnotation.getLiteralValue();

			Results.success("Entites with IdClass primary key are not supported!");
		} else {
			for (FieldSource<JavaClassSource> field : entityClass.getFields()) {
				if (field.hasAnnotation(Id.class)
						|| field.hasAnnotation(EmbeddedId.class)) {
					Type<JavaClassSource> fieldType = field.getType();

					entytPkQualifiedName = fieldType.getQualifiedName();
					simplePkName = fieldType.getSimpleName();

					break;
				}
			}
		}

		if (entytPkQualifiedName == null) {
			Results.fail("Unable to detect entity primary key class type!");
		}

		return super.execute(context);
	}

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

		source.addImport(entytPkQualifiedName);
		source.addImport(entityClass.getQualifiedName());

		return super.decorateSource(context, project, source);
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
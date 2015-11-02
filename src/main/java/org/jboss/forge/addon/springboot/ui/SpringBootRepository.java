package org.jboss.forge.addon.springboot.ui;

import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.parser.java.ui.AbstractJavaSourceCommand;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.springboot.facet.SpringBootFacet;
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
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import javax.inject.Inject;
import javax.persistence.Entity;
import java.io.FileNotFoundException;

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

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
		return super.execute(context);
	}

	@Override
	public JavaInterfaceSource decorateSource(UIExecutionContext context,
			Project project, JavaInterfaceSource source) throws Exception {
		JavaClassSource entityClass = ((JavaResource) repositoryEntity
				.getValue()).getJavaType();

		SpringBootFacet facet = project.getFacet(SpringBootFacet.class);

		return facet.exportRepositoryForEntity(source, entityClass, repositoryType.getValue());
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
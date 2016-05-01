package org.jboss.forge.addon.springboot.ui;

import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.javaee.jpa.PersistenceOperations;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.parser.java.ui.AbstractJavaSourceCommand;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.springboot.facet.SpringBootFacet;
import org.jboss.forge.addon.springboot.facet.SpringBootRestFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UISelection;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import javax.inject.Inject;
import javax.persistence.Entity;
import java.io.FileNotFoundException;
import java.util.List;

//@FacetConstraint(SpringBootRestFacet.class)
public class SpringBootRepositoryCommand extends
        AbstractJavaSourceCommand<JavaInterfaceSource> {

    public static final String DEFAULT_REPOSITORY_PACKAGE = ".repository";

    @Inject
    @WithAttributes(label = "For entity", required = true)
    private UISelectOne<JavaResource> forEntity;

    @Inject
    @WithAttributes(label = "Repository type", required = true)
    private UISelectOne<SpringBootRepositoryType> repositoryType;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(SpringBootRepositoryCommand.class)
                .name("Spring Boot: Repository")
                .category(Categories.create("Spring Boot"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        super.initializeUI(builder);

        UIContext context = builder.getUIContext();
        Project project = getSelectedProject(context);
        JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);

        final List<JavaResource> entities = persistenceOperations.getProjectEntities(project);
        forEntity.setValueChoices(entities);
        setDefaultTargetEntity(context.<FileResource<?>>getInitialSelection(), entities);

        getTargetPackage().setDescription("Repository package name");
        getTargetPackage().setDefaultValue(
                javaSourceFacet.getBasePackage() + DEFAULT_REPOSITORY_PACKAGE);

        repositoryType.setDefaultValue(SpringBootRepositoryType.PAGING_AND_SORTING);

        builder.add(forEntity);
        builder.add(repositoryType);
    }

    @Inject
    private PersistenceOperations persistenceOperations;

    private void setDefaultTargetEntity(UISelection<FileResource<?>> selection,
            List<JavaResource> entities)
            throws FileNotFoundException {
        if (selection.isEmpty()) {
            return;
        }
        int idx = entities.indexOf(selection.get());
        if (idx != -1) {
            final JavaResource defaultEntity = entities.get(idx);
            forEntity.setDefaultValue(defaultEntity);
            JavaClassSource entityClass = ((JavaResource) defaultEntity).getJavaType();
            getNamed().setDefaultValue(entityClass.getName() + "Repository");
        }
    }

    @Override
    public void validate(UIValidationContext validator) {
        super.validate(validator);
        try {
            if (!isTargetClassJPAEntity()) {
                validator.addValidationError(forEntity, "The selected class must be JPA entity");
            }
        } catch (FileNotFoundException e) {
            validator.addValidationError(forEntity,
                    "You must select existing JPA entity to create repository for");
        }
    }

    private boolean isTargetClassJPAEntity() throws FileNotFoundException {
        return forEntity.getValue().reify(JavaResource.class)
                .getJavaType().hasAnnotation(Entity.class);
    }

    @Inject
    private SpringBootFacet springBootFacet;

    @Override
    public JavaInterfaceSource decorateSource(UIExecutionContext context,
            Project project, JavaInterfaceSource source) throws Exception {
        return springBootFacet.exportRepositoryForEntity(source,
                forEntity.getValue().<JavaClassSource>getJavaType(),
                repositoryType.getValue());
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
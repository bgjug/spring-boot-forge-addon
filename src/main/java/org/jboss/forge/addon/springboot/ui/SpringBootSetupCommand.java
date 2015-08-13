package org.jboss.forge.addon.springboot.ui;

import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.springboot.facet.SpringBootFacet;
import org.jboss.forge.addon.springboot.spring.SpringBootMetadataRetriever;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import javax.inject.Inject;
import java.util.List;

public class SpringBootSetupCommand extends AbstractProjectCommand {

    @Inject
    @WithAttributes(label = "Application class", description = "The name of the generated applicationClass class", required = true, defaultValue = "Application")
    private UIInput<String> applicationClass;

    @Inject
    @WithAttributes(label = "Target package", type = InputType.JAVA_PACKAGE_PICKER)
    private UIInput<String> targetPackage;

    @Inject
    @WithAttributes(label = "Version", description = "The version of Spring Boot")
    private UISelectOne<String> version;

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private SpringBootMetadataRetriever metadataRetriever;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(SpringBootSetupCommand.class).name("Spring Boot: Setup")
                .category(Categories.create("Spring Boot"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Project project = getSelectedProject(builder.getUIContext());
        targetPackage.setValue(project.getFacet(JavaSourceFacet.class).getBasePackage());
        applicationClass.setDefaultValue("Application");

        final List<String> availableVersions = metadataRetriever.getAvailableVersions();
        version.setValueChoices(availableVersions);
        version.setDefaultValue(findLatestVersion(availableVersions));

        builder.add(targetPackage).add(applicationClass).add(version);
    }

    private String findLatestVersion(List<String> availableVersions) {
        for (int i = availableVersions.size() - 1; i >= 0; i--) {
            if (!availableVersions.get(i).endsWith("SNAPSHOT")) {
                return availableVersions.get(i);
            }
        }
        return availableVersions.get(availableVersions.size() - 1);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);
        if (project.hasFacet(SpringBootFacet.class)) {
            return Results.fail("Spring Boot is already installed in this project.");
        }

        metadataRetriever.setSelectedSpringBootVersion(version.getValue());
        facetFactory.install(project, SpringBootFacet.class);

        String application = this.applicationClass.getValue();
        String targetPackage = this.targetPackage.getValue();
        JavaResource javaResource = createApplicationClass(project, application, targetPackage);

        context.getUIContext().getAttributeMap().put(JavaResource.class, javaResource);
        context.getUIContext().setSelection(javaResource);
        return Results
                .success("Spring Boot version " + metadataRetriever.getSelectedSpringBootVersion()
                        + " was setup successfully");
    }

    private JavaResource createApplicationClass(Project project, String application,
            String targetPackage) {
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        JavaClassSource javaClass = createJavaClass(application, targetPackage);
        return java.saveJavaSource(javaClass);
    }

    private JavaClassSource createJavaClass(final String className, final String classPackage) {
        JavaClassSource application = Roaster.create(JavaClassSource.class).setName(className)
                .setPublic().getOrigin();

        application.addAnnotation("org.springframework.boot.autoconfigure.SpringBootApplication");

        application.addImport("org.springframework.boot.SpringApplication");
        application
                .addMethod()
                .setName("main")
                .setPublic()
                .setStatic(true)
                .setBody("SpringApplication.run(" + className + ".class, args);")
                .addParameter(String[].class, "args");

        if (classPackage != null && !classPackage.isEmpty()) {
            application.setPackage(classPackage);
        }
        return application;
    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Inject
    private ProjectFactory projectFactory;

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

}

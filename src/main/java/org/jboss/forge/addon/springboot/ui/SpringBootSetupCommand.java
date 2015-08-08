package org.jboss.forge.addon.springboot.ui;

import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.springboot.facet.SpringBootFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UISelection;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import javax.inject.Inject;

public class SpringBootSetupCommand extends AbstractProjectCommand {

    @Inject
    @WithAttributes(label = "Application class ", description = "The name of the generated application class", required = true, defaultValue = "Application")
    private UIInput<String> application;

    @Inject
    @WithAttributes(label = "Target package", type = InputType.JAVA_PACKAGE_PICKER)
    private UIInput<String> targetPackage;

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private FacetFactory facetFactory;

    @Inject
    @WithAttributes(label = "Target Directory", required = true)
    private UIInput<DirectoryResource> targetLocation;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(SpringBootSetupCommand.class).name("spring-boot-setup").category(Categories.create("Spring Boot"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Project project = getSelectedProject(builder.getUIContext());
        application.setDefaultValue("Application");
        if (project == null) {
            UISelection<FileResource<?>> currentSelection = builder.getUIContext().getInitialSelection();

            if (!currentSelection.isEmpty()) {
                FileResource<?> resource = currentSelection.get();
                if (resource instanceof DirectoryResource) {
                    targetLocation.setDefaultValue((DirectoryResource) resource);
                } else {
                    targetLocation.setDefaultValue(resource.getParent());
                }
            }
        } else {
            if (project.hasFacet(JavaSourceFacet.class)) {
                JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
                targetLocation.setDefaultValue(facet.getSourceDirectory()).setEnabled(false);

                targetPackage.setValue(project.getFacet(JavaSourceFacet.class).getBasePackage());
            }
        }
        builder.add(targetLocation).add(targetPackage).add(application);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {

        Project project = getSelectedProject(context);
        facetFactory.install(project, SpringBootFacet.class);


        DirectoryResource targetDir = targetLocation.getValue();

        JavaResource javaResource;

        String application = this.application.getValue();
        String targetPackage = this.targetPackage.getValue();
        if (project == null) {
            JavaClassSource javaClass = createJavaClass(application, targetPackage);
            javaResource = getJavaResource(targetDir, javaClass.getName());
            javaResource.setContents(javaClass);
        } else {
            JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
            JavaClassSource javaClass = createJavaClass(application, targetPackage);
            javaResource = java.saveJavaSource(javaClass);
        }
        context.getUIContext().getAttributeMap().put(JavaResource.class, javaResource);
        context.getUIContext().setSelection(javaResource);
        return Results.success("Command 'spring-boot-setup' successfully executed!");

    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    private JavaClassSource createJavaClass(final String className, final String classPackage) {
        JavaClassSource application = Roaster.create(JavaClassSource.class).setName(className).setPublic().getOrigin();

        application.addAnnotation("org.springframework.boot.autoconfigure.SpringBootApplication");


        application.addImport("org.springframework.boot.SpringApplication");
        application
                        .addMethod()
                        .setName("main")
                        .setPublic()
                        .setStatic(true)
                        .setBody("SpringApplication.run("+className+".class, args);")
                        .addParameter(String[].class, "args");

        if (classPackage != null && !classPackage.isEmpty()) {
            application.setPackage(classPackage);
        }
        return application;
    }

    private JavaResource getJavaResource(final DirectoryResource sourceDir, final String relativePath) {
        String path = relativePath.trim().endsWith(".java") ? relativePath.substring(0, relativePath.lastIndexOf(".java")) : relativePath;
        path = path.replace(".", "/") + ".java";
        JavaResource target = sourceDir.getChildOfType(JavaResource.class, path);
        return target;
    }
}
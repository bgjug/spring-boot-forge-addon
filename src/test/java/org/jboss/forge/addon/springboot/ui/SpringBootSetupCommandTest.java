package org.jboss.forge.addon.springboot.ui;

import org.apache.maven.model.Parent;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.projects.JavaWebProjectType;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.springboot.facet.SpringBootFacet;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.statements.Fail;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.jboss.forge.addon.springboot.spring.SpringBootMetadataRetriever.SPRING_BOOT_GROUP_ID;
import static org.jboss.forge.addon.springboot.spring.SpringBootMetadataRetriever.SPRING_BOOT_PARENT_ARTIFACT_ID;
import static org.jboss.forge.addon.springboot.spring.SpringBootMetadataRetriever.SPRING_BOOT_STARTER_WEB_ARTIFACT_ID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="ivan.st.ivanov@gmail.com">Ivan St. Ivanov</a>
 */
@RunWith(Arquillian.class)
public class SpringBootSetupCommandTest {

    @Inject
    protected UITestHarness testHarness;

    @Inject
    private ShellTest shellTest;

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    protected JavaWebProjectType javaWebProjectType;

    private Project project;

    @Deployment
    @AddonDependencies
    public static AddonArchive getDeployment() {
        return ShrinkWrap
                .create(AddonArchive.class)
                .addBeansXML()
                .addClass(SpringBootSetupCommand.class);
    }

    @Before
    public void setup() {
        this.project = projectFactory.createTempProject(javaWebProjectType.getRequiredFacets());
    }

    @Test
    public void shouldHaveSpringBootFacet() throws Exception {
        assertFalse(executeSetupCommand() instanceof Fail);
        assertTrue(project.hasFacet(SpringBootFacet.class));
    }

    @Test
    public void shouldAddDefaultParentDependenciesAndApplicationClass() throws Exception {
        assertFalse(executeSetupCommand() instanceof Fail);
        assertParentProject();
        assertWebStarterDependency();
        assertApplicationClass(project.getFacet(JavaSourceFacet.class).getBasePackage(),
                "Application");
    }

    @Test
    public void shouldAddCustomSpringBootVersion() throws Exception {
        final String version = "1.2.12.RELEASE";
        assertFalse(executeSetupCommand(version, null, null) instanceof Failed);
        assertVersion(version);
    }

    @Test
    public void shouldAddCustomApplicationClassAndPackage() throws Exception {
        String targetPackage = "bg.jug.sample";
        String applicationClass = "SampleApplication";
        assertFalse(executeSetupCommand(null, targetPackage, applicationClass
        ) instanceof Failed);
        assertApplicationClass(targetPackage, applicationClass);
    }

    @Test
    public void shouldSupportShellCommands() throws Exception {
        String version = "1.2.13.RELEASE";
        String targetPackage = "bg.jug.springboot";
        String applicationClass = "MyApplicationClass";

        shellTest.getShell().setCurrentResource(project.getRoot());
        Result result = shellTest.execute(
                "spring-boot-setup"
                        + " --version " + version
                        + " --targetPackage " + targetPackage
                        + " --applicationClass " + applicationClass,
                10, TimeUnit.SECONDS);

        assertFalse(result instanceof Failed);
        assertVersion(version);
        assertApplicationClass(targetPackage, applicationClass);
    }

    @Test
    public void reinstallingFacetShouldFail() throws Exception {
        executeSetupCommand();
        assertTrue(executeSetupCommand() instanceof Failed);
    }

    private Result executeSetupCommand() throws Exception {
        return executeSetupCommand(null, null, null);
    }

    private Result executeSetupCommand(String springBootVersion, String targetPackage,
            String applicationClass) throws Exception {
        try (CommandController commandController = testHarness
                .createCommandController(SpringBootSetupCommand.class, project.getRoot())) {
            commandController.initialize();
            if (springBootVersion != null) {
                commandController.setValueFor("version", springBootVersion);
            }
            if (applicationClass != null) {
                commandController.setValueFor("applicationClass", applicationClass);
            }
            if (targetPackage != null) {
                commandController.setValueFor("targetPackage", targetPackage);
            }
            return commandController.execute();
        }
    }

    private void assertParentProject() {
        Parent parent = getProjectParent();
        assertNotNull(parent);
        assertThat(parent.getGroupId(), is(SPRING_BOOT_GROUP_ID));
        assertThat(parent.getArtifactId(), is(SPRING_BOOT_PARENT_ARTIFACT_ID));
    }

    private void assertWebStarterDependency() {
        DependencyFacet dependencyFacet = project.getFacet(DependencyFacet.class);
        DependencyBuilder dependencyBuilder = DependencyBuilder.create(
                SPRING_BOOT_GROUP_ID + ":" + SPRING_BOOT_STARTER_WEB_ARTIFACT_ID);
        Dependency springBootWebStarter = dependencyFacet.getDirectDependency(dependencyBuilder);
        assertNotNull(springBootWebStarter);
        assertNull(dependencyBuilder.getCoordinate().getVersion());
    }

    private void assertVersion(String version) {
        Parent projectParent = getProjectParent();
        assertThat(projectParent.getVersion(), is(version));
    }

    private void assertApplicationClass(String targetPackage, String applicationClass) {
        JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
        DirectoryResource targetPackageDirectory = javaSourceFacet.getSourceDirectory()
                .getChildDirectory(targetPackage.replace('.', File.separatorChar));
        assertTrue(targetPackageDirectory.exists());
        assertTrue(targetPackageDirectory.getChild(applicationClass + ".java").exists());
    }

    private Parent getProjectParent() {
        return project.getFacet(MavenFacet.class).getModel().getParent();
    }
}

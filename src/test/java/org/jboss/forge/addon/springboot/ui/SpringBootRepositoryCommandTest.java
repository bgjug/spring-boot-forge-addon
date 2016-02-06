package org.jboss.forge.addon.springboot.ui;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.springboot.descriptors.SpringBootDescriptor;
import org.jboss.forge.addon.springboot.descriptors.SpringBootPropertiesDescriptor;
import org.jboss.forge.addon.springboot.facet.SpringBootFacet;
import org.jboss.forge.addon.springboot.facet.SpringBootJPAFacetImpl;
import org.jboss.forge.addon.springboot.facet.SpringBootRestFacet;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.roaster.model.JavaInterface;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.jboss.forge.addon.javaee.JavaEEPackageConstants.DEFAULT_ENTITY_PACKAGE;
import static org.jboss.forge.addon.springboot.ui.SpringBootRepositoryCommand.DEFAULT_REPOSITORY_PACKAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class SpringBootRepositoryCommandTest {

    public static final String FOR_ENTITY = "forEntity";
    public static final String REPOSITORY_TYPE = "repositoryType";
    public static final String TARGET_PACKAGE = "targetPackage";
    public static final String NAMED = "named";
    public static final String OVERWRITE = "overwrite";

    @Deployment
    @AddonDependencies
    public static AddonArchive getDeployment() {
        return ShrinkWrap.create(AddonArchive.class)
                .addBeansXML().addClass(ProjectHelper.class)
                .addClass(SpringBootRepositoryCommand.class)
                .addClass(SpringBootSetupCommand.class)
                .addClass(SpringBootSetupJPACommand.class)
                .addClass(SpringBootJPAFacetImpl.class)
                .addClass(SpringBootRestFacet.class)
                .addClass(SpringBootFacet.class)
                .addClass(SpringBootDescriptor.class)
                .addClass(SpringBootPropertiesDescriptor.class)
                .addClass(SpringBootSetupRestCommand.class)
                .addClass(SpringBootRepositoryType.class);
    }

    private Project project;

    @Inject
    private UITestHarness uiTestHarness;

    @Inject
    private ShellTest shellTest;

    @Inject
    private ProjectHelper projectHelper;

    @Before
    public void setUp() throws IOException {
        project = projectHelper.createSpringBootProject();
        projectHelper.createJPAEntity(project, "Customer");
    }

    @Test
    public void checkCommandMetadata() throws Exception {
        try (CommandController controller = uiTestHarness
                .createCommandController(SpringBootRepositoryCommand.class,
                        project.getRoot())) {
            controller.initialize();
            assertTrue(controller.getCommand() instanceof SpringBootRepositoryCommand);
            UICommandMetadata metadata = controller.getMetadata();
            assertEquals("Spring Boot: Repository", metadata.getName());
            assertEquals("Spring Boot", metadata.getCategory().getName());
            assertEquals(5, controller.getInputs().size());
            assertTrue(controller.hasInput(FOR_ENTITY));
            assertTrue(controller.hasInput(REPOSITORY_TYPE));
            assertTrue(controller.hasInput(TARGET_PACKAGE));
            assertTrue(controller.hasInput(NAMED));
            assertTrue(controller.hasInput(OVERWRITE));
        }
    }

    @Test
    public void testRepositoryCommandWithCrudRepository() throws Exception {
        final String entityName = "Customer";
        projectHelper.createJPAEntity(project, entityName);
        JavaResource repositoryResource = executeRepositoryCommand(entityName, "CustomerRepository",
                SpringBootRepositoryType.CRUD);

        assertNotNull(repositoryResource);
        assertThat(repositoryResource.getJavaType(),
                is(instanceOf(JavaInterface.class)));

        JavaInterfaceSource intf = repositoryResource.getJavaType();
        assertEquals(intf.getInterfaces().size(), 1);
        String interfaceName = intf.getInterfaces().get(0);
        assertThat(interfaceName, is("org.springframework.data.repository.CrudRepository"));
    }

    @Test(expected = IllegalStateException.class)
    public void testRepositoryCommandFailsWhenAppliedToNonEntity() throws Exception {
        final String nonEntityClassName = "NonEntity";
        projectHelper.createEmptyJavaClass(project,
                getEntityPackageName(project.getFacet(JavaSourceFacet.class).getBasePackage()),
                nonEntityClassName);
        executeRepositoryCommand(nonEntityClassName, "NonEntityRepository",
                SpringBootRepositoryType.CRUD);
    }

    private JavaResource executeRepositoryCommand(String forEntity, String repositoryName,
            SpringBootRepositoryType repositoryType) throws Exception {
        try (CommandController controller = uiTestHarness
                .createCommandController(SpringBootRepositoryCommand.class,
                        project.getRoot())) {
            controller.initialize();
            JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
            String basePackageName = facet.getBasePackage();
            String entityPackageName = getEntityPackageName(basePackageName);
            String repositoryPackageName = basePackageName + DEFAULT_REPOSITORY_PACKAGE;

            controller.setValueFor(FOR_ENTITY, entityPackageName + "." + forEntity);
            controller.setValueFor(NAMED, repositoryName);
            controller.setValueFor(TARGET_PACKAGE, basePackageName + DEFAULT_REPOSITORY_PACKAGE);
            controller.setValueFor(REPOSITORY_TYPE, repositoryType);

            controller.execute();

            return facet.getJavaResource(repositoryPackageName + "." + repositoryName);

        }
    }

    private String getEntityPackageName(String basePackageName) {
        return basePackageName + "." + DEFAULT_ENTITY_PACKAGE;
    }
}

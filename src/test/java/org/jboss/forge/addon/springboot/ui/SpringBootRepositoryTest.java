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
import static org.jboss.forge.addon.springboot.ui.SpringBootRepository.DEFAULT_REPOSITORY_PACKAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class SpringBootRepositoryTest {

	@Deployment
	@AddonDependencies
	public static AddonArchive getDeployment() {
        return ShrinkWrap.create(AddonArchive.class)
                .addBeansXML().addClass(ProjectHelper.class)
                .addClass(SpringBootRepository.class)
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
				.createCommandController(SpringBootRepository.class,
						project.getRoot())) {
			controller.initialize();
			// Checks the command metadata
			assertTrue(controller.getCommand() instanceof SpringBootRepository);
			UICommandMetadata metadata = controller.getMetadata();
			assertEquals("Spring Boot: Repository", metadata.getName());
			assertEquals("Spring", metadata.getCategory().getName());
			assertEquals("Boot", metadata.getCategory().getSubCategory()
					.getName());
			assertEquals(5, controller.getInputs().size());
			assertTrue(controller.hasInput("repositoryEntity"));
			assertTrue(controller.hasInput("repositoryType"));
			assertTrue(controller.hasInput("targetPackage"));
			assertTrue(controller.hasInput("named"));
			assertTrue(controller.hasInput("overwrite"));
		}
	}

    @Test
    public void testRepositoryCommandWithCrudRepository() throws Exception {
        try (CommandController controller = uiTestHarness
                .createCommandController(SpringBootRepository.class,
                        project.getRoot())) {
            controller.initialize();
            String basePackageName = project.getFacet(JavaSourceFacet.class)
                    .getBasePackage();
            String entityPackageName = basePackageName + "."
                    + DEFAULT_ENTITY_PACKAGE;
            String repositoryPackageName = basePackageName
                    + DEFAULT_REPOSITORY_PACKAGE;

            controller.setValueFor("repositoryEntity", entityPackageName + ".Customer");
            controller.setValueFor("named", "CustomerRepository");
            controller.setValueFor("targetPackage", repositoryPackageName);
            controller.setValueFor("repositoryType", SpringBootRepositoryType.CRUD);

            controller.execute();

            JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
            JavaResource repositoryResuource = facet
                    .getJavaResource(repositoryPackageName + ".CustomerRepository");
            assertNotNull(repositoryResuource);
            assertThat(repositoryResuource.getJavaType(),
                    is(instanceOf(JavaInterface.class)));

            JavaInterfaceSource intf = repositoryResuource.getJavaType();

            assertEquals(intf.getInterfaces().size(), 1);
            String interfaceName = intf.getInterfaces().get(0);
            assertThat(interfaceName, is("org.springframework.data.repository.CrudRepository"));
        }
    }

	@Test
	public void testRepositoryCommandWithPagingAndSortingRepository() throws Exception {
		String basePackageName = project.getFacet(JavaSourceFacet.class)
				.getBasePackage();
		String entityPackageName = basePackageName + "."
				+ DEFAULT_ENTITY_PACKAGE;
		String repositoryPackageName = basePackageName + DEFAULT_REPOSITORY_PACKAGE;
        try (CommandController controller = uiTestHarness
                .createCommandController(SpringBootRepository.class,
                        project.getRoot())) {
            controller.initialize();
            controller.setValueFor("repositoryEntity", entityPackageName + ".Customer");
            controller.setValueFor("named", "CustomerRepository");
            controller.setValueFor("targetPackage", repositoryPackageName);
            controller.setValueFor("repositoryType", SpringBootRepositoryType.PAGING_AND_SORTING);
            controller.execute();

            JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
            JavaResource repositoryResource = facet
                    .getJavaResource(repositoryPackageName + ".CustomerRepository");
            assertNotNull(repositoryResource);
            assertThat(repositoryResource.getJavaType(),
                    is(instanceOf(JavaInterface.class)));

            JavaInterfaceSource intf = repositoryResource.getJavaType();
            assertEquals(intf.getInterfaces().size(), 1);
            String interfaceName = intf.getInterfaces().get(0);
            assertEquals("org.springframework.data.repository.PagingAndSortingRepository", interfaceName);
        }
    }
}

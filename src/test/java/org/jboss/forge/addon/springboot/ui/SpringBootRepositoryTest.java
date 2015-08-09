package org.jboss.forge.addon.springboot.ui;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.springboot.facet.SpringBootFacet;
import org.jboss.forge.addon.springboot.facet.SpringBootJPAFacetImpl;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class SpringBootRepositoryTest {

	@Deployment
	@AddonDependencies({
			@AddonDependency(name = "org.jboss.forge.addon:ui-test-harness"),
			@AddonDependency(name = "org.jboss.forge.addon:shell-test-harness"),
			@AddonDependency(name = "org.jboss.forge.addon:maven"),
			@AddonDependency(name = "org.jboss.forge.furnace.container:cdi") })
	public static AddonArchive getDeployment() {
		AddonArchive result = ShrinkWrap.create(AddonArchive.class);
		return result;
	}

	private Project project;

	@Inject
	ProjectFactory projectFactory;

	@Inject
	FacetFactory facetFactory;

	@Before
	public void setUp() {
		project = projectFactory.createTempProject();

		facetFactory.install(project, SpringBootFacet.class);
		facetFactory.install(project, SpringBootJPAFacetImpl.class);
	}

	@Test
	public void testCrud() {
	}

	@Test
	public void testPaginAndSorting() {

	}
}

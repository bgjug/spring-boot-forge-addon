package org.jboss.forge.addon.springboot.facet;

import javax.inject.Inject;

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.DependencyFacet;

@FacetConstraint(SpringBootJPAFacetImpl.class)
public class SpringBootRestFacet extends AbstractFacet<Project> implements
		ProjectFacet {

	public SpringBootRestFacet() {

	}

	@Inject
	private DependencyInstaller dependencyInstaller;

	public static Dependency[] DEPENDENCIES = { DependencyBuilder.create(
			"org.springframework.data").setArtifactId("spring-data-rest-core") };

	@Override
	public boolean install() {
		installDependencies();
		return true;
	}

	private void installDependencies() {
		for (Dependency dependency : DEPENDENCIES) {
			dependencyInstaller.install(getFaceted(), dependency);
		}
	}

	@Override
	public boolean isInstalled() {
		DependencyFacet facet = getFaceted().getFacet(DependencyFacet.class);

		// application.properties has to exist
		boolean result = true;

		// check whether all dependencies are in place
		for (Dependency dependency : DEPENDENCIES) {
			result = result && facet.hasEffectiveDependency(dependency);
		}

		return result;
	}
}

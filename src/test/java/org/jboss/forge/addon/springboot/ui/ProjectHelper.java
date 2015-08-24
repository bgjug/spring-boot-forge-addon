package org.jboss.forge.addon.springboot.ui;

import static org.jboss.forge.addon.javaee.JavaEEPackageConstants.DEFAULT_ENTITY_PACKAGE;

import java.io.IOException;

import javax.inject.Inject;
import javax.persistence.GenerationType;

import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.javaee.jpa.PersistenceOperations;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.projects.JavaWebProjectType;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.springboot.facet.SpringBootFacet;
import org.jboss.forge.addon.springboot.facet.SpringBootJPAFacetImpl;
import org.jboss.forge.addon.springboot.facet.SpringBootRestFacet;
import org.jboss.forge.addon.springboot.spring.SpringBootMetadataRetriever;

public class ProjectHelper {

	@Inject
	private PersistenceOperations persistenceOperations;

	@Inject
	private FacetFactory facetFactory;

	@Inject
	private ProjectFactory projectFactory;

	@Inject
	private SpringBootMetadataRetriever retriever;

	@Inject
	protected JavaWebProjectType javaWebProjectType;

	public JavaResource createJPAEntity(Project project, String entityName)
			throws IOException {
		String packageName = project.getFacet(JavaSourceFacet.class)
				.getBasePackage() + "." + DEFAULT_ENTITY_PACKAGE;
		return persistenceOperations.newEntity(project, entityName,
				packageName, GenerationType.AUTO);
	}

	public Project createSpringBootProject() {
		Project project = projectFactory.createTempProject(javaWebProjectType
				.getRequiredFacets());
		retriever.setSelectedSpringBootVersion("1.2.5.RELEASE");

		facetFactory.install(project, SpringBootFacet.class);
		facetFactory.install(project, SpringBootJPAFacetImpl.class);
		facetFactory.install(project, SpringBootRestFacet.class);
		return project;
	}
	
	public void addSpringRestFacet(Project project) {
		facetFactory.install(project, SpringBootRestFacet.class);
	}
}

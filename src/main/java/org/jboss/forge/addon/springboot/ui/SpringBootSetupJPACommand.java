package org.jboss.forge.addon.springboot.ui;

import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.springboot.facet.SpringBootFacet;
import org.jboss.forge.addon.springboot.facet.SpringBootJPAFacetImpl;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

import java.lang.Override;
import java.lang.Exception;

import javax.inject.Inject;

@FacetConstraint(SpringBootFacet.class)
public class SpringBootSetupJPACommand extends AbstractProjectCommand {

	@Inject
	private ProjectFactory projectFactory;

	@Inject
	private FacetFactory facetFactory;

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(SpringBootSetupJPACommand.class)
				.name("Spring Boot: Setup JPA")
				.category(Categories.create("SpringBoot"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
		facetFactory.install(getSelectedProject(context),
				SpringBootJPAFacetImpl.class);
		return Results
				.success("Command 'Spring Boot: Setup JPA' successfully executed!");
	}

	@Override
	protected boolean isProjectRequired() {
		return true;
	}

	@Override
	protected ProjectFactory getProjectFactory() {
		return projectFactory;
	}
}
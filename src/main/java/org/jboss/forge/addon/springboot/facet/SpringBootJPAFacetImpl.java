package org.jboss.forge.addon.springboot.facet;

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.javaee.JavaEEPackageConstants;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.parser.java.resources.JavaResourceVisitor;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.visit.VisitContext;
import org.jboss.forge.addon.springboot.descriptors.SpringBootDescriptor;
import org.jboss.forge.addon.springboot.descriptors.SpringBootPropertiesDescriptor;
import org.jboss.forge.furnace.versions.Version;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import javax.inject.Inject;
import javax.persistence.Entity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="ivan.st.ivanov@gmail.com">Ivan St. Ivanov</a>
 */
@FacetConstraint(SpringBootFacet.class)
public class SpringBootJPAFacetImpl extends AbstractFacet<Project> implements
		JPAFacet<SpringBootDescriptor> {

	@Inject
	private DependencyInstaller dependencyInstaller;

	public static Dependency[] DEPENDENCIES = {
			DependencyBuilder.create("org.springframework.data").setArtifactId(
					"spring-data-jpa"),
			DependencyBuilder.create("org.hibernate").setArtifactId(
					"hibernate-entitymanager") };

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
		boolean result = getConfigFile().exists();

		// check whether all dependencies are in place
		for (Dependency dependency : DEPENDENCIES) {
			result = result && facet.hasEffectiveDependency(dependency);
		}

		return result;
	}

	@Override
	public String getEntityPackage() {
		JavaSourceFacet sourceFacet = getFaceted().getFacet(
				JavaSourceFacet.class);
		return sourceFacet.getBasePackage() + "."
				+ JavaEEPackageConstants.DEFAULT_ENTITY_PACKAGE;
	}

	@Override
	public DirectoryResource getEntityPackageDir() {
		JavaSourceFacet sourceFacet = getFaceted().getFacet(
				JavaSourceFacet.class);

		DirectoryResource entityRoot = sourceFacet.getBasePackageDirectory()
				.getChildDirectory(
						JavaEEPackageConstants.DEFAULT_ENTITY_PACKAGE);
		if (!entityRoot.exists()) {
			entityRoot.mkdirs();
		}

		return entityRoot;
	}

	@Override
	public List<JavaClassSource> getAllEntities() {
		final List<JavaClassSource> result = new ArrayList<>();
		JavaSourceFacet javaSourceFacet = getFaceted().getFacet(
				JavaSourceFacet.class);
		javaSourceFacet.visitJavaSources(new JavaResourceVisitor() {
			@Override
			public void visit(VisitContext context, JavaResource resource) {
				try {
					JavaType<?> type = resource.getJavaType();
					if (type.hasAnnotation(Entity.class) && type.isClass()) {
						result.add((JavaClassSource) type);
					}
				} catch (FileNotFoundException e) {
					throw new IllegalStateException(e);
				}
			}
		});

		return result;
	}

	@Override
	public SpringBootDescriptor getConfig() {
		final FileResource<?> configFile = getConfigFile();
		Properties properties = null;
		if (!configFile.exists()) {
			properties = new Properties();
		} else {
			properties = loadProperties(configFile.getResourceInputStream());
		}
		SpringBootDescriptor descriptor = new SpringBootPropertiesDescriptor(
				properties);
		return descriptor;
	}

	private Properties loadProperties(InputStream inputStream) {
		Properties result = new Properties();
		try {
			result.load(inputStream);
		} catch (IOException e) {
			// TODO: anything better to do?
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	public FileResource<?> getConfigFile() {
		ResourcesFacet resources = getFaceted().getFacet(ResourcesFacet.class);
		return (FileResource<?>) resources.getResourceDirectory().getChild(
				"application.properties");
	}

	@Override
	public void saveConfig(SpringBootDescriptor descriptor) {
		String output = descriptor.exportAsString();
		getConfigFile().setContents(output);
	}

	@Override
	public Version getSpecVersion() {
		// TODO: which JPA version do we stick to
		return null;
	}

	@Override
	public String getSpecName() {
		return "JPA";
	}
}

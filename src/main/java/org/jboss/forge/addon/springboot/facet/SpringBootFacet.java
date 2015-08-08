package org.jboss.forge.addon.springboot.facet;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;

import javax.inject.Inject;

/**
 * @author <a href="ivan.st.ivanov@gmail.com">Ivan St. Ivanov</a>
 */
public class SpringBootFacet extends AbstractFacet<Project> implements ProjectFacet {

    @Inject
    private DependencyInstaller dependencyInstaller;


    @Override
    public boolean install() {

        Project project = getFaceted();
        MavenFacet maven = project.getFacet(MavenFacet.class);
        Parent parent = new Parent();

        parent.setGroupId("org.springframework.boot");
        parent.setArtifactId("spring-boot-starter-parent");
        parent.setVersion("1.2.5.RELEASE");

        Model model = maven.getModel();
        model.setParent(parent);

        maven.setModel(model);

        dependencyInstaller.install(project, DependencyBuilder.create("org.springframework.boot:spring-boot-starter-web"));

        return true;
    }

    @Override
    public boolean isInstalled() {
        return false;
    }
}

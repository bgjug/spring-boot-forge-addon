package org.jboss.forge.addon.springboot.facet;

import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;

/**
 * @author <a href="ivan.st.ivanov@gmail.com">Ivan St. Ivanov</a>
 */
public class SpringBootFacet extends AbstractFacet<Project> implements ProjectFacet {


    @Override
    public boolean install() {
        return false;
    }

    @Override
    public boolean isInstalled() {
        return false;
    }
}

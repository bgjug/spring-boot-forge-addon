package org.jboss.forge.addon.springboot.facet;

import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.javaee.JavaEEPackageConstants;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.parser.java.resources.JavaResourceVisitor;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.visit.VisitContext;
import org.jboss.forge.furnace.versions.Version;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import javax.persistence.Entity;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="ivan.st.ivanov@gmail.com">Ivan St. Ivanov</a>
 */
@FacetConstraint(SpringBootFacet.class)
public class SpringBootJPAFacetImpl extends AbstractFacet<Project> implements JPAFacet {


    @Override
    public boolean install() {
        return false;
    }

    @Override
    public boolean isInstalled() {
        return false;
    }

    @Override
    public String getEntityPackage() {
        JavaSourceFacet sourceFacet = getFaceted().getFacet(JavaSourceFacet.class);
        return sourceFacet.getBasePackage() + "." + JavaEEPackageConstants.DEFAULT_ENTITY_PACKAGE;
    }

    @Override
    public DirectoryResource getEntityPackageDir() {
        JavaSourceFacet sourceFacet = getFaceted().getFacet(JavaSourceFacet.class);

        DirectoryResource entityRoot = sourceFacet.getBasePackageDirectory().getChildDirectory(
                JavaEEPackageConstants.DEFAULT_ENTITY_PACKAGE);
        if (!entityRoot.exists())
        {
            entityRoot.mkdirs();
        }

        return entityRoot;
    }

    @Override
    public List<JavaClassSource> getAllEntities() {
        final List<JavaClassSource> result = new ArrayList<>();
        JavaSourceFacet javaSourceFacet = getFaceted().getFacet(JavaSourceFacet.class);
        javaSourceFacet.visitJavaSources(new JavaResourceVisitor()
        {
            @Override
            public void visit(VisitContext context, JavaResource resource)
            {
                try
                {
                    JavaType<?> type = resource.getJavaType();
                    if (type.hasAnnotation(Entity.class) && type.isClass())
                    {
                        result.add((JavaClassSource) type);
                    }
                }
                catch (FileNotFoundException e)
                {
                    throw new IllegalStateException(e);
                }
            }
        });

        return result;
    }

    @Override
    public Object getConfig() {
        return null;
    }

    @Override
    public FileResource<?> getConfigFile() {
        return null;
    }

    @Override
    public void saveConfig(Object o) {

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

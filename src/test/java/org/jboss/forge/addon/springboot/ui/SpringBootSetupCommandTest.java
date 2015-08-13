package org.jboss.forge.addon.springboot.ui;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.parser.java.projects.JavaWebProjectType;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="ivan.st.ivanov@gmail.com">Ivan St. Ivanov</a>
 */
@RunWith(Arquillian.class)
public class SpringBootSetupCommandTest {

    @Inject
    protected UITestHarness testHarness;

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    protected JavaWebProjectType javaWebProjectType;

    private Project project;

    @Deployment
    @AddonDependencies
    public static AddonArchive getDeployment()
    {
        return ShrinkWrap
                .create(AddonArchive.class)
                .addBeansXML()
                .addClass(SpringBootSetupCommand.class)
                .addClass(ProjectFactory.class)
                .addClass(JavaWebProjectType.class);
    }

    @Before
    public void setup() {
        this.project = projectFactory.createTempProject(javaWebProjectType.getRequiredFacets());
    }

    @Test
    public void shouldHaveSpringBootFacet() {
        assertThat(project, is(not(null)));
    }
}

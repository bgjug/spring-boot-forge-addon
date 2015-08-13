package org.jboss.forge.addon.springboot.spring;

import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="ivan.st.ivanov@gmail.com">Ivan St. Ivanov</a>
 */
@ApplicationScoped
public class SpringBootMetadataRetriever {

    public static final String SPRING_BOOT_GROUP_ID = "org.springframework.boot";
    public static final String SPRING_BOOT_PARENT_ARTIFACT_ID = "spring-boot-starter-parent";
    public static final String SPRING_BOOT_STARTER_WEB_ARTIFACT_ID = "spring-boot-starter-web";

    @Inject
    private DependencyResolver dependencyResolver;

    public List<String> getAvailableVersions() {
        CoordinateBuilder springBootParentCoordinate = DependencyBuilder.create()
                .setGroupId(SPRING_BOOT_GROUP_ID)
                .setArtifactId(SPRING_BOOT_PARENT_ARTIFACT_ID)
                .getCoordinate();
        List<Coordinate> availableCoordinates = dependencyResolver.resolveVersions(
                DependencyQueryBuilder.create(springBootParentCoordinate));
        List<String> availableVersions = new ArrayList<>(availableCoordinates.size());
        for (Coordinate coordinate : availableCoordinates) {
            availableVersions.add(coordinate.getVersion());
        }
        return availableVersions;
    }

    private String selectedSpringBootVersion;

    public String getSelectedSpringBootVersion() {
        return selectedSpringBootVersion;
    }

    public void setSelectedSpringBootVersion(String selectedSpringBootVersion) {
        this.selectedSpringBootVersion = selectedSpringBootVersion;
    }
}

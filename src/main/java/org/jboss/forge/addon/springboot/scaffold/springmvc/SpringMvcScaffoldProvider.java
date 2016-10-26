/**
 *
 */
package org.jboss.forge.addon.springboot.scaffold.springmvc;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.Id;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;
import org.jboss.forge.addon.configuration.Configuration;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.scaffold.spi.AccessStrategy;
import org.jboss.forge.addon.scaffold.spi.ScaffoldGenerationContext;
import org.jboss.forge.addon.scaffold.spi.ScaffoldProvider;
import org.jboss.forge.addon.scaffold.spi.ScaffoldSetupContext;
import org.jboss.forge.addon.scaffold.util.ScaffoldUtil;
import org.jboss.forge.addon.springboot.facet.SpringBootFacet;
import org.jboss.forge.addon.springboot.scaffold.springmvc.freemarker.FreemarkerTemplateProcessor;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Field;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MemberSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.util.Types;

import freemarker.template.Template;

/**
 * The scaffold provider for SpringMVC
 */
public class SpringMvcScaffoldProvider implements ScaffoldProvider
{
   private static final String INDEX_JSP = "/index.jsp";
   private static final String INDEX_JSP_FTL = "/scaffold/springmvc/index.jsp.ftl";
   
   private static final String CONTROLLER_BEAN_TEMPLATE = "/scaffold/springmvc/BeanController.java.ftl";
   private static final String LIST_JSP_TEMPLATE = "/scaffold/springmvc/bean-list.jsp.ftl";
   private static final String FORM_JSP_TEMPLATE = "/scaffold/springmvc/bean-form.jsp.ftl";
   
   protected FreemarkerTemplateProcessor templateProcessor;
   
   protected Template controllerBeanTemplate;
   protected Template listJspTemplate;
   protected Template formJspTemplate;

   private Configuration config;
   private Project project;
   private DependencyInstaller dependencyInstaller;

   @Inject
   public SpringMvcScaffoldProvider(final Configuration config, final FreemarkerTemplateProcessor templateProcessor,
		   final DependencyInstaller dependencyInstaller)
   {
      this.config = config;
      this.templateProcessor = templateProcessor;
      this.dependencyInstaller = dependencyInstaller;
   }

   private void setProject(Project project)
   {
      this.project = project;
   }

   @Override
   public String getName()
   {
      return "SpringMVC(JSP)";
   }

   @Override
   public String getDescription()
   {
      return "Scaffold a SpringMVC project from JPA entities";
   }

   @Override
   public List<Resource<?>> setup(ScaffoldSetupContext setupContext)
   {
      setProject(setupContext.getProject());
      String targetDir = setupContext.getTargetDirectory();
      
		Dependency dependency = DependencyBuilder
				.create("org.apache.tomcat.embed")
				.setArtifactId("tomcat-embed-jasper").setScopeType("provided");
		if (!project.getFacet(DependencyFacet.class).hasDirectDependency(
				dependency)) {
			dependencyInstaller.install(project, dependency);
		}

		dependency = DependencyBuilder.create("javax.servlet")
				.setArtifactId("jstl").setScopeType("provided");
		if (!project.getFacet(DependencyFacet.class).hasDirectDependency(
				dependency)) {
			dependencyInstaller.install(project, dependency);
		}
      
      Resource<?> template = null;
      List<Resource<?>> resources = generateIndex(targetDir, template);
      return resources;
   }

   @Override
   public boolean isSetup(ScaffoldSetupContext setupContext)
   {
      Project project = setupContext.getProject();
      setProject(project);
      String targetDir = setupContext.getTargetDirectory();
      targetDir = targetDir == null ? "" : targetDir;
      if (project.hasAllFacets(WebResourcesFacet.class, DependencyFacet.class, JPAFacet.class, SpringBootFacet.class))
      {
         WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
         boolean areResourcesInstalled = web.getWebResource(targetDir + INDEX_JSP).exists();
         return areResourcesInstalled;
      }
      return false;
   }

   @Override
   public List<Resource<?>> generateFrom(ScaffoldGenerationContext generationContext)
   {
      setProject(generationContext.getProject());
      List<Resource<?>> generatedResources = new ArrayList<Resource<?>>();
      Collection<?> resources = generationContext.getResources();
      for (Object resource : resources)
      {
         JavaSource<?> javaSource = null;
         if (resource instanceof JavaResource)
         {
            JavaResource javaResource = (JavaResource) resource;
            try
            {
               javaSource = javaResource.getJavaType();
            }
            catch (FileNotFoundException fileEx)
            {
               throw new IllegalStateException(fileEx);
            }
         }
         else
         {
            continue;
         }

         JavaClassSource entity = (JavaClassSource) javaSource;
         //TODO check that entity has repository generated IF not generate it using spring-boot-repository
         //FIXME for now we expect there is a repository generated
         String targetDir = generationContext.getTargetDirectory();
         targetDir = (targetDir == null) ? "" : targetDir;
         config.setProperty(SpringMvcScaffoldProvider.class.getName() + "_targetDir", targetDir);
         String repositoryPackage = (String) generationContext.getAttribute("repositoryPackage");
         List<Resource<?>> generatedResourcesForEntity = this.generateFromEntity(targetDir, repositoryPackage, entity);
//
//         // TODO give plugins a chance to react to generated resources, use event bus?
//         // if (!generatedResources.isEmpty())
//         // {
//         // generatedEvent.fire(new ScaffoldGeneratedResources(provider, prepareResources(generatedResources)));
//         // }
//         generatedResources.addAll(generatedResourcesForEntity);
      }
      return generatedResources;
   }

   @Override
   public NavigationResult getSetupFlow(ScaffoldSetupContext setupContext)
   {
      Project project = setupContext.getProject();
      setProject(setupContext.getProject());
      NavigationResultBuilder builder = NavigationResultBuilder.create();
//      List<Class<? extends UICommand>> setupCommands = new ArrayList<>();
//      if (!project.hasFacet(JPAFacet.class))
//      {
//         builder.add(JPASetupWizard.class);
//      }
//      if (!project.hasFacet(CDIFacet.class))
//      {
//         setupCommands.add(CDISetupCommand.class);
//      }
//      if (!project.hasFacet(EJBFacet.class))
//      {
//         setupCommands.add(EJBSetupWizard.class);
//      }
//      if (!project.hasFacet(ServletFacet.class))
//      {
//         // TODO: FORGE-1296. Ensure that this wizard only sets up Servlet 3.0+
//         setupCommands.add(ServletSetupWizard.class);
//      }
//      if (!project.hasFacet(FacesFacet.class))
//      {
//         setupCommands.add(FacesSetupWizard.class);
//      }
//
//      Metadata compositeSetupMetadata = Metadata.forCommand(ScaffoldSetupWizard.class)
//               .name("Setup Facets")
//               .description("Setup all dependent facets for the Faces scaffold.");
//      builder.add(compositeSetupMetadata, setupCommands);
      return builder.build();
   }

   @Override
   public NavigationResult getGenerationFlow(ScaffoldGenerationContext generationContext)
   {
      NavigationResultBuilder builder = NavigationResultBuilder.create();
      builder.add(ScaffoldableEntitySelectionWizard.class);
      return builder.build();
   }

   protected List<Resource<?>> generateIndex(String targetDir, final Resource<?> template)
   {
      List<Resource<?>> result = new ArrayList<Resource<?>>();
      WebResourcesFacet web = this.project.getFacet(WebResourcesFacet.class);

      HashMap<Object, Object> context = new HashMap<Object, Object>();
      
      loadTemplates();

      // Basic pages

      result.add(ScaffoldUtil.createOrOverwrite(web.getWebResource(targetDir + INDEX_JSP),
               this.templateProcessor.processTemplate(context, this.templateProcessor.getTemplate(INDEX_JSP_FTL))));


      return result;
   }

   @Override
   public AccessStrategy getAccessStrategy()
   {
      return new SpringMvcAccessStrategy(this.project);
   }
   
//
//   
//
   private List<Resource<?>> generateFromEntity(String targetDir, final String repositoryPackage,
            final JavaClassSource entityClass)
   {
//      resetMetaWidgets();
//
      // Track the list of resources generated

      List<Resource<?>> result = new ArrayList<Resource<?>>();
      try
      {
         JavaSourceFacet java = this.project.getFacet(JavaSourceFacet.class);
         WebResourcesFacet web = this.project.getFacet(WebResourcesFacet.class);
//         JPAFacet<PersistenceCommonDescriptor> jpa = this.project.getFacet(JPAFacet.class);
//
         loadTemplates();
         Map<Object, Object> context = new HashMap<Object, Object>();
         context.put("entityClass", entityClass);
         String entity = decapitalize(entityClass.getName()); 
         context.put("entity", entity);
         context.put("entityName",entityClass.getName());
         
         //controller specific variables
         context.put("repositoryPackage", repositoryPackage);
         context.put("controllerPackage", java.getBasePackage() + "." + "controller");
         context.put("entityPackage", entityClass.getPackage());
         
//         context.put("rmEntity", ccEntity + "ToDelete");
         setPrimaryKeyMetaData(context, entityClass);
		 setFieldsColumnAndFormData(context, entityClass);
//
//         // Prepare qbeMetawidget
//         this.qbeMetawidget.setPath(entity.getQualifiedName());
//         StringWriter stringWriter = new StringWriter();
//         this.qbeMetawidget.write(stringWriter, this.backingBeanTemplateQbeMetawidgetIndent);
//         context.put("qbeMetawidget", stringWriter.toString().trim());
//
//         // Prepare removeEntityMetawidget
//         this.rmEntityMetawidget.setPath(entity.getQualifiedName());
//         stringWriter = new StringWriter();
//         this.rmEntityMetawidget.write(stringWriter, this.backingBeanTemplateRmEntityMetawidgetIndent);
//         context.put("rmEntityMetawidget", stringWriter.toString().trim());
//
//         // Prepare Java imports
//         Set<String> qbeMetawidgetImports = this.qbeMetawidget.getImports();
//         Set<String> rmEntityMetawidgetImports = this.rmEntityMetawidget.getImports();
//         Set<String> metawidgetImports = CollectionUtils.newHashSet();
//         metawidgetImports.addAll(qbeMetawidgetImports);
//         metawidgetImports.addAll(rmEntityMetawidgetImports);
//         metawidgetImports.remove(entity.getQualifiedName());
//         context.put("metawidgetImports",
//                  CollectionUtils.toString(metawidgetImports, ";\r\nimport ", true, false));
//
//         // Prepare JPA Persistence Unit
//         context.put("persistenceUnitName", jpa.getConfig().getOrCreatePersistenceUnit().getName());
//
         // Create the Backing Bean for this entity
         JavaClassSource controllerBean = Roaster.parse(JavaClassSource.class,
                  this.templateProcessor.processTemplate(context, this.controllerBeanTemplate));
         controllerBean.setPackage(java.getBasePackage() + "." + "controller");
         result.add(ScaffoldUtil.createOrOverwrite(java.getJavaResource(controllerBean), controllerBean.toString()));
//
//         // Set new context for view generation
//         String beanName = StringUtils.decapitalize(viewBean.getName());
//         context.put("beanName", beanName);
//         context.put("ccEntity", ccEntity);
//         context.put("entityName", StringUtils.uncamelCase(entity.getName()));
//         setPrimaryKeyMetaData(context, entity);
//
//         // Prepare entityMetawidget
//         this.entityMetawidget.setValue(StaticFacesUtils.wrapExpression(beanName + "." + ccEntity));
//         this.entityMetawidget.setPath(entity.getQualifiedName());
//         this.entityMetawidget.setReadOnly(false);
//         this.entityMetawidget.setStyle(null);
//
//         // Generate create
//         writeEntityMetawidget(context, this.createTemplateEntityMetawidgetIndent, this.createTemplateNamespaces);
//
         result.add(ScaffoldUtil.createOrOverwrite(
                  web.getWebResource(targetDir + "/" + entity + "/" + entity + "-form.jsp"),
                  this.templateProcessor.processTemplate(context, this.formJspTemplate)));
         
         result.add(ScaffoldUtil.createOrOverwrite(
                 web.getWebResource(targetDir + "/" + entity + "/" + entity + "-list.jsp"),
                 this.templateProcessor.processTemplate(context, this.listJspTemplate)));
//
//         // Generate view
//         this.entityMetawidget.setReadOnly(true);
//         writeEntityMetawidget(context, this.viewTemplateEntityMetawidgetIndent, this.viewTemplateNamespaces);
//
//         result.add(ScaffoldUtil.createOrOverwrite(
//                  web.getWebResource(targetDir + "/" + ccEntity + "/view.xhtml"),
//                  this.templateProcessor.processTemplate(context, this.viewTemplate)));
//
//         // Generate search
//         this.searchMetawidget.setValue(StaticFacesUtils.wrapExpression(beanName + ".example"));
//         this.searchMetawidget.setPath(entity.getQualifiedName());
//         this.beanMetawidget.setValue(StaticFacesUtils.wrapExpression(beanName + ".pageItems"));
//         this.beanMetawidget.setPath(viewBean.getQualifiedName() + "/pageItems");
//         writeSearchAndBeanMetawidget(context, this.searchTemplateSearchMetawidgetIndent,
//                  this.searchTemplateBeanMetawidgetIndent, this.searchTemplateNamespaces);
//
//         result.add(ScaffoldUtil.createOrOverwrite(
//                  web.getWebResource(targetDir + "/" + ccEntity + "/search.xhtml"),
//                  this.templateProcessor.processTemplate(context, this.searchTemplate)));
//
//         // Generate navigation
////         result.add(generateNavigation(targetDir));
//
//         context.put("viewPackage", viewBean.getPackage());
//
//         createInitializers(entity);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Error generating default scaffolding: " + e.getMessage(), e);
      }
      return result;
   }
//
//   
//   protected void createInitializers(final JavaClassSource entity) throws FacetNotFoundException, FileNotFoundException
//   {
//      boolean dirtyBit = false;
//      for (FieldSource<JavaClassSource> field : entity.getFields())
//      {
//         if (field.hasAnnotation(OneToOne.class))
//         {
//            AnnotationSource<JavaClassSource> oneToOne = field.getAnnotation(OneToOne.class);
//            if (oneToOne.getStringValue("mappedBy") == null && oneToOne.getStringValue("cascade") == null)
//            {
//               oneToOne.setEnumValue("cascade", CascadeType.ALL);
//               dirtyBit = true;
//            }
//            String methodName = "new" + StringUtils.capitalize(field.getName());
//            if (!entity.hasMethodSignature(methodName))
//            {
//               entity.addMethod().setName(methodName).setReturnTypeVoid().setPublic()
//                        .setBody("this." + field.getName() + " = new " + field.getType().getName() + "();");
//               dirtyBit = true;
//            }
//         }
//      }
//      for (MethodSource<JavaClassSource> method : entity.getMethods())
//      {
//         if (method.hasAnnotation(OneToOne.class))
//         {
//            AnnotationSource<JavaClassSource> oneToOne = method.getAnnotation(OneToOne.class);
//            if (oneToOne.getStringValue("mappedBy") == null && oneToOne.getStringValue("cascade") == null)
//            {
//               oneToOne.setEnumValue("cascade", CascadeType.ALL);
//               dirtyBit = true;
//            }
//            String fieldName = StringUtils.camelCase(method.getName().substring(3));
//            String methodName = "new" + StringUtils.capitalize(fieldName);
//            if (!entity.hasMethodSignature(methodName))
//            {
//               entity.addMethod().setName(methodName).setReturnTypeVoid().setPublic()
//                        .setBody("this." + fieldName + " = new " + method.getReturnType().getName() + "();");
//               dirtyBit = true;
//            }
//         }
//      }
//      if (dirtyBit)
//      {
//         this.project.getFacet(JavaSourceFacet.class).saveJavaSource(entity);
//      }
//   }
//
   
   private void setFieldsColumnAndFormData(Map<Object, Object> context, final JavaClassSource entity)
   {
		StringBuilder columnHeader = new StringBuilder();
		StringBuilder columnData = new StringBuilder();
		StringBuilder formData = new StringBuilder();
		for (MemberSource<JavaClassSource, ?> f : entity.getFields()) {
			columnHeader.append("<th>").append(f.getName()).append("</th>");
			columnData.append("<td>${item.").append(f.getName())
					.append("}</td>");
			
			String fieldType  = ((Field<?>)f).getType().getQualifiedName();
			//TODO form based on type
			if (f.hasAnnotation(Id.class)) {
				formData.append("<dl><dt><label for=\"").append(f.getName())
						.append("\">" + f.getName() + "</label>")
						.append("</dt><dd>")
						.append("<form:input path=\"" +f.getName() +"\" readonly=\"true\" />")
						.append("</dd></dl>");
			} else if(f.hasAnnotation(Version.class)) {
				 //skip the version
			} else {
				formData.append("<dl><dt><label for=\"").append(f.getName())
				.append("\">" + f.getName() + "</label>")
				.append("</dt><dd>")
				.append("<form:input path=\"" +f.getName() +"\"  />")
				.append("</dd></dl>");
			}
		}
		context.put("columnHeader", columnHeader.toString());
		context.put("columnData", columnData.toString());
		context.put("formData", formData.toString());
		
		
   }
   private void setPrimaryKeyMetaData(Map<Object, Object> context, final JavaClassSource entity)
   {
      String pkName = "id";
      String pkType = "Long";
      String nullablePkType = "Long";
      for (MemberSource<JavaClassSource, ?> m : entity.getMembers())
      {
         if (m.hasAnnotation(Id.class))
         {
            if (m instanceof Field)
            {
               Field<?> field = (Field<?>) m;
               pkName = field.getName();
               pkType = field.getType().getQualifiedName();
               nullablePkType = pkType;
               break;
            }

            MethodSource<?> method = (MethodSource<?>) m;
            pkName = method.getName().substring(3);
            if (method.getName().startsWith("get"))
            {
               pkType = method.getReturnType().getQualifiedName();
            }
            else
            {
               pkType = method.getParameters().get(0).getType().getQualifiedName();
            }
            nullablePkType = pkType;
            break;
         }
      }

      if (Types.isJavaLang(pkType))
      {
         nullablePkType = Types.toSimpleName(pkType);
      }
      else if ("int".equals(pkType))
      {
         nullablePkType = Integer.class.getSimpleName();
      }
      else if ("short".equals(pkType))
      {
         nullablePkType = Short.class.getSimpleName();
      }
      else if ("byte".equals(pkType))
      {
         nullablePkType = Byte.class.getSimpleName();
      }
      else if ("long".equals(pkType))
      {
         nullablePkType = Long.class.getSimpleName();
      }

      context.put("primaryKey", pkName);
      context.put("primaryKeyCC", StringUtils.capitalize(pkName));
      context.put("primaryKeyType", pkType);
      context.put("nullablePrimaryKeyType", nullablePkType);
   }
//
//   @SuppressWarnings({ "rawtypes", "unchecked" })
//   private void createErrorPageEntry(WebAppCommonDescriptor servletConfig, String errorLocation, String errorCode)
//   {
//      List<ErrorPageCommonType> allErrorPage = servletConfig.getAllErrorPage();
//      for (ErrorPageCommonType errorPageType : allErrorPage)
//      {
//         if (errorPageType.getErrorCode().equalsIgnoreCase(errorCode))
//         {
//            return;
//         }
//      }
//      servletConfig.createErrorPage().errorCode(errorCode).location(errorLocation);
//   }
   protected void loadTemplates()
   {
      if (this.controllerBeanTemplate == null)
      {
         this.controllerBeanTemplate = this.templateProcessor.getTemplate(CONTROLLER_BEAN_TEMPLATE);
      }
      if (this.formJspTemplate == null)
      {
         this.formJspTemplate = this.templateProcessor.getTemplate(FORM_JSP_TEMPLATE);
      }
      if (this.listJspTemplate == null)
      {
         this.listJspTemplate = this.templateProcessor.getTemplate(LIST_JSP_TEMPLATE);
      }
   }
   private String decapitalize(String in) {
		char firstChar = in.charAt(0);

		if (Character.isLowerCase(firstChar)) {
			return in;
		}

		if ((in.length() > 1) && (Character.isUpperCase(in.charAt(1)))) {
			return in;
		}

		return Character.toLowerCase(firstChar) + in.substring(1);
	}
}

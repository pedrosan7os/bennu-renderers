/**
 * 
 */
package pt.ist.fenixWebFramework.struts.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.ExceptionHandler;
import org.apache.struts.action.PlugIn;
import org.apache.struts.config.ExceptionConfig;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.MessageResourcesConfig;
import org.apache.struts.config.ModuleConfig;

import pt.ist.fenixWebFramework.Config;
import pt.ist.fenixWebFramework.FenixWebFramework;
import pt.ist.fenixWebFramework.struts.annotations.ActionAnnotationProcessor;
import pt.ist.fenixWebFramework.struts.annotations.Exceptions;
import pt.ist.fenixWebFramework.struts.annotations.Forward;
import pt.ist.fenixWebFramework.struts.annotations.Forwards;
import pt.ist.fenixWebFramework.struts.annotations.Input;
import pt.ist.fenixWebFramework.struts.annotations.Mapping;
import pt.ist.fenixWebFramework.struts.tiles.FenixDefinitionsFactory;
import pt.ist.fenixWebFramework.struts.tiles.PartialTileDefinition;
import pt.utl.ist.fenix.tools.util.FileUtils;

/**
 * @author - Shezad Anavarali (shezad@ist.utl.pt)
 * 
 */
public class StrutsAnnotationsPlugIn implements PlugIn {

    private static final String STRUTS_DEFAULT_RESOURCE_MESSAGE = "org.apache.struts.action.MESSAGE";

    private static final String PAGE_FILE_EXTENSION = ".jsp";

    private static final String INPUT_PAGE_AND_METHOD = ".do?page=0&method=";

    private static final String INPUT_DEFAULT_PAGE_AND_METHOD = ".do?page=0&method=prepare";

    private static final String EXCEPTION_KEY_DEFAULT_PREFIX = "resources.Action.exceptions.";

    private static final List<String> UPPER_BOUND_SUPERCLASSES = Arrays.asList("DispatchAction", "Action", "Object");

    private static boolean initialized = false;

    private static final Set<Class<?>> actionClasses = new HashSet<Class<?>>();

    public void destroy() {
    }

    public void init(ActionServlet servlet, ModuleConfig config) throws ServletException {

	if (!initialized) {
	    loadActionsFromFile(actionClasses);
	    PartialTileDefinition.init();
	    initialized = true;
	}

	final String modulePrefix = StringUtils.removeStart(config.getPrefix(), "/");

	for (Class<?> actionClass : actionClasses) {
	    Mapping mapping = actionClass.getAnnotation(Mapping.class);
	    if (mapping == null || !modulePrefix.equals(mapping.module())) {
		continue;
	    }
	    final ActionMapping actionMapping = new ActionMapping();

	    actionMapping.setPath(mapping.path());
	    actionMapping.setType(actionClass.getName());
	    actionMapping.setScope("request");
	    actionMapping.setParameter("method");
	    actionMapping.setValidate(true);

	    if (mapping.formBeanClass() != ActionForm.class) {
		final String formName = mapping.formBeanClass().getName();
		createFormBeanConfigIfNecessary(config, mapping, formName);
		actionMapping.setName(formName);
	    } else if (!mapping.formBean().isEmpty()) {
		actionMapping.setName(mapping.formBean());
	    }

	    if (mapping.input().isEmpty()) {
		actionMapping.setInput(findInputMethod(actionClass, mapping));
	    } else {
		registerInput(actionMapping, mapping.input());
	    }

	    String defaultResourcesName = getDefaultResourcesName(config);
	    Forwards forwards = actionClass.getAnnotation(Forwards.class);
	    if (forwards != null) {
		for (final Forward forward : forwards.value()) {
		    registerForward(actionMapping, forward, forwards, mapping, defaultResourcesName);
		}
	    }
	    registerSuperclassForwards(actionMapping, actionClass.getSuperclass(), mapping, defaultResourcesName);

	    Exceptions exceptions = actionClass.getAnnotation(Exceptions.class);
	    if (exceptions != null) {
		for (Class<? extends Exception> exClass : exceptions.value()) {
		    final ExceptionConfig exceptionConfig = new ExceptionConfig();
		    final Config appConfig = FenixWebFramework.getConfig();
		    final String exceptionHandler = appConfig.getExceptionHandlerClassname() == null ? ExceptionHandler.class
			    .getName() : appConfig.getExceptionHandlerClassname();
		    exceptionConfig.setHandler(exceptionHandler);
		    exceptionConfig.setType(exClass.getName());
		    exceptionConfig.setKey(EXCEPTION_KEY_DEFAULT_PREFIX + exClass.getSimpleName());
		    actionMapping.addExceptionConfig(exceptionConfig);
		}
	    }

	    config.addActionConfig(actionMapping);

	}

    }

    private static void registerSuperclassForwards(final ActionMapping actionMapping, Class<?> superclass, Mapping mapping,
	    String defaultResourcesName) {
	if (UPPER_BOUND_SUPERCLASSES.contains(superclass.getSimpleName())) {
	    return;
	}
	Forwards forwards = superclass.getAnnotation(Forwards.class);
	if (forwards == null) {
	    return;
	}
	for (final Forward forward : forwards.value()) {
	    try {
		actionMapping.findForward(forward.name());
	    } catch (NullPointerException ex) {
		// Forward wasn't registered in any subclass, so register it.
		registerForward(actionMapping, forward, forwards, mapping, defaultResourcesName);
	    }
	}
	registerSuperclassForwards(actionMapping, superclass.getSuperclass(), mapping, defaultResourcesName);
    }

    private static void registerInput(final ActionMapping actionMapping, String input) {
	if (isSimplePageFile(input)) {
	    PartialTileDefinition tileDefinition = new PartialTileDefinition(input);
	    FenixDefinitionsFactory.registerDefinition(tileDefinition);
	    actionMapping.setInput(tileDefinition.getName());
	} else {
	    // The input is using an existing tile definition
	    actionMapping.setInput(input);
	}
    }

    private static void registerForward(final ActionMapping actionMapping, final Forward forward, Forwards forwards,
	    Mapping mapping, String defaultResourcesName) {
	if (forward.useTile() && isSimplePageFile(forward.path())) {
	    PartialTileDefinition tileDefinition = new PartialTileDefinition(forward, forwards, mapping, defaultResourcesName);
	    FenixDefinitionsFactory.registerDefinition(tileDefinition);
	    actionMapping.addForwardConfig(new ActionForward(forward.name(), tileDefinition.getName(), forward.redirect(),
		    forward.contextRelative()));
	} else {
	    // The forward is using an existing tile definition
	    actionMapping.addForwardConfig(new ActionForward(forward.name(), forward.path(), forward.redirect(), forward
		    .contextRelative()));
	}
    }

    private static boolean isSimplePageFile(String str) {
	return (str.endsWith(PAGE_FILE_EXTENSION)) && (StringUtils.countMatches(str, PAGE_FILE_EXTENSION) == 1);
    }

    private void createFormBeanConfigIfNecessary(ModuleConfig config, Mapping mapping, final String formName) {
	FormBeanConfig formBeanConfig = config.findFormBeanConfig(formName);
	if (formBeanConfig == null) {
	    formBeanConfig = new FormBeanConfig();
	    formBeanConfig.setType(mapping.formBeanClass().getName());
	    formBeanConfig.setName(formName);
	    config.addFormBeanConfig(formBeanConfig);
	}
    }

    private static String getDefaultResourcesName(ModuleConfig config) {
	MessageResourcesConfig resourcesConfig = config.findMessageResourcesConfig(STRUTS_DEFAULT_RESOURCE_MESSAGE);
	return (resourcesConfig == null) ? null : resourcesConfig.getParameter();
    }

    private String findInputMethod(Class<?> actionClass, Mapping mapping) {
	for (Method method : actionClass.getMethods()) {
	    final Input input = method.getAnnotation(Input.class);
	    if (input != null) {
		return mapping.path() + INPUT_PAGE_AND_METHOD + method.getName();
	    }
	}
	return mapping.path() + INPUT_DEFAULT_PAGE_AND_METHOD;
    }

    private void loadActionsFromFile(final Set<Class<?>> actionClasses) {
	final InputStream inputStream = getClass().getResourceAsStream("/.actionAnnotationLog");
	if (inputStream != null) {
	    try {
		final String contents = FileUtils.readFile(inputStream);
		for (final String classname : contents.split(ActionAnnotationProcessor.ENTRY_SEPERATOR)) {
		    try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class<?> type = loader.loadClass(classname);
			actionClasses.add(type);
		    } catch (final ClassNotFoundException e) {
			e.printStackTrace();
		    }
		}
	    } catch (final IOException e) {
		e.printStackTrace();
	    }
	}
    }

}

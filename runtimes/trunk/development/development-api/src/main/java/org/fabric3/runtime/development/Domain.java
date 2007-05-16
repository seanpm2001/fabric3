package org.fabric3.runtime.development;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

import org.fabric3.host.runtime.InitializationException;
import org.fabric3.host.runtime.ScdlBootstrapper;
import org.fabric3.runtime.development.host.DevelopmentHostInfoImpl;
import org.fabric3.runtime.development.host.DevelopmentRuntime;

/**
 * Client API for instantiating a local Fabric3 development domain environment. Usage is as follows:
 * <pre>
 * Domain domain = new Domain();
 * domain.activate(url);
 * MyService service = domain.locateService(MyService.class, compositeUri, "MyComponent");
 * //...
 * domain.stop();
 *  </pre>
 * In the above example, <code>domain.activate(url)</code> transiently contributes and activates the composite in the
 * domain. As this operation is transient, the composite will be removed when <code>domain.stop</code> is called.
 * <p/>
 * This API is intended to be used within a development environment for prototyping. To setup, peform the following
 * steps:
 * <pre>
 * <ul>
 * <li> Download and install the Fabric3 development distribution. Set the system property
 * <code>fabric3.dev.home</code> to point to the distribution.
 * <li> Set the IDE classpath to include the jars in the /lib directory of the distribution. These jars include the
 * SCA,
 * Fabric3, and development runtime APIs.
 * </ul> Instantiate a Domain according to the usage outlined above.
 * </pre>
 * Note that instantiating a Domain and activating a composite will bootstrap a Fabric3 runtime in a child classloader
 * of the application classloader. This will ensure Fabric3 implementation classes are isolated from the application
 * classpath.
 *
 * @version $Rev$ $Date$
 */
public class Domain {
    public static final String FABRIC3_DEV_HOME = "fabric3.dev.home";
    public static final String SYSTEM_SCDL = "/system/system.scdl";
    public static final URI DOMAIN_URI = URI.create("fabric3://./domain");

    private DevelopmentRuntime runtime;

    public void activate(URL compositeFile) {
        if (runtime == null) {
            bootRuntime();
            runtime.activate(compositeFile);
        }
    }

    public void stop() {
        runtime.stop();
        runtime = null;
    }

    public <T> T connectTo(Class<T> interfaze, String componentUri) {
        if (runtime == null) {
            throw new IllegalStateException("No composite is activated");
        }
        return runtime.connectTo(interfaze, componentUri);
    }

    private void bootRuntime() {
        String home = System.getProperty(FABRIC3_DEV_HOME);
        if (home == null) {
            throw new InvalidFabric3HomeException("Fabric3 home system property not set", FABRIC3_DEV_HOME);
        }
        File baseDir = new File(home);
        if (!baseDir.exists()) {
            throw new InvalidFabric3HomeException("Fabric3 home system directory does not exist", home);
        }
        File libDir = new File(baseDir, "boot");
        if (!libDir.exists()) {
            throw new InvalidFabric3HomeException("Invalid Fabric3 installation: boot directory not found", home);
        }
        File[] libraries = libDir.listFiles();
        URL[] urls = new URL[libraries.length];
        for (int i = 0; i < libraries.length; i++) {
            try {
                urls[i] = libraries[i].toURI().toURL();
            } catch (MalformedURLException e) {
                throw new AssertionError(e);
            }
        }
        try {
            ClassLoader cl = new URLClassLoader(urls, getClass().getClassLoader());
            getClass().getClassLoader().loadClass("org.osoa.sca.ServiceUnavailableException");
            URL systemSCDL = new File(baseDir, SYSTEM_SCDL).toURI().toURL();
            Class<?> bootstrapperClass = cl.loadClass("org.fabric3.fabric.runtime.ScdlBootstrapperImpl");
            ScdlBootstrapper bootstrapper = (ScdlBootstrapper) bootstrapperClass.newInstance();
            bootstrapper.setScdlLocation(systemSCDL);
            Class<?> runtimeClass = cl.loadClass("org.fabric3.runtime.development.host.DevelopmentRuntimeImpl");
            runtime = (DevelopmentRuntime) runtimeClass.newInstance();
            URL baseDirUrl = baseDir.toURI().toURL();
            runtime.setHostInfo(new DevelopmentHostInfoImpl(DOMAIN_URI, baseDirUrl, baseDir, cl, cl));
            runtime.setHostClassLoader(cl);
            bootstrapper.bootstrap(runtime, cl);
        } catch (InstantiationException e) {
            throw new InvalidConfigurationException("Error instantiating runtime classes are missing", e);
        } catch (IllegalAccessException e) {
            throw new InvalidConfigurationException("Invalid configuration", e);
        } catch (ClassNotFoundException e) {
            throw new InvalidConfigurationException("Runtime classes are missing", e);
        } catch (InitializationException e) {
            throw new InvalidConfigurationException("Error initializing runtime", e);
        } catch (MalformedURLException e) {
            throw new InvalidConfigurationException("Error initializing runtime", e);
        }

    }

}

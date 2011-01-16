package org.fabric3.runtime.embedded.api;

import org.fabric3.host.RuntimeMode;
import org.fabric3.host.runtime.InitializationException;
import org.fabric3.host.runtime.ShutdownException;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Collection;

/**
 * Runtime holds name, configuration, runtime mode, his folder and related profiles. You can add profile to runtime.
 * Start and stop it.
 *
 * @author Michal Capo
 */
public interface EmbeddedRuntime {

    /**
     * Get runtime name.
     *
     * @return runtime name
     */
    String getName();

    /**
     * Get configuration of runtime.
     *
     * @return runtime configuration
     */
    URL getSystemConfig();

    /**
     * Runtime mode of runtime.
     *
     * @return runtime mode
     */
    RuntimeMode getRuntimeMode();

    /**
     * Folder where runtime is situated.
     *
     * @return folders name
     */
    File getRuntimeFolder();

    /**
     * Add profile to this runtime.
     *
     * @param profile to be added
     */
    void addProfile(EmbeddedProfile profile);

    /**
     * Get all profiles specific for this runtime.
     *
     * @return profiles collection, can be empty
     */
    Collection<EmbeddedProfile> getProfiles();

    /**
     * Start runtime.
     *
     * @throws org.fabric3.host.runtime.InitializationException
     *          when cannot start runtime
     */
    void startRuntime() throws InitializationException;

    /**
     * Stop runtime.
     *
     * @throws org.fabric3.host.runtime.ShutdownException
     *          when cannot stop runtime
     */
    void stopRuntime() throws ShutdownException;

    /**
     * Get component stored in this runtime.
     *
     * @param pClass of component you want to get
     * @param pURI   component URL you want to get
     * @param <T>    interface
     * @return component from runtime of null if component wasn't found
     */
    <T> T getComponent(Class<T> pClass, URI pURI);

}

/*
 * Fabric3
 * Copyright (c) 2009-2012 Metaform Systems
 *
 * Fabric3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version, with the
 * following exception:
 *
 * Linking this software statically or dynamically with other
 * modules is making a combined work based on this software.
 * Thus, the terms and conditions of the GNU General Public
 * License cover the whole combination.
 *
 * As a special exception, the copyright holders of this software
 * give you permission to link this software with independent
 * modules to produce an executable, regardless of the license
 * terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided
 * that you also meet, for each linked independent module, the
 * terms and conditions of the license of that module. An
 * independent module is a module which is not derived from or
 * based on this software. If you modify this software, you may
 * extend this exception to your version of the software, but
 * you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version.
 *
 * Fabric3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the
 * GNU General Public License along with Fabric3.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * ----------------------------------------------------
 *
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 *
 */
package org.fabric3.fabric.executor;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.oasisopen.sca.annotation.Constructor;
import org.oasisopen.sca.annotation.EagerInit;
import org.oasisopen.sca.annotation.Init;
import org.oasisopen.sca.annotation.Reference;

import org.fabric3.fabric.builder.BuilderNotFoundException;
import org.fabric3.fabric.command.BuildComponentCommand;
import org.fabric3.spi.builder.BuilderException;
import org.fabric3.spi.builder.component.ComponentBuilder;
import org.fabric3.spi.builder.component.ComponentBuilderListener;
import org.fabric3.spi.cm.ComponentManager;
import org.fabric3.spi.cm.RegistrationException;
import org.fabric3.spi.component.Component;
import org.fabric3.spi.executor.CommandExecutor;
import org.fabric3.spi.executor.CommandExecutorRegistry;
import org.fabric3.spi.executor.ExecutionException;
import org.fabric3.spi.model.physical.PhysicalComponentDefinition;

/**
 * Builds a component on a runtime.
 *
 * @version $Rev$ $Date$
 */
@EagerInit
public class BuildComponentCommandExecutor implements CommandExecutor<BuildComponentCommand> {

    private ComponentManager componentManager;
    private CommandExecutorRegistry commandExecutorRegistry;
    private Map<Class<?>, ComponentBuilder> builders;
    private List<ComponentBuilderListener> listeners = Collections.emptyList();

    @Constructor
    public BuildComponentCommandExecutor(@Reference ComponentManager componentManager, @Reference CommandExecutorRegistry commandExecutorRegistry) {
        this.componentManager = componentManager;
        this.commandExecutorRegistry = commandExecutorRegistry;
    }

    public BuildComponentCommandExecutor(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    @Init
    public void init() {
        commandExecutorRegistry.register(BuildComponentCommand.class, this);
    }

    @Reference(required = false)
    public void setBuilders(Map<Class<?>, ComponentBuilder> builders) {
        this.builders = builders;
    }

    @Reference(required = false)
    public void setListeners(List<ComponentBuilderListener> listeners) {
        this.listeners = listeners;
    }

    public void execute(BuildComponentCommand command) throws ExecutionException {
        try {
            PhysicalComponentDefinition definition = command.getDefinition();
            Component component = build(definition);
            URI classLoaderId = definition.getClassLoaderId();
            component.setClassLoaderId(classLoaderId);
            componentManager.register(component);
            for (ComponentBuilderListener listener : listeners) {
                listener.onBuild(component, definition);
            }
        } catch (BuilderException e) {
            throw new ExecutionException(e.getMessage(), e);
        } catch (RegistrationException e) {
            throw new ExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Builds a physical component from component definition.
     *
     * @param definition the component definition.
     * @return Component to be built.
     * @throws BuilderException if an exception building is encountered
     */
    @SuppressWarnings("unchecked")
    private Component build(PhysicalComponentDefinition definition) throws BuilderException {

        ComponentBuilder builder = builders.get(definition.getClass());
        if (builder == null) {
            throw new BuilderNotFoundException("Builder not found for " + definition.getClass().getName());
        }
        return builder.build(definition);
    }


}

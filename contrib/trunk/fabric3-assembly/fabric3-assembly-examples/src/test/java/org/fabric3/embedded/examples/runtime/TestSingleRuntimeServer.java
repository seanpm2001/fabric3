package org.fabric3.embedded.examples.runtime;

import org.fabric3.assembly.configuration.AssemblyConfig;
import org.fabric3.assembly.dependency.UpdatePolicy;
import org.fabric3.assembly.factory.ConfigurationBuilder;

import java.io.IOException;

/**
 * @author Michal Capo
 */
public class TestSingleRuntimeServer {

    public static void main(String[] args) throws IOException, InterruptedException {

        AssemblyConfig config = ConfigurationBuilder.getBuilder()
                .setVersion("1.8")
                .setUpdatePolicy(UpdatePolicy.ALWAYS)

                .addProfile("test2").dependency("org.codehaus.fabric3:fabric3-junit:1.8").dependency("org.codehaus.fabric3:fabric3-jaxb:1.8")

                .addServer("/tmp/fabric3_test_single")
                .addRuntime().withProfiles("web", "rest", "test2")

                .createConfiguration();


        config.process();
    }

}

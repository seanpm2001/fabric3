/*
 * See the NOTICE file distributed with this work for information
 * regarding copyright ownership.  This file is licensed
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.fabric3.contribution;

import java.io.File;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.jar.JarArchiver;

/**
 * Builds an archive suitable for contribution to an SCA Domain.
 *
 * @version $Rev$ $Date$
 * @goal package
 * @phase package
 */
public class Fabric3ContributionMojo extends AbstractMojo {
    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/package.html"};

    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    /**
     * Build output directory.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    protected File outputDirectory;

    /**
     * Name of the generated composite archive.
     *
     * @parameter expression="${project.build.finalName}"
     */
    protected String contributionName;

    /**
     * Classifier to add to the generated artifact.
     *
     * @parameter
     */
    protected String classifier;

    /**
     * Directory containing the classes to include in the archive.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    protected File classesDirectory;

    /**
     * Standard Maven archive configuration.
     *
     * @parameter
     */
    protected MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * The Jar archiver.
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
     * @required
     * @readonly
     */
    protected JarArchiver jarArchiver;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @component
     * @required
     * @readonly
     */
    protected MavenProjectHelper projectHelper;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File contribution = createArchive();

        if (classifier != null) {
            projectHelper.attachArtifact(project, "f3r", classifier, contribution);
        } else {
            project.getArtifact().setFile(contribution);
        }
    }

    protected File createArchive() throws MojoExecutionException {
        File contribution = getJarFile(outputDirectory, contributionName, classifier);

        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(contribution);
        archive.setForced(true);

        try {
            File contentDirectory = classesDirectory;
            if (!contentDirectory.exists()) {
                getLog().warn("JAR will be empty - no content was marked for inclusion!");
            } else {
                archiver.getArchiver().addDirectory(contentDirectory, DEFAULT_INCLUDES, DEFAULT_EXCLUDES);
            }

            archiver.createArchive(project, archive);

            return contribution;
        }
        catch (Exception e) {
            throw new MojoExecutionException("Error assembling contribution", e);
        }
    }

    protected File getJarFile(File buildDir, String finalName, String classifier) {
        if (classifier != null) {
            classifier = classifier.trim();
            if (classifier.length() > 0) {
                finalName = finalName + '-' + classifier;
            }
        }
        return new File(buildDir, finalName + ".composite");
    }
}
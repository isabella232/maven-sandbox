/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
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

package org.apache.maven.archetype.mojos;

import org.apache.maven.archetype.ArchetypeCreationRequest;
import org.apache.maven.archetype.ArchetypeCreationResult;
import org.apache.maven.archetype.Archetyper;
import org.apache.maven.archetype.common.ArchetypePropertiesManager;
import org.apache.maven.archetype.common.ArchetypeRegistryManager;
import org.apache.maven.archetype.ui.ArchetypeCreationConfigurator;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

/**
 * Creates sample archetype from current project.
 *
 * @author rafale
 * @requiresProject true
 * @goal create-from-project
 * @execute phase="generate-sources"
 */
public class CreateArchetypeFromProjectMojo
    extends AbstractMojo
{
    /** @component */
    ArchetypeCreationConfigurator configurator;

    /**
     * Enable the interactive mode to define the archetype from the project.
     *
     * @parameter expression="${interactive}" default-value="false"
     */
    private boolean interactive;

    /** @component */
    ArchetypeRegistryManager archetypeRegistryManager;

    /** @component */
    Archetyper archetyper;

    /** @component */
    private ArchetypePropertiesManager propertiesManager;

    /**
     * File extensions which are checked for project's text files (vs binary files).
     *
     * @parameter expression="${archetype.filteredExtentions}"
     */
    private String archetypeFilteredExtentions;

    /**
     * Directory names which are checked for project's sources main package.
     *
     * @parameter expression="${archetype.languages}"
     */
    private String archetypeLanguages;

    /**
     * The location of the registry file.
     *
     * @parameter expression="${user.home}/.m2/archetype.xml"
     */
    private File archetypeRegistryFile;

    /**
     * Velocity templates encoding.
     *
     * @parameter default-value="UTF-8" expression="${archetype.encoding}"
     */
    private String defaultEncoding;

    /**
     * Ignore the replica creation.
     *
     * @parameter expression="${archetype.ignoreReplica}"
     */
    private boolean ignoreReplica = true;

    /**
     * Create a partial archetype.
     *
     * @parameter expression="${archetype.partialArchetype}"
     */
    private boolean partialArchetype = false;

    /**
     * Create pom's velocity templates with CDATA preservasion. This uses the String replaceAll
     * method and risk to have some overly replacement capabilities (beware of '1.0' value).
     *
     * @parameter expression="${archetype.preserveCData}"
     */
    private boolean preserveCData = false;

    /** @parameter expression="${localRepository}" */
    private ArtifactRepository localRepository;

    /**
     * Poms in archetype are created with their initial parent.
     * This property is ignored when preserveCData is true.
     *
     * @parameter expression="${archetype.keepParent}"
     */
    private boolean keepParent = true;

    /**
     * The maven Project to create an archetype from.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The property file that holds the plugin configuration.
     *
     * @parameter default-value="target/archetype.properties" expression="${archetype.properties}"
     */
    private File propertyFile;

    /** @parameter expression="${basedir}/target" */
    private File outputDirectory;

    /** @parameter expression="${testMode}" */
    private boolean testMode;

    public void execute()
        throws
        MojoExecutionException,
        MojoFailureException
    {
        try
        {
            if ( propertyFile != null )
            {
                propertyFile.getParentFile().mkdirs();
            }

            List languages = archetypeRegistryManager.getLanguages( archetypeLanguages, archetypeRegistryFile );

            configurator.configureArchetypeCreation(
                project,
                new Boolean( interactive ),
                System.getProperties(),
                propertyFile,
                languages
            );

            List filtereds =
                archetypeRegistryManager.getFilteredExtensions(
                    archetypeFilteredExtentions,
                    archetypeRegistryFile
                );

            ArchetypeCreationRequest request = new ArchetypeCreationRequest()
                .setProject( project )
                .setPropertyFile( propertyFile )
                .setLanguages( languages )
                .setFiltereds( filtereds )
                .setIgnoreReplica( ignoreReplica )
                .setPreserveCData( preserveCData )
                .setKeepParent( keepParent )
                .setPartialArchetype( partialArchetype )
                .setArchetypeRegistryFile( archetypeRegistryFile )
                .setLocalRepository( localRepository );

            ArchetypeCreationResult result = archetyper.createArchetypeFromProject( request );

            if ( result.getCause() != null )
            {
                throw new MojoExecutionException( result.getCause().getMessage(), result.getCause() );
            }

            getLog().info( "Archetype created in target/generated-sources/archetypeng" );

            if ( testMode )
            {
                // Now here a properties file would be useful to write so that we could automate
                // some functional tests where we string together an:
                //
                // archetype create from project -> deploy it into a test repo
                // project create from archetype -> use the repository we deployed to archetype to
                // generate
                // test the output
                //
                // This of course would be strung together from the outside.
            }

        }
        catch ( Exception ex )
        {
            throw new MojoExecutionException( ex.getMessage(), ex );
        }
    }
}
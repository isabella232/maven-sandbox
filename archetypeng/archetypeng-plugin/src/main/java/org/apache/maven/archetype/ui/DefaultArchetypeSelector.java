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

package org.apache.maven.archetype.ui;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.common.ArchetypeArtifactManager;
import org.apache.maven.archetype.common.ArchetypeDefinition;
import org.apache.maven.archetype.common.ArchetypeFactory;
import org.apache.maven.archetype.common.ArchetypePropertiesManager;
import org.apache.maven.archetype.common.ArchetypeRegistryManager;
import org.apache.maven.archetype.exception.ArchetypeNotDefined;
import org.apache.maven.archetype.exception.ArchetypeSelectionFailure;
import org.apache.maven.archetype.exception.UnknownArchetype;
import org.apache.maven.archetype.exception.UnknownGroup;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.archetype.source.ArchetypeDataSourceException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.PropertyUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** @plexus.component */
public class DefaultArchetypeSelector
    extends AbstractLogEnabled
    implements ArchetypeSelector
{
    /** @plexus.requirement */
    private ArchetypeArtifactManager archetypeArtifactManager;

    /** @plexus.requirement */
    private ArchetypeFactory archetypeFactory;

    /** @plexus.requirement */
    private ArchetypePropertiesManager archetypePropertiesManager;

    /** @plexus.requirement */
    private ArchetypeRegistryManager archetypeRegistryManager;

    /** @plexus.requirement */
    private ArchetypeSelectionQueryer archetypeSelectionQueryer;

    /** @plexus.requirement role="org.apache.maven.archetype.source.ArchetypeDataSource" */
    private Map archetypeSources;

    private ArchetypeDefinition selectArchetype(
        Boolean interactiveMode,
        ArtifactRepository localRepository,
        List repositories
    )
        throws
        ArchetypeNotDefined,
        UnknownArchetype,
        UnknownGroup,
        IOException,
        PrompterException,
        ArchetypeSelectionFailure
    {
        ArchetypeDefinition archetypeDefinition = new ArchetypeDefinition();

        if ( interactiveMode.booleanValue() )
        {
            if ( !archetypeDefinition.isDefined() )
            {
                List archetypes = new ArrayList();

                File archetypeCatalogPropertiesFile = new File( System.getProperty( "user.home" ), ".m2/archetype-catalog.properties" );

                if ( archetypeCatalogPropertiesFile.exists() )
                {
                    Properties archetypeCatalogProperties = PropertyUtils.loadProperties( archetypeCatalogPropertiesFile );

                    getLogger().debug( "Using catalogs " + archetypeCatalogProperties );

                    String[] sources = StringUtils.split( archetypeCatalogProperties.getProperty( "sources" ), "," );

                    for ( int i = 0; i < sources.length; i++ )
                    {
                        String sourceRoleHint = sources[i];

                        getLogger().debug( "Reading catalog " + sourceRoleHint );

                        try
                        {
                            ArchetypeDataSource source = (ArchetypeDataSource) archetypeSources.get( sourceRoleHint );

                            archetypes.addAll(
                                source.getArchetypes( getArchetypeDataSourceProperties( sourceRoleHint, archetypeCatalogProperties ) ) );
                        }
                        catch ( ArchetypeDataSourceException e )
                        {
                            getLogger().warn( "Unable to get archetypes from " + sourceRoleHint + " source. [" + e.getMessage() + "]" );
                        }
                    }
                }

                if ( archetypes.size() == 0 )
                {
                    getLogger().debug( "Using wiki catalog" );

                    try
                    {
                        ArchetypeDataSource source = (ArchetypeDataSource) archetypeSources.get( "wiki" );

                        archetypes.addAll( source.getArchetypes( new Properties() ) );
                    }
                    catch ( ArchetypeDataSourceException e )
                    {
                        getLogger().warn( "Unable to get archetypes from default wiki  source. [" + e.getMessage() + "]" );
                    }
                }

                if ( archetypes.size() > 0 )
                {
                    Archetype archetype = archetypeSelectionQueryer.selectArchetype( archetypes );

                    archetypeDefinition.setArtifactId( archetype.getArtifactId() );

                    archetypeDefinition.setName( archetype.getArtifactId() );

                    archetypeDefinition.setGroupId( archetype.getGroupId() );

                    archetypeDefinition.setVersion( archetype.getVersion() );

                    archetypeDefinition.setRepository( archetype.getRepository() );

                    String goals = StringUtils.join( archetype.getGoals().iterator(), "," );

                    archetypeDefinition.setGoals( goals );
                }
            }
        }

        // Make sure the groupId and artifactId are valid, the version may just default to
        // the latest release.

        if ( !archetypeDefinition.isPartiallyDefined() )
        {
            throw new ArchetypeSelectionFailure( "No valid archetypes could be found to choose." );
        }

        //TODO: this needs to be remove and let the generator take the selection + configuration and do what's 
        //      necessary. The selector should not be downloading anything.

        repositories.add(
            archetypeRegistryManager.createRepository( archetypeDefinition.getRepository(), archetypeDefinition.getArtifactId() + "-repo" ) );

        if ( !archetypeArtifactManager.exists(
            archetypeDefinition,
            localRepository,
            repositories ) )
        {
            throw new UnknownArchetype(
                "The desired archetype does not exist (" + archetypeDefinition.getGroupId() + ":"
                    + archetypeDefinition.getArtifactId() + ":" + archetypeDefinition.getVersion()
                    + ")"
            );
        }
        else
        {
            return archetypeDefinition;
        }
    }

    private Properties getArchetypeDataSourceProperties( String sourceRoleHint,
                                                         Properties archetypeCatalogProperties )
    {
        Properties p = new Properties();

        for ( Iterator i = archetypeCatalogProperties.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            if ( key.startsWith( sourceRoleHint ) )
            {
                String k = key.substring( sourceRoleHint.length() + 1 );

                p.setProperty( k, archetypeCatalogProperties.getProperty( key ) );
            }
        }

        return p;
    }

    public void selectArchetype(
        ArchetypeGenerationRequest request,
        Boolean interactiveMode,
        File archetypeRegistryFile,
        List repositories
    )
        throws
        ArchetypeNotDefined,
        UnknownArchetype,
        UnknownGroup,
        IOException,
        PrompterException,
        ArchetypeSelectionFailure
    {
        ArchetypeDefinition definition = new ArchetypeDefinition();

        if ( interactiveMode.booleanValue() )
        {
            if ( !definition.isDefined() )
            {
                List archetypes = new ArrayList();

                File archetypeCatalogPropertiesFile = new File( System.getProperty( "user.home" ), ".m2/archetype-catalog.properties" );

                if ( archetypeCatalogPropertiesFile.exists() )
                {
                    Properties archetypeCatalogProperties = PropertyUtils.loadProperties( archetypeCatalogPropertiesFile );

                    getLogger().debug( "Using catalogs " + archetypeCatalogProperties );

                    String[] sources = StringUtils.split( archetypeCatalogProperties.getProperty( "sources" ), "," );

                    for ( int i = 0; i < sources.length; i++ )
                    {
                        String sourceRoleHint = sources[i];

                        getLogger().debug( "Reading catalog " + sourceRoleHint );

                        try
                        {
                            ArchetypeDataSource source = (ArchetypeDataSource) archetypeSources.get( sourceRoleHint );

                            archetypes.addAll(
                                source.getArchetypes( getArchetypeDataSourceProperties( sourceRoleHint, archetypeCatalogProperties ) ) );
                        }
                        catch ( ArchetypeDataSourceException e )
                        {
                            getLogger().warn( "Unable to get archetypes from " + sourceRoleHint + " source. [" + e.getMessage() + "]" );
                        }
                    }
                }

                if ( archetypes.size() == 0 )
                {
                    getLogger().debug( "Using wiki catalog" );

                    try
                    {
                        ArchetypeDataSource source = (ArchetypeDataSource) archetypeSources.get( "wiki" );

                        archetypes.addAll( source.getArchetypes( new Properties() ) );
                    }
                    catch ( ArchetypeDataSourceException e )
                    {
                        getLogger().warn( "Unable to get archetypes from default wiki  source. [" + e.getMessage() + "]" );
                    }
                }

                if ( archetypes.size() > 0 )
                {
                    Archetype archetype = archetypeSelectionQueryer.selectArchetype( archetypes );

                    definition.setArtifactId( archetype.getArtifactId() );

                    definition.setName( archetype.getArtifactId() );

                    definition.setGroupId( archetype.getGroupId() );

                    definition.setVersion( archetype.getVersion() );

                    definition.setRepository( archetype.getRepository() );

                    String goals = StringUtils.join( archetype.getGoals().iterator(), "," );

                    definition.setGoals( goals );
                }
            }
        }

        // Make sure the groupId and artifactId are valid, the version may just default to
        // the latest release.

        if ( !definition.isPartiallyDefined() )
        {
            throw new ArchetypeSelectionFailure( "No valid archetypes could be found to choose." );
        }

        //TODO: this needs to be remove and let the generator take the selection + configuration and do what's 
        //      necessary. The selector should not be downloading anything.

        repositories.add(
            archetypeRegistryManager.createRepository( definition.getRepository(), definition.getArtifactId() + "-repo" ) );

        if ( !archetypeArtifactManager.exists( definition, request.getLocalRepository(), repositories ) )
        {
            throw new UnknownArchetype(
                "The desired archetype does not exist (" + definition.getGroupId() + ":"
                    + definition.getArtifactId() + ":" + definition.getVersion()
                    + ")"
            );
        }
        else
        {
            request.setArchetypeGroupId( definition.getGroupId() );
            request.setArchetypeArtifactId( definition.getArtifactId() );
            request.setArchetypeVersion( definition.getVersion() );
            request.setArchetypeRepository( definition.getRepository() );
            request.setArchetypeGoals( definition.getGoals() );
            request.setArchetypeName( definition.getName() );
        }
    }
}
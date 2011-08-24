/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.mae.project.session;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.mae.MAEException;
import org.apache.maven.mae.boot.embed.MAEEmbeddingException;
import org.apache.maven.mae.boot.services.MAEServiceManager;
import org.apache.maven.mae.project.ProjectToolsException;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.impl.internal.EnhancedLocalRepositoryManager;
import org.sonatype.aether.repository.AuthenticationSelector;
import org.sonatype.aether.repository.ProxySelector;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

@Component( role = SessionInitializer.class )
public class DefaultSessionInitializer
    implements SessionInitializer
{

    private static final Logger LOGGER = Logger.getLogger( DefaultSessionInitializer.class );

    @Requirement
    private MAEServiceManager serviceManager;

    @Requirement
    private RepositorySystem mavenRepositorySystem;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private PlexusContainer container;

    @Override
    public synchronized void initializeSessionComponents( final ProjectToolsSession session )
        throws ProjectToolsException
    {
        if ( session.isInitialized() )
        {
            return;
        }

        RepositorySystemSession rss = getRepositorySystemSession( session );

        MavenSession mavenSession =
            new MavenSession( container, rss, session.getExecutionRequest(), session.getExecutionResult() );

        legacySupport.setSession( mavenSession );

        List<ArtifactRepository> artifactRepos =
            getArtifactRepositories( session.getRepositoryDefinitionsForResolution(), rss );

        List<RemoteRepository> remoteRepositories = getRemoteRepositories( rss, artifactRepos );

        ProjectBuildingRequest pbr = getProjectBuildingRequest( session, rss, artifactRepos );
        pbr = pbr.setRemoteRepositories( artifactRepos );
        pbr.setRepositorySession( rss );

        session.initialize( rss, pbr, artifactRepos, remoteRepositories );
    }

    private RepositorySystemSession getRepositorySystemSession( final ProjectToolsSession session )
        throws ProjectToolsException
    {
        final File localRepo = session.getLocalRepositoryDirectory();

        RepositorySystemSession sess = session.getRepositorySystemSession();
        if ( sess == null )
        {
            DefaultRepositorySystemSession rss;
            try
            {
                rss =
                    new DefaultRepositorySystemSession(
                                                        serviceManager.createAetherRepositorySystemSession( session.getExecutionRequest() ) );
            }
            catch ( MAEEmbeddingException e )
            {
                throw new ProjectToolsException( "Failed to create RepositorySystemSession instance: %s", e,
                                                 e.getMessage() );
            }

            if ( localRepo != null )
            {
                localRepo.mkdirs();
                rss.setLocalRepositoryManager( new EnhancedLocalRepositoryManager( localRepo ) );
            }

            rss.setWorkspaceReader( new SessionWorkspaceReader( session ) );

            sess = rss;
        }

        sess.getData().set( ProjectToolsSession.SESSION_KEY, session );

        return sess;
    }

    private ProjectBuildingRequest getProjectBuildingRequest( final ProjectToolsSession session,
                                                              final RepositorySystemSession rss,
                                                              final List<ArtifactRepository> artifactRepos )
        throws ProjectToolsException
    {
        ProjectBuildingRequest pbr = session.getProjectBuildingRequest();
        try
        {
            if ( pbr == null )
            {
                pbr = serviceManager.createProjectBuildingRequest( session.getTemplateProjectBuildingRequest() );

                pbr.setValidationLevel( session.getPomValidationLevel() );
                pbr.setProcessPlugins( session.isProcessPomPlugins() );
                pbr.setResolveDependencies( false );
                pbr.setSystemProperties( System.getProperties() );
                pbr.setInactiveProfileIds( new ArrayList<String>() );
                pbr.setRepositoryMerging( ProjectBuildingRequest.RepositoryMerging.REQUEST_DOMINANT );

                pbr.setRepositorySession( rss );
                pbr.setLocalRepository( mavenRepositorySystem.createLocalRepository( rss.getLocalRepository().getBasedir() ) );
                pbr.setRemoteRepositories( artifactRepos );
            }
            else
            {
                pbr = new DefaultProjectBuildingRequest( pbr );
                pbr.setRepositorySession( getRepositorySystemSession( session ) );
            }
        }
        catch ( final MAEException e )
        {
            throw new ProjectToolsException( "Failed to create project-building request: %s", e, e.getMessage() );
        }
        catch ( final InvalidRepositoryException e )
        {
            throw new ProjectToolsException( "Failed to create local-repository instance. Reason: %s", e,
                                             e.getMessage() );
        }

        return pbr;
    }

    private List<RemoteRepository> getRemoteRepositories( final RepositorySystemSession rss,
                                                          final List<ArtifactRepository> artifactRepos )
        throws ProjectToolsException
    {
        List<RemoteRepository> result = new ArrayList<RemoteRepository>();

        for ( final ArtifactRepository repo : artifactRepos )
        {
            RemoteRepository r = null;
            if ( repo instanceof RemoteRepository )
            {
                r = (RemoteRepository) repo;
            }
            else if ( repo instanceof MavenArtifactRepository )
            {
                r = new RemoteRepository( repo.getId(), "default", repo.getUrl() );
            }

            if ( r != null )
            {
                result.add( r );
            }
        }

        boolean selectorsEnabled = false;
        AuthenticationSelector authSelector = null;
        ProxySelector proxySelector = null;
        if ( rss != null )
        {
            selectorsEnabled = true;
            authSelector = rss.getAuthenticationSelector();
            proxySelector = rss.getProxySelector();
        }
        else
        {
            LOGGER.warn( "Cannot set proxy or authentication information on new RemoteRepositories; "
                + "RepositorySystemSession is not available in ProjectToolsSession instance." );
        }

        if ( selectorsEnabled )
        {
            for ( RemoteRepository r : result )
            {
                r.setAuthentication( authSelector.getAuthentication( r ) );
                r.setProxy( proxySelector.getProxy( r ) );
            }
        }

        return result;
    }

    private List<ArtifactRepository> getArtifactRepositories( final Repository[] repoDefinitions,
                                                              final RepositorySystemSession rss )
        throws ProjectToolsException
    {
        List<ArtifactRepository> repos =
            new ArrayList<ArtifactRepository>( repoDefinitions == null ? 0 : repoDefinitions.length );

        if ( repoDefinitions != null )
        {
            for ( final Repository repo : repoDefinitions )
            {
                try
                {
                    repos.add( mavenRepositorySystem.buildArtifactRepository( repo ) );
                }
                catch ( final InvalidRepositoryException e )
                {
                    throw new ProjectToolsException(
                                                     "Failed to create remote artifact repository instance from: %s\nReason: %s",
                                                     e, repo, e.getMessage() );
                }
            }
        }

        try
        {
            repos.add( mavenRepositorySystem.createDefaultRemoteRepository() );
        }
        catch ( final InvalidRepositoryException e )
        {
            throw new ProjectToolsException( "Failed to create default (central) repository instance: %s", e,
                                             e.getMessage() );
        }

        mavenRepositorySystem.injectAuthentication( rss, repos );

        return repos;
    }

}

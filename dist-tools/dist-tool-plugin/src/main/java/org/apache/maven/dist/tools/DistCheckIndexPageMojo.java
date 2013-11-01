package org.apache.maven.dist.tools;

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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReportException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Check index page for information about components.
 *
 * @author skygo
 */
@Mojo( name = "check-index-page", requiresProject = false )
public class DistCheckIndexPageMojo
        extends AbstractDistCheckMojo
{
    static final String FAILURES_FILENAME = "check-index-page.log";

    private static final Map<String, Object[]> INDEXES_REF;

    static
    {
        Map<String, Object[]> aMap = new HashMap<>();
        // url title version date
        aMap.put( "IP1", new Object[]
        {
            "http://maven.apache.org/plugins/", "Plugins", 2, 3
        } );
        aMap.put( "IP2", new Object[]
        {
            "http://maven.apache.org/shared/", "Shared", 1, 2
        } );
        aMap.put( "IP3", new Object[]
        {
            "http://maven.apache.org/skins/", "Skins", 1, null
        } );
        aMap.put( "IP4", new Object[]
        {
            "http://maven.apache.org/pom/", "Poms", 1, 2
        } );
        INDEXES_REF = Collections.unmodifiableMap( aMap );
    }
    /**
     * Ignore dist failure for <code>artifactId</code> or <code>artifactId:version</code>
     */
    @Parameter
    protected List<String> ignoreDistFailures;

    @Override
    protected String getFailuresFilename()
    {
        return FAILURES_FILENAME;
    }

    @Override
    public String getOutputName()
    {
        return "dist-tool-checkindexpage";
    }

    @Override
    public String getName( Locale locale )
    {
        return "Disttool> Index Page";
    }

    @Override
    public String getDescription( Locale locale )
    {
        return "Verification of index page";
    }

    @Override
    boolean useDetailed()
    {
        return true;
    }

    
    private static class CheckIndexPageResult
        extends AbstractCheckResult
    {

        private String indexVersion;
        private String indexDate;
        
        public CheckIndexPageResult( ConfigurationLineInfo r, String version )
        {
            super( r, version );
        }

        private void setIndexVersion( String ownText )
        {
            this.indexVersion = ownText;
        }

        private void setIndexDate( String ownText )
        {
            this.indexDate = ownText;
        }
    }
    private final Map<String, List<CheckIndexPageResult>> results = new HashMap<>();


    private void reportLine( Sink sink, CheckIndexPageResult cipr , boolean displayDate )
    {
        ConfigurationLineInfo cli = cipr.getConfigurationLine();

        sink.tableRow();
        sink.tableCell();
        sink.rawText( cipr.getConfigurationLine().getArtifactId() );
        sink.tableCell_();

        // LATEST column
        sink.tableCell();
        sink.link( cli.getMetadataFileURL( repoBaseUrl ) );
        sink.rawText( cipr.getVersion() );
        sink.link_();
        if ( cipr.getVersion().equals( cipr.indexVersion ) )
        {
            iconSuccess( sink );
        }
        else
        {
            iconError( sink );
            sink.lineBreak();
            sink.rawText( cipr.indexVersion + " in index page" );
        }
        sink.tableCell_();

        // DATE column
        if ( displayDate )
        {
            sink.tableCell();
            sink.rawText( cipr.getConfigurationLine().getReleaseDateFromMetadata() );
            if ( cipr.getConfigurationLine().getReleaseDateFromMetadata().equals( cipr.indexDate ) )
            {
                iconSuccess( sink );
            }
            else
            {
                iconError( sink );
                sink.lineBreak();
                sink.rawText( cipr.indexDate + " in index page" );
            }
            sink.tableCell_();
        }
        // central column
        
        sink.tableRow_();
    }

    @Override
    protected void executeReport( Locale locale )
            throws MavenReportException
    {
        if ( !outputDirectory.exists() )
        {
            outputDirectory.mkdirs();
        }
        try
        {
            this.execute();
        }
        catch ( MojoExecutionException ex )
        {
            throw new MavenReportException( ex.getMessage(), ex );
        }

        Sink sink = getSink();
        sink.head();
        sink.title();
        sink.text( "Check index pages" );
        sink.title_();
        sink.head_();

        sink.body();
        sink.section1();
        sink.paragraph();
        sink.text( "Check that index pages have been updated with latest release info." );
        sink.paragraph_();
        sink.section1_();

        for ( Map.Entry<String, List<CheckIndexPageResult>> result: results.entrySet() )
        {
            String indexPageId = result.getKey();
            Object[] indexPageInfo = INDEXES_REF.get( indexPageId );
            String indexPageUrl = (String) indexPageInfo[0];
            String indexPageName = (String) indexPageInfo[1];
            List<CheckIndexPageResult> indexPageResults = result.getValue();

            sink.sectionTitle2();
            sink.text( indexPageName + " index page: " );
            sink.anchor( indexPageUrl );
            sink.text( indexPageUrl );
            sink.anchor_();
            sink.sectionTitle2_();

            sink.table();
            sink.tableRow();
            sink.tableHeaderCell();
            sink.rawText( "Component" );
            sink.tableHeaderCell_();
            sink.tableHeaderCell();
            sink.rawText( "VERSION" );
            sink.tableHeaderCell_();
            boolean displayDate = indexPageInfo[3] != null;
            if ( displayDate )
            {
                sink.tableHeaderCell();
                sink.rawText( "DATE" );
                sink.tableHeaderCell_();
            }
            sink.tableRow_();

            for ( CheckIndexPageResult csr : indexPageResults )
            {
                reportLine( sink, csr, displayDate );
            }
            sink.table_();
        }
 
        sink.body_();
        sink.flush();
        sink.close();
    }

    private void updateIndexPageInfo( ConfigurationLineInfo cli, CheckIndexPageResult r, Object[] inf )
        throws IOException
    {
        try
        {
            Document doc = Jsoup.connect( (String) inf[0] ).get();
            Elements a = doc.select( "tr > td > a[href]:not(.externalLink)" );
            for ( Element e : a )
            {
                // skins do not have release date
                String art = e.attr( "href" );
                String id = cli.getArtifactId();
                // UGLY 
                if ( cli.getArtifactId().equals( "maven-parent" ) )
                {
                    id = "maven/";
                }
                if ( cli.getArtifactId().equals( "maven-skins" ) )
                {
                    id = "skins/";
                }
                if ( cli.getArtifactId().equals( "apache" ) )
                {
                    id = "asf/";
                }

                if ( art.contains( id ) )
                {
                    r.setIndexVersion( e.parent().parent().child( ( Integer ) inf[2] ).ownText() );
                    if ( inf[3] != null )
                    {
                        r.setIndexDate( e.parent().parent().child( ( Integer ) inf[3] ).ownText() );
                    }
               }
            }
        }
        catch ( IOException ioe )
        {
            throw new IOException( "IOException while reading " + (String) inf[0] , ioe );
        }
    }

    @Override
    protected void checkArtifact( ConfigurationLineInfo configLine, String version )
            throws MojoExecutionException
    {
        try
        {
            CheckIndexPageResult result = new CheckIndexPageResult( configLine, version );

            if ( configLine.getIndexPageId() != null )
            {
                if ( results.get( configLine.getIndexPageId() ) == null )
                {
                    results.put( configLine.getIndexPageId(), new LinkedList<CheckIndexPageResult>() );
                } 
                results.get( configLine.getIndexPageId() ).add( result );
                updateIndexPageInfo( configLine, result, INDEXES_REF.get( configLine.getIndexPageId() ) );
            }
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( ex.getMessage(), ex );
        }
    }
}

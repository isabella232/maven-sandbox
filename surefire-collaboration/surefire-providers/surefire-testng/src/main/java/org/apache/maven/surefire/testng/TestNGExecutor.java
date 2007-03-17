package org.apache.maven.surefire.testng;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.suite.SurefireTestSuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.TestNG;
import org.testng.internal.annotations.AnnotationConfiguration;
import org.testng.xml.XmlSuite;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Contains utility methods for executing TestNG.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class TestNGExecutor
{
    private TestNGExecutor()
    {
    }

    static void executeTestNG( SurefireTestSuite surefireSuite, String testSourceDirectory, XmlSuite suite,
                               ReporterManager reporterManager )
    {
        TestNG testNG = new TestNG( false );

        // turn off all TestNG output
        testNG.setVerbose( 0 );

        testNG.setXmlSuites( Collections.singletonList( suite ) );

        testNG.setListenerClasses( new ArrayList() );

        TestNGReporter reporter = new TestNGReporter( reporterManager, surefireSuite );
        testNG.addListener( (ITestListener) reporter );
        testNG.addListener( (ISuiteListener) reporter );

        String jre = System.getProperty("java.vm.version");
        if (jre.indexOf("1.4") > -1) {
            AnnotationConfiguration.getInstance().initialize(AnnotationConfiguration.JVM_14_CONFIG);
            AnnotationConfiguration.getInstance().getAnnotationFinder().addSourceDirs(new String[]{testSourceDirectory});
        } else {
            AnnotationConfiguration.getInstance().initialize(AnnotationConfiguration.JVM_15_CONFIG);
        }
        
        // Set source path so testng can find javadoc annotations if not in 1.5 jvm
        if ( testSourceDirectory != null )
        {
            testNG.setSourcePath( testSourceDirectory );
        }
        
        // workaround for SUREFIRE-49
        // TestNG always creates an output directory, and if not set the name for the directory is "null"
        testNG.setOutputDirectory( System.getProperty( "java.io.tmpdir" ) );
        
        testNG.runSuitesLocally();
        
        // need to execute report end after testng has completely finished as the 
        // reporter methods don't get called in the order that would allow for capturing
        // failures that happen in before/after suite configuration methods
        
        reporter.cleanupAfterTestsRun();
    }
}

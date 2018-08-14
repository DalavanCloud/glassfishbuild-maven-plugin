/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.build.utils;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.taskdefs.Zip.Duplicate;
import org.apache.tools.ant.types.ZipFileSet;

/**
 * Helper to create zip files using ant.
 */
final class ZipHelper {

    /**
     * Create a new {@code ZipHelper} instance.
     */
    private ZipHelper() {
    }

    /**
     * Lazy singleton holder.
     */
    private static class LazyHolder {

        /**
         * The singleton instance.
         */
        static final ZipHelper INSTANCE = new ZipHelper();
    }

    /**
     * Get the Singleton instance for {@code ZipHelper}.
     * @return the {@code ZipHelper} instance
     */
    static ZipHelper getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * Create a zip file.
     * @param properties Ant project properties
     * @param mavenLog Maven logger
     * @param duplicate behavior for duplicate file, one of "add", "preserve"
     * or "fail"
     * @param fsets list of {@code ZipFileSet} that describe the resources to
     * zip
     * @param target the {@code File} instance for the zip file to create
     */
    void zip(final Properties properties,
            final Log mavenLog,
            final String duplicate,
            final List<ZipFileSet> fsets,
            final File target) {

        Project antProject = new Project();
        antProject.addBuildListener(new AntBuildListener(mavenLog));
        Iterator it = properties.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            antProject.setProperty(key, properties.getProperty(key));
        }

        Zip zip = new Zip();
        zip.setProject(antProject);
        zip.setDestFile(target);
        Duplicate df = new Duplicate();
        df.setValue(duplicate);
        zip.setDuplicate(df);
        mavenLog.info(String.format("[zip] duplicate: %s", duplicate));

        List<ZipFileSet> filesets;
        if (fsets == null) {
            filesets = Collections.EMPTY_LIST;
        } else {
            filesets = fsets;
        }

        if (filesets.isEmpty()) {
            ZipFileSet zfs = MavenHelper.createZipFileSet(new File(""), "", "");
            // work around for
            // http://issues.apache.org/bugzilla/show_bug.cgi?id=42122
            zfs.setDirMode("755");
            zfs.setFileMode("644");
            filesets.add(zfs);
        }

        for (ZipFileSet fset : filesets) {
            zip.addZipfileset(fset);
            String desc = fset.getDescription();
            if (desc != null && !desc.isEmpty()) {
                mavenLog.info(String.format("[zip] %s", desc));
            }
        }
        zip.executeMain();
    }

    /**
     * {@code BuilderListener} implementation to log Ant events.
     */
    private static final class AntBuildListener implements BuildListener {

        /**
         * Maximum Event priority that is logged.
         */
        private static final int MAX_EVENT_PRIORITY = 3;

        /**
         * Maven logger.
         */
        private final Log log;

        /**
         * Create a new {@code AntBuildListener} instance.
         * @param mavenLog Maven logger
         */
        private AntBuildListener(final Log mavenLog) {
            this.log = mavenLog;
        }

        @Override
        public void buildStarted(final BuildEvent event) {
        }

        @Override
        public void buildFinished(final BuildEvent event) {
        }

        @Override
        public void targetStarted(final BuildEvent event) {
        }

        @Override
        public void targetFinished(final BuildEvent event) {
        }

        @Override
        public void taskStarted(final BuildEvent event) {
        }

        @Override
        public void taskFinished(final BuildEvent event) {
        }

        @Override
        public void messageLogged(final BuildEvent event) {
            if (event.getPriority() < MAX_EVENT_PRIORITY) {
                log.info(String.format("[zip] %s", event.getMessage()));
            } else {
                log.debug(String.format("[zip] %s", event.getMessage()));
            }
        }
    }
}

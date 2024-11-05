/*
 * The MIT License
 *
 * Copyright (c) 2016 Saponenko Denis
 * Copyright (c) 2018 Sam Gleske - https://github.com/samrocketman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.pipeline.multibranch.defaults;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TaskListener;
import jenkins.branch.MultiBranchProject;
import jenkins.branch.MultiBranchProjectFactory;
import jenkins.branch.MultiBranchProjectFactoryDescriptor;
import jenkins.branch.OrganizationFolder;
import jenkins.model.TransientActionFactory;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.cps.Snippetizer;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Recognizes and builds by default {@code Jenkinsfile}.
 */
public class PipelineMultiBranchDefaultsProjectFactory extends MultiBranchProjectFactory.BySCMSourceCriteria {
    public static final String SCRIPT = "Jenkinsfile";

    private String scriptId = SCRIPT;
    private boolean useSandbox = false;

    @DataBoundConstructor
    public PipelineMultiBranchDefaultsProjectFactory() { }

    /**
     * Set the script ID which will be used to reference the config file
     * management for the contents of a Jenkinsfile.
     *
     * @param scriptId The ID of the groovy script to read as a Jenkinsfile.
     */
    @DataBoundSetter
    public void setScriptId(String scriptId) {
        if(StringUtils.isEmpty(scriptId)) {
            this.scriptId = SCRIPT;
        } else {
            this.scriptId = scriptId;
        }
    }

    /**
     * Get the script ID which will be used to read the Jenkinsfile from config
     * file management.
     *
     * @return The Jenkinsfile script ID.
     */
    public String getScriptId() {
        return scriptId;
    }

    /**
     * Set whether or not a Jenkinsfile should run within a Groovy sandbox.
     *
     * @param useSandbox Set true to enable Groovy sandbox or false to run in
     *                   Jenkins master runtime.
     */
    @DataBoundSetter
    public void setUseSandbox(boolean useSandbox) {
        this.useSandbox = useSandbox;
    }

    /**
     * Get the current setting for whether or not to use a groovy sandbox.
     *
     * @return true if using a groovy sandbox is desired.
     */
    public boolean getUseSandbox() {
        return this.useSandbox;
    }

    @Override
    protected WorkflowMultiBranchProject doCreateProject(ItemGroup<?> parent, String name, Map<String,Object> attributes){
        WorkflowMultiBranchProject project = new WorkflowMultiBranchProject(parent, name);
        configureProjectFactoryFor(project);
        return project;
    }

    @Override
    public void updateExistingProject(MultiBranchProject<?, ?> project, Map<String, Object> attributes, TaskListener listener) throws IOException, InterruptedException {
        if (project instanceof WorkflowMultiBranchProject branchProject) {
            configureProjectFactoryFor(branchProject);
        }
    }

    @Override
    protected SCMSourceCriteria getSCMSourceCriteria(SCMSource source) {
        return new SCMSourceCriteria() {
            @Override
            public boolean isHead(Probe probe, TaskListener listener) throws IOException {
                return true;
            }

            @Override
            public int hashCode() {
                return getClass().hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return getClass().isInstance(obj);
            }
        };
    }

    @Extension
    public static class PerFolderAdder extends TransientActionFactory<OrganizationFolder> {

        @Override
        public Class<OrganizationFolder> type() {
            return OrganizationFolder.class;
        }

        @Override
        public Collection<? extends Action> createFor(OrganizationFolder target) {
            if (target.getProjectFactories().get(PipelineMultiBranchDefaultsProjectFactory.class) != null && target.hasPermission(Item.EXTENDED_READ)) {
                return Collections.singleton(new Snippetizer.LocalAction());
            } else {
                return Collections.emptySet();
            }
        }
    }

    @Extension
    public static class DescriptorImpl extends MultiBranchProjectFactoryDescriptor {
        @Override
        public MultiBranchProjectFactory newInstance() {
            return null;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "by default " + SCRIPT;
        }
    }

    private void configureProjectFactoryFor(WorkflowMultiBranchProject project) {
        PipelineBranchDefaultsProjectFactory projectFactory = new PipelineBranchDefaultsProjectFactory();
        projectFactory.setScriptId(scriptId);
        projectFactory.setUseSandbox(useSandbox);
        project.setProjectFactory(projectFactory);
    }
}

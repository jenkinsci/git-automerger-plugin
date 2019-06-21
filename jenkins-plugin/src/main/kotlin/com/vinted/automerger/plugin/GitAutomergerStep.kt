package com.vinted.automerger.plugin

import com.vinted.automerger.AutoMergerBuilder
import com.vinted.slf4j.adapter.SLF4JOutputStreamAdapter
import hudson.FilePath
import hudson.Launcher
import hudson.model.Descriptor
import hudson.model.Run
import hudson.model.TaskListener
import hudson.remoting.VirtualChannel
import hudson.tasks.BuildStepMonitor
import hudson.tasks.BuildStepMonitor.NONE
import hudson.tasks.Builder
import hudson.util.FormValidation
import jenkins.tasks.SimpleBuildStep
import org.jenkinsci.remoting.RoleChecker
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter
import java.io.File


class GitAutomergerStep @DataBoundConstructor constructor(
    val releaseBranchPattern: String,
    val mergeRules: List<JenkinsMergeRule>?,
    val logLevel: LogLevel,
    val remoteName: String,
    val checkoutFromRemote: Boolean
) : Builder(), SimpleBuildStep {

    override fun getDescriptor(): Descriptor<Builder> = DESCRIPTOR

    override fun getRequiredMonitorService(): BuildStepMonitor = NONE

    fun doCheckReleaseBranchPattern(@QueryParameter releaseBranchPattern: String): FormValidation {
        if (releaseBranchPattern.trim().isEmpty()) {
            return FormValidation.error("releaseBranchPattern can't be empty.")
        } else if (!releaseBranchPattern.contains("%")) {
            return FormValidation.error("You must use '%' which denotes version number in branch name")
        }
        return FormValidation.ok()
    }

    fun doCheckRemoteName(@QueryParameter remoteName: String): FormValidation {
        return if (remoteName.isEmpty()) {
            FormValidation.error("Remote name can't be empty. In most cases you should use 'origin'")
        } else {
            FormValidation.ok()
        }
    }

    override fun perform(run: Run<*, *>, workspace: FilePath, launcher: Launcher, listener: TaskListener) {
        val logger = SLF4JOutputStreamAdapter(listener.logger, logLevel.levelInt)

        workspace.act(object : FilePath.FileCallable<Any> {
            override fun checkRoles(checker: RoleChecker) {
                throw SecurityException()
            }

            override fun invoke(file: File, channel: VirtualChannel): Any {
                val builder = AutoMergerBuilder()
                    .pathToRepo(file)
                    .releaseBranchPattern(releaseBranchPattern)
                    .logger(logger)
                    .checkoutFromRemote(checkoutFromRemote)
                    .remoteName(remoteName)

                mergeRules.orEmpty().map(JenkinsMergeRule::toMergeConfig).forEach {
                    builder.addMergeConfig(it)
                }

                builder.build().automerge()

                return Unit
            }
        })
    }

    companion object {
        @JvmStatic
        val DESCRIPTOR = GitAutomergerDescriptor()
    }
}

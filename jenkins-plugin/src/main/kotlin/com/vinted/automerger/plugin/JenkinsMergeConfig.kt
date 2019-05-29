package com.vinted.automerger.plugin

import com.vinted.automerger.config.ConflictSolverMode
import com.vinted.automerger.config.MergeConfig
import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.util.FormValidation
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter

/**
 * This class is only wrapper to [MergeConfig] for Jenkins data-bounding.
 */
data class JenkinsMergeConfig @DataBoundConstructor constructor(
    val path: String,
    val mode: ConflictSolverMode
) : AbstractDescribableImpl<JenkinsMergeConfig>() {

    fun toMergeConfig() = MergeConfig(path, mode)

    fun doCheckPath(@QueryParameter path: String): FormValidation {
        if (path.isEmpty()) {
            return FormValidation.error("Path can't be empty")
        }

        return FormValidation.ok()
    }

    @Extension
    class DescriptorImpl : hudson.model.Descriptor<JenkinsMergeConfig>() {
        override fun getDisplayName(): String {
            return "Merge conflict config"
        }
    }
}

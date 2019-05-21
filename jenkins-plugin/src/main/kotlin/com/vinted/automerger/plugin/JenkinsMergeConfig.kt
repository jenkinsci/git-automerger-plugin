package com.vinted.automerger.plugin

import com.vinted.automerger.config.ConflictSolverMode
import com.vinted.automerger.config.MergeConfig
import hudson.Extension
import hudson.model.AbstractDescribableImpl
import org.kohsuke.stapler.DataBoundConstructor

/**
 * This class is only wrapper to [MergeConfig] for Jenkins data-bounding.
 */
data class JenkinsMergeConfig @DataBoundConstructor constructor(
    val path: String,
    val mode: ConflictSolverMode
) : AbstractDescribableImpl<JenkinsMergeConfig>() {

    fun toMergeConfig() = MergeConfig(path, mode)

    @Extension
    class DescriptorImpl : hudson.model.Descriptor<JenkinsMergeConfig>() {
        override fun getDisplayName(): String {
            return "Merge conflict config"
        }
    }
}

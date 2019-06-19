package com.vinted.automerger

import com.vinted.automerger.config.MergeRule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class AutoMergerBuilder {

    var releaseBranchPattern = "release/%"
        private set

    val mergeConfigs = mutableListOf<MergeRule>()

    var pathToRepo: File? = null
        private set

    var logger: Logger = LoggerFactory.getLogger(AutoMerger::class.java)
        private set

    /**
     * null means do not use remote branches
     */
    var remote: String? = null
        private set

    /**
     * Release branch pattern must contain `%` which denotes to version.
     * For example "release/%" is applicable release/8.10 and release/0.1
     */
    fun releaseBranchPattern(releaseBranchPattern: String) = apply { this.releaseBranchPattern = releaseBranchPattern }

    /**
     * Only configured conflicts are solved.
     */
    fun addMergeConfig(mergeRule: MergeRule) = apply { this.mergeConfigs += mergeRule }

    /**
     * Path to repo where `.git` folder exist
     */
    fun pathToRepo(pathToRepo: File) = apply { this.pathToRepo = pathToRepo }

    fun logger(logger: Logger) = apply { this.logger = logger }

    /**
     * @param remote makes tool to scan remote branches `refs/remotes/<remote>/%`.
     * Otherwise `refs/heads/%` are scanned.
     * In most cases you would like to use`origin`.
     * This property does not allow plugin to fetch remote repositories.
     */
    fun remote(remote: String) = apply { this.remote = remote }

    fun build() = AutoMerger(this)
}

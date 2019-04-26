package com.vinted.automerger

import com.vinted.automerger.config.MergeConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class AutoMergerBuilder {

    var releaseBranchPattern = "release/%"
        private set

    val mergeCongif = mutableListOf<MergeConfig>()

    var pathToRepo: File? = null
        private set

    var logger: Logger = LoggerFactory.getLogger(AutoMerger::class.java)
        private set

    /**
     * Release branch pattern must contain `%` which denotes to version.
     * For example "release/%" is applicable release/8.10 and release/0.1
     */
    fun releaseBranchPattern(releaseBranchPattern: String) = apply { this.releaseBranchPattern = releaseBranchPattern }

    /**
     * Only configured conflicts are solved.
     */
    fun addMergeConfig(mergeConfig: MergeConfig) = apply { this.mergeCongif += mergeConfig }

    /**
     * Path to repo where `.git` folder exist
     */
    fun pathToRepo(pathToRepo: File) = apply { this.pathToRepo = pathToRepo }

    fun logger(logger: Logger) = apply { this.logger = logger }

    fun build() = AutoMerger(this)
}

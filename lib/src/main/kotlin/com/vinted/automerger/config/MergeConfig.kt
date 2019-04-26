package com.vinted.automerger.config

/**
 * @param path must be relative path to file. Patterns is not supported
 */
data class MergeConfig(val path: String, val mode: ConflictSolverMode)

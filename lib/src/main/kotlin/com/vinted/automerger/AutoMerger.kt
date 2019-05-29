package com.vinted.automerger

import com.vinted.automerger.resolver.Resolver
import com.vinted.automerger.utils.checkNotEmpty
import com.vinted.automerger.utils.checkNotNull
import org.apache.maven.artifact.versioning.ComparableVersion
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.merge.MergeStrategy

class AutoMerger(autoMergerBuilder: AutoMergerBuilder) {
    private val releaseBranchPattern = autoMergerBuilder.releaseBranchPattern
        .checkNotEmpty("Pattern must be set. For example 'release/%'")

    private val mergeConfig = autoMergerBuilder.mergeConfigs.toList()

    private val pathToRepo = autoMergerBuilder.pathToRepo
        .checkNotNull("Path must be set")

    private val logger = autoMergerBuilder.logger

    private val git: Git

    val branchPatternRegex = releaseBranchPattern.replace("%", "([0-9.]+)").plus("$").toRegex()

    init {
        if (!pathToRepo.exists()) {
            throw IllegalArgumentException("Repository doesn't exist on $pathToRepo")
        }

        if (!pathToRepo.isDirectory) {
            throw IllegalArgumentException("Path $pathToRepo must be directory to repo")
        }

        if (!pathToRepo.resolve(".git").isDirectory) {
            throw IllegalArgumentException(".git was not found inside $pathToRepo")
        }

        git = Git.open(pathToRepo)
        logger.debug("Open git repo at $pathToRepo")
    }

    fun automerge() {
        val allReleaseBranches = allReleaseBranches()

        if (allReleaseBranches.size == 1) {
            logger.warn("Only applicable ${allReleaseBranches.first().name} branch was found. Nothing to merge")
            return
        }

        if (allReleaseBranches.isEmpty()) {
            logger.warn("Repository is missing 'master' and other applicable branches.")
            return
        }

        for ((from, to) in allReleaseBranches.windowed(2)) {
            logger.debug("Merge {} -> {}", from.name, to.name)
            with(git) {
                checkout().setName(to.name).call()
                val result = merge().include(repository.resolve(from.name)).setStrategy(MergeStrategy.RECURSIVE).call()

                try {
                    if (result.mergeStatus.isSuccessful) {
                        logger.debug("Merge was without conflict")
                    } else {
                        logger.debug("Conflict! Try to resolve them")

                        for ((fileName, _) in result.conflicts) {
                            logger.debug("> Conflict for file: $fileName")

                            tryResolveConflict(fileName)
                        }
                    }
                } catch (e: Exception) {
                    reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD").call()
                    throw e
                }
            }
        }
    }

    private fun tryResolveConflict(fileName: String) {
        val mode = mergeConfig.find { it.path == fileName }
        if (mode == null) {
            throw RuntimeException("There is no defined conflict solver for $fileName")
        } else {
            val currentFile = pathToRepo.resolve("$fileName")
            val tmpFile = pathToRepo.resolve("$fileName.tmp")

            if (tmpFile.exists()) {
                throw IllegalStateException("Can't create tmp file for merging: $tmpFile")
            }

            logger.debug("Solving $fileName")
            Resolver(currentFile.inputStream(), tmpFile.outputStream(), mode.mode).resolve()

            logger.debug("Conflict solved")
            tmpFile.copyTo(currentFile, overwrite = true)
            tmpFile.delete()
        }
    }

    private fun allReleaseBranches(): List<Ref> {
        return git.branchList().call()
            .flatMap { ref ->
                val name = ref.name.removePrefix("refs/heads/")
                val matches = branchPatternRegex.matchEntire(name)
                if (matches == null) {
                    emptyList()
                } else {
                    val version = ComparableVersion(matches.groups[1]!!.value)
                    listOf(ref to version)
                }
            }
            .sortedBy { it.second }
            .map { it.first }
            .plus(git.branchList().call().filter { it.name.endsWith("master") }.take(1))
    }
}

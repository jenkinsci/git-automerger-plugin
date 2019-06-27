package com.vinted.automerger

import com.vinted.automerger.resolver.Resolver
import com.vinted.automerger.utils.checkNotEmpty
import com.vinted.automerger.utils.checkNotNull
import org.apache.maven.artifact.versioning.ComparableVersion
import org.eclipse.jgit.api.*
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.merge.MergeStrategy

class AutoMerger(autoMergerBuilder: AutoMergerBuilder) {
    private val releaseBranchPattern = autoMergerBuilder.releaseBranchPattern
        .checkNotEmpty("Pattern must be set. For example 'release/%'")

    private val mergeConfig = autoMergerBuilder.mergeConfigs.toList()

    private val pathToRepo = autoMergerBuilder.pathToRepo
        .checkNotNull("Path must be set")

    private val logger = autoMergerBuilder.logger

    private val remoteName = autoMergerBuilder.remoteName
    private val checkoutFromRemote = autoMergerBuilder.checkoutFromRemote

    private val git: Git

    private val branchPatternRegex = releaseBranchPattern.replace("%", "([0-9.]+)").plus("$").toRegex()

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
        checkWorkingDir()

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
                            git.add().addFilepattern(fileName).call()
                        }

                        git.commit().call()
                    }
                } catch (e: Exception) {
                    reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD").call()
                    throw e
                }
            }
        }
    }

    private fun checkWorkingDir() {
        val status = git.status().call()
        if (!status.isClean) {
            logger.error("Working space is not clean")

            val dirtyFiles = status.added + status.removed + status.changed + status.untracked
            logger.debug("Dirty files: ${dirtyFiles.joinToString(", ")}")

            throw IllegalStateException("Working space is not clean")
        }
    }

    private fun tryResolveConflict(fileName: String) {
        val mode = mergeConfig.find { it.path == fileName }
        if (mode == null) {
            throw RuntimeException("There is no defined conflict solver for $fileName")
        } else {
            val currentFile = pathToRepo.resolve(fileName)
            val tmpFile = pathToRepo.resolve("$fileName.tmp")

            if (tmpFile.exists()) {
                throw IllegalStateException("Can't create tmp file for merging: $tmpFile")
            }

            logger.debug("Solving $fileName")
            Resolver(currentFile.inputStream(), tmpFile.outputStream(), mode.resolution).resolve()

            logger.debug("Conflict solved")
            tmpFile.copyTo(currentFile, overwrite = true)
            tmpFile.delete()
        }
    }

    private fun allReleaseBranches(): List<Ref> {
        if (checkoutFromRemote) {
            logger.info("Checkout from remote $remoteName")
            val gitRefPath = "refs/remotes/$remoteName/"
            val localBranches = git.branchList().call().map { it.name.removePrefix("refs/heads/") }
            git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call()
                .filter { it.name.startsWith(gitRefPath) }
                .forEach { ref ->
                    val name = ref.name.removePrefix(gitRefPath)

                    if (!name.matches(branchPatternRegex) && name != MASTER) {
                        return@forEach
                    }

                    logger.info("Checkout ${ref.name} -> $name")

                    if (localBranches.contains(name)) {
                        logger.debug("Local branch '$name' was found and it is used, try to pull")
                        val result = git.pull()
                            .setRemote(remoteName)
                            .setRemoteBranchName(name)
                            .setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
                            .call()

                        if (!result.isSuccessful) {
                            throw java.lang.IllegalStateException("Local branch '$name' diverged from remote")
                        }
                    } else {
                        logger.debug("Local branch '$name' was not found. Do checkout for it")
                        git.checkout()
                            .setName(name)
                            .setCreateBranch(true)
                            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                            .setStartPoint(ref.name)
                            .call()
                    }
                }
        }
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
            .plus(git.branchList().call().filter { it.name.endsWith(MASTER) }.take(1))
    }

    companion object {
        private const val MASTER = "master"
    }
}

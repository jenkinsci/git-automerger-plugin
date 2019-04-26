package com.vinted.automerger.testutils

import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File

class RepoExtension : BeforeEachCallback, AfterEachCallback {
    lateinit var path: File
        private set

    lateinit var git: Git
        private set

    override fun beforeEach(context: ExtensionContext) {
        path = File.createTempFile("AutomergerTestRepository-${System.currentTimeMillis()}", "")

        if (!path.delete()) {
            throw RuntimeException("Failed delete $path file")
        }

        git = Git.init().setDirectory(path).call()

        with(git) {
            commit().setAllowEmpty(true).setMessage("init commit").call()
            createRelease("8.9")
            createRelease("8.10")
            checkoutMaster()
        }
    }

    override fun afterEach(context: ExtensionContext) {
        if (!path.deleteRecursively()) {
            System.err.println("Failed delete temporally created repo folder $path")
        }
    }
}

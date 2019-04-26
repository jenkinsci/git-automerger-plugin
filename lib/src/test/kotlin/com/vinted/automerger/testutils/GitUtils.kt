package com.vinted.automerger.testutils

import org.eclipse.jgit.api.Git

fun Git.checkoutBranch(branch: String) {
    checkout().setName(branch).call()
}

fun Git.checkoutMaster() {
    checkoutBranch("master")
}

fun Git.createRelease(version: String) {
    createBranchWithCommit("release/$version", "Release $version")
}

fun Git.createBranchWithCommit(branch: String, commitMessage: String) {
    checkoutMaster()
    checkout().setCreateBranch(true).setName(branch).call()
    commit().setAllowEmpty(true).setMessage(commitMessage).call()
    checkoutMaster()
}

fun Git.commitAll(message: String) {
    add().addFilepattern(".").call()
    commit().setMessage(message).call()
}

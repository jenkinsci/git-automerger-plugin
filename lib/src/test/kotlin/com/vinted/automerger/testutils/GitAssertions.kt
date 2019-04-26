package com.vinted.automerger.testutils

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.junit.jupiter.api.Assertions.assertEquals

fun Git.assertLatestCommitMessages(branchName: String, vararg messages: String) {
    val branch = branchList().call().find { it.name == "refs/heads/$branchName" }
    branch ?: throw AssertionError("Branch $branchName not found")

    val realMessages = log()
        .setMaxCount(messages.size)
        .add(branch.objectId)
        .setRevFilter(RevFilter.NO_MERGES)
        .call()
        .map { it.shortMessage }

    assertEquals(messages.toSet(), realMessages.take(messages.size).toSet())
}

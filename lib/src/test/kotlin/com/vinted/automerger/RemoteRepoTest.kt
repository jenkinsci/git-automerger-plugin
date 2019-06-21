package com.vinted.automerger

import com.vinted.automerger.testutils.*
import org.eclipse.jgit.transport.URIish
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension

class RemoteRepoTest {
    @RegisterExtension
    @JvmField
    val origin: RepoExtension = DefaultRepoExtension()

    @RegisterExtension
    @JvmField
    val repo: RepoExtension = RepoExtension()

    private lateinit var defaultBuilder: AutoMergerBuilder

    @BeforeEach
    fun setUp() {
        with(repo.git) {
            remoteAdd().setName("origin").setUri(URIish(origin.path.absolutePath)).call()
            fetch().setRemote("origin").call()
        }

        defaultBuilder = AutoMergerBuilder().pathToRepo(repo.path).checkoutFromRemote(true)
    }

    @Test
    fun useExistingRemote() {
        val fixture = defaultBuilder.build()

        fixture.automerge()

        with(repo.git) {
            assertLatestCommitMessages("release/8.9", "Release 8.9")
            assertLatestCommitMessages("release/8.10", "Release 8.10", "Release 8.9")
            assertLatestCommitMessages("master", "Release 8.10", "Release 8.9", "init commit")
        }
    }

    @Test
    fun masterWasCheckout() {
        val commit = "New commit in origin/master"
        with(origin.git) {
            checkoutMaster()
            commit().setMessage(commit).setAllowEmpty(true).call()
        }

        val fixture = defaultBuilder.build()

        fixture.automerge()

        with(repo.git) {
            val messages = commitMessageList("master")

            assertTrue(messages.contains(commit), "Commit from origin/master was not found")
        }
    }

    @Test
    fun localBranchAlreadyExist() {
        with(repo.git) {
            createBranchWithCommit("release/8.9", "foo")
        }

        val fixture = defaultBuilder.build()

        assertThrows<IllegalStateException> {
            fixture.automerge()
        }
    }
}

package com.vinted.automerger.plugin

import hudson.FilePath
import hudson.model.Label
import hudson.model.Result.SUCCESS
import hudson.remoting.VirtualChannel
import org.eclipse.jgit.api.Git
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.remoting.RoleChecker
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule
import java.io.File

class GitAutomergerStepPipelineTest {

    @Rule
    @JvmField
    val jenkinsRule = JenkinsRule()

    lateinit var project: WorkflowJob
    lateinit var workspace: FilePath
    lateinit var git: Git

    @Before
    fun setUp() {
        project = jenkinsRule.createProject(WorkflowJob::class.java)

        workspace = jenkinsRule.instance.getWorkspaceFor(project)!!

        workspace.act(object : FilePath.FileCallable<Any> {
            override fun checkRoles(checker: RoleChecker) {
                throw SecurityException()
            }

            override fun invoke(file: File, channel: VirtualChannel): Any {
                git = Git.init().setDirectory(file).call()

                return Unit
            }
        })
    }

    @Test
    fun runWithBasicConfig() {
        project.definition = CpsFlowDefinition(
            """
            node {
                gitAutomerger logLevel: 'INFO',
                              mergeRules: [
                                  [path: 'CHANGELOG.md', resolution: 'MERGE_NEWER_TOP'],
                                  [path: 'version', resolution: 'KEEP_NEWER'],
                              ],
                              detailConflictReport: true,
                              checkoutFromRemote: true
            }
        """.trimIndent(), true
        )

        val build = project.scheduleBuild2(0)!!.get()

        println(build.log)

        assertEquals(SUCCESS, build.result)
    }

    @Test
    fun runAutomergerInAnotherNode() {
        jenkinsRule.createSlave(Label.parseExpression("android"))

        project.definition = CpsFlowDefinition(
            """
            node("android") {
                sh "git init"
            
                gitAutomerger logLevel: 'INFO',
                              mergeRules: [
                                  [path: 'CHANGELOG.md', resolution: 'MERGE_NEWER_TOP'],
                                  [path: 'version', resolution: 'KEEP_NEWER'],
                              ],
                              detailConflictReport: true,
                              checkoutFromRemote: true
            }
        """.trimIndent(), true
        )

        val build = project.scheduleBuild2(0)!!.get()

        println(build.log)

        assertEquals(SUCCESS, build.result)
    }
}

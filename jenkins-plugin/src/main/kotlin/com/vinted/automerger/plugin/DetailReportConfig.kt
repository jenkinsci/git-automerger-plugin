package com.vinted.automerger.plugin

import hudson.util.FormValidation
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.QueryParameter

class DetailReportConfig @DataBoundConstructor constructor() {
    @set:DataBoundSetter
    var limitAuthorsInDetailReport: Int = 3
    @set:DataBoundSetter
    var limitCommitsInDetailReport: Int = 3


    fun doCheckLimitAuthorsInDetailReport(@QueryParameter number: Int): FormValidation {
        return if (number < 1) {
            FormValidation.error("You must let report at least one author")
        } else {
            FormValidation.ok()
        }
    }

    fun doCheckLimitCommitsInDetailReport(@QueryParameter number: Int): FormValidation {
        return if (number < 0) {
            FormValidation.error("Comments limit must be 0 or any positive number")
        } else {
            FormValidation.ok()
        }
    }
}

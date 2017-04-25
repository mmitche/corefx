// Import the utility functionality.

import jobs.generation.JobReport;
import org.dotnet.ci.pipelines.Pipeline

// The input project name (e.g. dotnet/corefx)
def project = GithubProject
// The input branch name (e.g. master)
def branch = GithubBranchName

// **************************
// Define innerloop testing.  These jobs run on every merge and a subset of them run on every PR, the ones
// that don't run per PR can be requested via a magic phrase.
// **************************
newPipeline = Pipeline.createPipelineForGithub(this, project, branch, 'portable-linux-innerloop.groovy')

['netcoreapp'].each { targetGroup ->
	['Debug', 'Release'].each { configurationGroup ->
		['PortableLinux'].each { osName ->
            // Avoid running into issues where Configuration set in the environment affects the build (though
            // these are usually build bugs).  Instead use BuildConfig as the parameter name.
			newPipeline.triggerPipelineOnEveryGithubPR("${osName} ${configurationGroup}", ['Config':configurationGroup])
			newPipeline.triggerPipelineOnGithubPush(['Config':configurationGroup])
		}
	}
}

JobReport.Report.generateJobReport(out)
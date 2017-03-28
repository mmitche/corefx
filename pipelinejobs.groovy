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
			newPipeline.triggerPipelineOnEveryGithubPR("${osName} ${configurationGroup}", ['Configuration':configurationGroup])
			newPipeline.triggerPipelineOnGithubPush("${osName} ${configurationGroup}", ['Configuration':configurationGroup])
		}
	}
}

JobReport.Report.generateJobReport(out)
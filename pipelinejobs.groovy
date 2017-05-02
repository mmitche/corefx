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
def linuxPipeline = Pipeline.createPipelineForGithub(this, project, branch, 'portable-linux.groovy')

['netcoreapp'].each { targetGroup ->
	['Debug', 'Release'].each { configurationGroup ->
		['Linux x64'].each { osName ->
            // Avoid running into issues where 'Configuration' set in the environment affects the build (though
            // these are usually build bugs).  Instead use Config as the parameter name.
			linuxPipeline.triggerPipelineOnEveryGithubPR("${osName} ${configurationGroup}", ['Config':configurationGroup, 'OuterLoop':false])
		}
	}
}

// Create a pipeline for portable windows
def windowsPipeline = Pipeline.createPipelineForGithub(this, project, branch, 'portable-windows.groovy')
['netcoreapp'].each { targetGroup ->
	['Debug', 'Release'].each { configurationGroup ->
		['Windows x64'].each { osName ->
            // Avoid running into issues where 'Configuration' set in the environment affects the build (though
            // these are usually build bugs).  Instead use Config as the parameter name.
			windowsPipeline.triggerPipelineOnEveryGithubPR("${osName} ${configurationGroup}", ['Config':configurationGroup, 'OuterLoop':false])
		}
	}
}

JobReport.Report.generateJobReport(out)
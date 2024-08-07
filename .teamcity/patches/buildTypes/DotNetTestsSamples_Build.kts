package patches.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetBuild
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, create a buildType with id = 'DotNetTestsSamples_Build'
in the project with id = 'DotNetTestsSamples', and delete the patch script.
*/
create(RelativeId("DotNetTestsSamples"), BuildType({
    id("DotNetTestsSamples_Build")
    name = "Build"

    vcs {
        root(RelativeId("DotNetTestsSamples_HttpsGithubComChubatovatigerDotnettestssamplesGitRefsHeadsMain"))
    }

    steps {
        dotnetBuild {
            id = "dotnet"
            projects = "dotnettests.sln"
            sdk = "7"
        }
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
    }
}))


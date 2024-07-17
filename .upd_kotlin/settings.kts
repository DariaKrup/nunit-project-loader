import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2024.03"

project {

    buildType(Build)

    subProject(DotNetTestsSamples)
}

object Build : BuildType({
    name = "Build"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        powerShell {
            id = "jetbrains_powershell"
            scriptMode = file {
                path = "build.ps1"
            }
            noProfile = false
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
})


object DotNetTestsSamples : Project({
    name = "DotNetTestsSamples"

    vcsRoot(DotNetTestsSamples_HttpsGithubComChubatovatigerDotnettestssamplesGitRefsHeadsMain)

    buildType(DotNetTestsSamples_Build)
})

object DotNetTestsSamples_Build : BuildType({
    name = "Build"

    vcs {
        root(DotNetTestsSamples_HttpsGithubComChubatovatigerDotnettestssamplesGitRefsHeadsMain)
    }

    steps {
        powerShell {
            id = "jetbrains_powershell"
            scriptMode = file {
                path = "TestProject1/generateTests.ps1"
            }
            noProfile = false
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
})

object DotNetTestsSamples_HttpsGithubComChubatovatigerDotnettestssamplesGitRefsHeadsMain : GitVcsRoot({
    name = "https://github.com/chubatovatiger/dotnettestssamples.git#refs/heads/main"
    url = "https://github.com/chubatovatiger/dotnettestssamples.git"
    branch = "refs/heads/main"
    branchSpec = "refs/heads/*"
    authMethod = password {
        password = "credentialsJSON:7dfcbd69-cb10-4446-8733-42c14a045427"
    }
})

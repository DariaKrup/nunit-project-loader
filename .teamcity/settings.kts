import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.DotnetMsBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetMsBuild
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetRestore
import jetbrains.buildServer.configs.kotlin.buildSteps.nunit
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

    vcsRoot(HttpsGithubComNunitNunitProjectLoaderGitRefsHeadsMain)

    buildType(id1NUnitStepForDslDotCover)
    buildType(TwoNUnitSteps2dlls)
    buildType(id2NUnitStepsDotCoverDocker)
}

object id1NUnitStepForDslDotCover : BuildType({
    id("1NUnitStepForDslDotCover")
    name = "1 NUnit step for DSL + dotCover"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        dotnetRestore {
            name = "Restore .sln"
            id = "Restore_sln"
            projects = "nunit-project-loader.sln"
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        dotnetMsBuild {
            name = "msbuild project"
            id = "msbuild_project"
            projects = "nunit-project-loader.sln"
            version = DotnetMsBuildStep.MSBuildVersion.CrossPlatform
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        nunit {
            name = "NUnit: 1st dll"
            id = "NUnit_1st_dll"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-lib-1.dll"""
            useProjectFile = true
            coverage = dotcover {
                toolPath = "%teamcity.tool.JetBrains.dotCover.CommandLineTools.bundled%"
            }
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

object id2NUnitStepsDotCoverDocker : BuildType({
    id("2NUnitStepsDotCoverDocker")
    name = "2 NUnit steps: dotCover, Docker"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        dotnetRestore {
            name = "Restore .sln"
            id = "dotnet_restore"
            projects = "nunit-project-loader.sln"
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        dotnetMsBuild {
            name = "msbuild project"
            id = "dotnet"
            projects = "nunit-project-loader.sln"
            version = DotnetMsBuildStep.MSBuildVersion.CrossPlatform
            logging = DotnetMsBuildStep.Verbosity.Normal
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        nunit {
            name = "NUnit"
            id = "NUnit"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-lib-1.dll"""
            coverage = dotcover {
                toolPath = "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%"
            }
        }
        nunit {
            name = "NUnit Second Lib"
            id = "NUnit_2_dll"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-lib-2.dll"""
            reduceTestFeedback = true
            param("plugin.docker.imagePlatform", "linux")
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
            param("plugin.docker.imageId", "mcr.microsoft.com/dotnet/sdk:8.0")
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

object TwoNUnitSteps2dlls : BuildType({
    name = "2 NUnit steps: 2 dlls"

    vcs {
        root(HttpsGithubComNunitNunitProjectLoaderGitRefsHeadsMain)
    }

    steps {
        dotnetRestore {
            name = "Restore .sln"
            id = "dotnet_restore"
            projects = "nunit-project-loader.sln"
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        dotnetMsBuild {
            name = "msbuild project"
            id = "dotnet"
            projects = "nunit-project-loader.sln"
            version = DotnetMsBuildStep.MSBuildVersion.CrossPlatform
            logging = DotnetMsBuildStep.Verbosity.Normal
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        nunit {
            name = "NUnit"
            id = "NUnit"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-lib-1.dll"""
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        nunit {
            name = "NUnit Second Lib"
            id = "NUnit_2_dll"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-lib-2.dll"""
            reduceTestFeedback = true
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
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

object HttpsGithubComNunitNunitProjectLoaderGitRefsHeadsMain : GitVcsRoot({
    name = "https://github.com/nunit/nunit-project-loader.git#refs/heads/main"
    url = "https://github.com/nunit/nunit-project-loader.git"
    branch = "refs/heads/main"
    branchSpec = "refs/heads/*"
    authMethod = password {
        userName = ""
        password = ""
    }
})

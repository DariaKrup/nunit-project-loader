package patches.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.DotnetMsBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetMsBuild
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetRestore
import jetbrains.buildServer.configs.kotlin.buildSteps.nunitConsole
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, create a buildType with id = '1NUnitStepForAllDlls'
in the root project, and delete the patch script.
*/
create(DslContext.projectId, BuildType({
    id("1NUnitStepForAllDlls")
    name = "✔️ 1 NUnit step for all dlls"

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
        nunitConsole {
            name = "NUnit: dlls"
            id = "NUnit_1st_dll"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\*.dll"""
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
}))


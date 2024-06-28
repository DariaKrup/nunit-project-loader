import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.DotnetMsBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.NUnitConsoleStep
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetMsBuild
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetRestore
import jetbrains.buildServer.configs.kotlin.buildSteps.nunitConsole
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

    buildType(NUnitStepsDllWithFailedTests)
    buildType(BuildFromTemplate)
    buildType(id2NUnitStepsDotCoverDockerMonoLinux)
    buildType(id1NUnitStepForDslDotCover)
    buildType(id1NUnitStepWithExclusionRule)
    buildType(BuildFromNUnitMetaRunner)
    buildType(TwoNUnitSteps2dlls)
    buildType(id2NUnitStepsDotCoverDocker)
    buildType(id1NUnitStepForAllDlls)
    buildType(id1NUnitStepForAllFields)

    template(TemplateNUnit)
}

object id1NUnitStepForAllDlls : BuildType({
    templates(TemplateNUnit)
    id("1NUnitStepForAllDlls")
    name = "✔️ 1 NUnit step for all dlls"
})

object id1NUnitStepForAllFields : BuildType({
    templates(TemplateNUnit)
    id("1NUnitStepForAllFields")
    name = "✔️ 1 NUnit step for all fields"

    steps {
        nunitConsole {
            name = "NUnit: dlls"
            id = "NUnit_1st_dll"
            workingDir = "src"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-*.dll"""
            excludeTests = "Fixture1"
            includeCategories = "A,B"
            excludeCategories = "C"
            reduceTestFeedback = true
            useProjectFile = true
            args = "--framework=net"
            configFile = "build.cake"
            coverage = dotcover {
                toolPath = "%teamcity.tool.JetBrains.dotCover.CommandLineTools.bundled%"
            }
            dockerImage = "mono:latest"
            dockerImagePlatform = NUnitConsoleStep.ImagePlatform.Linux
            dockerPull = true
            dockerRunParameters = """--e "ARG=g""""
        }
    }
})

object id1NUnitStepForDslDotCover : BuildType({
    id("1NUnitStepForDslDotCover")
    name = "✔️ 1 NUnit step for DSL + dotCover"

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

object id1NUnitStepWithExclusionRule : BuildType({
    id("1NUnitStepWithExclusionRule")
    name = "✔️ 1 NUnit step with exclusion rule"

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
            includeTests = """bin\Debug\net20\test-*.dll"""
            excludeTests = """bin\Debug\net20\test-lib-2.dll"""
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
    name = "✔️ 2 NUnit steps: dotCover, Docker (.Net framework, Windows)"

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
        nunitConsole {
            name = "NUnit"
            id = "NUnit"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-lib-1.dll"""
            coverage = dotcover {
                toolPath = "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%"
            }
        }
        nunitConsole {
            name = "NUnit Second Lib"
            id = "NUnit_2_dll"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-lib-2.dll"""
            reduceTestFeedback = true
            coverage = dotcover {
                toolPath = "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%"
                assemblyFilters = ""
                attributeFilters = ""
                args = ""
            }
            dockerImage = "mcr.microsoft.com/dotnet/framework/sdk:3.5"
            dockerImagePlatform = NUnitConsoleStep.ImagePlatform.Windows
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

object id2NUnitStepsDotCoverDockerMonoLinux : BuildType({
    id("2NUnitStepsDotCoverDockerMonoLinux")
    name = "✔️ 2 NUnit steps: dotCover, Docker (Mono, Linux)"

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
        nunitConsole {
            name = "NUnit"
            id = "NUnit"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-lib-1.dll"""
            coverage = dotcover {
                toolPath = "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%"
            }
            dockerImage = "mono:latest"
            dockerImagePlatform = NUnitConsoleStep.ImagePlatform.Linux
        }
        nunitConsole {
            name = "NUnit Second Lib"
            id = "NUnit_2_dll"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-lib-2.dll"""
            reduceTestFeedback = true
            dockerImage = "mono:latest"
            dockerImagePlatform = NUnitConsoleStep.ImagePlatform.Linux
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

object BuildFromNUnitMetaRunner : BuildType({
    name = "Build from NUnit Meta Runner"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        step {
            id = "NUnitRunnerProject_NewRunnerNUnitProjectLoader_1NUnitStepForAllDlls"
            type = "NUnitRunnerProject_NewRunnerNUnitProjectLoader_1NUnitStepForAllDlls"
            executionMode = BuildStep.ExecutionMode.DEFAULT
            param("passphrase", "credentialsJSON:50e897e1-a348-4260-a026-00d14d21d5fc")
            param("teamcity.step.phase", "")
        }
    }
})

object BuildFromTemplate : BuildType({
    templates(TemplateNUnit)
    name = "Build from NUnit Template"

    steps {
        nunitConsole {
            name = "NUnit: dlls"
            id = "NUnit_1st_dll"
            nunitPath = "%teamcity.tool.NUnit.Console.3.15.0%"
            includeTests = """bin\Debug\net20\test-*.dll"""
            useProjectFile = true
            coverage = dotcover {
                toolPath = "%teamcity.tool.JetBrains.dotCover.CommandLineTools.bundled%"
            }
        }
    }
})

object NUnitStepsDllWithFailedTests : BuildType({
    name = "✔️ NUnit steps: dll with failed tests"

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
        nunitConsole {
            name = "NUnit"
            id = "NUnit"
            enabled = false
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-lib-1.dll"""
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        nunitConsole {
            name = "NUnit Second Lib"
            id = "NUnit_2_dll"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """
                bin\Debug\net20\test-lib-1.dll
                bin\Debug\net20\test-lib-2.dll
            """.trimIndent()
            reduceTestFeedback = true
            useProjectFile = true
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

object TwoNUnitSteps2dlls : BuildType({
    name = "✔️ 2 NUnit steps: 2 dlls"

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
        nunitConsole {
            name = "NUnit"
            id = "NUnit"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-lib-1.dll"""
            param("dotNetCoverage.dotCover.home.path", "%teamcity.tool.JetBrains.dotCover.CommandLineTools.DEFAULT%")
        }
        nunitConsole {
            name = "NUnit Second Lib"
            id = "NUnit_2_dll"
            nunitPath = "%teamcity.tool.NUnit.Console.3.17.0%"
            includeTests = """bin\Debug\net20\test-lib-2.dll"""
            reduceTestFeedback = true
            useProjectFile = true
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

object TemplateNUnit : Template({
    name = "Template_NUnit"

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
            includeTests = """bin\Debug\net20\test-*.dll"""
            useProjectFile = true
            coverage = dotcover {
                toolPath = "%teamcity.tool.JetBrains.dotCover.CommandLineTools.bundled%"
            }
        }
    }

    triggers {
        vcs {
            id = "TRIGGER_1"
        }
    }

    features {
        perfmon {
            id = "BUILD_EXT_1"
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

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetBuild
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

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

version = "2024.07"

project {

    buildType(Build)
    buildType(Test)
}

object Build : BuildType({
    name = "✅ Build"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        dotnetBuild {
            id = "dotnet"
            projects = "nunit-project-loader.sln"
            args = "-restore -noLogo"
            sdk = "4.6.2 6"
        }
        step {
            id = "dotNetGenericRunner"
            type = "jetbrains.dotNetGenericRunner"
            param("dotNetTestRunner.Type", "GenericProcess")
            param("proc_additional_commandline", """%teamcity.build.workingDir%\bin\Debug\net462\nunit-project-loader.tests.dll""")
            param("proc_path", """%teamcity.tool.NUnit.Console.DEFAULT%\bin\net462\nunit3-console.exe""")
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

object Test : BuildType({
    name = "Test"

    params {
        param("KEK", "%teamcity.agent.hardware.cpuCount%")
    }

    steps {
        script {
            id = "simpleRunner"
            scriptContent = "ee"
        }
    }
})

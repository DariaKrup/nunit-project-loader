package patches.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, change the buildType with id = 'TwoNUnitSteps2dlls'
accordingly, and delete the patch script.
*/
changeBuildType(RelativeId("TwoNUnitSteps2dlls")) {
    check(name == "2 NUnit steps: 2 dlls") {
        "Unexpected name: '$name'"
    }
    name = "✔️ 2 NUnit steps: 2 dlls"
}

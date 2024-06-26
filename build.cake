#tool NuGet.CommandLine&version=6.9.1
#tool nuget:?package=GitVersion.CommandLine&version=5.6.3
#tool nuget:?package=GitReleaseManager&version=0.17.0
#tool nuget:?package=NUnit.ConsoleRunner&version=3.17.0
#tool nuget:?package=NUnit.ConsoleRunner&version=3.15.5
#tool nuget:?package=NUnit.ConsoleRunner&version=3.18.0-dev00037
#tool nuget:?package=NUnit.ConsoleRunner.NetCore&version=3.18.0-dev00037

// Load scripts 
#load cake/parameters.cake

//////////////////////////////////////////////////////////////////////
// ARGUMENTS  
//////////////////////////////////////////////////////////////////////

var target = Argument("target", Argument("t", "Default"));

// Additional arguments defined in the cake scripts:
//   --configuration
//   --packageVersion

//////////////////////////////////////////////////////////////////////
// SETUP AND TEARDOWN
//////////////////////////////////////////////////////////////////////

Setup<BuildParameters>((context) =>
{
	var parameters = BuildParameters.Create(context);

	if (BuildSystem.IsRunningOnAppVeyor)
		AppVeyor.UpdateBuildVersion(parameters.PackageVersion + "-" + AppVeyor.Environment.Build.Number);

	Information("Building {0} version {1} of NUnit Project Loader.", parameters.Configuration, parameters.PackageVersion);

	return parameters;
});

//////////////////////////////////////////////////////////////////////
// DUMP SETTINGS
//////////////////////////////////////////////////////////////////////

Task("DumpSettings")
	.Does<BuildParameters>((parameters) =>
	{
		parameters.DumpSettings();
	});

//////////////////////////////////////////////////////////////////////
// CLEAN
//////////////////////////////////////////////////////////////////////

Task("Clean")
	.Does<BuildParameters>((parameters) =>
	{
		Information("Cleaning " + parameters.OutputDirectory);
		CleanDirectory(parameters.OutputDirectory);
		Information("Cleaning " + parameters.PackageDirectory);
		CleanDirectory(parameters.PackageDirectory);
		Information("Deleting log files");
		DeleteFiles(parameters.ProjectDirectory + "*.log");
	});

//////////////////////////////////////////////////////////////////////
// INITIALIZE FOR BUILD
//////////////////////////////////////////////////////////////////////

Task("NuGetRestore")
    .Does(() =>
	{
		NuGetRestore(SOLUTION_FILE, new NuGetRestoreSettings()
		{
			Source = new string[]
			{
				"https://www.nuget.org/api/v2",
				"https://www.myget.org/F/nunit/api/v2"
			}
		});
	});

//////////////////////////////////////////////////////////////////////
// BUILD
//////////////////////////////////////////////////////////////////////

Task("Build")
	.IsDependentOn("Clean")
    .IsDependentOn("NuGetRestore")
    .Does<BuildParameters>((parameters) =>
    {
		if(IsRunningOnWindows())
		{
			MSBuild(SOLUTION_FILE, new MSBuildSettings()
				.SetConfiguration(parameters.Configuration)
				.SetMSBuildPlatform(MSBuildPlatform.Automatic)
				.SetVerbosity(Verbosity.Minimal)
				.SetNodeReuse(false)
				.SetPlatformTarget(PlatformTarget.MSIL)
			);
		}
		else
		{
			XBuild(SOLUTION_FILE, new XBuildSettings()
				.WithTarget("Build")
				.WithProperty("Configuration", parameters.Configuration)
				.SetVerbosity(Verbosity.Minimal)
			);
		}
    });

//////////////////////////////////////////////////////////////////////
// TEST
//////////////////////////////////////////////////////////////////////

Task("Test")
	.IsDependentOn("Build")
	.IsDependentOn("TestNet462")
	.IsDependentOn("TestNet60");

Task("TestNet462")
	.Does<BuildParameters>((parameters) =>
	{
		string runner = parameters.ToolsDirectory + $"NUnit.ConsoleRunner.{CONSOLE_VERSION_FOR_UNIT_TESTS}/tools/nunit3-console.exe";
		StartProcess(runner, parameters.OutputDirectory + "net462/nunit-project-loader.tests.dll");
		// We will have empty logs so long as we test against 3.15.5 and 3.17.0
		DeleteEmptyLogFiles(parameters.ProjectDirectory);
	});

Task("TestNet60")
	.Does<BuildParameters>((parameters) =>
	{
		// Need to use dev build for this test
		string runner = parameters.ToolsDirectory + $"NUnit.ConsoleRunner.3.18.0-dev00037/tools/nunit3-console.exe";
		StartProcess(runner, parameters.OutputDirectory + "net6.0/nunit-project-loader.tests.dll");
	});

//////////////////////////////////////////////////////////////////////
// PACKAGE
//////////////////////////////////////////////////////////////////////

Task("BuildNuGetPackage")
	.Does<BuildParameters>((parameters) =>
	{
		CreateDirectory(parameters.PackageDirectory);

		BuildNuGetPackage(parameters);
	});

Task("InstallNuGetPackage")
	.Does<BuildParameters>((parameters) =>
	{
		// Ensure we aren't inadvertently using the chocolatey install
		if (DirectoryExists(parameters.ChocolateyInstallDirectory))
			DeleteDirectory(parameters.ChocolateyInstallDirectory, new DeleteDirectorySettings() { Recursive = true });

		CleanDirectory(parameters.NuGetInstallDirectory);
		Unzip(parameters.NuGetPackage, parameters.NuGetInstallDirectory);

		Information($"Unzipped {parameters.NuGetPackageName} to { parameters.NuGetInstallDirectory}");
	});

Task("VerifyNuGetPackage")
	.IsDependentOn("InstallNuGetPackage")
	.Does<BuildParameters>((parameters) =>
	{
		Check.That(parameters.NuGetInstallDirectory,
			HasFiles("LICENSE.txt", "nunit_256.png"),
			HasDirectory("tools/net20").WithFiles("nunit-project-loader.dll", "nunit.engine.api.dll"));
		Information("Verification was successful!");
	});

Task("TestNuGetPackage")
	.IsDependentOn("InstallNuGetPackage")
	.Does<BuildParameters>((parameters) =>
	{
		new NuGetPackageTester(parameters).RunPackageTests(PackageTests);

		// We will have empty logs so long as we test against 3.15.5 and 3.17.0
		DeleteEmptyLogFiles(parameters.ProjectDirectory);
	});

Task("BuildChocolateyPackage")
	.IsDependentOn("Build")
	.Does<BuildParameters>((parameters) =>
	{
		CreateDirectory(parameters.PackageDirectory);

		BuildChocolateyPackage(parameters);
	});

Task("InstallChocolateyPackage")
	.Does<BuildParameters>((parameters) =>
	{
		// Ensure we aren't inadvertently using the nuget install
		if (DirectoryExists(parameters.NuGetInstallDirectory))
			DeleteDirectory(parameters.NuGetInstallDirectory, new DeleteDirectorySettings() { Recursive = true });

		CleanDirectory(parameters.ChocolateyInstallDirectory);
		Unzip(parameters.ChocolateyPackage, parameters.ChocolateyInstallDirectory);

		Information($"Unzipped {parameters.ChocolateyPackageName} to { parameters.ChocolateyInstallDirectory}");
	});

Task("VerifyChocolateyPackage")
	.IsDependentOn("InstallChocolateyPackage")
	.Does<BuildParameters>((parameters) =>
	{
		Check.That(parameters.ChocolateyInstallDirectory,
			HasDirectory("tools").WithFiles(
				"LICENSE.txt", "VERIFICATION.txt"),
			HasDirectory("tools/net20").WithFiles(
				"nunit-project-loader.dll", "nunit.engine.api.dll"));
		Information("Verification was successful!");
	});

Task("TestChocolateyPackage")
	.IsDependentOn("InstallChocolateyPackage")
	.Does<BuildParameters>((parameters) =>
	{
		new ChocolateyPackageTester(parameters).RunPackageTests(PackageTests);

		// We will have empty logs so long as we test against 3.15.5 and 3.17.0
		DeleteEmptyLogFiles(parameters.ProjectDirectory);
	});

PackageTest[] PackageTests = new PackageTest[]
{
	new PackageTest()
	{
		Description = "Project with one assembly, all tests pass",
		Arguments = "PassingAssembly.nunit",
		TestConsoleVersions =  CONSOLE_VERSIONS_FOR_PACKAGE_TESTS,
		ExpectedResult = new ExpectedResult("Passed")
		{
			Total = 4,
			Passed = 4,
			Failed = 0,
			Warnings = 0,
			Inconclusive = 0,
			Skipped = 0,
			Assemblies = new[] { new ExpectedAssemblyResult("test-lib-1.dll", "net-2.0") }
		}
	},
	new PackageTest()
	{
		Description = "Project with one assembly, some failures",
		Arguments = "FailingAssembly.nunit",
		TestConsoleVersions =  CONSOLE_VERSIONS_FOR_PACKAGE_TESTS,
		ExpectedResult = new ExpectedResult("Failed")
		{
			Total = 9,
			Passed = 4,
			Failed = 2,
			Warnings = 0,
			Inconclusive = 1,
			Skipped = 2,
			Assemblies = new[] { new ExpectedAssemblyResult("test-lib-2.dll", "net-2.0") }
		}
	},
	new PackageTest()
	{
		Description = "Project with both assemblies",
		Arguments = "BothAssemblies.nunit",
		TestConsoleVersions =  CONSOLE_VERSIONS_FOR_PACKAGE_TESTS,
		ExpectedResult = new ExpectedResult("Failed")
		{
			Total = 13,
			Passed = 8,
			Failed = 2,
			Warnings = 0,
			Inconclusive = 1,
			Skipped = 2,
			Assemblies = new[] {
				new ExpectedAssemblyResult("test-lib-1.dll", "net-2.0"),
				new ExpectedAssemblyResult("test-lib-2.dll", "net-2.0")
			}
		}
	},
	new PackageTest()
	{
		Description = "Project with one assembly, all tests pass",
		Arguments = "PassingAssemblyNetCore.nunit",
		TestConsoleVersions =  new string[] { "NetCore.3.18.0-dev00037" },
		ExpectedResult = new ExpectedResult("Passed")
		{
			Total = 4,
			Passed = 4,
			Failed = 0,
			Warnings = 0,
			Inconclusive = 0,
			Skipped = 0,
			Assemblies = new[] { new ExpectedAssemblyResult("test-lib-1.dll", "net-2.0") }
		}
	},
	new PackageTest()
	{
		Description = "Project with one assembly, some failures",
		Arguments = "FailingAssemblyNetCore.nunit",
		TestConsoleVersions =  new string[] { "NetCore.3.18.0-dev00037" },
		ExpectedResult = new ExpectedResult("Failed")
		{
			Total = 9,
			Passed = 4,
			Failed = 2,
			Warnings = 0,
			Inconclusive = 1,
			Skipped = 2,
			Assemblies = new[] { new ExpectedAssemblyResult("test-lib-2.dll", "net-2.0") }
		}
	},
	new PackageTest()
	{
		Description = "Project with both assemblies",
		Arguments = "BothAssembliesNetCore.nunit",
		TestConsoleVersions =  new string[] { "NetCore.3.18.0-dev00037" },
		ExpectedResult = new ExpectedResult("Failed")
		{
			Total = 13,
			Passed = 8,
			Failed = 2,
			Warnings = 0,
			Inconclusive = 1,
			Skipped = 2,
			Assemblies = new[] {
				new ExpectedAssemblyResult("test-lib-1.dll", "net-2.0"),
				new ExpectedAssemblyResult("test-lib-2.dll", "net-2.0")
			}
		}
	}
};

//////////////////////////////////////////////////////////////////////
// PUBLISH
//////////////////////////////////////////////////////////////////////

static bool hadPublishingErrors = false;

Task("PublishPackages")
	.Description("Publish nuget and chocolatey packages according to the current settings")
	.IsDependentOn("PublishToMyGet")
	.IsDependentOn("PublishToNuGet")
	.IsDependentOn("PublishToChocolatey")
	.Does(() =>
	{
		if (hadPublishingErrors)
			throw new Exception("One of the publishing steps failed.");
	});

// This task may either be run by the PublishPackages task,
// which depends on it, or directly when recovering from errors.
Task("PublishToMyGet")
	.Description("Publish packages to MyGet")
	.Does<BuildParameters>((parameters) =>
	{
		if (!parameters.ShouldPublishToMyGet)
			Information("Nothing to publish to MyGet from this run.");
		else
			try
			{
				PushNuGetPackage(parameters.NuGetPackage, parameters.MyGetApiKey, parameters.MyGetPushUrl);
				PushChocolateyPackage(parameters.ChocolateyPackage, parameters.MyGetApiKey, parameters.MyGetPushUrl);
			}
			catch (Exception)
			{
				hadPublishingErrors = true;
			}
	});

// This task may either be run by the PublishPackages task,
// which depends on it, or directly when recovering from errors.
Task("PublishToNuGet")
	.Description("Publish packages to NuGet")
	.Does<BuildParameters>((parameters) =>
	{
		if (!parameters.ShouldPublishToNuGet)
			Information("Nothing to publish to NuGet from this run.");
		else
			try
			{
				PushNuGetPackage(parameters.NuGetPackage, parameters.NuGetApiKey, parameters.NuGetPushUrl);
			}
			catch (Exception)
			{
				hadPublishingErrors = true;
			}
	});

// This task may either be run by the PublishPackages task,
// which depends on it, or directly when recovering from errors.
Task("PublishToChocolatey")
	.Description("Publish packages to Chocolatey")
	.Does<BuildParameters>((parameters) =>
	{
		if (!parameters.ShouldPublishToChocolatey)
			Information("Nothing to publish to Chocolatey from this run.");
		else
			try
			{
				PushChocolateyPackage(parameters.ChocolateyPackage, parameters.ChocolateyApiKey, parameters.ChocolateyPushUrl);
			}
			catch (Exception)
			{
				hadPublishingErrors = true;
			}
	});

//////////////////////////////////////////////////////////////////////
// CREATE A DRAFT RELEASE
//////////////////////////////////////////////////////////////////////

Task("CreateDraftRelease")
	.Does<BuildParameters>((parameters) =>
	{
		if (parameters.IsReleaseBranch)
		{
			// NOTE: Since this is a release branch, the pre-release label
			// is "pre", which we don't want to use for the draft release.
			// The branch name contains the full information to be used
			// for both the name of the draft release and the milestone,
			// i.e. release-2.0.0, release-2.0.0-beta2, etc.
			string milestone = parameters.BranchName.Substring(8);
			string releaseName = $"NUnit Project Loader Extension {milestone}";

			Information($"Creating draft release...");

			try
			{
				GitReleaseManagerCreate(parameters.GitHubAccessToken, GITHUB_OWNER, GITHUB_REPO, new GitReleaseManagerCreateSettings()
				{
					Name = releaseName,
					Milestone = milestone
				});
			}
			catch
			{
				Error($"Unable to create draft release for {releaseName}.");
				Error($"Check that there is a {milestone} milestone with at least one closed issue.");
				Error("");
				throw;
			}
		}
		else
		{
			Information("Skipping Release creation because this is not a release branch");
		}
	});

//////////////////////////////////////////////////////////////////////
// CREATE A PRODUCTION RELEASE
//////////////////////////////////////////////////////////////////////

Task("CreateProductionRelease")
	.Does<BuildParameters>((parameters) =>
	{
		if (parameters.IsProductionRelease)
		{
			string token = parameters.GitHubAccessToken;
			string tagName = parameters.PackageVersion;
			string assets = $"\"{parameters.NuGetPackage},{parameters.ChocolateyPackage}\"";

			Information($"Publishing release {tagName} to GitHub");

			GitReleaseManagerAddAssets(token, GITHUB_OWNER, GITHUB_REPO, tagName, assets);
			GitReleaseManagerClose(token, GITHUB_OWNER, GITHUB_REPO, tagName);
		}
		else
		{
			Information("Skipping CreateProductionRelease because this is not a production release");
		}
	});

//////////////////////////////////////////////////////////////////////
// TASK TARGETS
//////////////////////////////////////////////////////////////////////

Task("Package")
	.IsDependentOn("Build")
	.IsDependentOn("PackageNuGet")
	.IsDependentOn("PackageChocolatey");

Task("PackageNuGet")
	.IsDependentOn("BuildNuGetPackage")
	.IsDependentOn("VerifyNuGetPackage")
	.IsDependentOn("TestNuGetPackage");

Task("PackageChocolatey")
	.IsDependentOn("BuildChocolateyPackage")
	.IsDependentOn("VerifyChocolateyPackage")
	.IsDependentOn("TestChocolateyPackage");

Task("BuildTestAndPackage")
	.IsDependentOn("Clean")
	.IsDependentOn("Build")
	.IsDependentOn("Test")
	.IsDependentOn("Package");

Task("Travis")
	.IsDependentOn("Build")
	.IsDependentOn("Test");

Task("Appveyor")
	.IsDependentOn("Build")
	.IsDependentOn("Test")
	.IsDependentOn("Package")
	.IsDependentOn("PublishPackages")
	.IsDependentOn("CreateDraftRelease")
	.IsDependentOn("CreateProductionRelease");

Task("Default")
    .IsDependentOn("Build");

//////////////////////////////////////////////////////////////////////
// EXECUTION
//////////////////////////////////////////////////////////////////////

RunTarget(target);

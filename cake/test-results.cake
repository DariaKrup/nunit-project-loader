// ***********************************************************************
// Copyright (c) Charlie Poole and TestCentric GUI contributors.
// Licensed under the MIT License. See LICENSE.txt in root directory.
// ***********************************************************************

// This file contains classes used to interpret the result XML that is
// produced by test runs of the GUI.

public abstract class ResultSummary
{
	public string OverallResult { get; set; }
	public int Total  { get; set; }
	public int Passed  { get; set; }
	public int Failed  { get; set; }
	public int Warnings  { get; set; }
	public int Inconclusive  { get; set; }
	public int Skipped { get; set; }
}

public class ExpectedResult : ResultSummary
{
	public ExpectedResult(string overallResult)
	{
		if (string.IsNullOrEmpty(overallResult))
			throw new ArgumentNullException(nameof(overallResult));

		OverallResult = overallResult;
		
		// Initialize counters to -1, indicating no expected value.
		// Set properties of those items to be checked.
		Total = Passed = Failed = Warnings = Inconclusive = Skipped = -1;
	}

	public ExpectedAssemblyResult[] Assemblies { get; set; } = new ExpectedAssemblyResult[0];
}

public class ExpectedAssemblyResult
{
	public ExpectedAssemblyResult(string name, string expectedRuntime)
	{
		Name = name;
		Runtime = expectedRuntime;
	}

	public string Name { get; }
	public string Runtime { get; }
}

public class ActualResult : ResultSummary
{
	public ActualResult(string resultFile)
	{
		var doc = new XmlDocument();
		doc.Load(resultFile);

		Xml = doc.DocumentElement;
		if (Xml.Name != "test-run")
			throw new Exception("The test-run element was not found.");

		OverallResult = GetAttribute(Xml, "result");
		Total = IntAttribute(Xml, "total");
		Passed = IntAttribute(Xml, "passed");
		Failed = IntAttribute(Xml, "failed");
		Warnings = IntAttribute(Xml, "warnings");
		Inconclusive = IntAttribute(Xml, "inconclusive");
		Skipped = IntAttribute(Xml, "skipped");

		var assemblies = new List<ActualAssemblyResult>();
		var engineVersion = new Version(GetAttribute(Xml, "engine-version"));

		foreach (XmlNode node in Xml.SelectNodes("//test-suite[@type='Assembly']"))
			assemblies.Add(new ActualAssemblyResult(node, engineVersion));

		Assemblies = assemblies.ToArray();
	}

	public XmlNode Xml { get; }

	public ActualAssemblyResult[] Assemblies { get; }

	private string GetAttribute(XmlNode node, string name)
	{
		return node.Attributes[name]?.Value;
	}

	private int IntAttribute(XmlNode node, string name)
	{
		string s = GetAttribute(node, name);
		// TODO: We should replace 0 with -1, representing a missing counter
		// attribute, after issue #707 is fixed.
		return s == null ? 0 : int.Parse(s);
	}
}

public class ActualAssemblyResult
{
	public ActualAssemblyResult(XmlNode xml, Version engineVersion)
    {
		Name = xml.Attributes["name"]?.Value;

		var env = xml.SelectSingleNode("environment");
		var settings = xml.SelectSingleNode("settings");

		// If TargetRuntimeFramework setting is not present, the GUI will have crashed anyway
		var runtimeSetting = settings.SelectSingleNode("setting[@name='ImageTargetFrameworkName']");
		TargetRuntime = runtimeSetting?.Attributes["value"]?.Value;

		// Engine 3.10 and earlier doesn't provide enough info
		if (engineVersion >= new Version(3,11,0,0))
			Runtime = DeduceActualRuntime(xml);
	}

	public string Name { get; }
	public string Runtime { get; }

	public string TargetRuntime { get; }

	// Code to determine the runtime actually used is adhoc
	// and doesn't work for all assemblies because the needed
    // information may not be present in the result file.
	// TODO: Modify result file schema so this can be cleaner
	private static string DeduceActualRuntime(XmlNode assembly)
	{
		var env = assembly.SelectSingleNode("environment");
		// The TargetRuntimeFramework setting is only present
        // under the 3.12 and later versions of the engine.
		var runtimeSetting = assembly.SelectSingleNode("settings/setting[@name='TargetRuntimeFramework']");
		var targetRuntime = runtimeSetting?.Attributes["value"]?.Value;
		if (targetRuntime != null)
			return targetRuntime;

		var clrVersion = env.Attributes["clr-version"]?.Value;
		if (clrVersion == null)
			return null;

		if (clrVersion.StartsWith("2.0"))
			return "net-2.0";
		if (clrVersion.StartsWith("4.0"))
			return "net-4";

		return null;
	}
}

public class PackageTestReport
{
	public PackageTest Test;
	public ActualResult Result;
	public List<string> Errors;
	public List<string> Warnings;

	public PackageTestReport(PackageTest test, ActualResult actualResult)
	{
		Test = test;
		Result = actualResult;
		Errors = new List<string>();
		Warnings = new List<string>();

		var expectedResult = test.ExpectedResult;

		ReportMissingFiles();

		if (actualResult.OverallResult == null)
			Errors.Add("   The test-run element has no result attribute.");
		else if (expectedResult.OverallResult != actualResult.OverallResult)
			Errors.Add($"   Expected: Overall Result = {expectedResult.OverallResult} But was: {actualResult.OverallResult}");
		CheckCounter("Test Count", expectedResult.Total, actualResult.Total);
		CheckCounter("Passed", expectedResult.Passed, actualResult.Passed);
		CheckCounter("Failed", expectedResult.Failed, actualResult.Failed);
		CheckCounter("Warnings", expectedResult.Warnings, actualResult.Warnings);
		CheckCounter("Inconclusive", expectedResult.Inconclusive, actualResult.Inconclusive);
		CheckCounter("Skipped", expectedResult.Skipped, actualResult.Skipped);

		var expectedAssemblies = expectedResult.Assemblies;
		var actualAssemblies = actualResult.Assemblies;

		for (int i = 0; i < expectedAssemblies.Length && i < actualAssemblies.Length; i++)
        {
			var expected = expectedAssemblies[i];
			var actual = actualAssemblies[i];

			if (expected.Name != actual.Name)
				Errors.Add($"   Expected: {expected.Name} But was: { actual.Name}");
			else if (actual.Runtime == null)
				Warnings.Add($"Unable to determine actual runtime used for {expected.Name}");
			else if (!expected.Runtime.StartsWith(actual.Runtime))
				Errors.Add($"   Assembly {actual.Name} Expected: {expected.Runtime} But was: {actual.Runtime}");
        }

		for (int i = actualAssemblies.Length; i < expectedAssemblies.Length; i++)
			Errors.Add($"   Assembly {expectedAssemblies[i].Name} was not found");

		for (int i = expectedAssemblies.Length; i < actualAssemblies.Length; i++)
			Errors.Add($"   Found unexpected assembly {actualAssemblies[i].Name}");
	}

	public PackageTestReport(PackageTest test, Exception ex)
	{
		Test = test;
		Result = null;
		Errors = new List<string>();
		Errors.Add($"     {ex.Message}");
	}

	public void Display(int index)
	{
		Console.WriteLine();
		Console.WriteLine($"{index}. {Test.Description}");
		Console.WriteLine($"   ConsoleVersion: {Test.ConsoleVersion}");
		Console.WriteLine($"   Args: {Test.Arguments}");
		Console.WriteLine();

		foreach (var error in Errors)
			Console.WriteLine(error);

		if (Errors.Count == 0)
		{
			Console.WriteLine("   SUCCESS: Test Result matches expected result!");
		}
		else
		{
			Console.WriteLine();
			Console.WriteLine("   ERROR: Test Result not as expected!");
		}

		foreach (var warning in Warnings)
			Console.WriteLine("   WARNING: " + warning);
	}

	// File level errors, like missing or mal-formatted files, need to be highlighted
	// because otherwise it's hard to detect the cause of the problem without debugging.
	// This method finds and reports that type of error.
	private void ReportMissingFiles()
	{
		// Start with all the top-level test suites. Note that files that
		// cannot be found show up as Unknown as do unsupported file types.
		var suites = Result.Xml.SelectNodes(
			"//test-suite[@type='Unknown'] | //test-suite[@type='Project'] | //test-suite[@type='Assembly']");

		// If there is no top-level suite, it generally means the file format could not be interpreted
		if (suites.Count == 0)
			Errors.Add("   No top-level suites! Possible empty command-line or misformed project.");

		foreach (XmlNode suite in suites)
		{
			// Narrow down to the specific failures we want
			string runState = GetAttribute(suite, "runstate");
			string suiteResult = GetAttribute(suite, "result");
			string label = GetAttribute(suite, "label");
			string site = suite.Attributes["site"]?.Value ?? "Test";
			if (runState == "NotRunnable" || suiteResult == "Failed" && site == "Test" && (label == "Invalid" || label == "Error"))
			{
				string message = suite.SelectSingleNode("reason/message")?.InnerText;
				Errors.Add($"   {message}");
			}
		}
	}

	private void CheckCounter(string label, int expected, int actual)
	{
		// If expected value of counter is negative, it means no check is needed
		if (expected >= 0 && expected != actual)
			Errors.Add($"   Expected: {label} = {expected} But was: {actual}");
	}

	private string GetAttribute(XmlNode node, string name)
	{
		return node.Attributes[name]?.Value;
	}
}

public class ResultReporter
{
	private string _packageName;
	private List<PackageTestReport> _reports = new List<PackageTestReport>();

	public ResultReporter(string packageName)
	{
		_packageName = packageName;
	}

	public void AddReport(PackageTestReport report)
	{
		_reports.Add(report);
	}

	public bool ReportResults()
	{
		Console.WriteLine("\n=================================================="); ;
		Console.WriteLine($"Test Results for {_packageName}");
		Console.WriteLine("=================================================="); ;

		Console.WriteLine("\nTest Environment");
		Console.WriteLine($"   OS Version: {Environment.OSVersion.VersionString}");
		Console.WriteLine($"  CLR Version: {Environment.Version}\n");

		int index = 0;
		bool hasErrors = false;

		foreach (var report in _reports)
		{
			hasErrors |= report.Errors.Count > 0;
			report.Display(++index);
		}

		return hasErrors;
	}
}
package com.yourorg.agent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class E2EOrchestrator {

	public static void main(String[] args) throws Exception {

		String defectDescription = "Login fails intermittently with special characters.";
		System.out.println("\uD83D\uDCC5 Calling Excel MAPPER...");

		List<ExcelMapper.MappingRow> mapping = ExcelMapper.loadMapping("mapping.xlsx");

		StringBuilder mapPrompt = new StringBuilder();
		for (ExcelMapper.MappingRow row : mapping) {
			mapPrompt.append(row.functionality).append(":").append(row.testCaseId).append("; ");
		}

		String prompt = "Given this defect description: \"" + defectDescription + "\" Given this mapping: " + mapPrompt + ". Which test cases are impacted?";
		System.out.println("\uD83E\uDD16 Calling LLM CLIENT...");

		String llmResponse = LLMClient.queryLLM(prompt);

		// Dummy impacted list from LLM for testing
		List<String> impactedTestCases = Arrays.asList("TC001", "TC002");

		Set<String> impactedClasses = new HashSet<>();
		for (ExcelMapper.MappingRow row : mapping) {
			if (impactedTestCases.contains(row.testCaseId)) {
				impactedClasses.add(row.javaClassName); // e.g., "com.yourorg.LoginTest"
			}
		}

		String repoUrl = "https://github.com/Avinash-Golla/automation-tests.git";
		String repoPath = "./repo/automation-tests/automation-tests";
		String suiteXmlPath = repoPath + "/testng-impacted.xml";
		String testClassesPath = repoPath + "/target/test-classes";

		// 1. Clone or update repo
		RepoCloner.cloneOrUpdateRepo(repoUrl, repoPath);

		// 2. Compile repo using Maven
		compileAutomationTests(repoPath);

		// 3. Create suite XML
		Files.createDirectories(Paths.get(repoPath));
		TestNGSuiteGenerator.generateSuite(new ArrayList<>(impactedClasses), suiteXmlPath);

		// 4. Run TestNG using external process with correct classpath
		runTestNGWithCustomClasspath(suiteXmlPath, testClassesPath);
	}

	private static void compileAutomationTests(String projectDir) throws Exception {
		String mvnCmd = "C:\\Users\\avina\\Downloads\\apache-maven-3.9.10\\bin\\mvn.cmd";
		ProcessBuilder builder = new ProcessBuilder(mvnCmd, "test-compile");
		builder.directory(new File(projectDir));
		builder.inheritIO();
		Process process = builder.start();
		int result = process.waitFor();
		if (result != 0) {
			throw new RuntimeException("\u274C Compilation failed for automation-tests project.");
		}
		System.out.println("\u2705 Compilation successful.");
	}

	private static void runTestNGWithCustomClasspath(String suiteXmlPath, String testClassesPath) throws Exception {
		String sep = System.getProperty("path.separator");

		String testngJar = "C:\\Users\\avina\\.m2\\repository\\org\\testng\\testng\\7.9.0\\testng-7.9.0.jar";
		String jcommanderJar = "C:\\Users\\avina\\.m2\\repository\\com\\beust\\jcommander\\1.82\\jcommander-1.82.jar";

		String classpath = String.join(sep, testClassesPath, testngJar, jcommanderJar);

		System.out.println("\uD83D\uDCE6 Launching TestNG with classpath:\n" + classpath);

		ProcessBuilder builder = new ProcessBuilder(
				"java",
				"-cp",
				classpath,
				"org.testng.TestNG",
				suiteXmlPath
		);

		builder.inheritIO();
		Process process = builder.start();
		int result = process.waitFor();

		if (result != 0) {
			throw new RuntimeException("\u274C TestNG execution failed.");
		} else {
			System.out.println("\u2705 TestNG executed successfully.");
		}
	}
}

package com.yourorg.agent;

import org.testng.TestNG;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class E2EOrchestrator {

	public static void main(String[] args) throws Exception {

		Scanner sc=new Scanner(System.in);
		String defectDescription =sc.nextLine();
		System.out.println("Calling Excel MAPPER...");

		List<ExcelMapper.MappingRow> mapping = ExcelMapper.loadMapping("mapping.xlsx");
		System.out.println(mapping);

		StringBuilder mapPrompt = new StringBuilder();
		for (ExcelMapper.MappingRow row : mapping) {
			mapPrompt.append(row.functionality).append(":").append(row.testCaseId).append("; ");
		}
		System.out.println(mapPrompt);

		String prompt = "Given this defect description: \"" + defectDescription + "\" Given this mapping: " + mapPrompt + ". Which test cases are impacted?";
		System.out.println("Calling LLM CLIENT...");

		String llmResponse = LLMClient.queryLLM(prompt);
		System.out.println("This is the LLM response");
		System.out.println(llmResponse);


		Set<String> impactedTestCaseIds = new HashSet<>();
		Set<String> impactedClassMethods = new HashSet<>();

		llmResponse = llmResponse.replace("\\n", "\n");
		String[] lines = llmResponse.split("\\R");

		for (String line : lines) {
			line = line.trim();
			if (line.startsWith("*") || line.startsWith("-")) {
				line = line.substring(1).trim();
			}
			if (line.contains(":")) {
				String[] parts = line.split(":");
				if (parts.length == 2) {
					String className = parts[0].trim();
					String methodName = parts[1].trim();
					impactedTestCaseIds.add(methodName);
					impactedClassMethods.add(className.substring(3) + ":" + methodName);
				}
			}
		}
		System.out.println("Impacted Test class Methods: "+impactedClassMethods);

		System.out.println("Impacted Test Case IDs: " + impactedTestCaseIds);

		Set<String> impactedClasses = new HashSet<>();
		for (ExcelMapper.MappingRow row : mapping) {
			if (impactedTestCaseIds.contains(row.testCaseId)) {
				impactedClasses.add(row.javaClassName.trim());
			}
		} // this has to be checked

		System.out.println(" Impacted classes: " + impactedClasses);

		// Git repo clone/update and compilation
		String repoUrl = "https://github.com/Avinash-Golla/automation-tests.git";
		String repoPath = "./repo";
		String suiteXmlPath = repoPath + "/automation-tests/testng-impacted.xml";
		String testClassesPath = repoPath + "/automation-tests/target/test-classes";

		RepoCloner.cloneOrUpdateRepo(repoUrl, repoPath);
		Thread.sleep(3000);

		//compileAutomationTests(repoPath + "/automation-tests");


		Files.createDirectories(Paths.get(repoPath));
		TestNGSuiteGenerator.generateSuite(new ArrayList<>(impactedClassMethods), suiteXmlPath);
		//System.out.println("testng-impacted.xml generated at: " + suiteXmlPath);


		runUsingMavenTestSuite(suiteXmlPath,repoPath);
	}

	private static void runUsingMavenTestSuite(String suiteXmlPath, String repoPath) throws Exception {
		System.out.println("\uD83D\uDE80 Running with Maven command line...");

		String mvnCmd = "C:\\Users\\golavinash\\Downloads\\apache-maven-3.9.10\\bin\\mvn.cmd";
//		List<String> mvnCommandClean = Arrays.asList(
//				mvnCmd,
//				"clean",
//				"-DsuiteXmlFile=" + suiteXmlPath
//		);
		List<String> mvnCommand = Arrays.asList(
				mvnCmd,
				"test",
				"-DsuiteXmlFile=" + suiteXmlPath
		);

		File automationTestsDir = new File(repoPath+"/automation-tests");
		System.out.println("Running Maven in Directory"+automationTestsDir.getAbsolutePath());
//		ProcessBuilder builderClean = new ProcessBuilder(mvnCommandClean);
//		builderClean.directory(automationTestsDir);
//		builderClean.inheritIO();
//		Process processClean = builderClean.start();
//		int resultClean = processClean.waitFor();

		ProcessBuilder builder = new ProcessBuilder(mvnCommand);
		builder.directory(automationTestsDir);
		builder.inheritIO();
		Process process = builder.start();
		int result = process.waitFor();

		System.out.println("Maven exited with code " + result);
		if (result != 0) {
			throw new RuntimeException("Maven test execution failed.");
		}
	}

	private static void runUsingTestNGJavaAPI(String suiteXmlPath) {
		System.out.println("Running with TestNG Java API...");
		TestNG testng = new TestNG();
		testng.setTestSuites(Collections.singletonList(suiteXmlPath));
		testng.run();
	}


}






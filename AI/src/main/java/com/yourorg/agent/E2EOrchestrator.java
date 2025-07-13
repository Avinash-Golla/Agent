package com.yourorg.agent;

import org.testng.TestNG;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class E2EOrchestrator {

	public static void main(String[] args) throws Exception {

		String defectDescription = "Only return test cases whose functionality matches closely with the defect description: sign";
		System.out.println("\uD83D\uDCC5 Calling Excel MAPPER...");

		List<ExcelMapper.MappingRow> mapping = ExcelMapper.loadMapping("mapping.xlsx");

		StringBuilder mapPrompt = new StringBuilder();
		for (ExcelMapper.MappingRow row : mapping) {
			mapPrompt.append(row.functionality).append(":").append(row.testCaseId).append("; ");
		}

		String prompt = "Given this defect description: \"" + defectDescription + "\" Given this mapping: " + mapPrompt + ". Which test cases are impacted?";
		System.out.println("\uD83E\uDD16 Calling LLM CLIENT...");

		String llmResponse = LLMClient.queryLLM(prompt);
		System.out.println("This is the LLM response");
		System.out.println(llmResponse);

		// ✅ Extract impacted class-method pairs (e.g., LoginTest:TC001)
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
					impactedClassMethods.add(className + ":" + methodName);
				}
			}
		}

		System.out.println("✅ Impacted Test Case IDs: " + impactedTestCaseIds);

		Set<String> impactedClasses = new HashSet<>();
		for (ExcelMapper.MappingRow row : mapping) {
			if (impactedTestCaseIds.contains(row.testCaseId)) {
				impactedClasses.add(row.javaClassName.trim());
			}
		}

		System.out.println("✅ Impacted classes: " + impactedClasses);

		// Git repo clone/update and compilation
		String repoUrl = "https://github.com/Avinash-Golla/automation-tests.git";
		String repoPath = "./repo";
		String suiteXmlPath = repoPath + "/automation-tests/testng-impacted.xml";
		String testClassesPath = repoPath + "/automation-tests/target/test-classes";

		RepoCloner.cloneOrUpdateRepo(repoUrl, repoPath);
		Thread.sleep(3000);

		compileAutomationTests(repoPath + "/automation-tests");

		// ✅ Generate testng suite based on class-method pairs
		Files.createDirectories(Paths.get(repoPath));
		TestNGSuiteGenerator.generateSuite(new ArrayList<>(impactedClassMethods), suiteXmlPath);
		System.out.println("✅ testng-impacted.xml generated at: " + suiteXmlPath);

		// ✅ Run tests
		runUsingMavenTestSuite(suiteXmlPath);
	}

	private static void runUsingMavenTestSuite(String suiteXmlPath) throws Exception {
		System.out.println("\uD83D\uDE80 Running with Maven command line...");

		String mvnCmd = "C:\\Users\\avina\\Downloads\\apache-maven-3.9.10\\bin\\mvn.cmd";

		List<String> mvnCommand = Arrays.asList(
				mvnCmd,
				"test",
				"-DsuiteXmlFile=" + suiteXmlPath
		);

		File automationTestsDir = new File("./repo/automation-tests");
		ProcessBuilder builder = new ProcessBuilder(mvnCommand);
		builder.directory(automationTestsDir);
		builder.inheritIO();
		Process process = builder.start();
		int result = process.waitFor();

		System.out.println("Maven exited with code " + result);
		if (result != 0) {
			throw new RuntimeException("❌ Maven test execution failed.");
		}
	}

	private static void runUsingTestNGJavaAPI(String suiteXmlPath) {
		System.out.println("\uD83D\uDE80 Running with TestNG Java API...");
		TestNG testng = new TestNG();
		testng.setTestSuites(Collections.singletonList(suiteXmlPath));
		testng.run();
	}

	private static void runUsingCommandLineProcess(String testClassesPath, String suiteXmlPath) throws Exception {
		System.out.println("\uD83D\uDE80 Running with Java CLI...");

		String sep = System.getProperty("path.separator");
		String testngJar = "C:\\Users\\avina\\.m2\\repository\\org\\testng\\testng\\7.9.0\\testng-7.9.0.jar";
		String jcommanderJar = "C:\\Users\\avina\\.m2\\repository\\com\\beust\\jcommander\\1.82\\jcommander-1.82.jar";
		String classpath = String.join(sep, testClassesPath, testngJar, jcommanderJar);

		List<String> command = Arrays.asList(
				"java", "-cp", classpath, "org.testng.TestNG", suiteXmlPath
		);

		System.out.println("\uD83D\uDCBB Command: " + String.join(" ", command));
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.inheritIO();
		Process process = builder.start();
		int result = process.waitFor();

		if (result != 0) {
			throw new RuntimeException("❌ TestNG execution via CLI failed.");
		} else {
			System.out.println("✅ TestNG executed successfully.");
		}
	}

	private static void compileAutomationTests(String projectDir) throws Exception {
		System.out.println("\uD83D\uDD27 Compiling with Maven...");
		String mvnCmd = "C:\\Users\\avina\\Downloads\\apache-maven-3.9.10\\bin\\mvn.cmd";
		ProcessBuilder builder = new ProcessBuilder(mvnCmd, "test-compile");
		builder.directory(new File(projectDir));
		builder.inheritIO();
		Process process = builder.start();
		int result = process.waitFor();
		if (result != 0) {
			throw new RuntimeException("❌ Compilation failed.");
		}
		System.out.println("✅ Compilation successful.");
	}
}





//package com.yourorg.agent;
//
//import org.testng.TestNG;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.*;
//
//public class E2EOrchestrator {
//
//	public static void main(String[] args) throws Exception {
//
//		String defectDescription = "Payment";
//		System.out.println("\uD83D\uDCC5 Calling Excel MAPPER...");
//
//		List<ExcelMapper.MappingRow> mapping = ExcelMapper.loadMapping("mapping.xlsx");
//
//		StringBuilder mapPrompt = new StringBuilder();
//		for (ExcelMapper.MappingRow row : mapping) {
//			mapPrompt.append(row.functionality).append(":").append(row.testCaseId).append("; ");
//		}
//
//		String prompt = "Given this defect description: \"" + defectDescription + "\" Given this mapping: " + mapPrompt + ". Which test cases are impacted?";
//		System.out.println("\uD83E\uDD16 Calling LLM CLIENT...");
//
//		String llmResponse = LLMClient.queryLLM(prompt);
//		System.out.println("This is the LLM response");
//		System.out.println(llmResponse);
//
//		// ✅ Parse LLM response to get class names
//		Set<String> impactedClasses = new HashSet<>();
//		String[] lines = llmResponse.split("\\R");
//		for (String line : lines) {
//			line = line.trim();
//			if (line.startsWith("-")) {
//				line = line.substring(1).trim();
//			}
//			if (line.contains(":")) {
//				String[] parts = line.split(":");
//				if (parts.length == 2) {
//					String classNameFromLLM = parts[0].trim();
//					for (ExcelMapper.MappingRow row : mapping) {
//						if (row.javaClassName.contains(classNameFromLLM)) {
//							String formattedClassName = row.javaClassName.trim().replace(".tests.", ".");
//							impactedClasses.add(formattedClassName);
//						}
//					}
//				}
//			}
//		}
//		System.out.println("✅ Impacted classes: " + impactedClasses);
//
//		String repoUrl = "https://github.com/Avinash-Golla/automation-tests.git";
//		String repoPath = "./repo";
//		String suiteXmlPath = repoPath + "/automation-tests/testng-impacted.xml";
//		String testClassesPath = repoPath + "/automation-tests/target/test-classes";
//
//		// Clone or update repo
//		RepoCloner.cloneOrUpdateRepo(repoUrl, repoPath);
//
//		// Wait to ensure cloning completes and files are available
//		Thread.sleep(3000);
//
//		// Compile using Maven
//		compileAutomationTests(repoPath + "/automation-tests");
//
//		// Create Suite
//		Files.createDirectories(Paths.get(repoPath));
//		TestNGSuiteGenerator.generateSuite(new ArrayList<>(impactedClasses), suiteXmlPath);
//
//		// Choose one of the methods below:
//		//runUsingTestNGJavaAPI(suiteXmlPath);
//		//runUsingCommandLineProcess(testClassesPath, suiteXmlPath);
//		runUsingMavenTestSuite(suiteXmlPath);
//	}
//
//	// ✅ Approach 1: Use Maven CLI to run suite
//	private static void runUsingMavenTestSuite(String suiteXmlPath) throws Exception {
//		System.out.println("\uD83D\uDE80 Running with Maven command line...");
//
//		String mvnCmd = "C:\\Users\\avina\\Downloads\\apache-maven-3.9.10\\bin\\mvn.cmd";
//
//		List<String> mvnCommand = Arrays.asList(
//				mvnCmd,
//				"test",
//				"-DsuiteXmlFile=" + suiteXmlPath
//		);
//
//		// Change working directory to the automation-tests folder
//		File automationTestsDir = new File("./repo/automation-tests");
//
//		ProcessBuilder builder = new ProcessBuilder(mvnCommand);
//		builder.directory(automationTestsDir); // 👈 RUN FROM CORRECT FOLDER
//		builder.inheritIO();
//		Process process = builder.start();
//		int result = process.waitFor();
//
//		System.out.println("Maven exited with code " + result);
//		if (result != 0) {
//			throw new RuntimeException("❌ Maven test execution failed.");
//		}
//	}
//
//	// ✅ Approach 2: Directly use TestNG Java API
//	private static void runUsingTestNGJavaAPI(String suiteXmlPath) {
//		System.out.println("\uD83D\uDE80 Running with TestNG Java API...");
//		TestNG testng = new TestNG();
//		testng.setTestSuites(Collections.singletonList(suiteXmlPath));
//		testng.run();
//	}
//
//	// ✅ Approach 3: Use Java command line process
//	private static void runUsingCommandLineProcess(String testClassesPath, String suiteXmlPath) throws Exception {
//		System.out.println("\uD83D\uDE80 Running with Java CLI...");
//
//		String sep = System.getProperty("path.separator");
//
//		String testngJar = "C:\\Users\\avina\\.m2\\repository\\org\\testng\\testng\\7.9.0\\testng-7.9.0.jar";
//		String jcommanderJar = "C:\\Users\\avina\\.m2\\repository\\com\\beust\\jcommander\\1.82\\jcommander-1.82.jar";
//
//		String classpath = String.join(sep, testClassesPath, testngJar, jcommanderJar);
//
//		List<String> command = Arrays.asList(
//				"java", "-cp", classpath, "org.testng.TestNG", suiteXmlPath
//		);
//
//		System.out.println("💻 Command: " + String.join(" ", command));
//		ProcessBuilder builder = new ProcessBuilder(command);
//		builder.inheritIO();
//		Process process = builder.start();
//		int result = process.waitFor();
//
//		if (result != 0) {
//			throw new RuntimeException("❌ TestNG execution via CLI failed.");
//		} else {
//			System.out.println("✅ TestNG executed successfully.");
//		}
//	}
//
//	private static void compileAutomationTests(String projectDir) throws Exception {
//		System.out.println("\uD83D\uDD27 Compiling with Maven...");
//		String mvnCmd = "C:\\Users\\avina\\Downloads\\apache-maven-3.9.10\\bin\\mvn.cmd";
//		ProcessBuilder builder = new ProcessBuilder(mvnCmd, "test-compile");
//		builder.directory(new File(projectDir));
//		builder.inheritIO();
//		Process process = builder.start();
//		int result = process.waitFor();
//		if (result != 0) {
//			throw new RuntimeException("❌ Compilation failed.");
//		}
//		System.out.println("✅ Compilation successful.");
//	}
//}


//package com.yourorg.agent;
//
//import org.testng.TestNG;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.*;
//
//public class E2EOrchestrator {
//
//	public static void main(String[] args) throws Exception {
//
//		String defectDescription = "Payment";
//		System.out.println("\uD83D\uDCC5 Calling Excel MAPPER...");
//
//		List<ExcelMapper.MappingRow> mapping = ExcelMapper.loadMapping("mapping.xlsx");
//
//		StringBuilder mapPrompt = new StringBuilder();
//		for (ExcelMapper.MappingRow row : mapping) {
//			mapPrompt.append(row.functionality).append(":").append(row.testCaseId).append("; ");
//		}
//
//		String prompt = "Given this defect description: \"" + defectDescription + "\" Given this mapping: " + mapPrompt + ". Which test cases are impacted?";
//		System.out.println("\uD83E\uDD16 Calling LLM CLIENT...");
//
//		String llmResponse = LLMClient.queryLLM(prompt);
//		System.out.println("This is the LLM response");
//		System.out.println(llmResponse);
//
//		// ✅ Parse LLM response into test cases
//		List<String> impactedTestCases = new ArrayList<>();
//		String[] lines = llmResponse.split("\\R");
//		for (String line : lines) {
//			line = line.trim();
//			if (line.startsWith("-")) {
//				line = line.substring(1).trim();
//			}
//			if (line.contains(":")) {
//				String[] parts = line.split(":");
//				if (parts.length == 2) {
//					impactedTestCases.add(parts[1].trim());
//				}
//			}
//		}
//        System.out.println("The impacted testcases are"+ impactedTestCases);
//
//		Set<String> impactedClasses = new HashSet<>();
//		for (ExcelMapper.MappingRow row : mapping) {
//			if (impactedTestCases.contains(row.testCaseId)) {
//				String className = row.javaClassName.trim().replace(".tests.", ".");
//				impactedClasses.add(className);
//			}
//		}
//
//		String repoUrl = "https://github.com/Avinash-Golla/automation-tests.git";
//		String repoPath = "./repo";
//		String suiteXmlPath = repoPath + "/automation-tests/testng-impacted.xml";
//		String testClassesPath = repoPath + "/automation-tests/target/test-classes";
//
//		// Clone or update repo
//		RepoCloner.cloneOrUpdateRepo(repoUrl, repoPath);
//
//		// Wait to ensure cloning completes and files are available
//		Thread.sleep(3000);
//
//		// Compile using Maven
//		compileAutomationTests(repoPath + "/automation-tests");
//
//		// Create Suite
//		Files.createDirectories(Paths.get(repoPath));
//		TestNGSuiteGenerator.generateSuite(new ArrayList<>(impactedClasses), suiteXmlPath);
//
//		// Choose one of the methods below:
//		//runUsingTestNGJavaAPI(suiteXmlPath);
//		//runUsingCommandLineProcess(testClassesPath, suiteXmlPath);
//		runUsingMavenTestSuite(suiteXmlPath);
//	}
//
//	// ✅ Approach 1: Use Maven CLI to run suite
//	private static void runUsingMavenTestSuite(String suiteXmlPath) throws Exception {
//		System.out.println("\uD83D\uDE80 Running with Maven command line...");
//
//		String mvnCmd = "C:\\Users\\avina\\Downloads\\apache-maven-3.9.10\\bin\\mvn.cmd";
//
//		List<String> mvnCommand = Arrays.asList(
//				mvnCmd,
//				"test",
//				"-DsuiteXmlFile=" + suiteXmlPath
//		);
//
//		// Change working directory to the automation-tests folder
//		File automationTestsDir = new File("./repo/automation-tests");
//
//		ProcessBuilder builder = new ProcessBuilder(mvnCommand);
//		builder.directory(automationTestsDir); // 👈 RUN FROM CORRECT FOLDER
//		builder.inheritIO();
//		Process process = builder.start();
//		int result = process.waitFor();
//
//		System.out.println("Maven exited with code " + result);
//		if (result != 0) {
//			throw new RuntimeException("❌ Maven test execution failed.");
//		}
//	}
//
//	// ✅ Approach 2: Directly use TestNG Java API
//	private static void runUsingTestNGJavaAPI(String suiteXmlPath) {
//		System.out.println("\uD83D\uDE80 Running with TestNG Java API...");
//		TestNG testng = new TestNG();
//		testng.setTestSuites(Collections.singletonList(suiteXmlPath));
//		testng.run();
//	}
//
//	// ✅ Approach 3: Use Java command line process
//	private static void runUsingCommandLineProcess(String testClassesPath, String suiteXmlPath) throws Exception {
//		System.out.println("\uD83D\uDE80 Running with Java CLI...");
//
//		String sep = System.getProperty("path.separator");
//
//		String testngJar = "C:\\Users\\avina\\.m2\\repository\\org\\testng\\testng\\7.9.0\\testng-7.9.0.jar";
//		String jcommanderJar = "C:\\Users\\avina\\.m2\\repository\\com\\beust\\jcommander\\1.82\\jcommander-1.82.jar";
//
//		String classpath = String.join(sep, testClassesPath, testngJar, jcommanderJar);
//
//		List<String> command = Arrays.asList(
//				"java", "-cp", classpath, "org.testng.TestNG", suiteXmlPath
//		);
//
//		System.out.println("💻 Command: " + String.join(" ", command));
//		ProcessBuilder builder = new ProcessBuilder(command);
//		builder.inheritIO();
//		Process process = builder.start();
//		int result = process.waitFor();
//
//		if (result != 0) {
//			throw new RuntimeException("❌ TestNG execution via CLI failed.");
//		} else {
//			System.out.println("✅ TestNG executed successfully.");
//		}
//	}
//
//	private static void compileAutomationTests(String projectDir) throws Exception {
//		System.out.println("\uD83D\uDD27 Compiling with Maven...");
//		String mvnCmd = "C:\\Users\\avina\\Downloads\\apache-maven-3.9.10\\bin\\mvn.cmd";
//		ProcessBuilder builder = new ProcessBuilder(mvnCmd, "test-compile");
//		builder.directory(new File(projectDir));
//		builder.inheritIO();
//		Process process = builder.start();
//		int result = process.waitFor();
//		if (result != 0) {
//			throw new RuntimeException("❌ Compilation failed.");
//		}
//		System.out.println("✅ Compilation successful.");
//	}
//}


//package com.yourorg.agent;
//
//import org.testng.TestNG;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.*;
//
//public class E2EOrchestrator {
//
//	public static void main(String[] args) throws Exception {
//
//		String defectDescription = "Payment";
//		System.out.println("📅 Calling Excel MAPPER...");
//
//		List<ExcelMapper.MappingRow> mapping = ExcelMapper.loadMapping("mapping.xlsx");
//
//		StringBuilder mapPrompt = new StringBuilder();
//		for (ExcelMapper.MappingRow row : mapping) {
//			mapPrompt.append(row.functionality).append(":").append(row.testCaseId).append("; ");
//		}
//
//		String prompt = "Given this defect description: \"" + defectDescription + "\" Given this mapping: " + mapPrompt + ". Which test cases are impacted?";
//		System.out.println("🤖 Calling LLM CLIENT...");
//
//		String llmResponse = LLMClient.queryLLM(prompt);
//		System.out.println("This is the LLM response");
//		System.out.println(llmResponse);
//
//		// Dummy impacted test case IDs for now
//		List<String> impactedTestCases = Arrays.asList("TC001", "TC003");
//
//		Set<String> impactedClasses = new HashSet<>();
//		for (ExcelMapper.MappingRow row : mapping) {
//			if (impactedTestCases.contains(row.testCaseId)) {
//				String className = row.javaClassName.trim().replace(".tests.", ".");
//				impactedClasses.add(className);
//			}
//		}
//
//		String repoUrl = "https://github.com/Avinash-Golla/automation-tests.git";
//		String repoPath = "./repo";
//		String suiteXmlPath = repoPath + "/automation-tests/testng-impacted.xml";
//		String testClassesPath = repoPath + "/automation-tests/target/test-classes";
//
//		// Clone or update repo
//		RepoCloner.cloneOrUpdateRepo(repoUrl, repoPath);
//
//		// Wait to ensure cloning completes and files are available
//		Thread.sleep(3000);
//
//		// Compile using Maven
//		compileAutomationTests(repoPath + "/automation-tests");
//
//		// Create Suite
//		Files.createDirectories(Paths.get(repoPath));
//		TestNGSuiteGenerator.generateSuite(new ArrayList<>(impactedClasses), suiteXmlPath);
//
//		// Choose one of the methods below:
//		//runUsingTestNGJavaAPI(suiteXmlPath);
//		//runUsingCommandLineProcess(testClassesPath, suiteXmlPath);
//		runUsingMavenTestSuite(suiteXmlPath);
//	}
//
//	// ✅ Approach 1: Use Maven CLI to run suite
//	private static void runUsingMavenTestSuite(String suiteXmlPath) throws Exception {
//		System.out.println("🚀 Running with Maven command line...");
//
//		String mvnCmd = "C:\\Users\\avina\\Downloads\\apache-maven-3.9.10\\bin\\mvn.cmd";
//
//		List<String> mvnCommand = Arrays.asList(
//				mvnCmd,
//				"test",
//				"-DsuiteXmlFile=" + suiteXmlPath
//		);
//
//		// Change working directory to the automation-tests folder
//		File automationTestsDir = new File("./repo/automation-tests");
//
//		ProcessBuilder builder = new ProcessBuilder(mvnCommand);
//		builder.directory(automationTestsDir); // 👈 RUN FROM CORRECT FOLDER
//		builder.inheritIO();
//		Process process = builder.start();
//		int result = process.waitFor();
//
//		System.out.println("Maven exited with code " + result);
//		if (result != 0) {
//			throw new RuntimeException("❌ Maven test execution failed.");
//		}
//	}
//
//
//	// ✅ Approach 2: Directly use TestNG Java API
//	private static void runUsingTestNGJavaAPI(String suiteXmlPath) {
//		System.out.println("🚀 Running with TestNG Java API...");
//		TestNG testng = new TestNG();
//		testng.setTestSuites(Collections.singletonList(suiteXmlPath));
//		testng.run();
//	}
//
//	// ✅ Approach 3: Use Java command line process
//	private static void runUsingCommandLineProcess(String testClassesPath, String suiteXmlPath) throws Exception {
//		System.out.println("🚀 Running with Java CLI...");
//
//		String sep = System.getProperty("path.separator");
//
//		String testngJar = "C:\\Users\\avina\\.m2\\repository\\org\\testng\\testng\\7.9.0\\testng-7.9.0.jar";
//		String jcommanderJar = "C:\\Users\\avina\\.m2\\repository\\com\\beust\\jcommander\\1.82\\jcommander-1.82.jar";
//
//		String classpath = String.join(sep, testClassesPath, testngJar, jcommanderJar);
//
//		List<String> command = Arrays.asList(
//				"java", "-cp", classpath, "org.testng.TestNG", suiteXmlPath
//		);
//
//		System.out.println("💻 Command: " + String.join(" ", command));
//		ProcessBuilder builder = new ProcessBuilder(command);
//		builder.inheritIO();
//		Process process = builder.start();
//		int result = process.waitFor();
//
//		if (result != 0) {
//			throw new RuntimeException("❌ TestNG execution via CLI failed.");
//		} else {
//			System.out.println("✅ TestNG executed successfully.");
//		}
//	}
//
//	private static void compileAutomationTests(String projectDir) throws Exception {
//		System.out.println("🔧 Compiling with Maven...");
//		String mvnCmd = "C:\\Users\\avina\\Downloads\\apache-maven-3.9.10\\bin\\mvn.cmd";
//		ProcessBuilder builder = new ProcessBuilder(mvnCmd, "test-compile");
//		builder.directory(new File(projectDir));
//		builder.inheritIO();
//		Process process = builder.start();
//		int result = process.waitFor();
//		if (result != 0) {
//			throw new RuntimeException("❌ Compilation failed.");
//		}
//		System.out.println("✅ Compilation successful.");
//	}


////package com.yourorg.agent;
////
////import org.testng.TestNG;
////
////import java.io.File;
////import java.nio.file.Files;
////import java.nio.file.Paths;
////import java.util.*;
////
////public class E2EOrchestrator {
////
////	public static void main(String[] args) throws Exception {
////
////		String defectDescription = "Login fails intermittently with special characters.";
////		System.out.println("\uD83D\uDCC5 Calling Excel MAPPER...");
////
////		List<ExcelMapper.MappingRow> mapping = ExcelMapper.loadMapping("mapping.xlsx");
////
////		StringBuilder mapPrompt = new StringBuilder();
////		for (ExcelMapper.MappingRow row : mapping) {
////			mapPrompt.append(row.functionality).append(":").append(row.testCaseId).append("; ");
////		}
////
////		String prompt = "Given this defect description: \"" + defectDescription + "\" Given this mapping: " + mapPrompt + ". Which test cases are impacted?";
////		System.out.println("\uD83E\uDD16 Calling LLM CLIENT...");
////
////		String llmResponse = LLMClient.queryLLM(prompt);
////		System.out.println("This is the LLM response");
////		System.out.println(llmResponse);
////
////		// Dummy impacted list from LLM for testing
////		List<String> impactedTestCases = Arrays.asList("TC001", "TC003");
////
////		Set<String> impactedClasses = new HashSet<>();
////		for (ExcelMapper.MappingRow row : mapping) {
////			if (impactedTestCases.contains(row.testCaseId)) {
////				// ✅ Normalize class names to remove incorrect .tests. if present
////				String className = row.javaClassName.trim().replace(".tests.", ".");
////				impactedClasses.add(className);
////			}
////		}
////
////		String repoUrl = "https://github.com/Avinash-Golla/automation-tests.git";
////		String repoPath = "./repo";
////		String suiteXmlPath = repoPath + "/automation-tests/testng-impacted.xml";
////		String testClassesPath = repoPath + "/automation-tests/target/test-classes";
////
////		// 1. Clone or update repo
////		RepoCloner.cloneOrUpdateRepo(repoUrl, repoPath);
////
////		// 2. Compile repo using Maven
////		compileAutomationTests(repoPath + "/automation-tests");
////
////		// 3. Create suite XML
////		Files.createDirectories(Paths.get(repoPath));
////		TestNGSuiteGenerator.generateSuite(new ArrayList<>(impactedClasses), suiteXmlPath);
////
////		// 4. Run TestNG using external process with correct classpath
////		runTestNGWithCustomClasspath(suiteXmlPath, testClassesPath);
////	}
////
////	private static void compileAutomationTests(String projectDir) throws Exception {
////		String mvnCmd = "C:\\Users\\avina\\Downloads\\apache-maven-3.9.10\\bin\\mvn.cmd";
////		ProcessBuilder builder = new ProcessBuilder(mvnCmd, "test-compile");
////		builder.directory(new File(projectDir));
////		builder.inheritIO();
////		Process process = builder.start();
////		int result = process.waitFor();
////		if (result != 0) {
////			throw new RuntimeException("\u274C Compilation failed for automation-tests project.");
////		}
////		System.out.println("\u2705 Compilation successful.");
////	}
////
////	private static void runTestNGWithCustomClasspath(String suiteXmlPath, String testClassesPath) throws Exception {
////		String sep = System.getProperty("path.separator");
////
////		String testngJar = "C:\\Users\\avina\\.m2\\repository\\org\\testng\\testng\\7.9.0\\testng-7.9.0.jar";
////		String jcommanderJar = "C:\\Users\\avina\\.m2\\repository\\com\\beust\\jcommander\\1.82\\jcommander-1.82.jar";
////
////		String classpath = String.join(sep, testClassesPath, testngJar, jcommanderJar);
////
////		System.out.println("📦 Launching TestNG with classpath:\n" + classpath);
////		System.out.println("🔧 Suite path: " + suiteXmlPath);
////
////		List<String> command = Arrays.asList(
////				"java", "-cp", classpath, "org.testng.TestNG", suiteXmlPath
////		);
////
////		System.out.println("💻 Command: " + String.join(" ", command));
////		testClassesPath = "./repo/automation-tests/target/test-classes";
////		 testngJar = "C:\\Users\\avina\\.m2\\repository\\org\\testng\\testng\\7.9.0\\testng-7.9.0.jar";
////		 jcommanderJar = "C:\\Users\\avina\\.m2\\repository\\com\\beust\\jcommander\\1.82\\jcommander-1.82.jar";
////		String suiteXml = "./repo/automation-tests/testng-impacted.xml";
////
////		 sep = System.getProperty("path.separator");
////		 classpath = String.join(sep, testClassesPath, testngJar, jcommanderJar);
////
////		List<String> command1 = Arrays.asList(
////				"java",
////				"-cp",
////				classpath,
////				"org.testng.TestNG",
////				suiteXml
////		);
////
//////		ProcessBuilder builder = new ProcessBuilder(command1);
//////		builder.inheritIO();
//////		Process process = builder.start();
//////		int result = process.waitFor();
////		List<String> mvnCommand = Arrays.asList(
////				"mvn",
////				"test",
////				"-DsuiteXmlFile=repo/automation-tests/testng-impacted.xml"
////		);
////
////		ProcessBuilder builder = new ProcessBuilder(mvnCommand);
////		builder.inheritIO();                 // forward STDOUT/STDERR to your console
////
////		Process process = builder.start();
////		int exitCode = process.waitFor();    // block until Maven finishes
////
////		System.out.println("Maven exited with code " + exitCode);
////		TestNG testng = new TestNG();
////		testng.setTestSuites(
////				Collections.singletonList("repo/automation-tests/testng-impacted.xml")
////		);
////		testng.run();
////	}
////
////}
////package com.yourorg.agent;
////
////import org.testng.TestNG;
////
////import java.io.File;
////import java.io.IOException;
////import java.nio.file.*;
////import java.util.*;
////import java.util.concurrent.TimeUnit;
////
////public class E2EOrchestrator {
////
////	private static final Path BASE_DIR        = Paths.get("").toAbsolutePath();
////	private static final Path REPO_DIR        = BASE_DIR.resolve("repo");
////	private static final Path PROJECT_DIR     = REPO_DIR.resolve("automation-tests");
////	private static final Path SUITE_XML       = PROJECT_DIR.resolve("testng-impacted.xml");
////	private static final Path TEST_CLASSES    = PROJECT_DIR.resolve("target/test-classes");
////	private static final String GIT_URL       = "https://github.com/Avinash-Golla/automation-tests.git";
////	private static final String MAPPING_FILE  = "mapping.xlsx";
////	private static final long   PROCESS_TIMEOUT_MINUTES = 5;
////
////	public static void main(String[] args) {
////		try {
////			log("📂 Working directory: " + BASE_DIR);
////			Map<String, String> mapping    = loadMapping();
////			Set<String> impactedClasses    = determineImpactedClasses(mapping);
////			cloneOrUpdateRepo();
////			compileProject();
////			generateSuiteXml(impactedClasses);
////			runImpactedTests();
////			log("✅ All done.");
////		} catch (Exception e) {
////			e.printStackTrace();
////			System.exit(1);
////		}
////	}
////
////	private static Map<String, String> loadMapping() throws Exception {
////		log("📄 Loading mapping from " + MAPPING_FILE);
////		List<ExcelMapper.MappingRow> rows = ExcelMapper.loadMapping(MAPPING_FILE);
////		Map<String, String> map = new HashMap<>();
////		for (ExcelMapper.MappingRow row : rows) {
////			map.put(row.testCaseId, row.javaClassName.trim().replace(".tests.", "."));
////		}
////		return map;
////	}
////
////	private static Set<String> determineImpactedClasses(Map<String, String> mapping) {
////		String defectDescription = "Login fails intermittently with special characters.";
////		log("💡 Querying LLM with defect: " + defectDescription);
////
////		// Build prompt from mapping
////		StringBuilder promptBuilder = new StringBuilder();
////		mapping.forEach((tc, cls) -> promptBuilder.append(tc).append("->").append(cls).append("; "));
////		String prompt = String.format(
////				"Given this defect: \"%s\" and mapping: %s Which test cases are impacted?",
////				defectDescription, promptBuilder
////		);
////
////		String llmResponse = LLMClient.queryLLM(prompt);
////		log("🤖 LLM responded: " + llmResponse);
////
////		// TODO: parse actual LLM output; using dummy for now
////		List<String> impacted = Arrays.asList("TC001", "TC003");
////		log("🔥 Impacted test IDs: " + impacted);
////
////		Set<String> classes = new HashSet<>();
////		for (String tc : impacted) {
////			String cls = mapping.get(tc);
////			if (cls != null) {
////				classes.add(cls);
////			}
////		}
////		log("🏷 Impacted classes: " + classes);
////		return classes;
////	}
////
////	private static void cloneOrUpdateRepo() throws IOException, InterruptedException {
////		if (Files.exists(PROJECT_DIR)) {
////			log("🔄 Updating existing repo...");
////			runCommand(REPO_DIR, "git", "pull");
////		} else {
////			log("🌱 Cloning repo from " + GIT_URL);
////			Files.createDirectories(REPO_DIR);
////			runCommand(REPO_DIR, "git", "clone", GIT_URL, PROJECT_DIR.getFileName().toString());
////		}
////	}
////
////	private static void compileProject() throws IOException, InterruptedException {
////		log("🔧 Compiling project via Maven (test-compile)...");
////		runCommand(PROJECT_DIR, resolveMavenCmd(), "clean", "test-compile");
////		log("✅ Compilation successful.");
////	}
////
////	private static void generateSuiteXml(Set<String> impactedClasses) throws Exception {
////		log("📝 Generating TestNG suite at " + SUITE_XML);
////		Files.createDirectories(SUITE_XML.getParent());
////		TestNGSuiteGenerator.generateSuite(new ArrayList<>(impactedClasses), SUITE_XML.toString());
////	}
////
////	private static void runImpactedTests() {
////		log("🚀 Running impacted tests via embedded TestNG...");
////		TestNG testng = new TestNG();
////		testng.setTestSuites(Collections.singletonList(SUITE_XML.toString()));
////		testng.setOutputDirectory(PROJECT_DIR.resolve("target/test-output").toString());
////		testng.run();
////	}
////
////	private static void runCommand(Path workingDir, String... command)
////			throws IOException, InterruptedException {
////		log("▶️ " + String.join(" ", command));
////		ProcessBuilder pb = new ProcessBuilder(command)
////				.directory(workingDir.toFile())
////				.inheritIO();
////		Process p = pb.start();
////		boolean finished = p.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
////		if (!finished) {
////			p.destroyForcibly();
////			throw new RuntimeException("Process timed out: " + Arrays.toString(command));
////		}
////		if (p.exitValue() != 0) {
////			throw new RuntimeException("Command failed (" + p.exitValue() + "): " + Arrays.toString(command));
////		}
////	}
////
////	private static String resolveMavenCmd() {
////		String os = System.getProperty("os.name").toLowerCase();
////		return os.contains("win") ? "mvn.cmd" : "mvn";
////	}
////
////	private static void log(String msg) {
////		System.out.println(msg);
////	}
////}
//
//package com.yourorg.agent;
//
//import org.testng.TestNG;
//
//import java.io.IOException;
//import java.io.UncheckedIOException;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.nio.file.*;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Stream;
//
//public class E2EOrchestrator {
//
//	// ——— Constants & (initially) blank paths ———
//	private static final Path BASE_DIR       = Paths.get("").toAbsolutePath();
//	private static final Path REPO_DIR       = BASE_DIR.resolve("repo");
//	private static final String GIT_URL      = "https://github.com/Avinash-Golla/automation-tests.git";
//	private static final String MAPPING_FILE = "mapping.xlsx";
//	private static final long   TIMEOUT_MIN  = 5;
//
//	// Will be set at runtime once we know where pom.xml lives
//	private static Path PROJECT_DIR;
//	private static Path TEST_CLASSES;
//	private static Path DEPENDENCY_DIR;
//	private static Path SUITE_XML;
//
//	public static void main(String[] args) {
//		try {
//			log("📂 Base working directory: " + BASE_DIR);
//
//			// 1) Load your mapping
//			Map<String,String> mapping = loadMapping();
//
//			// 2) Ask your LLM which test IDs are impacted → map to Java class names
//			Set<String> impactedClasses = determineImpactedClasses(mapping);
//
//			// 3) Clone or update the tests repo
//			cloneOrUpdateRepo();
//
//			// 4) Locate the folder under 'repo' that contains the pom.xml
//			PROJECT_DIR = locateProjectDir();
//			TEST_CLASSES  = PROJECT_DIR.resolve("target/test-classes");
//			DEPENDENCY_DIR= PROJECT_DIR.resolve("target/dependency");
//			SUITE_XML     = PROJECT_DIR.resolve("testng-impacted.xml");
//
//			// 5) Compile & copy ALL test-scope dependencies
//			compileAndCopyDependencies();
//
//			// 6) Generate your impacted-only suite XML
//			generateSuiteXml(impactedClasses);
//
//			// 7) Launch TestNG in-JVM with a ClassLoader over test-classes + all jars
//			runImpactedTests(impactedClasses);
//
//			log("✅ E2E orchestration complete.");
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			System.exit(1);
//		}
//	}
//
//	// ——— 1) Excel Mapping ———
//	private static Map<String,String> loadMapping() throws Exception {
//		log("📄 Loading mapping from " + MAPPING_FILE);
//		var rows = ExcelMapper.loadMapping(MAPPING_FILE);
//		Map<String,String> map = new HashMap<>();
//		for (var row : rows) {
//			String cls = row.javaClassName.trim().replace(".tests.", ".");
//			map.put(row.testCaseId, cls);
//		}
//		log("🔍 Loaded " + map.size() + " mapping entries");
//		return map;
//	}
//
//	// ——— 2) Query LLM & pick impacted classes ———
//	private static Set<String> determineImpactedClasses(Map<String,String> mapping) {
//		String defect = "Login fails intermittently with special characters.";
//		log("💡 Querying LLM for defect: \"" + defect + "\"");
//
//		var prompt = new StringBuilder("Given this defect: \"")
//				.append(defect).append("\" and mapping: ");
//		mapping.forEach((tc, cls) -> prompt.append(tc).append("->").append(cls).append("; "));
//		prompt.append(" Which test cases are impacted?");
//
//		String llmOutput = LLMClient.queryLLM(prompt.toString());
//		log("🤖 LLM says:\n" + llmOutput);
//
//		// TODO parse real output; dummy for demo:
//		var impactedIds = List.of("TC001","TC003");
//		log("🔥 Impacted test IDs: " + impactedIds);
//
//		Set<String> classes = new HashSet<>();
//		for (String id : impactedIds) {
//			if (mapping.containsKey(id)) {
//				classes.add(mapping.get(id));
//			} else {
//				log("⚠️  No mapping for test ID " + id);
//			}
//		}
//		log("🏷 Impacted classes: " + classes);
//		return classes;
//	}
//
//	// ——— 3) Git clone / pull ———
//	private static void cloneOrUpdateRepo() throws IOException, InterruptedException {
//		if (Files.exists(REPO_DIR)) {
//			log("🔄 Updating existing repo at " + REPO_DIR);
//		} else {
//			log("🌱 Creating folder " + REPO_DIR);
//			Files.createDirectories(REPO_DIR);
//		}
//
//		// If the subfolder 'automation-tests' already contains pom.xml, we pull.
//		Path candidate = REPO_DIR.resolve("automation-tests");
//		if (Files.exists(candidate.resolve("pom.xml"))) {
//			runCommand(candidate, "git", "pull");
//		} else {
//			// remove any stale folder
//			if (Files.exists(candidate)) {
//				deleteRecursively(candidate);
//			}
//			log("🌱 Cloning " + GIT_URL);
//			// omit the target dir argument so git uses the repo basename
//			runCommand(REPO_DIR, "git", "clone", GIT_URL);
//		}
//	}
//
//	// ——— 4) Find the folder under REPO_DIR that has a pom.xml ———
//	private static Path locateProjectDir() throws IOException {
//		log("🔎 Locating project folder with pom.xml under " + REPO_DIR);
//		try (Stream<Path> walk = Files.walk(REPO_DIR, 2)) {
//			Optional<Path> found = walk
//					.filter(p -> p.getFileName().toString().equals("pom.xml"))
//					.findFirst();
//			if (found.isEmpty()) {
//				throw new RuntimeException("❌ Could not find a pom.xml under " + REPO_DIR);
//			}
//			Path projectFolder = found.get().getParent();
//			log("✔️  Found pom.xml in " + projectFolder);
//			return projectFolder;
//		}
//	}
//
//	// ——— 5) Compile & copy test-scope dependencies ———
//	private static void compileAndCopyDependencies() throws IOException, InterruptedException {
//		String mvn = resolveMavenCmd();
//
//		log("🔧 mvn clean test-compile");
//		runCommand(PROJECT_DIR, mvn, "clean", "test-compile");
//
//		log("📦 mvn dependency:copy-dependencies (test scope)");
//		Files.createDirectories(DEPENDENCY_DIR);
//		runCommand(
//				PROJECT_DIR,
//				mvn,
//				"dependency:copy-dependencies",
//				"-DincludeScope=test",
//				"-DoutputDirectory=" + DEPENDENCY_DIR
//		);
//		log("✅ Dependencies copied into " + DEPENDENCY_DIR);
//	}
//
//	// ——— 6) Generate TestNG suite XML ———
//	private static void generateSuiteXml(Set<String> impactedClasses) throws Exception {
//		log("📝 Generating TestNG suite at " + SUITE_XML);
//		Files.createDirectories(SUITE_XML.getParent());
//		TestNGSuiteGenerator.generateSuite(new ArrayList<>(impactedClasses),
//				SUITE_XML.toString());
//	}
//
//	// ——— 7) Embed TestNG with custom URLs ———
//	private static void runImpactedTests(Set<String> impactedClasses) throws Exception {
//		log("🚀 Running impacted tests via embedded TestNG");
//
//		// build URL list: target/test-classes + every jar under target/dependency
//		List<URL> urls = new ArrayList<>();
//		urls.add(TEST_CLASSES.toUri().toURL());
//		try (var stream = Files.list(DEPENDENCY_DIR)) {
//			stream.filter(p -> p.toString().endsWith(".jar"))
//					.forEach(jar -> {
//						try { urls.add(jar.toUri().toURL()); }
//						catch (IOException e) { throw new UncheckedIOException(e); }
//					});
//		}
//
//		URLClassLoader loader = new URLClassLoader(
//				urls.toArray(new URL[0]),
//				Thread.currentThread().getContextClassLoader()
//		);
//		Thread.currentThread().setContextClassLoader(loader);
//
//		// load classes explicitly, so TestNG need not re-parse XML
//		Class<?>[] classes = impactedClasses.stream()
//				.map(name -> {
//					try { return loader.loadClass(name); }
//					catch (ClassNotFoundException ex) {
//						throw new RuntimeException("Cannot load test class: " + name, ex);
//					}
//				})
//				.toArray(Class<?>[]::new);
//
//		TestNG testng = new TestNG();
//		testng.setTestClasses(classes);
//		testng.setOutputDirectory(PROJECT_DIR.resolve("target/test-output").toString());
//		testng.run();
//	}
//
//	// ——— Utility ———
//
//	private static void runCommand(Path dir, String... cmd)
//			throws IOException, InterruptedException {
//		log("▶️  " + String.join(" ", cmd) + "  (in " + dir + ")");
//		ProcessBuilder pb = new ProcessBuilder(cmd)
//				.directory(dir.toFile())
//				.inheritIO();
//		Process proc = pb.start();
//		if (!proc.waitFor(TIMEOUT_MIN, TimeUnit.MINUTES)) {
//			proc.destroyForcibly();
//			throw new RuntimeException("Timed out: " + String.join(" ", cmd));
//		}
//		if (proc.exitValue() != 0) {
//			throw new RuntimeException("Command failed (" + proc.exitValue()
//					+ "): " + String.join(" ", cmd));
//		}
//	}
//
//	private static void deleteRecursively(Path path) throws IOException {
//		Files.walk(path)
//				.sorted(Comparator.reverseOrder())
//				.forEach(p -> {
//					try { Files.delete(p); }
//					catch (IOException e) { /* ignore */ }
//				});
//	}
//
//	private static String resolveMavenCmd() {
//		String os = System.getProperty("os.name").toLowerCase();
//		return os.contains("win") ? "mvn.cmd" : "mvn";
//	}
//
//	private static void log(String msg) {
//		System.out.println(msg);
//	}
//}

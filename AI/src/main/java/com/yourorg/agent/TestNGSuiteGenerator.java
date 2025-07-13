package com.yourorg.agent;

import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TestNGSuiteGenerator {

	// Expects inputs like "LoginTest:TC001", "PaymentTest:TC002"
	public static void generateSuite(List<String> classAndMethods, String outputPath) throws Exception {

		System.out.println("🔍 Input classAndMethods:");
		if (classAndMethods == null || classAndMethods.isEmpty()) {
			System.out.println("⚠️ No impacted test cases provided. Skipping suite generation.");
			return;
		}
		classAndMethods.forEach(System.out::println);

		Map<String, List<String>> classToMethods = new HashMap<>();

		for (String entry : classAndMethods) {
			String[] parts = entry.split(":");
			if (parts.length != 2) continue;
			String className = parts[0].trim();
			String methodName = parts[1].trim();

			classToMethods
					.computeIfAbsent(className, k -> new ArrayList<>())
					.add(methodName);
		}

		StringBuilder xml = new StringBuilder();
		xml.append("<suite name=\"RegressionSuite\">\n");
		xml.append("  <test name=\"ImpactedTests\">\n");
		xml.append("    <classes>\n");

		for (Map.Entry<String, List<String>> entry : classToMethods.entrySet()) {
			String className = entry.getKey();
			List<String> methods = entry.getValue();

			xml.append("      <class name=\"com.yourorg.").append(className).append("\">\n");
			xml.append("        <methods>\n");
			for (String method : methods) {
				xml.append("          <include name=\"").append(method).append("\"/>\n");
			}
			xml.append("        </methods>\n");
			xml.append("      </class>\n");
		}

		xml.append("    </classes>\n");
		xml.append("  </test>\n");
		xml.append("</suite>\n");

		try (FileWriter writer = new FileWriter(outputPath, StandardCharsets.UTF_8)) {
			writer.write(xml.toString());
		}

		System.out.println("✅ testng-impacted.xml generated at: " + outputPath);
	}
}



////package com.yourorg.agent;
////
////import java.util.List;
////import java.io.FileWriter;
////
////public class TestNGSuiteGenerator {
////
////public static void generateSuite(List<String> testClasses, String outputPath) throws Exception
////{
////	StringBuilder xml =new StringBuilder();
////	xml.append("<suite name=\"RegressionSuite\">\n  <test name=\"ImpactedTests\">\n  <classes>\n");
////	for(String cls:testClasses)
////	{
////		xml.append("   <class name=\"").append(cls).append("\"/>\n");
////	}
////
////	xml.append("   </classes>\n  </test>\n</suite>");
////    FileWriter writer =new FileWriter(outputPath);
////    writer.write(xml.toString());
////    writer.close();
////
////}
////
////}
//
//package com.yourorg.agent;
//
//import java.util.List;
//import java.io.FileWriter;
//
//public class TestNGSuiteGenerator {
//
//	public static void generateSuite(List<String> testClasses, String outputPath) throws Exception {
//		StringBuilder xml = new StringBuilder();
//		xml.append("<suite name=\"RegressionSuite\">\n  <test name=\"ImpactedTests\">\n    <classes>\n");
//
//		for (String cls : testClasses) {
//			// Ensure fully qualified class names like com.yourorg.LoginTest
//			xml.append("      <class name=\"").append(cls.trim()).append("\"/>\n");
//		}
//
//		xml.append("    </classes>\n  </test>\n</suite>");
//
//		try (FileWriter writer = new FileWriter(outputPath)) {
//			writer.write(xml.toString());
//		}
//
//		System.out.println("✅ testng-impacted.xml generated at: " + outputPath);
//	}
//}
//
//package com.yourorg.agent;
//
//import java.io.FileWriter;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//
//public class TestNGSuiteGenerator {
//
//	// Expects inputs like "LoginTest:TC001", "PaymentTest:TC002"
//	public static void generateSuite(List<String> classAndMethods, String outputPath) throws Exception {
//		Map<String, List<String>> classToMethods = new HashMap<>();
//
//		for (String entry : classAndMethods) {
//			String[] parts = entry.split(":");
//			if (parts.length != 2) continue;
//			String className = parts[0].trim();
//			String methodName = parts[1].trim();
//
//			classToMethods
//					.computeIfAbsent(className, k -> new ArrayList<>())
//					.add(methodName);
//		}
//
//		StringBuilder xml = new StringBuilder();
//		xml.append("<suite name=\"RegressionSuite\">\n");
//		xml.append("  <test name=\"ImpactedTests\">\n");
//		xml.append("    <classes>\n");
//
//		for (Map.Entry<String, List<String>> entry : classToMethods.entrySet()) {
//			String className = entry.getKey();
//			List<String> methods = entry.getValue();
//
//			xml.append("      <class name=\"com.yourorg.").append(className).append("\">\n");
//			xml.append("        <methods>\n");
//			for (String method : methods) {
//				xml.append("          <include name=\"").append(method).append("\"/>\n");
//			}
//			xml.append("        </methods>\n");
//			xml.append("      </class>\n");
//		}
//
//		xml.append("    </classes>\n");
//		xml.append("  </test>\n");
//		xml.append("</suite>\n");
//
//		try (FileWriter writer = new FileWriter(outputPath, StandardCharsets.UTF_8)) {
//			writer.write(xml.toString());
//		}
//
//		System.out.println("✅ testng-impacted.xml generated at: " + outputPath);
//	}
//}


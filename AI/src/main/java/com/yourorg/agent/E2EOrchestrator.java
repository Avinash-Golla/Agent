package com.yourorg.agent;

import java.util.*;


public class E2EOrchestrator {

	public static void main(String args[]) throws Exception{

		String defectDescription ="Login fails intermittently with special characters. ";

		System.out.println("Calling Excel MAPPER");

		List<ExcelMapper.MappingRow> mapping =ExcelMapper.loadMapping("mapping.xlsx");

		StringBuilder mapPrompt =new StringBuilder();

		for(ExcelMapper.MappingRow row: mapping)
		{
			mapPrompt.append(row.functionality).append(":").append(row.testCaseId).append("; ");
		}

		String prompt ="Given this defect description: \""+defectDescription +"\" Given this mapping: " + mapPrompt + ". Which test cases are impacted?";
		System.out.println("Calling LLM CLIENT");
		String llmResponse=LLMClient.queryLLM(prompt);

		List<String> impactedTestCases=Arrays.asList("TC001", "TC002");
		Set<String> impactedClasses =new HashSet<>();
		for(ExcelMapper.MappingRow row: mapping)
		{
			if(impactedTestCases.contains(row.testCaseId))
			{
				impactedClasses.add(row.javaClassName);
			}
		}

		String repoUrl = "https://github.com/Avinash-Golla/automation-tests.git";
		String localPath = "./repo";

		RepoCloner.cloneOrUpdateRepo(repoUrl, localPath);

		// Generate suite and run tests
		TestNGSuiteGenerator.generateSuite(new ArrayList<>(impactedClasses), "./repo/testng-impacted.xml");
		TestNGRunner.runSuite("./repo/testng-impacted.xml");


	}


}

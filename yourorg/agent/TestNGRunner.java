package com.yourorg.agent;

import org.testng.TestNG;

import java.util.Collections;


public class TestNGRunner {
	
	public static void runSuite(String suiteXmlPath)
	{
		TestNG testng =new TestNG();
		testng.setTestSuites(Collections.singletonList(suiteXmlPath));
		testng.run();
		
	}

}

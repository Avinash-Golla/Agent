package com.yourorg.agent;

import java.util.List;
import java.io.FileWriter;

public class TestNGSuiteGenerator {
	
public static void generateSuite(List<String> testClasses, String outputPath) throws Exception
{
	StringBuilder xml =new StringBuilder();
	xml.append("<suite name=\"RegressionSuite\">\n  <test name=\"ImpactedTests\">\n  <classes>\n");
	for(String cls:testClasses)
	{
		xml.append("   <class name=\"").append(cls).append("\"/>\n");
	}
	
	xml.append("   </classes>\n  </test>\n</suite>");
    FileWriter writer =new FileWriter(outputPath);
    writer.write(xml.toString());
    writer.close();
    
}

}

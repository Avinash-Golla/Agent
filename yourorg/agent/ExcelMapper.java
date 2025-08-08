package com.yourorg.agent;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;
import java.util.*;

public class ExcelMapper {
	
	public static class MappingRow{
		public String functionality;
		public String testCaseId;
		public String javaClassName;
		public MappingRow(String functionality, String testCaseId,String javaClassName)
		{
			this.functionality=functionality;
			this.testCaseId=testCaseId;
			this.javaClassName=javaClassName;
		}
		
	
	}

	public static List<MappingRow> loadMapping(String excelPath) throws Exception{
		List<MappingRow> mapping =new ArrayList<>();
		FileInputStream fis= new FileInputStream(excelPath);
		Workbook workbook=new XSSFWorkbook(fis);
		Sheet sheet =workbook.getSheetAt(0);
		Iterator<Row> iterator=sheet.iterator();
		iterator.next();
		while(iterator.hasNext())
		 {
			Row row=iterator.next();
			String  functionality=row.getCell(0).getStringCellValue();
			String testCaseId = row.getCell(1).getStringCellValue();
			String javaClassName = row.getCell(2).getStringCellValue();
			mapping.add(new MappingRow(functionality, testCaseId, javaClassName));
			 
		 }
		
		workbook.close();
		fis.close();
		return mapping;
	}
}

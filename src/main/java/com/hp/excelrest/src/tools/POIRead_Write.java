
package com.hp.excelrest.src.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.hp.excelrest.src.config.Config;
import com.hp.excelrest.src.rallydev.Iteration;
import com.hp.excelrest.src.testcase.Assertions;
import com.hp.excelrest.src.testcase.QueueTestCase;
import com.hp.excelrest.src.testcase.RestTestCase;
import com.hp.excelrest.src.testcase.Spreadsheet;
import com.hp.excelrest.src.testcase.TestCase;
import com.hp.excelrest.src.testcase.Variable;

public class POIRead_Write {

	private Utils utils = new Utils();
	private RestTestCase rest;
	private QueueTestCase qtc;
	private TestCase tc;
	private LinkedList<TestCase> tcs;
	static Logger log = Logger.getLogger(POIRead_Write.class);

	public POIRead_Write(){}

	public int findColumnThatContains(Row row, String cellContent) {
		for (Cell cell : row) {
			if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
				if (cell.getRichStringCellValue().getString().trim().contains(cellContent)) {
					return cell.getColumnIndex();  
				}
			}
		}
		return -1;
	}

	public int findRow(Sheet sheet, String cellContent) {
		for (Row row : sheet) {
			for (Cell cell : row) {
				if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
					if (cell.getRichStringCellValue().getString().trim().equals(cellContent)) {
						return row.getRowNum();  
					}
				}
			}
		}               
		return -1;
	}

	public int findColumn(Sheet sheet, String cellContent) {
		for (Row row : sheet) {
			for (Cell cell : row) {
				if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
					if (cell.getRichStringCellValue().getString().trim().equals(cellContent)) {
						return cell.getColumnIndex();  
					}
				}
			}
		}               
		return -1;
	}

	public String getStringValue(Cell cell) {
		String value = "";
		try{
			value = cell.getStringCellValue().trim();

		}catch (Exception e) {
			try {
				value = new java.text.DecimalFormat("0").format(cell.getNumericCellValue());
			} catch (Exception e2) {
				value="";
			}
		}
		return value;
	}

	/***************************************************************************************
	 * Read Test cases from excel spreadsheet
	 * @param tests spreadsheet location
	 * @return This method returns a list containing all test cases
	 * token, sessionId, appId, userId, userToken - used on some specific projects
	 ***************************************************************************************/
	public LinkedList<TestCase> readTestCases(Config config, String fileLocation, String sheetToRead, String token, String sessionId, String appId, String userId, String userToken) throws InvalidFormatException, IOException {
		
		tcs = new LinkedList<TestCase>();

		log.debug("File Location: "+fileLocation);
		InputStream inp = new FileInputStream(fileLocation);
		Workbook wb = WorkbookFactory.create(inp);
		int column_number;
		Sheet sheet;
		Cell xlsCell;

		String[] serverList = config.getRest().getUrl();

		if(serverList == null) { 
			log.error("REST server URL not found! Check the XML config file.");
			System.exit(1);
		}

		HSSFFormulaEvaluator.evaluateAllFormulaCells(wb);


		sheet = wb.getSheet(sheetToRead);
		String sheetName = sheetToRead;
		if(sheet!=null){

			log.info("Sheet found: "+sheetName+" at: "+fileLocation);

			//iterate into all rows
			for(int row_number=findRow(sheet, "tc_id")+1; row_number<=sheet.getLastRowNum(); row_number++) {

				tc = new TestCase();
				tc.setSheetName(sheetName);

				Row row = sheet.getRow(findRow(sheet, "TEST_TYPE"));
				Cell test_type = null;
				try {

					test_type = row.getCell(findColumn(sheet, "TEST_TYPE")+1); 
				}catch (Exception e) {
					log.error("Error reading test cases. Check: "+fileLocation+", sheet: "+sheetName);
					log.error("Verify if the spreadsheet has some blank lines between the test cases or, in the end, and remove them.");
					log.error(e.getMessage());
					System.exit(1);
				}
				tc.setTestType(getStringValue(test_type));
				tc.setRowNumber(row_number);

				if(findRow(sheet, ("rallyDevId")) != -1){
					row = sheet.getRow(row_number);
					column_number = findColumn(sheet, "rallyDevId");
					xlsCell = row.getCell(column_number);
					String tmp = getStringValue(xlsCell);
					if(tmp.isEmpty())tc.setRallyDevId("empty"); 
					else
						tc.setRallyDevId(getStringValue(xlsCell));
					log.debug("rallyDevId row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
				} else{
					log.error("rallyDevId header not found!");
					System.exit(1);
				}

				if(findRow(sheet, ("tc_id")) != -1){
					row = sheet.getRow(row_number);
					column_number = findColumn(sheet, "tc_id");
					xlsCell = row.getCell(column_number);
					tc.setId(getStringValue(xlsCell));
					log.debug("tc_id row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
				} else{
					log.error("tc_id header not found!");
					System.exit(1);
				}

				if(findRow(sheet, ("tc_name")) != -1){
					row = sheet.getRow(row_number);
					column_number = findColumn(sheet, "tc_name");
					xlsCell = row.getCell(column_number);
					tc.setName(getStringValue(xlsCell));
					log.debug("tc_name row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
				} else{
					log.error("tc_name header not found!");
					System.exit(1);
				}

				if(findRow(sheet, ("tc_description")) != -1){
					row = sheet.getRow(row_number);
					column_number = findColumn(sheet, "tc_description");
					xlsCell = row.getCell(column_number);
					tc.setDescription(getStringValue(xlsCell));
					log.debug("tc_description row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
				} else{
					log.error("tc_description header not found!");
					System.exit(1);
				}

				if(findRow(sheet, ("defect_number")) != -1){
					row = sheet.getRow(row_number);
					column_number = findColumn(sheet, "defect_number");
					xlsCell = row.getCell(column_number);
					tc.setDefect_number(getStringValue(xlsCell));
					log.debug("defect_number row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
				} else{
					log.error("defect_number header not found!");
					System.exit(1);
				}

				if(findRow(sheet, ("tc_result")) != -1){
					row = sheet.getRow(row_number);
					column_number = findColumn(sheet, "tc_result");
					xlsCell = row.getCell(column_number);
					try{
						log.debug("tc_result row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
						if(getStringValue(xlsCell).equalsIgnoreCase("PASS")) tc.setLastResultPass(true);
						else tc.setLastResultPass(false);
					}catch (Exception e) {
						log.error("Error reading test cases. Check: "+fileLocation+", sheet: "+sheetName);
						log.error("tc_result row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
						log.error(e.getMessage());
						log.error(e.getCause());
						System.exit(1);
					}
				} else{
					log.error("tc_result header not found!");
					System.exit(1);
				}

				if(findRow(sheet, ("tc_assertions")) != -1){
					row = sheet.getRow(row_number);
					column_number = findColumn(sheet, "tc_assertions");
					xlsCell = row.getCell(column_number);
					log.debug("tc_assertions row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));

					LinkedList<Assertions> assertionList = new LinkedList<Assertions>();
					if(!getStringValue(xlsCell).isEmpty()){
						String values[] = xlsCell.getStringCellValue().split("\n");
						for(int j=0; j<values.length; j++) {
							String div[] = values[j].split("=");
							if(div.length==2){
								Assertions assertion = new Assertions(div[0].toString().trim().replace("(char)61",""+(char)61),div[1].toString().trim().replace("(char)61",""+(char)61));
								assertionList.add(assertion);
							} else {
								log.error("tc_assertions Error parsing assertions at row: "+(row_number+1)+", column: "+(findColumn(sheet, "tc_assertions")+1));
								log.error("Content: "+getStringValue(xlsCell));
								System.exit(1);
							}
						}
					}
					tc.setAssertions(assertionList);
				} else{
					log.error("tc_assertions header not found!");
					System.exit(1);
				}

				if(findRow(sheet, ("tc_vars")) != -1){
					row = sheet.getRow(row_number);
					column_number = findColumn(sheet, "tc_vars");
					xlsCell = row.getCell(column_number);
					log.debug("tc_vars row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));

					LinkedList<Variable> varList = new LinkedList<Variable>();
					if(!getStringValue(xlsCell).isEmpty()){
						String values[] = xlsCell.getStringCellValue().split("\n");
						for(int j=0; j<values.length; j++) {
							String div[] = values[j].split("=");
							if(div.length==3){
								Variable var = new Variable(div[0].toString().trim().replace("(char)61",""+(char)61),div[1].toString().trim().replace("(char)61",""+(char)61),div[2].toString().trim().replace("(char)61",""+(char)61));
								varList.add(var);
							} else {
								log.error("tc_vars Error parsing tc_vars at row: "+(row_number+1)+", column: "+(column_number+1));
								log.error("Content: "+getStringValue(xlsCell));
								log.info("\n");
								log.info("###################################################################");
								log.info("#                      TC_VARS INSTRUCTIONS                       #");
								log.info("#                These are the options for TC_VARS                #");
								log.info("###################################################################");
								log.info("#	jsonkey{id}=jsonkeyToBeFound=TCExecutedtoSearchjsonkey          #");
								log.info("# xmlelement{id}=xmlElementToBeFound=TCExecutedtoSearchXmlElement #");
								log.info("# response=StringToBeReplaced=TCExecutedtoSearch                  #");
								log.info("# header=StringToBeFound=TCExecutedtoSearchHeader                 #");
								log.info("# local_var=StringToBeReplaced=ContentThatWillReplacetheString    #");
								log.info("###################################################################");
								log.info("# Remove any blank line. Each instruction should  be placed in    #");
								log.info("# one line (use break line for more than one ALT+ENTER)           #");
								log.info("###################################################################");
								System.exit(1);
							}
						}
					}
					tc.setVars(varList);

				} else{
					log.error("tc_vars header not found!");
					System.exit(1);
				}

				if(findRow(sheet, ("tc_execute")) != -1){
					row = sheet.getRow(row_number);
					column_number = findColumn(sheet, "tc_execute");
					xlsCell = row.getCell(column_number);

					try{
						log.debug("tc_execute row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+xlsCell.getBooleanCellValue());
						tc.setExecutable(xlsCell.getBooleanCellValue());
					}catch (Exception e) {
						log.error("Error reading test cases. Check: "+fileLocation+", sheet: "+sheetName);
						log.error("tc_execute row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+xlsCell.getBooleanCellValue());
						log.error(e.getMessage());
						log.error(e.getCause());
						System.exit(1);
					}
				} else{
					log.error("tc_execute header not found!");
					System.exit(1);
				}

				/* ******************************** set variables on assertions ********************************************/
				//replace the variables found by the ones listed in the tc_vars as local_var   
				if(tc.getVars().size()>0){

					//if the tc_assertions have some variable, replace by the content identified in the variable list
					LinkedList<Assertions> assertionList = new LinkedList<Assertions>();
					assertionList = tc.getAssertions();
					for(int j=0; j<tc.getAssertions().size(); j++){
						String str = tc.getAssertions().get(j).getContent();
						for(int i=0; i<tc.getVars().size(); i++){
							if(tc.getVars().get(i).getType().trim().equalsIgnoreCase("local_var")){
								str = str.replace(tc.getVars().get(i).getName(), tc.getVars().get(i).getContent());
								tc.getAssertions().get(j).setContent(str.trim());
							}
						}
					}
					tc.setAssertions(assertionList);
				}

				/* ************************************** if the test case sheet name is rest_tests ************************************************/
				rest = new RestTestCase();
				if(tc.getTestType().equalsIgnoreCase("REST")){

					if(findRow(sheet, ("tc_path")) != -1){
						row = sheet.getRow(row_number);
						column_number = findColumn(sheet, "tc_path");
						xlsCell = row.getCell(column_number);

						int serverNumber = 0;
						for(int i=0; i < serverList.length; i++){
							if(getStringValue(xlsCell).trim().contains("server"+i))
								serverNumber = i; 
						}

						rest.setPath(getStringValue(xlsCell).trim().replace("server"+serverNumber, serverList[serverNumber]));
						log.debug("Path after replace server: "+rest.getPath());

						log.debug("tc_path row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
					} else{
						log.error("tc_path header not found!");
						System.exit(1);
					}

					if(findRow(sheet, ("tc_method")) != -1){
						row = sheet.getRow(row_number);
						column_number = findColumn(sheet, "tc_method");
						xlsCell = row.getCell(column_number);
						rest.setMethod(getStringValue(xlsCell));
						log.debug("tc_method row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
					} else{
						log.error("tc_method header not found!");
						System.exit(1);
					}

					if(findRow(sheet, ("tc_body")) != -1){
						row = sheet.getRow(row_number);
						column_number = findColumn(sheet, "tc_body");
						xlsCell = row.getCell(column_number);

						int serverNumber = 0;
						for(int i=0; i < serverList.length; i++){
							if(getStringValue(xlsCell).trim().contains("server"+i))
								serverNumber = i; 
						}


						String tmp = getStringValue(xlsCell).trim().replace("server"+serverNumber, serverList[serverNumber]);

						//replace some string to address the needs of the IOT Voice Project
						if(sessionId!=null) tmp = tmp.replace("$%SESSIONID%$", sessionId);
						if(appId!=null)	tmp = tmp.replace("$%APPLICATIONID%$", appId);
						if(userId!=null) tmp = tmp.replace("$%USERID%$", userId);
						if(userToken!=null) tmp = tmp.replace("$%USERTOKEN%$", userToken);
						tmp = tmp.replace("$%TIMESTAMP%$", utils.getCurrentimeStampISO8601());

						rest.setBody(tmp);
						log.debug("tc_body row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
						log.debug("Body after replace server: "+rest.getBody());

					} else{
						log.error("tc_body header not found!");
						System.exit(1);
					}


					if(findRow(sheet, ("tc_headers")) != -1){
						row = sheet.getRow(row_number);
						column_number = findColumn(sheet, "tc_headers");
						xlsCell = row.getCell(column_number);

						String headercontent = getStringValue(xlsCell);

						int credentials = -1;
						if(!headercontent.isEmpty()){
							String values[] = headercontent.split("\n");
							for(int j=0; j<values.length; j++) {
								if(values[j].contains("credentials=")){
									values[j] = values[j].replace("credentials=", "");
									credentials = j;
								}
							}

							if(credentials!=-1){
								byte[] encoded_credentials = Base64.encodeBase64(values[credentials].getBytes()); 
								values[credentials]= "Authorization: Basic "+new String(encoded_credentials);
							}

							StringBuilder builder = new StringBuilder();
							for(String stmp : values) {
								builder.append(stmp+"\n");
							}
							headercontent = builder.toString().trim();
							headercontent = headercontent.replace("gettoken", token);


						}

						rest.setHeaders(headercontent);
						log.debug("tc_headers row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+headercontent);
					} else{
						log.error("tc_headers header not found!");
						System.exit(1);
					}

					/* ******************************** set variables on rest (local_var) ********************************************/ 
					if(tc.getVars().size()>0){


						String path = rest.getPath();
						for(int i=0; i<tc.getVars().size(); i++){
							if(tc.getVars().get(i).getType().trim().equalsIgnoreCase("local_var"))
								path = path.replace(tc.getVars().get(i).getName(), tc.getVars().get(i).getContent());
						}
						rest.setPath(path.trim());

						String body = rest.getBody();
						for(int i=0; i<tc.getVars().size(); i++){
							if(tc.getVars().get(i).getType().trim().equalsIgnoreCase("local_var"))
								body = body.replace(tc.getVars().get(i).getName(), tc.getVars().get(i).getContent());
						}
						rest.setBody(body.trim());

						//if the tc_queue_to_consume have some variable, replace by the content identified in the variable list
						String headers = rest.getHeaders();
						for(int i=0; i<tc.getVars().size(); i++){
							if(tc.getVars().get(i).getType().trim().equalsIgnoreCase("local_var"))
								headers = headers.replace(tc.getVars().get(i).getName(), tc.getVars().get(i).getContent());
						}
						rest.setHeaders(headers.trim());
					}
					/* ***********************************************************************************************************/
				}
				tc.setResttc(rest);
				/* ****************************************************************************************************************************/

				/* ************************************** if the test case sheet name is queue_tests ************************************************/
				qtc = new QueueTestCase();
				if(tc.getTestType().equalsIgnoreCase("QUEUE")){

					if(findRow(sheet, ("tc_queue_to_publish")) != -1){
						row = sheet.getRow(row_number);
						column_number = findColumn(sheet, "tc_queue_to_publish");
						xlsCell = row.getCell(column_number);
						qtc.setQueue_to_publish(getStringValue(xlsCell));
						log.debug("tc_queue_to_publish row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
					} else{
						log.error("tc_queue_to_publish header not found!");
						System.exit(1);
					}

					if(findRow(sheet, ("tc_message_to_publish")) != -1){
						row = sheet.getRow(row_number);
						column_number = findColumn(sheet, "tc_message_to_publish");
						xlsCell = row.getCell(column_number);
						qtc.setMessage_to_publish(getStringValue(xlsCell));
						log.debug("tc_message_to_publish row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
					} else{
						log.error("tc_message_to_publish header not found!");
						System.exit(1);
					}

					if(findRow(sheet, ("tc_queue_to_consume")) != -1){
						row = sheet.getRow(row_number);
						column_number = findColumn(sheet, "tc_queue_to_consume");
						xlsCell = row.getCell(column_number);
						qtc.setQueue_to_consume(getStringValue(xlsCell));
						log.debug("tc_queue_to_consume row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
					} else{
						log.error("tc_queue_to_consume header not found!");
						System.exit(1);
					}


					/* ******************************** set variables on queue (local_var) ********************************************/ 
					if(tc.getVars().size()>0){
						//if the tc_message_to_publish have some variable, replace by the content identified in the variable list
						String str = qtc.getMessage_to_publish();
						for(int i=0; i<tc.getVars().size(); i++){
							if(tc.getVars().get(i).getType().trim().equalsIgnoreCase("local_var"))
								str = str.replace(tc.getVars().get(i).getName(), tc.getVars().get(i).getContent());
						}
						qtc.setMessage_to_publish(str);

						//if the tc_queue_to_publish have some variable, replace by the content identified in the variable list
						str = qtc.getQueue_to_publish();
						for(int i=0; i<tc.getVars().size(); i++){
							if(tc.getVars().get(i).getType().trim().equalsIgnoreCase("local_var"))
								str = str.replace(tc.getVars().get(i).getName(), tc.getVars().get(i).getContent());
						}
						qtc.setQueue_to_publish(str);

						//if the tc_queue_to_consume have some variable, replace by the content identified in the variable list
						str = qtc.getQueue_to_consume();
						for(int i=0; i<tc.getVars().size(); i++){
							if(tc.getVars().get(i).getType().trim().equalsIgnoreCase("local_var"))
								str = str.replace(tc.getVars().get(i).getName(), tc.getVars().get(i).getContent());
						}
						qtc.setQueue_to_consume(str);
					}
				}
				/* ****************************************************************************************************************************/
				tc.setQueuetc(qtc);

				if(row_number>0) {
					tcs.add(tc);
				}
			}

		} else {
			log.debug("The following sheet could not be found: "+sheetName+" in the spreadsheet: "+fileLocation);
		}



		/* ************************************** print test cases in debug mode ************************************************/
		log.debug("Listing test cases from xls..");
		log.debug("Number of Test cases: "+tcs.size());

		int count = 1;
		Iterator itTC = tcs.iterator();
		while(itTC.hasNext()){
			TestCase tc = (TestCase) itTC.next();
			log.debug("tc_row_number: "+tc.getRowNumber());
			log.debug("tc_sheet_name: "+tc.getSheetName());
			log.debug("tc_number: "+count);
			log.debug("rallyDevId: "+tc.getRallyDevId());
			log.debug("tc_id: "+tc.getId());
			log.debug("tc_name: "+tc.getName());
			log.debug("tc_description: "+tc.getDescription());
			log.debug("tc_queue_to_publish: "+tc.getQueuetc().getQueue_to_publish());
			log.debug("tc_message_to_publish: "+tc.getQueuetc().getMessage_to_publish());
			log.debug("tc_queue_to_consume: "+tc.getQueuetc().getQueue_to_consume());
			log.debug("tc_path: "+tc.getResttc().getPath());
			log.debug("tc_method: "+tc.getResttc().getMethod());
			log.debug("tc_body: "+tc.getResttc().getBody());
			log.debug("tc_header: "+tc.getResttc().getHeaders());

			Iterator itAssertion = tc.getAssertions().iterator();
			while(itAssertion.hasNext()){
				Assertions assertion = (Assertions) itAssertion.next();
				log.debug("tc_assertion: "+assertion.getType()+"="+assertion.getContent());
			}
			Iterator itVar = tc.getVars().iterator();
			while(itVar.hasNext()){
				Variable var = (Variable) itVar.next();
				log.debug("tc_variable: "+var.getName()+"="+var.getContent());

			}
			log.debug("tc_execute: "+new Boolean(tc.isExecutable()).toString());
			count++;
		}
		/* **********************************************************************************************************************/
		if (inp != null) inp.close();

		return tcs;
	}

	public LinkedList<Iteration> readIterationsSheet(String fileLocation){

		LinkedList<Iteration> itList = new LinkedList<Iteration>();

		try {
			InputStream inp = new FileInputStream(fileLocation);
			Workbook wb = WorkbookFactory.create(inp);
			int column_number;
			Sheet sheet;
			Cell xlsCell;
			String sheetName = "iterations";
			sheet = wb.getSheet(sheetName);
			Row row;

			if(sheet!=null){

				log.info("Sheet found: "+sheetName+" at: "+fileLocation);

				for(int row_number=findRow(sheet, "Name")+1; row_number<=sheet.getLastRowNum(); row_number++) {

					Iteration it = new Iteration();
					if(findRow(sheet, ("Name")) != -1){
						row = sheet.getRow(row_number);
						column_number = findColumn(sheet, "Name");
						xlsCell = row.getCell(column_number);
						it.setName(getStringValue(xlsCell));
						log.debug("Name row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
					} else{
						log.error("Name header was not found on iterations sheet!");
						log.error("Do not try to get the Sprint dates!");
						itList.clear();
						return itList;
					}

					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd");

					if(findRow(sheet, ("EndDate")) != -1){
						row = sheet.getRow(row_number);
						column_number = findColumn(sheet, "EndDate");
						xlsCell = row.getCell(column_number);

						Date startDate = sdf.parse(getStringValue(xlsCell));

						it.setStartDate(startDate);
						log.debug("EndDate row: "+(row_number+1)+", column_number: "+(column_number+1)+", content: "+getStringValue(xlsCell));
					} else{
						log.error("EndDate header was not found on iterations sheet!");
						log.error("Do not try to get the Sprint dates!");
						itList.clear();
						return itList;
					}

					itList.add(it);
				}
			} else {
				log.error("Cannot found iterations sheet! Do not try to get the Sprint dates!");
			}

		} catch(Exception e) {
			log.error(e.getMessage());
			log.error(e.getStackTrace());
			log.error("FAIL! Unable to get sprints from iterations spreadsheet!");
		}
		return itList;
	}

	public String identifyZone(String severity, String priority){

		String zone = "TBD";
		if((severity.equals("Crash/Data Loss"))&&(priority.equals("Normal")||priority.equals("High Attention")||priority.equals("Resolve Immediately"))){
			return "Red";
		}
		if((severity.equals("Major Problem"))&&(priority.equals("High Attention")||priority.equals("Resolve Immediately"))){
			return "Red";
		}
		if(severity.equals("Crash/Data Loss")&&(priority.equals("Low"))){
			return "Yellow";
		}
		if((severity.equals("Major Problem"))&&(priority.equals("Low")||priority.equals("Normal"))){
			return "Yellow";
		}
		if((severity.equals("Minor Problem"))&&(priority.equals("Normal")||priority.equals("High Attention")||priority.equals("Resolve Immediately"))){
			return "Yellow";
		}
		if(severity.equals("Minor Problem")&&(priority.equals("Low"))){
			return "Green";
		}
		if((severity.equals("Cosmetic"))&&(priority.equals("Low")||priority.equals("Normal")||priority.equals("High Attention")||priority.equals("Resolve Immediately"))){
			return "Green";
		}
		return zone;
	}

	public void updateReport(Config config, String projectId, LinkedList<String[]> results, LinkedList<Iteration> itList) throws InvalidFormatException, IOException {

		int objectIDColumn = -1;
		int LastRunColumn = -1,CreationDate = -1,LastUpdateDate = -1,StartDate = -1,EndDate = -1,ClosedDate = -1,AcceptedDate = -1,InProgressDate = -1, SeverityColumn = -1, PriorityColumn = -1, Description = -1, TestCaseStatus = -1;
		log.info("Result File Location: "+config.getReport().getOutput());

		InputStream inp = new FileInputStream(config.getReport().getOutput());
		Workbook wb = WorkbookFactory.create(inp);

		Sheet sheet = wb.getSheet(config.getReport().getSheet());

		if(sheet==null) sheet = wb.createSheet(config.getReport().getSheet());

		/****************************************** Writing sheet content *********************************************/
		log.debug("writing report into the sheet...");
		for(int row=0; row<results.size(); row++) {
			String result[] = results.get(row);
			String severity = "", priority = "", creation = "";
			//Row r = sheet.getRow(row);

			Row r = sheet.createRow(row);
			for(int column=0; column<result.length; column++) {

				if(result[column]!=null) {

					if(objectIDColumn == column) {
						log.debug("Column: "+column+1);
						log.debug("Row: "+row+1);
						log.debug("Cel Content: "+result[column]);
						String url = config.getRallyDev().getUrl()+"#/"+projectId+"/detail/"+config.getReport().getType()+"/"+result[column];
						log.debug("URL: "+url.toString());
						Cell cell = r.createCell(column);

						HSSFHyperlink url_link=new HSSFHyperlink(HSSFHyperlink.LINK_URL);
						url_link.setAddress(url);
						cell.setCellValue(url);         
						cell.setHyperlink(url_link);
					} else {

						log.debug("Column: "+column+1);
						log.debug("Row: "+row);
						log.debug("Cel Content: "+result[column]);
						try {

							//setting defect age on TestCaseStatus
							if(column==TestCaseStatus) {
								SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd");
								Date date = sdf.parse(creation);

								Date today = new Date();
								Date now = sdf.parse(sdf.format(today));
								long diff = now.getTime() - date.getTime();
								long between = diff / 1000L / 60L / 60L / 24L;
								result[column]=Long.toString(between);
							}
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						//setting defect zoning in Description
						if((column==SeverityColumn)) severity = result[column];
						if(column==PriorityColumn) priority = result[column];

						if((column==Description)&&(!severity.isEmpty())&&(!priority.isEmpty())){
							result[column]=identifyZone(severity, priority);
						}

						if((column==LastRunColumn)
								||(column==CreationDate)
								||(column==LastUpdateDate)
								||(column==StartDate)
								||(column==EndDate)
								||(column==ClosedDate)
								||(column==AcceptedDate)	
								||(column==InProgressDate)){
							if(!(result[column]).equalsIgnoreCase("no entry")){
								String[] parts =  result[column].split("T");
								result[column] = parts[0];
								if(column==CreationDate) creation = result[column];

								try{
									SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd");
									Date date = sdf.parse(result[column]);

									Iterator it = itList.iterator();
									while(it.hasNext()) {
										Iteration iteration = (Iteration) it.next();
										if(date.before(iteration.getStartDate())){
											result[column]=iteration.getName();
											break;
										}
									}
								} catch(Exception e) {
									log.error(e.getMessage());
									log.error(e.getStackTrace());
									log.error("FAIL! Unable to compare dates to collect the Sprint name!");
								}

							}
						}

						Cell cell = r.createCell(column);
						String tmp = result[column];
						tmp = tmp.replaceAll("<font color="+(char)34+"red"+(char)34+">","")
								.replaceAll("<font color="+(char)34+"orange"+(char)34+">", "")
								.replaceAll("<font color="+(char)34+"green"+(char)34+">", "")
								.replaceAll("</font>", "");
						cell.setCellValue(tmp); 
					}
					if(result[column].equalsIgnoreCase("ObjectID")) objectIDColumn = column;
					if(result[column].equalsIgnoreCase("CreationDate")) CreationDate = column;
					if(result[column].equalsIgnoreCase("LastUpdateDate")) LastUpdateDate = column;
					if(result[column].equalsIgnoreCase("StartDate")) StartDate = column;
					if(result[column].equalsIgnoreCase("EndDate")) EndDate = column;
					if(result[column].equalsIgnoreCase("ClosedDate")) ClosedDate = column;
					if(result[column].equalsIgnoreCase("AcceptedDate")) AcceptedDate = column;	
					if(result[column].equalsIgnoreCase("InProgressDate")) InProgressDate = column;
					if(result[column].equalsIgnoreCase("LastRun")) LastRunColumn = column;
					if(result[column].equalsIgnoreCase("Severity")) SeverityColumn = column;
					if(result[column].equalsIgnoreCase("Priority")) PriorityColumn = column;

					//this fields are rally result fields that we using to set different values after some calc in the result spreadsheet
					if(result[column].equalsIgnoreCase("Description")) Description = column;
					if(result[column].equalsIgnoreCase("TestCaseStatus")) TestCaseStatus = column;
				}
			}
			/**************************************************************************************************************/
		}

		for(int row=sheet.getLastRowNum(); row>=results.size(); row--){
			Row r = sheet.getRow(row);
			sheet.removeRow(r);	
		}

		FileOutputStream fileOut = new FileOutputStream(config.getReport().getOutput());
		wb.write(fileOut);
		fileOut.close();
	}

	public void updateTestCases(LinkedList<Spreadsheet> sList){

		Iterator itss = sList.iterator();
		while(itss.hasNext()) {
			Spreadsheet xls = (Spreadsheet) itss.next();

			try {
				Workbook wb = null;
				InputStream inp = null;

				log.info("Updating Test Cases: "+xls.getFileLocation());

				File file = new File(xls.getFileLocation());
				boolean fileExist = file.exists();

				if(!file.exists())  {
					log.error("File not found:  "+xls.getFileLocation());
					System.exit(1);
				} else {
					inp = new FileInputStream(xls.getFileLocation());
					wb = WorkbookFactory.create(inp);
				}

				Font font = wb.createFont();
				font.setBoldweight(Font.BOLDWEIGHT_BOLD);

				/** Identifying all sheet name*/
				/*LinkedList<String> sheetNames = new LinkedList<String>();
				String tmp = "";
				for(int i=0; i<tcs.size(); i++){
					if(!tcs.get(i).getSheetName().equalsIgnoreCase(tmp)){
						sheetNames.add(tcs.get(i).getSheetName());
						tmp = tcs.get(i).getSheetName(); 
						log.info("Test case sheet name: "+tcs.get(i).getSheetName());
					}
				}*/ 

				String result;
				log.debug("updating test cases...");

				tcs = xls.getTcs();
				for(int j=0; j<tcs.size(); j++) {

					Sheet sheet = wb.getSheet(tcs.get(j).getSheetName());

					if(tcs.get(j).isResult_success()) result = "PASS";
					else result = "FAIL";
					if(!tcs.get(j).isExecutable()) result="N/A";

					if(sheet!=null){
						Row r = sheet.getRow(tcs.get(j).getRowNumber());

						int rallyDevId_column = findColumn(sheet, "rallyDevId");
						log.debug("rallyDevId_column: "+rallyDevId_column);
						log.debug("rallyDevId_row: "+tcs.get(j).getRowNumber());
						log.debug("Cel Content: "+tcs.get(j).getRallyDevId());

						if(rallyDevId_column!=-1){ 

							Cell xlsCell = r.getCell(rallyDevId_column);
							String originalTC_ID = getStringValue(xlsCell);
							log.debug("Original TC_ID: "+originalTC_ID);

							Cell cell = r.createCell(rallyDevId_column);
							cell.setCellValue(tcs.get(j).getRallyDevId()); 

							/*if(!originalTC_ID.equals(tcs.get(j).getId())){
							for(int row_number=findRow(sheet, "rallyDevId")+1; row_number<=sheet.getLastRowNum(); row_number++) {
								r = sheet.getRow(row_number);

								while(findColumnThatContains(r, originalTC_ID)!=-1){
									int column_number = findColumnThatContains(r, originalTC_ID);

									if(column_number!=-1){
										xlsCell = r.getCell(column_number);
										String originalValue = getStringValue(xlsCell);
										log.debug(originalValue);
										String newValue = originalValue.replaceAll(originalTC_ID, tcs.get(j).getId());
										xlsCell = r.createCell(column_number);
										xlsCell.setCellValue(newValue); 
										log.info("row_number: "+(r.getRowNum())+", column_number: "+(column_number+1)+", original: "+originalValue+", new: "+newValue);
									}
								}
							}
						}*/

							/*CellStyle style = wb.createCellStyle();
						style.setBorderBottom(CellStyle.BORDER_THIN);
						style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
						style.setBorderLeft(CellStyle.BORDER_THIN);
						style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
						style.setBorderRight(CellStyle.BORDER_THIN);
						style.setRightBorderColor(IndexedColors.BLACK.getIndex());
						style.setBorderTop(CellStyle.BORDER_THIN);
						style.setTopBorderColor(IndexedColors.BLACK.getIndex());
						style.setAlignment(CellStyle.ALIGN_CENTER);
						cell.setCellStyle(style);*/
						}

						int tc_tc_result = findColumn(sheet, "tc_result");
						log.debug("tc_result_column: "+tc_tc_result);
						log.debug("tc_result_row: "+tcs.get(j).getRowNumber());
						log.debug("Cel Content: "+result);

						if(tc_tc_result!=-1){ 
							Cell cell = r.createCell(tc_tc_result);
							cell.setCellValue(result);
							/*CellStyle style = formatResult(wb, result);
						style.setFont(font);
						cell.setCellStyle(formatResult(wb, result));*/
						}


					} else log.error("Sheet name could not be found: "+tcs.get(j).getSheetName());
				}
				FileOutputStream fileOut = new FileOutputStream(xls.getFileLocation());
				wb.write(fileOut);
				fileOut.close();

			} catch(Exception e) {
				log.error(e.getMessage());
				log.error(e.getStackTrace());
				log.error("FAIL! Unable to update input spreadsheets!");
			}
		}
	}

	public void createReport(Config config, String projectId, LinkedList<String[]> results) throws InvalidFormatException, IOException {

		int objectIDColumn = -1;
		int LastRunColumn = -1,CreationDate = -1,LastUpdateDate = -1,StartDate = -1,EndDate = -1,ClosedDate = -1,AcceptedDate = -1,InProgressDate = -1;

		log.info("Result File Location: "+config.getReport().getOutput());

		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet sheet = workbook.createSheet(config.getReport().getType());

		/****************************************** Writing sheet content *********************************************/
		log.debug("writing report into the sheet...");
		for(int row=0; row<results.size(); row++) {
			String result[] = results.get(row);
			Row r = sheet.createRow(row);
			for(int column=0; column<result.length; column++) {

				if(result[column]!=null) {

					if(objectIDColumn == column) {
						log.debug("Column: "+column+1);
						log.debug("Row: "+row+1);
						log.debug("Cel Content: "+result[column]);
						String url = config.getRallyDev().getUrl()+"#/"+projectId+"/detail/"+config.getReport().getType()+"/"+result[column];
						log.debug("URL: "+url.toString());
						Cell cell = r.createCell(column);

						HSSFHyperlink url_link=new HSSFHyperlink(HSSFHyperlink.LINK_URL);
						url_link.setAddress(url);
						cell.setCellValue(result[column]);         
						cell.setHyperlink(url_link);
					} else {

						log.debug("Column: "+column+1);
						log.debug("Row: "+row);
						log.debug("Cel Content: "+result[column]);

						if((column==LastRunColumn)
								||(column==CreationDate)
								||(column==LastUpdateDate)
								||(column==StartDate)
								||(column==EndDate)
								||(column==ClosedDate)
								||(column==AcceptedDate)	
								||(column==InProgressDate)){
							if(!(result[column]).equalsIgnoreCase("no entry")){
								String[] parts =  result[column].split("T");
								result[column] = parts[0];
							}
						}
						Cell cell = r.createCell(column);
						String tmp = result[column];
						tmp = tmp.replaceAll("<font color="+(char)34+"red"+(char)34+">","")
								.replaceAll("<font color="+(char)34+"orange"+(char)34+">", "")
								.replaceAll("<font color="+(char)34+"green"+(char)34+">", "")
								.replaceAll("</font>", "");
						cell.setCellValue(tmp); 
					}

					if(result[column].equalsIgnoreCase("ObjectID")) objectIDColumn = column;
					if(result[column].equalsIgnoreCase("CreationDate")) CreationDate = column;
					if(result[column].equalsIgnoreCase("LastUpdateDate")) LastUpdateDate = column;
					if(result[column].equalsIgnoreCase("StartDate")) StartDate = column;
					if(result[column].equalsIgnoreCase("EndDate")) EndDate = column;
					if(result[column].equalsIgnoreCase("ClosedDate")) ClosedDate = column;
					if(result[column].equalsIgnoreCase("AcceptedDate")) AcceptedDate = column;	
					if(result[column].equalsIgnoreCase("InProgressDate")) InProgressDate = column;
					if(result[column].equalsIgnoreCase("LastRun")) LastRunColumn = column;
				}
			}
			/**************************************************************************************************************/
		}

		FileOutputStream fileOut = new FileOutputStream(config.getReport().getOutput());
		workbook.write(fileOut);
		fileOut.close();
	}

	public CellStyle formatResult(Workbook wb, String result){

		CellStyle style = wb.createCellStyle();
		style.setBorderBottom(CellStyle.BORDER_THIN);
		style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderLeft(CellStyle.BORDER_THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderRight(CellStyle.BORDER_THIN);
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setBorderTop(CellStyle.BORDER_THIN);
		style.setTopBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(CellStyle.ALIGN_CENTER);

		try {
			if(result.equalsIgnoreCase("PASS")) {
				style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
				style.setFillPattern(CellStyle.SOLID_FOREGROUND);

			}
			//if FAIL set cell background red
			else if(result.equalsIgnoreCase("FAIL")) { 
				style.setFillForegroundColor(IndexedColors.RED.getIndex());
				style.setFillPattern(CellStyle.SOLID_FOREGROUND);

			} 		
			else { 
				style.setFillForegroundColor(IndexedColors.AQUA.getIndex());
				style.setFillPattern(CellStyle.SOLID_FOREGROUND);

			}

		} catch(Exception e) {
			log.error(e.getMessage());
			log.error(e.getStackTrace());
			System.exit(1);
		}
		return style;
	}
}



package com.hp.excelrest.src.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.commons.httpclient.Header;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import com.hp.excelrest.src.config.Config;
import com.hp.excelrest.src.rallydev.Iteration;
import com.hp.excelrest.src.rallydev.RallyDevConn;
import com.hp.excelrest.src.testcase.Assertions;
import com.hp.excelrest.src.testcase.Spreadsheet;
import com.hp.excelrest.src.testcase.TestCase;
import com.hp.excelrest.src.testcase.Variable;
import com.hp.excelrest.src.tools.Connections;
import com.hp.excelrest.src.tools.ExcelToHtml;
import com.hp.excelrest.src.tools.JXLRead_Write;
import com.hp.excelrest.src.tools.POIRead_Write;
import com.hp.excelrest.src.tools.PostMail;
import com.hp.excelrest.src.tools.TimeController;
import com.hp.excelrest.src.tools.Utils;
import com.hp.excelrest.src.tools.XML_Reader;

import junit.framework.Test;

public class Main {

	public Main(){}

	static Logger log = Logger.getLogger(Main.class);

	private LinkedList<Variable> global_vars;
	private LinkedList<TestCase> tcs = new LinkedList<TestCase>();
	private LinkedList<Spreadsheet> spreadsheets = new LinkedList<Spreadsheet>();
	private XML_Reader xmlConfig;
	private Config config;
	private JXLRead_Write jxl = new JXLRead_Write();
	private POIRead_Write poi = new POIRead_Write();
	private RallyDevConn rallyconn =  new RallyDevConn();
	private String configFile, proxy, rallyUserId, projectId, userRef;
	private PostMail postmail = new PostMail(); 
	private String token, sessionId, appId, userId, userToken, buildNumber, xlsOutput, pwd, username, updatetc, regtestresult, server, defectID, diffPDFPath, projectName;
	private boolean fail = false, isRegression = false, reducedOutput = false;
	private Connections conn;
	private static String xlsInput[] = {};
	private static String sheetsToRead[] = {};
	private static String serverList[] = {};
	private Assertions assertions = new Assertions();
	private Utils utils = new Utils();
	private int waitTime=0;

	public void help(){
		log.info("#########################################################");
		log.info("#                AUTOMATION TESTS HELP                  #");
		log.info("#########################################################");
		log.info("# /config=xml_config_file                               #");
		log.info("# /sheets=sheets_to_read (use comma for more than one)  #");
		log.info("# */build=build_number (Required if /reg_results true)  #");
		log.info("# */input=xls_input_file                                #");
		log.info("# */output=xls_output_file                              #");
		log.info("# */update_tcs=[false|true]                             #");
		log.info("# */reg_results=[false|true]                            #");
		log.info("# */username=rallydev_username                          #");
		log.info("# */pwd=rallydev_password                               #");
		log.info("# */servers=service_url_list (comma separated)          #");
		log.info("# */defectID=type_defect_ID_for_reproduction            #");
		log.info("# */diff-pdf=type_the_diff-PDF_Path                     #");
		log.info("# */wait=waiting_time_in_seconds-default is 300 seconds #");
		log.info("# */regression=[false|true]                             #");
		log.info("# * - optional                                          #");
		log.info("#########################################################");
		log.info(" ");
		log.info("#########################################################");
		log.info("#                RALLYDEV REPORT HELP                   #");
		log.info("# /config=xml_config_file                               #");
		log.info("# */output=xls_output_file                              #");
		log.info("# */username=rallydev_username                          #");
		log.info("# */pwd=rallydev_password                               #");
		log.info("#########################################################");

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

		log.info("help: -help | /help");    

	}

	public void run(String[] args) throws InvalidFormatException, IOException, KeyManagementException{

		log.info("Starting ExcelRest automation execution...");

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		log.info(dateFormat.format(date));

		token=""; server=""; configFile=""; buildNumber=""; xlsOutput=""; pwd=""; username=""; updatetc=""; regtestresult=""; defectID=""; diffPDFPath=""; projectName="";

		for(int i=0; i<args.length; i++){
			if(((args[i].contains("-help"))||(args[i].contains("/help")))){
				help();
				System.exit(0);
			}
		}

		for(int i=0; i<args.length; i++){

			if(args[i].contains("/sessionId=")){
				sessionId = args[i].replace("/sessionId=", "");
			}
			if(args[i].contains("/appId=")){
				appId = args[i].replace("/appId=", "");
			}
			if(args[i].contains("/userId=")){
				userId = args[i].replace("/userId=", "");
			}
			if(args[i].contains("/userToken=")){
				userToken = args[i].replace("/userToken=", "");
			}
			if(args[i].contains("/token=")){
				token = args[i].replace("/token=", "");
			}
			if(args[i].contains("/config=")){
				configFile = args[i].replace("/config=", "");
			}
			if(args[i].contains("/build=")){
				buildNumber = args[i].replace("/build=", "");
			}
			if(args[i].contains("/update_tcs=")){
				updatetc = args[i].replace("/update_tcs=", "");
			}
			if(args[i].contains("/reg_results=")){
				regtestresult = args[i].replace("/reg_results=", "");
			}
			if(args[i].contains("/sheets=")){
				String tmp = args[i].replace("/sheets=", "");
				sheetsToRead = tmp.split(",");
			}
			if(args[i].contains("/input=")){
				String tmp = args[i].replace("/input=", "");
				xlsInput = tmp.split(",");
			}
			if(args[i].contains("/output=")){
				xlsOutput = args[i].replace("/output=", "");
			}
			if(args[i].contains("/username=")){
				username = args[i].replace("/username=", "");
			}
			if(args[i].contains("/pwd=")){
				pwd = args[i].replace("/pwd=", "");
			}
			if(args[i].contains("/project=")){
				projectName = args[i].replace("/project=", "");
			}
			if(args[i].contains("/servers=")){
				String tmp = args[i].replace("/servers=", "");
				serverList = tmp.split(",");
			}
			if(args[i].contains("/defectID=")){
				defectID = args[i].replace("/defectID=", "");
			}
			if(args[i].contains("/diff-pdf=")){
				diffPDFPath = args[i].replace("/diff-pdf=", "");
			}
			if(args[i].contains("/reducedOutput")){
				reducedOutput = Boolean.parseBoolean(args[i].replace("/reducedOutput=", ""));
			}
			if(args[i].contains("/wait=")){
				try{
					waitTime = Integer.parseInt(args[i].replace("/wait=", ""));
				}catch (Exception e) {log.info(e.getStackTrace()); log.info(e.getMessage());}
			}
			if(args[i].contains("/regression=")){
				isRegression = Boolean.parseBoolean(args[i].replace("/regression=", ""));
			}
		}

		if(!(new File(configFile).exists())){
			log.error("Cannot found config file.");
			help();
			System.exit(1);
		}

		log.trace("reading configuration from xml config file...");
		xmlConfig = new XML_Reader(configFile);
		config = xmlConfig.readConfig();

		/*Overwrite config properties if parameters are set*/
		if(serverList.length>0) {
			config.getRest().setUrl(serverList);
		}

		if(!username.isEmpty()) config.getRallyDev().setUsername(username);
		if(!pwd.isEmpty()) config.getRallyDev().setPassword(pwd);
		//if(!xlsInput.isEmpty()) config.getTestCase().setInput(xlsInput);
		if(!xlsOutput.isEmpty()) config.getTestCase().setOutput(xlsOutput);
		if(!xlsOutput.isEmpty()) config.getReport().setOutput(xlsOutput);
		if(!xlsOutput.isEmpty()) config.getDefect().setOutput(xlsOutput);
		if(!updatetc.isEmpty()) config.getRallyDev().setUpdateTestCase(Boolean.parseBoolean(updatetc));
		if(!regtestresult.isEmpty()) config.getRallyDev().setRegisterTestResult(Boolean.parseBoolean(regtestresult));
		if(!projectName.isEmpty()) config.getRallyDev().setProject(projectName);

		if((config.getTestCase().isRun())&&(buildNumber.isEmpty())&&config.getRallyDev().isRegisterTestResult()){
			log.error("Cannot identify build number. The build number is required In order to registry test results on RallyDev!");
			help();
			System.exit(1);
		}

		try{
			log.info("############################ TEST CASES CONFIGURATION ############################");
			log.info("TESTCASE_RUN: "+new Boolean(config.getTestCase().isRun()).toString());
			log.info("TESTCASE_INPUT: "+config.getTestCase().getInput());
			log.info("TESTCASE_OUTPUT: "+config.getTestCase().getOutput());
			log.info("TESTCASE_REST_URL: "+config.getRest().getUrl());
			log.info("RALLYDEV_UPDATE_TEST_CASE: "+new Boolean(config.getRallyDev().isUpdateTestCase()).toString());
			log.info("RALLYDEV_REGISTER_TEST_RESULT: "+new Boolean(config.getRallyDev().isRegisterTestResult()).toString());
			log.info("TESTCASE_POSTMAIL: "+new Boolean(config.getTestCase().isPostMail()).toString());
			log.info("TESTCASE_PROXY: "+new Boolean(config.getTestCase().isProxy()).toString());
			log.info("#################################################################################\n");

			log.info("######################### DEFECT REPORT CONFIGURATION ###########################");
			log.info("DEFECT_RUN: "+new Boolean(config.getDefect().isRun()).toString());
			log.info("DEFECT_OUTPUT"+config.getDefect().getOutput());
			String stateFilter[] = config.getDefect().getStateFilter();
			for(int i=0; i<stateFilter.length; i++) log.info("DEFECT_STATE: "+stateFilter[i]);
			log.info("DEFECT_POSTMAIL: "+new Boolean(config.getDefect().isPostMail()).toString());
			log.info("#################################################################################\n");

			log.info("############################  REPORT CONFIGURATION ##############################");
			log.info("REPORT_RUN: "+new Boolean(config.getReport().isRun()).toString());
			log.info(("REPORT_TYPE: "+config.getReport().getType()));		
			log.info(("REPORT_OUTPUT: "+config.getReport().getOutput()));
			String fields[] = config.getReport().getFields();
			for(int i=0; i<fields.length; i++) log.info("REPORT_FIELD: "+fields[i]);
			log.info("REPORT_POSTMAIL: "+new Boolean(config.getReport().isPostMail()).toString());
			log.info("#################################################################################\n");

			log.info("###########################  RALLYDEV CONFIGURATION #############################");
			log.info("RALLYDEV_URL: "+config.getRallyDev().getUrl());
			log.info("RALLYDEV_USERNAME: "+config.getRallyDev().getUsername());
			log.info("RALLYDEV_PASSWORD: "+config.getRallyDev().getPassword());
			log.info("RALLYDEV_PROJECT: "+config.getRallyDev().getProject());
			log.info("RALLYDEV_PROXY: "+new Boolean(config.getRallyDev().isProxy()).toString());
			log.info("#################################################################################\n");

			log.info("############################# PROXY CONFIGURATION ###############################");
			log.info("PROXY_HOST: "+config.getProxy().getHost());
			log.info("PROXY_PORT: "+config.getProxy().getPort());
			log.info("#################################################################################\n");

			log.info("############################# EMAIL CONFIGURATION ###############################");
			log.info("EMAIL_SMTP: "+config.getEmail().getSmtpName());
			log.info("EMAIL_MSG: "+config.getEmail().getMsgTxt());
			log.info("EMAIL_SUBJECT: "+config.getEmail().getSubjectTxt());
			log.info("EMAIL_FROM_ADDRESS: "+config.getEmail().getFromAddress());
			String toAddress[] = config.getEmail().getToAddressList();
			for(int i=0; i<toAddress.length; i++) log.info("EMAIL_TO_ADDRESS: "+toAddress[i]);
			log.info("#################################################################################\n");
		}
		catch (Exception e) {
			// TODO: handle exception
		}

		/************************** Delete all test cases from a project *************************/  		

		//String [] formattedIDList = {};
		//startRallyDevConnection();
		//DO NOT ENABLE THIS UNLESS YOU ARE SURE ABOUT WHAT YOU ARE DOING: rallyconn.deleteAllTestCases(projectId);
		//rallyconn.deleteTCbyFormattedId(projectId, formattedIDList);
		//stopRallyDevConnection();
		//System.exit(0);
		/*****************************************************************************************/

		/************************************* Run test cases *************************************/
		if(config.getTestCase().isRun()){		
			log.info("executing test cases...");

			log.info("reading test cases from spreadsheet...");

			boolean sheetFound;
			LinkedList<TestCase> newtcs = new LinkedList<TestCase>();
			for(int i=0; i<sheetsToRead.length; i++){
				sheetFound=false;
				for(int j=0; j<xlsInput.length; j++){

					//ignore if the sheet name came blank (this helps in the bash script to call the framework)
					if(sheetsToRead[i].equalsIgnoreCase("")) sheetFound=true; 
					else {
						newtcs = poi.readTestCases(config, xlsInput[j], sheetsToRead[i], token, sessionId, appId, userId, userToken);
					}
					if(newtcs.size() > 0){
						sheetFound=true;
						Spreadsheet ss = new Spreadsheet();
						ss.setTcs(newtcs);
						ss.setFileLocation(xlsInput[j]);
						spreadsheets.add(ss);
						tcs.addAll(newtcs);
					}
				}

				if(!sheetFound){
					log.error("Sheet not found: "+sheetsToRead[i]);
					System.exit(1);
				}
			}

			log.info("All sheets were found!");

			if(tcs.isEmpty()){
				log.error("TestCase list is empty, please check the input file...");
				System.exit(1);
			}

			conn = new Connections(config);

			assertions.setConn(conn);

			executeQueueTestCases();

			executeRestTestCases();

			/** Full output - full information about the whole test execution **/
			log.info("writing test case results...");
			jxl.writeTestResults(config.getTestCase().getOutput(), tcs);

			/** Failure output - only the failures and test cases related with those failures are displayed**/
			String fileLocation = config.getTestCase().getOutput().replaceAll(".xls", "-failures.xls");
			jxl.writeTestFailures(fileLocation, tcs, identifyTestFailuresandDependecies(tcs));

			/** the spreadsheet with these failures is available also on HTML format**/
			String htmlLocation = fileLocation.replaceAll(".xls", ".html");
			new ExcelToHtml(new FileInputStream(new File(fileLocation))).getHTML(htmlLocation);	

			/** Reduced output - all test cases are displayed with characters limitation on some fields**/
			if(reducedOutput) {
				fileLocation = config.getTestCase().getOutput().replaceAll(".xls", "-reduced.xls");
				jxl.writeTestResultsReduced(fileLocation, tcs);
			}


			if(config.getRallyDev().isUpdateTestCase()){
				log.info("starting rallydev connection...");
				startRallyDevConnection();

				if(config.getRallyDev().isUpdateTestCase()){
					log.info("updating rallydev testcases...");
					updateRallyDevTestCases();
					poi.updateTestCases(spreadsheets);
					log.info("rallydev testcases updated...");

					if(config.getRallyDev().isRegisterTestResult()){
						log.info("registering new rallydev test results...");	
						registerRallyDevTestResult();
						log.info("new rallydev test results registered...");
					}
				}
				log.info("stoping rallydev connection...");
				stopRallyDevConnection();

				/** used to register the result of each test case in the test cases spreadsheet */
			} else poi.updateTestCases(spreadsheets);


			if(config.getTestCase().isPostMail()){
				log.info("sending email...");
				postmail.sendAttachment(config.getEmail(), config.getTestCase().getOutput());
				log.info("email sent...");
			}
			log.info("Finishing test cases execution...");
		}
		/******************************************************************************************/


		/*********************************** Run defect report ************************************/
		if(config.getDefect().isRun()){
			log.info("starting rallydev connection...");
			startRallyDevConnection();

			log.info("executing defect report...");
			executeDefectReport();

			log.info("stoping rallydev connection...");
			stopRallyDevConnection();

			if(config.getDefect().isPostMail()){
				log.info("sending email...");
				postmail.sendAttachment(config.getEmail(), config.getDefect().getOutput());
				log.info("email sent...");
			}

		}
		/******************************************************************************************/

		/*********************************** Run report ************************************/
		if(config.getReport().isRun()){
			//generates a xls file with the report requested 
			log.info("starting rallydev connection...");
			startRallyDevConnection();

			log.info("executing report...");
			executeReport();

			log.info("stoping rallydev connection...");
			stopRallyDevConnection();

			if(config.getReport().isPostMail()){
				log.info("sending email...");
				postmail.sendAttachment(config.getEmail(), config.getReport().getOutput());
				log.info("email sent...");
			}

		}

		/******************************************************************************************/


		/******************************** Searching for failures **********************************/
		if(tcs!=null){
			Iterator itTC = tcs.iterator();
			while(itTC.hasNext()){
				TestCase tc = (TestCase) itTC.next();
				if((!tc.isResult_success())&&(tc.isExecutable())) {
					fail = true;
					log.error("FAIL! TC_ID: "+tc.getId()+" - "+tc.getName());				
				}
			}
		}
		/******************************************************************************************/

		if(fail){ 
			if(config.getTestCase().isRun()) log.info("FAIL! Some failure were found.");
			log.info("Finishing ExcelRest automation execution...");
			System.exit(1);
		} else { 
			if(config.getTestCase().isRun()) log.info("PASS! No failures were found.");
			log.info("Finishing ExcelRest automation execution...");
			System.exit(0);
		}
	}



	public void startRallyDevConnection(){

		if(config.getRallyDev().isProxy()){
			if(config.getProxy().getHost().contains("http://")) proxy = config.getProxy().getHost()+":"+config.getProxy().getPort();
			else proxy = "http://"+config.getProxy().getHost()+":"+config.getProxy().getPort();
		} else proxy = "";

		rallyconn.start(config.getRallyDev().getUrl(), config.getRallyDev().getUsername(), config.getRallyDev().getPassword(), proxy);

		rallyUserId = rallyconn.getUserId(config.getRallyDev().getUsername());
		userRef = rallyconn.getUserRef(config.getRallyDev().getUsername());
		log.info("Username: "+config.getRallyDev().getUsername()+", UserId: "+rallyUserId+", UserRef: "+userRef);

		projectId = rallyconn.getProjectId(config.getRallyDev().getProject());
		log.info("Project name: "+config.getRallyDev().getProject()+", ProjectId: "+projectId);
	}

	public void stopRallyDevConnection(){
		rallyconn.stop();
	}

	public void updateRallyDevTestCases(){
		tcs = rallyconn.updateTestCases(tcs, rallyUserId, projectId);
	}

	public void registerRallyDevTestResult(){
		rallyconn.newTesCaseResult(tcs, projectId, userRef, buildNumber);
	}

	public void executeDefectReport(){
		jxl.writeDefectReport(config, projectId, rallyconn.queryDefects(projectId, config.getDefect().getStateFilter()));
	}

	public void executeReport() throws InvalidFormatException, IOException{

		LinkedList<Iteration> itList = new LinkedList<Iteration>();
		if(!config.getReport().getSheet().contains("iterations")){
			itList = poi.readIterationsSheet(config.getReport().getOutput());
		}

		File f = new File(config.getReport().getOutput());

		if(f.exists())	poi.updateReport(config, projectId, rallyconn.query(projectId, config.getReport()),itList);
		else poi.createReport(config, projectId, rallyconn.query(projectId, config.getReport()));
	}

	public void executeRestTestCases() {

		log.info("Starting rest test cases execution.");
		int restListPos=0;

		Iterator itcs = tcs.iterator();
		while(itcs.hasNext()) {

			TestCase tc = (TestCase) itcs.next();
			boolean isSpecialCase=false;

			if(tc.getTestType().equalsIgnoreCase("REST")){

				log.info("Test Case: "+tc.getId());

				/*if defectID is not empty, reproducing defect (execute only the test cases with the TC Ids in the TC_ID or, TC_Name)
				 * When the option /defectID is informed in the command line the only tests that will run is the ones with the defectID  in the TC_ID or, TC_Name 
				 */
				if(!defectID.isEmpty()) {
					if((tc.getId().contains(defectID))||(tc.getName().contains(defectID))||tc.getDefect_number().contains(defectID)) tc.setExecutable(true); 
					else tc.setExecutable(false);
				}

				/*if executing regression tests, the tests containing registered defects should not run. That is, with the option isRegression=true only the test
				cases without defectID in the defect_number column should run
				 */
				if((isRegression)&&(!tc.getDefect_number().isEmpty())){
					tc.setExecutable(false);
				}

				//iterate into the assertions
				if(tc.isExecutable()){

					tc.setResult_date(utils.dateToXsdDatetimeFormatter());

					/* ******************  checking if is an special case *************************/
					Iterator itAssertion = tc.getAssertions().iterator();
					while(itAssertion.hasNext()){
						Assertions assertion = (Assertions) itAssertion.next();

						if(assertion.getType().equalsIgnoreCase("Special.Case")){
							isSpecialCase=true;
						}
					}
					/* ***************************************************************************/

					/* ******************  checking if is an special case *************************/
					Iterator itVariable = tc.getVars().iterator();
					while(itVariable.hasNext()){
						Variable variable = (Variable) itVariable.next();
						if(!variable.getType().equalsIgnoreCase("local_var")){
							checkGlobalVariables(tc, variable);
						}
					}
					/* ***************************************************************************/

					if(!isSpecialCase){

						log.info("executing...");
						log.debug("executing rest test case: ");
						log.debug("rallyDevId: "+tc.getRallyDevId());
						log.debug("tc_id: "+tc.getId());
						log.debug("url_path: "+config.getRest().getUrl()+tc.getResttc().getPath());
						log.debug("method: "+tc.getResttc().getMethod());
						log.debug("body: "+tc.getResttc().getBody());
						log.debug("headers: "+tc.getResttc().getHeaders());

						//tc.setFull_path(config.getRest().getUrl()+tc.getResttc().getPath());

						if(tc.getResttc().getMethod().equalsIgnoreCase("POST")) {
							if(tc.getResttc().getBody().startsWith("file=")) { 
								tcs.set(restListPos, conn.postFile(tc));							
							}
							else if(tc.getResttc().getBody().startsWith("XML=")) { 
								tcs.set(restListPos, conn.postMultipleFiles(tc));
							}
							else {

								/* THE FTP IS NOT BEING BLOCKED ANTMORE 
								 * used to avoid ftp funne blocking
								if(tc.getResttc().getPath().contains("executions")){

									log.info("Waiting "+waitTime+" seconds in order to avoid the FTP funne blocking.");

									TimeController workaroundTime = new TimeController();
									workaroundTime.startTime();

									while(workaroundTime.getElapsedTime() < waitTime){
										workaroundTime.waitFor(1000); 
										System.out.print(".");
									}
								}*/
								tcs.set(restListPos, conn.simplePost(tc));

								/*/KLUDGE: retry post in case of failures (workaround to avoid some intermittent AAA errors)
								tc = assertions.checkTestResult(tc, diffPDFPath);
								if(!tc.isResult_success()) {
									TimeController ExcelRestTime = new TimeController();
									ExcelRestTime.startTime();
									ExcelRestTime.waitFor(1000);
									log.error("ERROR: Retry POST to avoid environment problems.");
									tcs.set(restListPos, conn.simplePost(tc));
								}*/
							}
						} else {
							if(tc.getResttc().getMethod().equalsIgnoreCase("GET")){
								log.info("Checking body content.. an file should be downloaded if started with 'save_file='. Body: "+ tc.getResttc().getBody());
								if(tc.getResttc().getBody().startsWith("save_file=")){
									String fileLocation = tc.getResttc().getBody().replace("save_file=", "");
									log.info("Downloading file at: "+tc.getResttc().getBody());
									tcs.set(restListPos, conn.get(tc, fileLocation));

								} else{
									log.info("Executing simple GET...");
									tcs.set(restListPos, conn.get(tc, ""));
								}
							} else {
								if(tc.getResttc().getMethod().equalsIgnoreCase("PUT")) {

									if(tc.getResttc().getBody().startsWith("file=")) { 
										//tcs.set(restListPos, conn.postFile(tc));							
									} else{
										tcs.set(restListPos, conn.simplePut(tc));
									}
								} else {
									if(tc.getResttc().getMethod().equalsIgnoreCase("GET_KRAKEN")) {
										log.info("Executing GET_KRAKEN...");
										tcs.set(restListPos, conn.postKraken(tc));
									}
									else {
										if((tc.getResttc().getMethod().equalsIgnoreCase("DELETE"))&&(tc.getResttc().getBody().isEmpty())) {
											log.info("Executing DELETE...");
											tcs.set(restListPos, conn.delete(tc));
										}

										else {
											if(tc.getResttc().getMethod().equalsIgnoreCase("PATCH")) {
												log.info("Executing PATCH...");
												tcs.set(restListPos, conn.simplePatch(tc));
											} else{
												if((tc.getResttc().getMethod().equalsIgnoreCase("DELETE"))&&(!tc.getResttc().getBody().isEmpty())) {
													log.info("Executing DELETE_WITH_BODY...");
													tcs.set(restListPos, conn.deleteWBody(tc));
												} else{
													if(tc.getResttc().getMethod().equalsIgnoreCase("POSTMULTI")) {
														log.info("Executing MULTIPART...");
														tcs.set(restListPos, conn.postMultipartJson(tc));
													} else if(tc.getResttc().getMethod().contains("GET_WAIT")) {

														int time = 5;
														if(tc.getResttc().getMethod().contains("=")){
															String values[] = tc.getResttc().getMethod().toString().split("=");
															if(values.length==2){
																time = Integer.parseInt(values[1].toString());
															}
														}

														log.info("starting GET_WAIT...Timeout: "+time+" seconds");
														TimeController ExcelRestTime = new TimeController();
														ExcelRestTime.startTime();

														boolean tcsuccess=true;

														while((!tc.isResult_success()) && (ExcelRestTime.getElapsedTime()<time) && (tcsuccess)){
															tc = conn.get(tc, "");

															tc = assertions.checkTestResult(tc, diffPDFPath);
															ExcelRestTime.waitFor(1000); 
															log.info("Timeout: "+time);
															log.info("Elapsed Time: "+ExcelRestTime.getElapsedTime());

															//if the status is 'not found' or 'ok' continue iterating
															if((tc.getResult_status()!=404)&&(tc.getResult_status()!=200)) tcsuccess = false;
														}
														tcs.set(restListPos, tc);
													} else{
														if(tc.getResttc().getMethod().equalsIgnoreCase("OPTIONS")) {
															log.info("Executing OPTIONS...");
															tcs.set(restListPos, conn.simpleOptions(tc));
														} else {
															if(!tc.getResttc().getMethod().contains("none")){
																log.info("METHOD NOT FOUND!");
																System.exit(1);
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}

						log.info("checking test case result...");
						tc = assertions.checkTestResult(tc, diffPDFPath);
						tcs.set(restListPos, tc);

					} else {
						log.info("Assertion Special Case...");
						tc = assertions.checkTestResult(tc, diffPDFPath);
					}

					log.info("Set "+tc.getId()+" as executed!");
					tc.setExecuted(true);
				} else log.info("tc_execute: false");
			}
			tcs.set(restListPos, tc);
			restListPos++;
		}
		log.info("Finishing rest test cases execution.");
	}

	public TestCase checkGlobalVariables(TestCase tc, Variable var){


		log.info("Checking global variables.");
		String response = "", id = "";

		ListIterator<TestCase> itcs = tcs.listIterator(tcs.size()-1);

		try{
			if(var.getType().contains("jsonkey")){

				while((itcs.hasPrevious())&(response.isEmpty())) {
					TestCase tc2 = (TestCase) itcs.previous();
					if((var.getContent().equalsIgnoreCase(tc2.getId()))&(tc2.isExecuted())){
						response = tc2.getResponse();
						id = tc2.getId();
						log.debug("tc_id: "+tc2.getId()+" response: "+response);
					}
				}
				String jsonvalue = "";
				if((response != null)&&(response != "")){
					String values[] = var.getName().split(",");
					int listPosition = -1;
					try {
						if(values.length > 1) 
							listPosition = Integer.parseInt(values[1]);	
					} catch (Exception e) {
						log.error(e.getMessage());
						log.error("Unable to convert jsonkey listPositon!");
					} 

					jsonvalue = utils.jsonKeyFinder(response, values[0],listPosition);
					log.info("keyfinder: "+"Id: "+id+"Name: "+var.getName()+"Value: "+jsonvalue);
				} else {
					jsonvalue = "No jsonkey was found! content: "+var.getContent()+" name: "+var.getName()+" type: "+var.getType();
				}

				String path = tc.getResttc().getPath();
				path = path.replace(var.getType(), jsonvalue);
				tc.getResttc().setPath(path);
				log.debug("Path: "+path);

				String description = tc.getDescription();
				description = description.replace(var.getType(), jsonvalue);
				tc.setDescription(description);
				log.debug("Description: "+description);

				String name = tc.getName();
				name = name.replace(var.getType(), jsonvalue);
				tc.setName(name);
				log.debug("Name: "+name);

				String body = tc.getResttc().getBody();
				body = body.replace(var.getType(), jsonvalue);
				tc.getResttc().setBody(body);
				log.debug("Body: "+body);

				String headers = tc.getResttc().getHeaders();
				headers = headers.replace(var.getType(), jsonvalue);
				tc.getResttc().setHeaders(headers);
				log.debug("Headers: "+headers);

				for(int i=0; i < tc.getAssertions().size();i++){
					String content = tc.getAssertions().get(i).getContent();
					content = content.replace(var.getType(), jsonvalue);
					tc.getAssertions().get(i).setContent(content);
					log.debug("Type: "+tc.getAssertions().get(i).getType());
					log.debug("Content: "+tc.getAssertions().get(i).getContent());
				}

				/* ****************** replace in the assertion *************************/
				for(int i=0; i<tc.getAssertions().size(); i++){
					tc.getAssertions().get(i).setContent(tc.getAssertions().get(i).getContent().replace(var.getType(), jsonvalue));
					log.info(var.getType()+" = "+tc.getAssertions().get(i).getContent());
				}

			}
		}catch (Exception e) {
			log.error(e.getMessage());
			log.error("FAIL! Unable to check jsonkey!");
		}


		try{
			if(var.getType().contains("xmlelement")){

				while((itcs.hasPrevious())&(response.isEmpty())) {
					TestCase tc2 = (TestCase) itcs.previous();

					if((var.getContent().equalsIgnoreCase(tc2.getId()))&(tc2.isExecuted())){
						response = tc2.getResponse();
						id = tc2.getId();
						log.debug("tc_id: "+tc2.getId()+" response: "+response);
					}
				}
				String xmlelement = "";
				if((response != null)&&(response != "")){
					xmlelement = utils.xmlElementFinder(response, var.getName());
					log.info("xmlElementfinder: "+"Id: "+id+"Name: "+var.getName()+"Value: "+xmlelement);
				} else {
					xmlelement = "No xml element was found! content: "+var.getContent()+" name: "+var.getName()+" type: "+var.getType();
				}

				String path = tc.getResttc().getPath();
				path = path.replace(var.getType(), xmlelement);
				tc.getResttc().setPath(path);
				log.debug("Path: "+path);

				String body = tc.getResttc().getBody();
				body = body.replace(var.getType(), xmlelement);
				tc.getResttc().setBody(body);
				log.debug("Body: "+body);

				String headers = tc.getResttc().getHeaders();
				headers = headers.replace(var.getType(), xmlelement);
				tc.getResttc().setHeaders(headers);
				log.debug("Headers: "+headers);

			}
		}catch (Exception e) {
			log.error(e.getMessage());
			log.error("FAIL! Unable to check xmlelement!");
		}

		//this method tries to found a string between two strings
		try{
			if(var.getType().contains("stringBetween")){

				while((itcs.hasPrevious())&(response.isEmpty())) {
					TestCase tc2 = (TestCase) itcs.previous();

					if((var.getContent().equalsIgnoreCase(tc2.getId()))&(tc2.isExecuted())){
						response = tc2.getResponse();
						id = tc2.getId();
						log.debug("tc_id: "+tc2.getId()+" response: "+response);
					}
				}
				String stringBetween = "";
				if((response != null)&&(response != "")){
					String values[] = var.getName().split(",");
					stringBetween = utils.subStringBetween(response, values[0], values[1]);

					log.info("stringBetween: "+"Id: "+id+"Name: "+var.getName()+"Value: "+stringBetween);
				} else {
					stringBetween = "Trying to found string between: content: "+var.getContent()+" name: "+var.getName()+" type: "+var.getType();
				}

				String path = tc.getResttc().getPath();
				path = path.replace(var.getType(), stringBetween);
				tc.getResttc().setPath(path);
				log.debug("Path: "+path);

				String description = tc.getDescription();
				description = description.replace(var.getType(), stringBetween);
				tc.setDescription(description);
				log.debug("Description: "+description);

				String body = tc.getResttc().getBody();
				body = body.replace(var.getType(), stringBetween);
				tc.getResttc().setBody(body);
				log.debug("Body: "+body);

				String headers = tc.getResttc().getHeaders();
				headers = headers.replace(var.getType(), stringBetween);
				tc.getResttc().setHeaders(headers);
				log.debug("Headers: "+headers);

			}
		}catch (Exception e) {
			log.error(e.getMessage());
			log.error("FAIL! Unable to check xmlelement!");
		}

		try{
			if(var.getType().equalsIgnoreCase("response")){

				while((itcs.hasPrevious())&(response.isEmpty())) {
					TestCase tc2 = (TestCase) itcs.previous();

					if((var.getContent().equalsIgnoreCase(tc2.getId()))&(tc2.isExecuted())){
						response = tc2.getResponse();
						log.debug("tc_id: "+tc2.getId()+" response: "+response);
					}
				}

				String path = tc.getResttc().getPath();
				path = path.replace(var.getName(), response);
				tc.getResttc().setPath(path);
				log.debug("Path: "+path);

				String body = tc.getResttc().getBody();
				body = body.replace(var.getName(), response);
				tc.getResttc().setBody(body);
				log.debug("Body: "+body);

				String headers = tc.getResttc().getHeaders();
				headers = headers.replace(var.getName(), response);
				tc.getResttc().setHeaders(headers);
				log.debug("Headers: "+headers);
			}
		}catch (Exception e) {
			log.error(e.getMessage());
			log.error("FAIL! Unable to replace response!");
		}


		try{
			if(var.getType().equalsIgnoreCase("header")){

				Header[] headers = null;

				while((itcs.hasPrevious())&(headers == null)) {
					TestCase tc2 = (TestCase) itcs.previous();
					if((var.getContent().equalsIgnoreCase(tc2.getId()))&(tc2.isExecuted())){
						headers = tc2.getResponseHeaders();
						log.info("tc_id: "+tc2.getId());
					}
				}

				String headerParamValue = "";
				if(headers!=null){
					for (Header h : headers) {
						log.debug(h.getName()+": "+h.getValue()+"\n");
						if(h.getName().equalsIgnoreCase(var.getName())){
							headerParamValue = h.getValue();
						}
					}

					String path = tc.getResttc().getPath();
					path = path.replace("header", headerParamValue);
					tc.getResttc().setPath(path);
					log.info("Path: "+path);

					String body = tc.getResttc().getBody();
					body = body.replace("header", headerParamValue);
					tc.getResttc().setBody(body);
					log.info("Body: "+body);

					String hdrs = tc.getResttc().getHeaders();
					hdrs = hdrs.replace("header", headerParamValue);
					tc.getResttc().setHeaders(hdrs);
					log.info("Headers: "+hdrs);
				}
			}
		}catch (Exception e) {
			log.error(e.getMessage());
			log.error("FAIL! Unable to check header!");
		}

		/*String hd = "";
		Header[] headers = method.getResponseHeaders();
		for (Header h : headers) {
			hd += h.getName()+": "+h.getValue()+"\n";
		}*/



		return tc;
	}


	public void executeQueueTestCases() {

		if(!config.getQueue().getHost().isEmpty()) {

			log.info("Starting queue test cases execution.");

			int queueListPos=0;

			log.info("purge Queues before start the execution...");
			conn.queuePurge(tcs.get(0).getQueuetc().getQueue_to_publish());
			conn.queuePurge(tcs.get(0).getQueuetc().getQueue_to_consume());

			Iterator itcs = tcs.iterator();
			while(itcs.hasNext()) {

				TestCase tc = (TestCase) itcs.next();
				boolean isSpecialCase=false;

				if(tc.getTestType().equalsIgnoreCase("QUEUE")){

					log.info("Test Case: "+tc.getId());
					//iterate into the assertions
					if(tc.isExecutable()){
						tc.setResult_date(utils.dateToXsdDatetimeFormatter());

						/* ******************  checking if is an special case *************************/
						Iterator itAssertion = tc.getAssertions().iterator();
						while(itAssertion.hasNext()){
							Assertions assertion = (Assertions) itAssertion.next();

							if(assertion.getType().equalsIgnoreCase("Special.Case")){
								isSpecialCase=true;
							}
						}
						/* ***************************************************************************/

						if(!isSpecialCase){

							log.info("executing...");
							log.info("starting timer...");

							//while publish queue is not empty and timeout was not achieved
							TimeController ExcelRestTime = new TimeController();
							ExcelRestTime.startTime();

							tc.setFull_path("");

							log.info("publishing message...");
							conn.publishMessage(tc);

							log.info("wait until ExcelRest proccess the message..Timeout: 90 seconds");
							while(((conn.getNumberOfMessages(tc.getQueuetc().getQueue_to_publish())>0)
									||(conn.getNumberOfMessages(tc.getQueuetc().getQueue_to_consume())>1))
									&&(ExcelRestTime.getElapsedTime()<90)){
								ExcelRestTime.waitFor(5000); 
								log.info(".");
							}

							if(ExcelRestTime.getElapsedTime()<90) {
								log.info("proccessing time: "+ExcelRestTime.getElapsedTime());

								log.info("consuming message...");
								tc = conn.consumeMessage(tc);	

								log.info("checking test case result...");
								tc = assertions.checkTestResult(tc, diffPDFPath);

								tcs.set(queueListPos, tc);
							} else {
								log.info("Timeout processing message: "+ExcelRestTime.getElapsedTime());
								tc.setResult_description("Timeout processing message: "+ExcelRestTime.getElapsedTime()+" seconds. Check the number of messages in the queues.");
								tc.setResult_success(false);
							}
						} else {
							log.info("Assertion Special Case...");
							tc = assertions.checkTestResult(tc, diffPDFPath);
						}
					} else log.info("tc_execute: false");
				}
				tcs.set(queueListPos, tc);
				queueListPos++;
			}
			log.info("Finishing queue test cases execution.");

		} else {
			log.error("Queue host is empty! Do not execute queue tests!");
		}
	}

	public LinkedList<String> identifyTestFailuresandDependecies(LinkedList<TestCase> tcs) {

		LinkedList<String> tcsToWrite = new LinkedList<String>();

		for(int row=0; row<tcs.size(); row++) {
			if((!tcs.get(row).isResult_success())&(tcs.get(row).isExecutable())) {
				
				if(!tcsToWrite.contains(tcs.get(row).getId())) tcsToWrite.add(tcs.get(row).getId());

				if(tcs.get(row).getVars()!=null)
					for (Variable v : tcs.get(row).getVars()) {
						if(!tcsToWrite.contains(v.getContent())){
							tcsToWrite.add(v.getContent());	
						}
					} 
			}
		}
		log.info("Total of test case failures and dependencies: "+ tcsToWrite.size());
		return tcsToWrite;
	}


	/*public void executeQueueTestCases() {

		log.info("Starting queue test cases execution.");

		log.info("reading test cases from spreadsheet...");
		tcs = jxl.readTestCases(config.getTestCase().getInput(), "queue_tests");

		//all methods to connect with the queue should be in the Connections class
		Connections conn = new Connections(config, tcs);

		log.info("purge Queues before start the execution...");
		conn.queuePurge(tcs.get(0).getQueuetc().getQueue_to_publish());
		conn.queuePurge(tcs.get(0).getQueuetc().getQueue_to_consume());

		log.info("starting timer...");
		TimeController s = new TimeController();
		s.startTime();

		log.info("publishing messages...");
		conn.publishMessage();

		//while publish queue is not empty and timeout was not achieved
		TimeController ExcelRestTime = new TimeController();
		ExcelRestTime.startTime();
		log.info("wait until ExcelRest proccess all the messages..Timeout: 90 seconds");
		while((conn.getNumberOfMessages(tcs.getFirst().getQueuetc().getQueue_to_publish())>0)&&(ExcelRestTime.getElapsedTime()<90)){
			ExcelRestTime.waitFor(5000);
			log.info(".");
		}
		log.debug("proccessing time: "+ExcelRestTime.getElapsedTime());

		log.info("consuming messages...");
		tcs = conn.consumeMessage();

		log.info("checking test case results...");
		Assertions assertions = new Assertions();
		tcs = assertions.checkTestResults(tcs, conn);

		log.info("writing test case results...");
		jxl.writeTestResults(config.getTestCase().getOutput(), tcs);

		log.info("Finishing queue test cases execution.");
	}*/
	public static void main(String[] args) throws InvalidFormatException, IOException, KeyManagementException {

		Main main = new Main();
		//main.executeQueueTestCases("./config/config.xml", "./queueTests.xls", "./results.xls");
		main.run(args);
	}
}

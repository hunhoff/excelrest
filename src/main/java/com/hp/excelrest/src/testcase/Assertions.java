package com.hp.excelrest.src.testcase;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.hp.excelrest.src.tools.Connections;
import com.hp.excelrest.src.tools.Utils;

public class Assertions {

	static Logger log = Logger.getLogger(Assertions.class);
	private String type;
	private String content;
	private Connections conn;
	private String result_description;
	private String resultDescriptionFailures;
	private int check_result;
	private String downloadLocation;

	public Assertions(){}

	public Assertions(String type, String content) {
		super();
		this.type = type;
		this.content = content;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public Connections getConn() {
		return conn;
	}
	public void setConn(Connections conn) {
		this.conn = conn;
	}

	public TestCase checkTestResult(TestCase tc, String diffPDFPath) {

		log.info("tc_id: "+tc.getId());
		log.info("tc_name: "+tc.getName());
		log.info("tc_execute: "+new Boolean(tc.isExecutable()).toString());

		Utils utils = new Utils();

		result_description = "";
		resultDescriptionFailures = "";
		check_result = 0;

		if(tc.getAssertions().size()==0){
			check_result = 1;
			result_description = "No assertion! Do not check the result!";
		}

		Iterator itAssertion = tc.getAssertions().iterator();
		while(itAssertion.hasNext()){
			Assertions assertion = (Assertions) itAssertion.next();
			log.debug("tc_assertion: type="+assertion.getType()+", content="+assertion.getContent());

			if(assertion.getType().equalsIgnoreCase("String.Contains")){
				log.info("checking if the response contains the assertion content...");
				log.debug("response: "+tc.getResponse());

				if(tc.getResponse().contains(assertion.getContent())){
					log.info("PASS - String.Contains: "+assertion.getContent());
					result_description = result_description + "\nPASS - String.Contains: "+assertion.getContent();
					if(check_result!=2) check_result = 1;
				} 
				else { 
					log.info("FAIL - String.Contains: "+assertion.getContent());
					result_description = result_description + "\nFAIL - String.Contains: "+assertion.getContent();
					resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - String.Contains: "+assertion.getContent();
					check_result = 2;
				}
			}

			if(assertion.getType().equalsIgnoreCase("String.notContains")){
				log.info("checking if the response not contains the assertion content...");
				log.debug("response: "+tc.getResponse());

				if(!tc.getResponse().contains(assertion.getContent())){
					log.info("PASS - String.notContains: "+assertion.getContent());
					result_description = result_description + "\nPASS - String.notContains: "+assertion.getContent();
					if(check_result!=2) check_result = 1;
				} 
				else { 
					log.info("FAIL - String.notContains: "+assertion.getContent());
					result_description = result_description + "\nFAIL - String.notContains: "+assertion.getContent();
					resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - String.notContains: "+assertion.getContent();
					check_result = 2;
				}
			}

			if(assertion.getType().equalsIgnoreCase("status")){

				log.info("checking response status...");
				log.debug("response: "+tc.getResult_status());

				if(tc.getResult_status()==Integer.parseInt(assertion.getContent().trim())){
					log.info("PASS - Status found: "+assertion.getContent());
					result_description = result_description + "\nPASS - Status found: "+assertion.getContent();
					if(check_result!=2) check_result = 1;
				} 
				else { 
					log.info("FAIL - Status NOT found: "+assertion.getContent());
					check_result = 2;
					result_description = result_description + "\nFAIL - Status NOT found: "+assertion.getContent();
					resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - Status NOT found: "+assertion.getContent();
				}
			}

			if(assertion.getType().equalsIgnoreCase("file.exists")){

				log.info("checking if the following file exists: "+assertion.getContent());
				File file = new File(assertion.getContent());

				if(file.exists()){
					log.info("PASS: the file exists.");
					result_description = result_description + "File.Exists: The following file was correctly identified: "+assertion.getContent();
					if(check_result!=2) check_result = 1;

					/*removing the file in order to avoid inconsistent results in other test cases
					boolean success = file.delete();
					if (!success) {
						result_description = result_description + " Error removing file: "+assertion.getContent();
						resultDescriptionFailures = resultDescriptionFailures + " Error removing file: "+assertion.getContent();
						log.warn("Error removing file: "+assertion.getContent());
					} else {
						log.warn("The file was removed in order to avoid inconsistent results.");
						result_description = result_description + " The file was removed in order to avoid inconsistent results";
						resultDescriptionFailures = resultDescriptionFailures + " The file was removed in order to avoid inconsistent results";
					}*/
				} 
				else { 
					log.info("Fail: the file was NOT found.");
					check_result = 2;
					result_description = result_description + "File.Exists: The following file was NOT found: "+assertion.getContent();
					resultDescriptionFailures = resultDescriptionFailures + "File.Exists: The following file was NOT found: "+assertion.getContent();
				}
			}

			if(assertion.getType().equalsIgnoreCase("Special.Case")){

				if(assertion.getContent().equalsIgnoreCase("isEmpty")){
					log.info("checking if the queues are empty...");
					int pubQueueMessages, consQueueMessages;
					pubQueueMessages = conn.getNumberOfMessages(tc.getQueuetc().getQueue_to_publish());
					consQueueMessages = conn.getNumberOfMessages(tc.getQueuetc().getQueue_to_consume());

					if((pubQueueMessages==0)&&(consQueueMessages==0)){
						log.info("Special.Cases: isEmpty - The queues are empty.");
						result_description = result_description + "Special.Cases: isEmpty - The queues are empty.";
						check_result = 1;
					} else if((pubQueueMessages>0)&&(consQueueMessages>0)){
						log.info("Special.Cases: isEmpty - The queues are NOT empty!");
						result_description = result_description + "Special.Cases: isEmpty - The queues are NOT empty!";
					} else if(pubQueueMessages>0){
						log.info("Special.Cases: isEmpty - The queue to publish is NOT empty!");
						result_description = result_description + "Special.Cases: isEmpty - The queue to publish is NOT empty!";
					} else if(consQueueMessages>0){
						log.info("Special.Cases: isEmpty - The queue to consume is NOT empty!");
						result_description = result_description + "Special.Cases: isEmpty - The queue to consume is NOT empty!";

					}
				}
			}

			if(assertion.getType().equalsIgnoreCase("setResult")){

				log.info("Setting the result with the description information...");
				result_description = tc.getDescription();
				if(check_result!=2) check_result = 1;
			}

			if(assertion.getType().equalsIgnoreCase("setSuccess")){

				log.info("FAIL or PASS the test case - FALSE or TRUE");

				if(assertion.getContent().contains("true")){
					log.info("PASS - setSuccess=true");
					check_result = 1;
					result_description = result_description + "\nPASS - setSuccess=true";
				} 

				if(assertion.getContent().contains("false")){
					log.info("FAIL - setSuccess=false");
					check_result = 2;
					result_description = result_description + "\nFAIL - setSuccess=false";
					resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - setSuccess=false";
				}
			}

			if(assertion.getType().equalsIgnoreCase("file.download")){

				log.info("executing file download...");

				downloadLocation = System.getProperty("java.io.tmpdir")+"/downloaded"; 

				log.info("Donwload location: "+downloadLocation);

				boolean download = false;

				String urlToDownload = StringEscapeUtils.unescapeXml(assertion.getContent().trim());

				download = conn.downloadFile(tc, urlToDownload, downloadLocation);

				if(download){
					log.info("PASS - The donwload was executed sucessfully.");
					result_description = result_description + "\nPASS - The donwload was executed sucessfully: "+urlToDownload;
					if(check_result!=2) check_result = 1;
				} 
				else { 
					log.info("FAIL - The download could not be executed: "+urlToDownload);
					check_result = 2;
					result_description = result_description + "\nFAIL - The download could not be executed: "+assertion.getContent();
					resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - The download could not be executed: "+assertion.getContent();
				}
			}

			if(assertion.getType().equalsIgnoreCase("file.download.xml")){

				log.info("executing file download searching url on xml response...");

				if(assertion.getContent().isEmpty())
					downloadLocation = System.getProperty("java.io.tmpdir")+"/downloaded"; 
				else downloadLocation = assertion.getContent().trim();

				log.info("Donwload location: "+downloadLocation);

				String response = tc.getResponse();
				log.info("response: "+response);
				String xmlelement;

				boolean download = false;

				if((response != null)&&(response != "")){
					xmlelement = utils.xmlElementFinder(response, "content-reference");
				} else {
					xmlelement = "No xml element was found! Name: content-reference";
				}

				String urlToDownload = StringEscapeUtils.unescapeXml(xmlelement);

				if(!xmlelement.contains("No xml element was found")){
					log.info("xmlElementfinder: content-reference: unescape url to download: "+xmlelement);
					download = conn.downloadFile(tc, urlToDownload, downloadLocation);
				}

				if(download){
					log.info("PASS - The donwload was executed sucessfully.");
					result_description = result_description + "\nPASS - The donwload was executed sucessfully: "+urlToDownload;
					if(check_result!=2) check_result = 1;
				} 
				else { 
					log.info("FAIL - The download could not be executed: "+urlToDownload);
					check_result = 2;
					result_description = result_description + "\nFAIL - The download could not be executed: "+assertion.getContent();
					resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - The download could not be executed: "+assertion.getContent();
				}
			}

			if(assertion.getType().equalsIgnoreCase("file.download.json")){

				log.info("executing file download search url on json response...");

				if(assertion.getContent().isEmpty())
					downloadLocation = System.getProperty("java.io.tmpdir")+"/downloaded"; 
				else downloadLocation = assertion.getContent().trim();

				log.info("Donwload location: "+downloadLocation);

				String response = tc.getResponse();
				log.info("response: "+response);

				String jsonvalue, urlToDownload;
				boolean download = false;

				if((response != null)&&(response != "")){
					jsonvalue = utils.jsonKeyFinder(response, "assetUrl", -1);
				} else {
					jsonvalue = "No json key was found! Name: assetUrl";
				}

				urlToDownload = jsonvalue;
				if(!jsonvalue.contains("No json key was found")){
					log.info("jsonKeyFinder: assetUrl: "+jsonvalue);
					download = conn.downloadFile(tc, urlToDownload, downloadLocation);
				}

				if(download){
					log.info("PASS - The donwload was executed sucessfully.");
					result_description = result_description + "\nPASS - The donwload was executed sucessfully: "+urlToDownload;
					if(check_result!=2) check_result = 1;
				} 
				else { 
					log.info("FAIL - The download could not be executed: "+urlToDownload);
					check_result = 2;
					result_description = result_description + "\nFAIL - The download could not be executed: "+assertion.getContent();
					resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - The download could not be executed: "+assertion.getContent();
				}
			}

			if(assertion.getType().equalsIgnoreCase("download.location")){
				downloadLocation=assertion.getContent();
			}

			if(assertion.getType().equalsIgnoreCase("file.compare")){

				int exitValue=1;
				String errors = "";

				log.info("File 1 to compare: "+downloadLocation);
				log.info("File 2 to compare: "+assertion.getContent());

				if(assertion.getContent().contains(".pdf")){

					if(!diffPDFPath.isEmpty()){	

						log.info("############################ PDF HANDLING - DIFFPDF ############################");
						String currentRelativePath = "";
						File f = new File("");
						currentRelativePath = f.getAbsolutePath();

						log.info("Current path for diff-pdf tool is: " + diffPDFPath);

						try {

							String command = diffPDFPath+" --output-diff="+downloadLocation+"diff "+downloadLocation+" "+assertion.getContent();
							log.info("Command: "+command);
							Process p = Runtime.getRuntime().exec(command);
							String line;
							BufferedReader bri = new BufferedReader
									(new InputStreamReader(p.getInputStream()));
							BufferedReader bre = new BufferedReader
									(new InputStreamReader(p.getErrorStream()));
							while ((line = bri.readLine()) != null) {
								log.error(line);
								errors = line;
							}
							bri.close();
							while ((line = bre.readLine()) != null) {
								log.error(line);
								errors = errors+" "+line;
							}
							bre.close();
							p.waitFor();
							exitValue = p.exitValue();
							log.info("Exit Value: "+exitValue);
						}
						catch (Exception err) {
							err.printStackTrace();
							log.info("FAIL - "+err.getLocalizedMessage());
							check_result = 2;
							result_description = result_description + "\nFAIL - "+err.getLocalizedMessage();
							resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - "+err.getLocalizedMessage();
						}

						if(exitValue==0){
							log.info("PASS - The pdf's are the same.");
							result_description = result_description + "\nPASS - "+downloadLocation+" is equal of "+assertion.getContent();
							if(check_result!=2) check_result = 1;
						} 
						else { 
							log.info("FAIL - "+errors+" "+"Expected file: "+assertion.getContent());
							check_result = 2;
							result_description = result_description + "\nFAIL - "+errors+" "+downloadLocation+" is different of "+assertion.getContent();
							resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - "+errors+" "+downloadLocation+" is different of "+assertion.getContent();
						}
						log.info("#################################################################################");
					} else { 
						log.info("FAIL - Diff-PDF was not found, please check if the '/diffPDFPath' parameter was correctly set in the command line: "+diffPDFPath);
						check_result = 2;
						result_description = result_description + "\nFAIL - Diff-PDF was not found, please check if the '/diffPDFPath' parameter was correctly set: "+diffPDFPath;
						resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - Diff-PDF was not found, please check if the '/diffPDFPath' parameter was correctly set: "+diffPDFPath;
					}
				} else 
					if(assertion.getContent().contains(".tif")){
						try {

							if(utils.compareTiffImages(assertion.getContent(), downloadLocation)){
								log.info("PASS - The images are the same.");
								result_description = result_description + "\nPASS - "+downloadLocation+" is equal of "+assertion.getContent();
								if(check_result!=2) check_result = 1;
							} 
							else { 
								log.info("FAIL - "+downloadLocation+" is different of "+assertion.getContent());
								check_result = 2;
								result_description = result_description + "\nFAIL - "+downloadLocation+" is different of "+assertion.getContent();
								resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - "+downloadLocation+" is different of "+assertion.getContent();
							}

						} catch (IOException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
							log.info("FAIL - "+e.getLocalizedMessage());
							check_result = 2;
							result_description = result_description + "\nFAIL - "+e.getLocalizedMessage();
							resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - "+e.getLocalizedMessage();
						}
					} else 
						if((assertion.getContent().contains(".bmp"))
								||(assertion.getContent().contains(".jpg"))
								||(assertion.getContent().contains(".jpeg"))
								||(assertion.getContent().contains(".wbmp"))
								||(assertion.getContent().contains(".png"))
								||(assertion.getContent().contains(".gif"))) {

							try {

								if(utils.compareImages(assertion.getContent(), downloadLocation)){
									log.info("PASS - The images are the same.");
									result_description = result_description + "\nPASS - "+downloadLocation+" is equal of "+assertion.getContent();
									if(check_result!=2) check_result = 1;
								} 
								else { 
									log.info("FAIL - "+downloadLocation+" is different of "+assertion.getContent());
									check_result = 2;
									result_description = result_description + "\nFAIL - "+downloadLocation+" is different of "+assertion.getContent();
									resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - "+downloadLocation+" is different of "+assertion.getContent();
								}

							} catch (IOException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
								log.info("FAIL - "+e.getLocalizedMessage());
								check_result = 2;
								result_description = result_description + "\nFAIL - "+e.getLocalizedMessage();
								resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - "+e.getLocalizedMessage();
							}
						}
						else {
							log.info("FAIL - The file extension informed was not found in the implemented comparison methods: "+assertion.getContent());
							check_result = 2;
							result_description = result_description + "\nFAIL - The file extension informed was not found in the implemented comparison methods: "+assertion.getContent();
							resultDescriptionFailures = resultDescriptionFailures + "\nFAIL - The file extension informed was not found in the implemented comparison methods: "+assertion.getContent();
						}
			}

			/*if(assertion.getType().contains("save_global")){


				if(assertion.getType().contains("responsekey")){
					log.info("saving global keys from response");

					try {
						JSONObject json = (JSONObject)new JSONParser().parse(tc.getResponse());
						System.out.println("status=" + json.get("status"));


					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}


					String jsonText = tc.getResponse();
					JSONParser parser = new JSONParser();  
					KeyFinder finder = new KeyFinder();  
					finder.setMatchKey("metadataID");  
					try{    
						while(!finder.isEnd()){      
							parser.parse(jsonText, finder, true);     
							if(finder.isFound()){        
								finder.setFound(false);        
								System.out.println("found metadataID:");        
								System.out.println(finder.getValue());      
							}    
						}             
					}  catch(ParseException pe) {    
						pe.printStackTrace();  
					}


					System.exit(0);
				}
			}*/


		}
		tc.setResult_description(result_description);
		tc.setResultDescriptionFailures(resultDescriptionFailures);
		if((check_result==0)||(check_result==2)) tc.setResult_success(false);
		if(check_result==1) tc.setResult_success(true);

		return tc;
	}
}

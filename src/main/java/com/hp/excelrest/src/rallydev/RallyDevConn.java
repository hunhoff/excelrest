package com.hp.excelrest.src.rallydev;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hp.excelrest.src.testcase.Assertions;
import com.hp.excelrest.src.testcase.TestCase;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.DeleteRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.DeleteResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.UpdateResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;
import com.rallydev.rest.util.Ref;


public class RallyDevConn { 

	private String rallyUrl;
	private String userName;
	private String password;
	private String proxy;
	private RallyRestApi restApi;

	static Logger log = Logger.getLogger(RallyDevConn.class);

	public RallyDevConn(){}

	/**
	 *******************************************************************************************
	 * Start new RallyRestApi using the user credentials
	 * @param rallyUrl - (i.e: https://rally1.rallydev.com) 
	 * @param userName - usually the user email
	 * @param password - user password
	 * @param proxy - (i.e: http://web-proxy:8088)
	 *******************************************************************************************
	 **/
	public void start(String rallyUrl, String userName, String password, String proxy) {

		this.rallyUrl = rallyUrl;
		this.userName = userName;
		this.password = password;
		this.proxy = proxy;

		log.info("Starting RallyRestApi connection...");
		log.debug("rallyUrl="+rallyUrl+", userName="+userName+", password="+password+", proxy="+proxy);


		try {
			restApi = new RallyRestApi(new URI(rallyUrl), userName, password);
			restApi.setApplicationName("Start using Rally Rest API...");

			if(!proxy.isEmpty())
				restApi.setProxy(new URI(proxy));

		} catch (URISyntaxException e) {
			log.error(e.getMessage());
			log.error(e.getReason());
			log.error(e.getStackTrace());
			System.exit(1);
		}
	}

	public void newTesCaseResult(LinkedList<TestCase> tcs, String projectId, String userRef, String build){

		String result, testCaseRef, resultDesc;

		log.info("Creating Test Case Results...");

		Iterator itTC = tcs.iterator();
		while(itTC.hasNext()){
			TestCase tc = (TestCase) itTC.next();

			try {
				//register the result only if the test case were executed
				if(tc.isExecutable()){
					if(tc.isResult_success()) result = "Pass";
					else result = "Fail";

					JsonObject newTestCaseResult = new JsonObject();
					newTestCaseResult.addProperty("Verdict", result);
					newTestCaseResult.addProperty("Date", tc.getResult_date());

					//resultDesc = "Response: "+tc.getResponse()+" Result Description: "+tc.getResult_description();
					//Removing response in order to avoid issues registering test results
					resultDesc = "Result Description: "+tc.getResult_description();
					newTestCaseResult.addProperty("Notes", resultDesc);
					newTestCaseResult.addProperty("Build", build);
					newTestCaseResult.addProperty("Tester", userRef);

					testCaseRef = getTestCaseRef(projectId, tc.getRallyDevId());
					newTestCaseResult.addProperty("TestCase", testCaseRef);
					//newTestCaseResult.addProperty("Duration", duration);

					log.info("New TC Result JSON: "+newTestCaseResult.toString());
					CreateRequest createRequest = new CreateRequest("testcaseresult", newTestCaseResult);
					CreateResponse createResponse = restApi.create(createRequest);            

					if (createResponse.wasSuccessful()) {
						log.info(String.format("Created %s", createResponse.getObject().get("_ref").getAsString()));
					}else { 
						log.error("Fail to create test case result using the following Json: "+newTestCaseResult.getAsString());
						System.exit(1);
					}
				}
			} catch (Exception e) {
				log.error("RallyDev Failure! Fail to register result.");
				log.error(e.getMessage());
				log.error(e.getCause());
				log.error(e.getStackTrace());
				stop();
				start(rallyUrl, userName, password, proxy);
				//System.exit(1);
			}

		}
	}

	public void deleteTCbyFormattedId(String projectId, String[] formattedIDList){

		String ref = "";

		log.info("searching test case on rallydev...");
		try{
			QueryRequest tc = new QueryRequest("testcase");
			tc.setProject("/project/"+projectId);
			tc.setLimit(100000);

			QueryResponse queryResponse = restApi.query(tc);
			if (queryResponse.wasSuccessful()) {
				//log.debug(String.format("Total results: %d", queryResponse.getTotalResultCount()));

				for (JsonElement result : queryResponse.getResults()) {
					JsonObject testcase = result.getAsJsonObject();

					for(int i=0; i<formattedIDList.length; i++) {

						if(testcase.get("FormattedID").getAsString().equals(formattedIDList[i])) {
							ref = testcase.get("_ref").getAsString();

							//System.out.println(ref.toString());
							log.info(testcase.get("FormattedID").getAsString()+" deleted");

							log.info("\nDeleting testcase...");
							DeleteRequest deleteRequest = new DeleteRequest(ref);
							DeleteResponse deleteResponse = restApi.delete(deleteRequest);
							if (deleteResponse.wasSuccessful()) {
								log.info("Deleted defect.");
							}
						}
					}
				}

			}else {
				System.err.println("The following errors occurred: ");
				for (String err : queryResponse.getErrors()) {
					log.error("\t" + err);
				}
			}
		}catch (Exception e) {
			log.error(e.getMessage());
			System.exit(1);
		}	
	}

	public void deleteAllTestCases(String projectId){

		String ref = "";

		log.info("searching test case on rallydev...");
		try{
			QueryRequest tc = new QueryRequest("testcase");
			tc.setProject("/project/"+projectId);
			tc.setLimit(100000);

			QueryResponse queryResponse = restApi.query(tc);
			if (queryResponse.wasSuccessful()) {
				//log.debug(String.format("Total results: %d", queryResponse.getTotalResultCount()));

				for (JsonElement result : queryResponse.getResults()) {
					JsonObject testcase = result.getAsJsonObject();
					ref = testcase.get("_ref").getAsString();

					System.out.println(ref.toString());

					System.out.println("\nDeleting testcase...");
					DeleteRequest deleteRequest = new DeleteRequest(ref);
					DeleteResponse deleteResponse = restApi.delete(deleteRequest);
					if (deleteResponse.wasSuccessful()) {
						System.out.println("Deleted defect.");
					}
				}

			}else {
				System.err.println("The following errors occurred: ");
				for (String err : queryResponse.getErrors()) {
					log.error("\t" + err);
				}
			}
		}catch (Exception e) {
			log.error(e.getMessage());
			System.exit(1);
		}	
	}

	public LinkedList<TestCase> updateTestCases(LinkedList<TestCase> tcs, String userId, String projectId){

		int listPos=0;

		Iterator itTC = tcs.iterator();
		while(itTC.hasNext()){
			TestCase tc = (TestCase) itTC.next();
			String ref = "";

			try {

				log.info("Creating/Updating Test Cases...");
				JsonObject testCase = new JsonObject();

				log.info("Name: "+ tc.getName());
				testCase.addProperty("Name", tc.getName());
				testCase.addProperty("Description", tc.getDescription());
				testCase.addProperty("Owner", "/user/"+userId);
				testCase.addProperty("Method", "Automated");
				testCase.addProperty("Type", "Functional");
				testCase.addProperty("Project", "/project/"+projectId);

				String validationInput="", validationExpectedResult="";

				if(tc.getSheetName().equalsIgnoreCase("queue_tests")){
					validationInput = "Queue_to_publish:\n"+tc.getQueuetc().getQueue_to_publish()+"\n\nMessage_to_publish:\n" + tc.getQueuetc().getMessage_to_publish();
					validationExpectedResult = "Queue_to_consume:\n"+tc.getQueuetc().getQueue_to_consume();
				}
				else if(tc.getSheetName().equalsIgnoreCase("rest_tests")){
					validationInput = "Path:\n"+tc.getResttc().getPath()
							+"\n\nMethod:\n" + tc.getResttc().getMethod()
							+"\n\nBody:\n" + tc.getResttc().getBody()
							+"\n\nHeader:\n" + tc.getResttc().getHeaders();
					validationExpectedResult = "Queue_to_consume:\n"+tc.getQueuetc().getQueue_to_consume();
				} 

				testCase.addProperty("ValidationInput", validationInput);

				validationExpectedResult = validationExpectedResult +" Assertions: ";
				Iterator itAssertion = tc.getAssertions().iterator();
				while(itAssertion.hasNext()){
					Assertions assertion = (Assertions) itAssertion.next();
					validationExpectedResult = validationExpectedResult +" "+assertion.getType()+"="+assertion.getContent();
				}				
				testCase.addProperty("ValidationExpectedResult", validationExpectedResult);

				/* checking if test case already exist */
				ref = getTestCaseRef(projectId, tc.getRallyDevId());				


				/* if ref is not empty test case exist.. update test case */
				if(!ref.isEmpty()){
					log.info("Updating existing test case...");
					UpdateRequest updateRequest = new UpdateRequest(ref, testCase);
					UpdateResponse updateResponse = restApi.update(updateRequest);
					JsonObject obj = updateResponse.getObject();
					log.info(obj.get("FormattedID").getAsString()+" updated!");

				} else {
					log.info("Creating new test case...");
					CreateRequest createRequest = new CreateRequest("testcase", testCase);
					log.debug("Request body: "+createRequest.getBody());				

					CreateResponse createResponse = restApi.create(createRequest);
					log.debug("Response: "+createResponse.getObject().toString());

					JsonObject project = createResponse.getObject().getAsJsonObject();
					log.info("Created. TC_ID: "+ project.get("FormattedID").getAsString());
					log.debug("Ref:"+Ref.getRelativeRef(createResponse.getObject().get("_ref").getAsString()));
					log.info(String.format("Created %s", createResponse.getObject().get("_ref").getAsString()));

					/* updating test case id */
					tcs.get(listPos).setRallyDevId(project.get("FormattedID").getAsString());
				}
				listPos++;
			} catch (Exception e) {
				log.error("RallyDev Failure! Fail to update tc.");
				log.error(e.getMessage());
				log.error(e.getCause());
				log.error(e.getStackTrace());
				//System.exit(1);
			}
		}


		return tcs;
	}

	public String getTestCaseRef(String projectId, String tcFormattedID){
		String ref = "";

		log.info("searching test case "+tcFormattedID+" on rallydev...");
		try{
			QueryRequest tc = new QueryRequest("testcase");
			tc.setProject("/project/"+projectId);
			tc.setFetch(new Fetch("FormattedID","Name"));
			tc.setQueryFilter(new QueryFilter("FormattedID", "=", tcFormattedID.trim()));

			QueryResponse queryResponse = restApi.query(tc);
			if (queryResponse.wasSuccessful()) {
				log.debug(String.format("Total results: %d", queryResponse.getTotalResultCount()));

				for (JsonElement result : queryResponse.getResults()) {
					JsonObject testcase = result.getAsJsonObject();

					if(testcase.get("FormattedID").getAsString().equalsIgnoreCase(tcFormattedID.trim())){
						ref = testcase.get("_ref").getAsString();
						log.info("FormattedID: "+tcFormattedID+", ref: "+ref);
					}
				} 
			}else {
				System.err.println("The following errors occurred: ");
				for (String err : queryResponse.getErrors()) {
					log.error("\t" + err);
				}
			}
		}catch (Exception e) {
			log.error("RallyDev Failure! Fail to get test case ref.");
			log.error(e.getMessage());
			//System.exit(1);
		}	
		return ref;
	}

	public String getUserRef(String username) {

		String userRef = "";		
		QueryRequest users = new QueryRequest("users");
		users.setQueryFilter(new QueryFilter("UserName", "=", username));
		QueryResponse queryResponse;

		try {
			queryResponse = restApi.query(users);

			if (queryResponse.wasSuccessful()) {
				for (JsonElement result : queryResponse.getResults()) {
					JsonObject user = result.getAsJsonObject();
					userRef = user.get("_ref").getAsString();
				}
			} else {
				log.error("The following errors occurred: ");
				for (String err : queryResponse.getErrors()) {
					log.error("\t" + err);
				}
			}
		} catch (IOException e) {
			log.error(e.getMessage());
			System.exit(1);
		}

		if(userRef.equalsIgnoreCase("")) {
			log.error("username not found: "+username+". Cannot identify userRef");
			System.exit(1);
		}
		return userRef;
	}

	public String getUserId(String username) {

		String userId = "";		
		QueryRequest users = new QueryRequest("users");
		users.setQueryFilter(new QueryFilter("UserName", "=", username));
		QueryResponse queryResponse;

		try {
			queryResponse = restApi.query(users);

			if (queryResponse.wasSuccessful()) {
				for (JsonElement result : queryResponse.getResults()) {
					JsonObject project = result.getAsJsonObject();
					userId = project.get("ObjectID").getAsString();
				}
			} else {
				log.error("The following errors occurred: ");
				for (String err : queryResponse.getErrors()) {
					log.error("\t" + err);
				}
			}
		} catch (IOException e) {
			log.error(e.getMessage());
			System.exit(1);
		}

		if(userId.equalsIgnoreCase("")) {
			log.error("username not found: "+username+". Cannot identify userId");
			System.exit(1);
		}
		return userId;
	}

	public String getProjectId(String projectName){

		String projId = "";		
		QueryRequest projects = new QueryRequest("projects");
		projects.setQueryFilter(new QueryFilter("Name", "=", projectName));
		QueryResponse queryResponse;

		try {
			queryResponse = restApi.query(projects);

			if (queryResponse.wasSuccessful()) {
				for (JsonElement result : queryResponse.getResults()) {
					JsonObject project = result.getAsJsonObject();
					projId = project.get("ObjectID").getAsString();
				}
			} else {
				log.error("The following errors occurred: ");
				for (String err : queryResponse.getErrors()) {
					log.error("\t" + err);
				}
			}
		} catch (IOException e) {
			log.error(e.getMessage());
			System.exit(1);
		}

		if(projId.equalsIgnoreCase("")) {
			log.error("project name not found: "+projectName+". Cannot identify projectId");
			System.exit(1);
		}
		return projId;
	}


	public LinkedList<Defect> queryDefects(String projectId, String state[]){

		LinkedList<Defect> list = new LinkedList<Defect>();

		try {
			QueryRequest defects = new QueryRequest("defect");
			defects.setProject("/project/"+projectId);
			//defects.setFetch(new Fetch("FormattedID", "Name", "State", "Priority", "Severity", "Owner", "Blocked"));

			if(state[0]!=null){
				QueryFilter filter = new QueryFilter("State","=", state[0]);
				for(int i=1; i<state.length; i++){
					filter = filter.or(new QueryFilter("State", "=", state[i]));
				}
				defects.setQueryFilter(filter);
			}
			

			//defects.setQueryFilter(new QueryFilter("State", "=", "Open").or(new QueryFilter("State", "=", "Fixed").or(new QueryFilter("State", "=", "Submitted").or(new QueryFilter("State", "=", "Closed")))));
			defects.setOrder("Severity ASC,Priority ASC,FormattedID ASC");

			//Return up to 5, 1 per page
			//defects.setPageSize(1);
			defects.setLimit(100000);

			QueryResponse queryResponse;
			queryResponse = restApi.query(defects);
			if (queryResponse.wasSuccessful()) {
				//System.out.println(String.format("Total results: %d", queryResponse.getTotalResultCount()));
				for (JsonElement result : queryResponse.getResults()) {
					JsonObject defect = result.getAsJsonObject();
					System.out.println(defect.toString());

					Defect df = new Defect();

					if(!defect.get("ObjectID").isJsonNull()){
						df.setObjectID(defect.get("ObjectID").getAsString());
					}else df.setObjectID("no entry");

					if(!defect.get("FormattedID").isJsonNull()){
						df.setFormattedID(defect.get("FormattedID").getAsString());
					}else df.setFormattedID("no entry");

					if(!defect.get("Name").isJsonNull()){
						df.setName(defect.get("Name").getAsString());
					}else df.setName("no entry");

					if(!defect.get("Priority").isJsonNull()){
						df.setPriority(defect.get("Priority").getAsString());
					}else df.setPriority("no entry");

					if(!defect.get("Severity").isJsonNull()){
						df.setSeverity(defect.get("Severity").getAsString());
					}else df.setSeverity("no entry");

					if(!defect.get("State").isJsonNull()){
						df.setState(defect.get("State").getAsString());
					}else df.setState("no entry");

					if(!defect.get("Blocked").isJsonNull()){
						df.setBlocked(defect.get("Blocked").getAsString());
					}else df.setBlocked("no entry");

					if(!defect.get("Resolution").isJsonNull()){
						df.setResolution(defect.get("Resolution").getAsString());
					}else df.setResolution("no entry");

					if(!defect.get("Environment").isJsonNull()){
						df.setEnvironment(defect.get("Environment").getAsString());
					}else df.setEnvironment("no entry");

					if(!defect.get("CreationDate").isJsonNull()){
						df.setCreationDate(defect.get("CreationDate").getAsString());
					}else df.setCreationDate("no entry");

					if(!defect.get("FoundInBuild").isJsonNull()){
						df.setFoundInBuild(defect.get("FoundInBuild").getAsString());
					}else df.setFoundInBuild("no entry");

					if(!defect.get("VerifiedInBuild").isJsonNull()){
						df.setVerifiedInBuild(defect.get("VerifiedInBuild").getAsString());
					}else df.setVerifiedInBuild("no entry");

					if(!defect.get("FixedInBuild").isJsonNull()){
						df.setFixedInBuild(defect.get("FixedInBuild").getAsString());
					}else df.setFixedInBuild("no entry");

					if(!defect.get("Iteration").isJsonNull()){
						df.setIteration(defect.get("Iteration").getAsJsonObject().get("_refObjectName").getAsString());
					}else df.setIteration("no entry");

					if(!defect.get("Release").isJsonNull()){
						df.setRelease(defect.get("Release").getAsJsonObject().get("_refObjectName").getAsString());
					}else df.setRelease("no entry");

					if(!defect.get("Owner").isJsonNull()){
						df.setOwner(defect.get("Owner").getAsJsonObject().get("_refObjectName").getAsString());
					}else df.setOwner("no entry");

					if(!defect.get("SubmittedBy").isJsonNull()){
						df.setSubmitter(defect.get("SubmittedBy").getAsJsonObject().get("_refObjectName").getAsString());
					}else df.setSubmitter("no entry");

					list.add(df);
				}
			} else {
				log.error("The following errors occurred: ");
				for (String err : queryResponse.getErrors()) {
					log.error("\t" + err);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}

	public LinkedList<String[]> query(String projectId, Report report){

		LinkedList<String[]> resultList = new LinkedList<String[]>();
		String fields[] = report.getFields();
		String[] results = new String[report.getFields().length];
		resultList.add(fields);

		try {
			QueryRequest qreq = new QueryRequest(report.getType());
			log.info("Report Type: "+report.getType());
			qreq.setProject("/project/"+projectId);

			qreq.setLimit(100000);

			try {

				int numberOfFilters = Integer.parseInt(report.getFilters().get(0).getId());
				for(int i=0; i<report.getFilters().size(); i++){
					if(numberOfFilters < Integer.parseInt(report.getFilters().get(i).getId())) 
						numberOfFilters = Integer.parseInt(report.getFilters().get(i).getId());
				}
				QueryFilter[] filters = new QueryFilter[numberOfFilters+1];
				
				int countFilters = 0;
				QueryFilter filter = null;
				String currentNumber=countFilters+"";

				filter = new QueryFilter(report.getFilters().get(0).getField(),report.getFilters().get(0).getOperator(), report.getFilters().get(0).getValue());
				for(int i=0; i < report.getFilters().size(); i++){

					if(report.getFilters().get(i).getId().contains(currentNumber)){
						filter = filter.and(new QueryFilter(report.getFilters().get(i).getField(),report.getFilters().get(i).getOperator(), report.getFilters().get(i).getValue()));
						filters[countFilters] = filter;
					}else{
						countFilters++;
						currentNumber=countFilters+"";
						filter = new QueryFilter(report.getFilters().get(i).getField(),report.getFilters().get(i).getOperator(), report.getFilters().get(i).getValue());
						filters[countFilters] = filter;
					}
				}

				filter = filters[0];
				for(int i=1; i<filters.length; i++){
					filter = filter.or(filters[i]);
				}
				if(filter!=null){
					log.info(filter);
					qreq.setQueryFilter(filter);
				} else log.error("Filter is null!");
			} catch (Exception e) {
				log.error("Unable to set filter!");
				log.error(e.getMessage());
			}

			QueryResponse queryResponse;
			queryResponse = restApi.query(qreq);
			if (queryResponse.wasSuccessful()) {
				log.info(String.format("Total results: %d", queryResponse.getTotalResultCount()));

				for (JsonElement result : queryResponse.getResults()) {
					JsonObject resultJson = result.getAsJsonObject();

					log.debug("RESULT JSON: "+resultJson.toString());

					results = new String[report.getFields().length];
					for(int i=0; i < fields.length; i++){

						log.debug("Identifying content of '"+fields[i]+"' field." );

						try{
							if(!resultJson.get(fields[i].toString()).getAsJsonObject().get("_refObjectName").isJsonNull()){
								results[i] = resultJson.get(fields[i].toString()).getAsJsonObject().get("_refObjectName").getAsString();
								log.debug("Content: "+results[i]);
							} 
						}catch (Exception e) {

							try {
								if(!resultJson.get(fields[i].toString()).isJsonNull()){
									results[i] = resultJson.get(fields[i].toString()).getAsString();
									log.debug("Content: "+results[i]);
								} else results[i] = "no entry";
							} catch (Exception e2) {
								log.error("Failed when trying to get content from field = '"+fields[i]+"'"+"! Remove this field and try again!");
								System.exit(1);
							}
						}

						/*if(!resultJson.get(fields[i].toString()).isJsonNull()){
							results[i] = resultJson.get(fields[i].toString()).getAsString();
						} else if(!resultJson.get(fields[i].toString()).getAsJsonObject().get("_refObjectName").isJsonNull()){
							results[i] = resultJson.get(fields[i].toString()).getAsJsonObject().get("_refObjectName").getAsString();
						} else results[i] = "no entry";*/

					}
					resultList.add(results);
				}
			} else {
				log.error("Remove the last field listed!");

				log.error("The following errors occurred: ");
				for (String err : queryResponse.getErrors()) {
					log.error("\t" + err);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
			log.error("Report Fail! Check project, type and fields informed in the config XML file...");
			System.exit(1);
		}

		return resultList;
	}

	public void stop(){
		try {
			restApi.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}

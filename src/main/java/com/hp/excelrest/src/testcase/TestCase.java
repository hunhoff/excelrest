package com.hp.excelrest.src.testcase;

import java.util.LinkedList;

import org.apache.commons.httpclient.Header;

public class TestCase {

	/* ********************************* test case configuration **********************************/
	private boolean run;
	private String input;
	private String output;
	private boolean postMail;
	private boolean proxy;
	/* *************************************************************************************************/
	
	/* ********************************* default test case parameters **********************************/
	private boolean isExecuted = false;
	private String token;
	private String defect_number;
	private String sheetName;
	private String testType;
	private int rowNumber;
	private String id;
	private String rallyDevId;
	private String name;
	private String description;
	private LinkedList<Assertions> assertions = new LinkedList<Assertions>();
	private LinkedList<Variable> vars = new LinkedList<Variable>();
	private boolean result_success = false;
	private boolean isLastResultPass = false;
	private int result_status;
	private Header[] responseHeaders;
	private String responseHeadersStr = "";
	private String response;
	private String result_description;
	private String resultDescriptionFailures = "";
	private String result_date;
	private boolean execute = false;
	private String full_path;
	
	/* *************************************************************************************************/
	
	/* *************************************** test case types *****************************************/
	private QueueTestCase queuetc = null;
	private RestTestCase resttc = null;
	/* *************************************************************************************************/
	
	public TestCase(){}
	
	public String getRallyDevId() {
		return rallyDevId;
	}

	public void setRallyDevId(String rallyDevId) {
		this.rallyDevId = rallyDevId;
	}
	
	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public boolean isPostMail() {
		return postMail;
	}

	public void setPostMail(boolean postMail) {
		this.postMail = postMail;
	}

	public boolean isRun() {
		return run;
	}

	public void setRun(boolean run) {
		this.run = run;
	}
	
	public boolean isExecuted() {
		return isExecuted;
	}

	public void setExecuted(boolean isExecuted) {
		this.isExecuted = isExecuted;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LinkedList<Variable> getVars() {
		return vars;
	}

	public void setVars(LinkedList<Variable> vars) {
		this.vars = vars;
	}

	public boolean isResult_success() {
		return result_success;
	}

	public void setResult_success(boolean result_success) {
		this.result_success = result_success;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public String getResult_description() {
		return result_description;
	}

	public void setResult_description(String result_description) {
		this.result_description = result_description;
	}
	
	public String getResultDescriptionFailures() {
		return resultDescriptionFailures;
	}

	public void setResultDescriptionFailures(String resultDescriptionFailures) {
		this.resultDescriptionFailures = resultDescriptionFailures;
	}

	public String getResult_date() {
		return result_date;
	}

	public void setResult_date(String result_date) {
		this.result_date = result_date;
	}

	public boolean isExecutable() {
		return execute;
	}

	public void setExecutable(boolean execute) {
		this.execute = execute;
	}

	public String getSheetName() {
		return sheetName;
	}

	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}

	public int getRowNumber() {
		return rowNumber;
	}

	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}

	public QueueTestCase getQueuetc() {
		return queuetc;
	}

	public void setQueuetc(QueueTestCase queuetc) {
		this.queuetc = queuetc;
	}

	public RestTestCase getResttc() {
		return resttc;
	}

	public void setResttc(RestTestCase resttc) {
		this.resttc = resttc;
	}

	public LinkedList<Assertions> getAssertions() {
		return assertions;
	}

	public void setAssertions(LinkedList<Assertions> assertions) {
		this.assertions = assertions;
	}

	public int getResult_status() {
		return result_status;
	}

	public void setResult_status(int result_status) {
		this.result_status = result_status;
	}

	public String getFull_path() {
		return full_path;
	}

	public void setFull_path(String full_path) {
		this.full_path = full_path;
	}

	public boolean isProxy() {
		return proxy;
	}

	public void setProxy(boolean proxy) {
		this.proxy = proxy;
	}

	public String getTestType() {
		return testType;
	}

	public void setTestType(String testType) {
		this.testType = testType;
	}

	public String getDefect_number() {
		return defect_number;
	}

	public void setDefect_number(String defect_number) {
		this.defect_number = defect_number;
	}

	public boolean isLastResultPass() {
		return isLastResultPass;
	}

	public void setLastResultPass(boolean isLastResultPass) {
		this.isLastResultPass = isLastResultPass;
	}

	public Header[] getResponseHeaders() {
		return responseHeaders;
	}
	
	public String getResponseHeadersStr() {
		return responseHeadersStr;
	}

	public void setResponseHeaders(Header[] responseHeaders) {
		this.responseHeaders = responseHeaders;
	}
	
	public void setResponseHeadersStr(String responseHeadersStr) {
		this.responseHeadersStr = responseHeadersStr;
	}
	
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

}

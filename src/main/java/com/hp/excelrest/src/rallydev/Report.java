package com.hp.excelrest.src.rallydev;

import java.util.LinkedList;

public class Report {
	
	private boolean run;
	private boolean postMail;
	private String output;
	private String sheet;
	private String fields[];
	private LinkedList<Filter> filters =  new LinkedList<Filter>();
	private String type;
	private LinkedList<String[]> results =  new LinkedList<String[]>();
	
	public LinkedList<Filter> getFilters() {
		return filters;
	}

	public void setFilters(LinkedList<Filter> filters) {
		this.filters = filters;
	}
		
	public Report(){}


	public boolean isRun() {
		return run;
	}

	public void setRun(boolean run) {
		this.run = run;
	}

	public boolean isPostMail() {
		return postMail;
	}

	public void setPostMail(boolean postMail) {
		this.postMail = postMail;
	}

	public String[] getFields() {
		return fields;
	}

	public void setFields(String[] fields) {
		this.fields = fields;
	}


	public String getOutput() {
		return output;
	}


	public void setOutput(String output) {
		this.output = output;
	}


	public String getType() {
		return type;
	}


	public void setType(String type) {
		this.type = type;
	}


	public LinkedList<String[]> getResults() {
		return results;
	}


	public void setResults(LinkedList<String[]> results) {
		this.results = results;
	}
	public String getSheet() {
		return sheet;
	}

	public void setSheet(String sheet) {
		this.sheet = sheet;
	}
}
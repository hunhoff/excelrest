package com.hp.excelrest.src.testcase;

import java.util.LinkedList;

public class Spreadsheet {
	
	private LinkedList<TestCase> tcs;
	private String fileLocation;
	
	public String getFileLocation() {
		return fileLocation;
	}

	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}

	public LinkedList<TestCase> getTcs() {
		return tcs;
	}

	public void setTcs(LinkedList<TestCase> tcs) {
		this.tcs = tcs;
	}

}

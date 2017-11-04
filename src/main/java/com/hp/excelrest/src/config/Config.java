
package com.hp.excelrest.src.config;

import com.hp.excelrest.src.rallydev.Defect;
import com.hp.excelrest.src.rallydev.RallyDev;
import com.hp.excelrest.src.rallydev.Report;
import com.hp.excelrest.src.testcase.TestCase;

public class Config {

	private TestCase testCase;
	private RallyDev rallyDev;
	private Defect defect;
	private Proxy proxy;
	private Email email;
	private Queue queue;
	private Rest rest;
	private Report report;
	
	public Config(TestCase testCase, RallyDev rallyDev, Defect defect,
			Proxy proxy, Email email, Queue queue, Rest rest, Report report) {
		super();
		this.testCase = testCase;
		this.rallyDev = rallyDev;
		this.defect = defect;
		this.proxy = proxy;
		this.email = email;
		this.queue = queue;
		this.rest = rest;
		this.report = report;
	}
	
	public Config(){}
	
	public TestCase getTestCase() {
		return testCase;
	}
	public void setTestCase(TestCase testCase) {
		this.testCase = testCase;
	}
	public RallyDev getRallyDev() {
		return rallyDev;
	}
	public void setRallyDev(RallyDev rallyDev) {
		this.rallyDev = rallyDev;
	}
	public Defect getDefect() {
		return defect;
	}
	public void setDefect(Defect defect) {
		this.defect = defect;
	}
	public Proxy getProxy() {
		return proxy;
	}
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}
	public Email getEmail() {
		return email;
	}
	public void setEmail(Email email) {
		this.email = email;
	}
	public Queue getQueue() {
		return queue;
	}
	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	public Rest getRest() {
		return rest;
	}

	public void setRest(Rest rest) {
		this.rest = rest;
	}

	public Report getReport() {
		return report;
	}

	public void setReport(Report report) {
		this.report = report;
	}
}
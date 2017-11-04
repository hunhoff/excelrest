package com.hp.excelrest.src.rallydev;

public class RallyDev {
	
	private boolean updateTestCase;
	private boolean registerTestResult;
	private String url;
	private String username;
	private String password;
	private String project;
	private boolean proxy;
	
	public RallyDev(){}
	
	public RallyDev(boolean updateTestCase, boolean registerTestResult,
			String url, String username, String password, String project, boolean proxy) {
		super();
		this.updateTestCase = updateTestCase;
		this.registerTestResult = registerTestResult;
		this.url = url;
		this.username = username;
		this.password = password;
		this.project = project;
	}

	public boolean isUpdateTestCase() {
		return updateTestCase;
	}
	public void setUpdateTestCase(boolean updateTestCase) {
		this.updateTestCase = updateTestCase;
	}
	public boolean isRegisterTestResult() {
		return registerTestResult;
	}
	public void setRegisterTestResult(boolean registerTestResult) {
		this.registerTestResult = registerTestResult;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getProject() {
		return project;
	}
	public void setProject(String project) {
		this.project = project;
	}

	public boolean isProxy() {
		return proxy;
	}

	public void setProxy(boolean proxy) {
		this.proxy = proxy;
	}
}

package com.hp.excelrest.src.testcase;

public class RestTestCase {

	public RestTestCase(){}
	
	private String path = "";
	private String method = "";
	private String body = "";
	private String headers = "";
	
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public String getHeaders() {
		return headers;
	}
	public void setHeaders(String headers) {
		this.headers = headers;
	}
}

package com.hp.excelrest.src.config;

public class Queue {

	private String username;
	private String password;
	private String vhost;
	private String host="";
	
	public Queue(){}
	
	public Queue(String username, String password, String vhost, String host) {
		super();
		this.username = username;
		this.password = password;
		this.vhost = vhost;
		this.host = host;
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
	public String getVhost() {
		return vhost;
	}
	public void setVhost(String vhost) {
		this.vhost = vhost;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	
}

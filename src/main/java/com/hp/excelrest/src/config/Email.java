package com.hp.excelrest.src.config;

public class Email {

	private String smtpName = "";
	private String msgTxt = "";
	private String subjectTxt = "";
	private String fromAddress = "";
	private String toAddressList[];
	
	public Email(){}
	
	public Email(String smtpName, String msgTxt, String subjectTxt,
			String fromAddress, String[] toAddressList) {
		super();
		this.smtpName = smtpName;
		this.msgTxt = msgTxt;
		this.subjectTxt = subjectTxt;
		this.fromAddress = fromAddress;
		this.toAddressList = toAddressList;
	}	
	
	public String getSmtpName() {
		return smtpName;
	}
	public void setSmtpName(String smtpName) {
		this.smtpName = smtpName;
	}
	public String getMsgTxt() {
		return msgTxt;
	}
	public void setMsgTxt(String msgTxt) {
		this.msgTxt = msgTxt;
	}
	public String getSubjectTxt() {
		return subjectTxt;
	}
	public void setSubjectTxt(String subjectTxt) {
		this.subjectTxt = subjectTxt;
	}
	public String getFromAddress() {
		return fromAddress;
	}
	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}
	public String[] getToAddressList() {
		return toAddressList;
	}
	public void setToAddressList(String[] toAddressList) {
		this.toAddressList = toAddressList;
	}
	
}

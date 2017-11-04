
package com.hp.excelrest.src.tools;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.hp.excelrest.src.config.Config;
import com.hp.excelrest.src.config.Email;
import com.hp.excelrest.src.config.Proxy;
import com.hp.excelrest.src.config.Queue;
import com.hp.excelrest.src.config.Rest;
import com.hp.excelrest.src.rallydev.Defect;
import com.hp.excelrest.src.rallydev.Filter;
import com.hp.excelrest.src.rallydev.RallyDev;
import com.hp.excelrest.src.rallydev.Report;
import com.hp.excelrest.src.testcase.TestCase;



/**
 *******************************************************************************
 *Class responsible to read the XML files and manage their informations.
 *@author      Alessandro Hunhoff 
 *@author 	   alessandro.hunhoff@hp.com 
 *@since       1.0.0
 *@version     1.0.23
 *******************************************************************************
 **/

public class XML_Reader {

	static Logger log = Logger.getLogger(XML_Reader.class);
	private String fileName;

	/**
	 *************************************************************************************
	 * Class contructor. 
	 * @param fileName - local and name where the XML file is located.
	 *************************************************************************************
	 */
	public XML_Reader(String fileName) {
		this.fileName = fileName;
	}


	/**
	 *************************************************************************************
	 * Load the XML file.
	 * @return This method returns all children as a list.
	 * @throws JDOMException
	 * @throws IOException
	 *************************************************************************************
	 **/
	private List loadFile() throws JDOMException, IOException {
		File f = new File(fileName);
		SAXBuilder sb = new SAXBuilder();   		  
		Document d = sb.build(f);   		  
		Element element = d.getRootElement();   
		return element.getChildren();   
	}

	/**
	 *************************************************************************************
	 * Read a XML file with the following format:
	 *<configuration>
	 *	<execution>
	 *		<testcases run="TRUE|FALSE">
	 *			<input>xls_input_file</input>
	 *			<output>xls_output_file</output>
	 *			<rallydev>
	 *				<updateTestCases run="TRUE|FALSE"></updateTestCases>
	 *				<registerTestResult run="TRUE|FALSE"></registerTestResult>
	 *			</rallydev>
	 *			<postmail run="TRUE|FALSE"></postmail>
	 *		</testcases>
	 *		<defectreport run="TRUE|FALSE">
	 *			<output>xls_output_file</output>
	 *			<stateList>
	 *				<state>Open</state>
	 *				<state>Fixed</state>
	 *				<state>Submitted</state>
	 *				<state>Closed</state>
	 *			</stateList>
	 *			<postmail run="TRUE|FALSE"></postmail>
	 *		</defectreport>
	 *	</execution>
	 *	<queue>
	 *		<username></username>
	 *		<password></password>
	 *		<vhost>/</vhost>
	 *		<host></host>
	 *	</queue>
	 *	<rest>
	 *    <url><url>
	 *	</rest>
	 *	<rallydev>
	 *		<url></url>
	 *		<username></username>
	 *		<password></password>
	 *	</rallydev>
	 *	<proxy>
	 *		<host></host>
	 *		<port></port>
	 *	</proxy>
	 *	<email>
	 *		<smtpName></smtpName>
	 *		<msgTxt></msgTxt>
	 *		<subjectTxt></subjectTxt>
	 *		<fromAddress></fromAddress>
	 *		<toAddress-list>
	 *			<toAddress></toAddress>
	 *			<toAddress></toAddress>
	 *		</toAddress-list>
	 *	</email>
	 *</configuration>
	 * @return RegKey LinkedList containing the XML information.
	 *************************************************************************************
	 */	
	public Config readConfig() {

		TestCase testcase = new TestCase();
		Defect defect = new Defect();
		Report report = new Report();
		Proxy proxy = new Proxy();
		Email email = new Email();
		RallyDev rallydev = new RallyDev();
		Queue queue = new Queue();
		Rest rest = new Rest();
		LinkedList<Filter> filters =  new LinkedList<Filter>();
		
		

		try {
			List elements = loadFile();
			Iterator i = elements.iterator();    		

			while(i.hasNext()) {  
				Element element = (Element) i.next();

				if(element.getName().equalsIgnoreCase("execution")){

					if (element.getChild("testcase") != null){
						Element tcListChild = element.getChild("testcase");
						testcase.setRun(Boolean.parseBoolean(tcListChild.getAttributeValue("run")));
						testcase.setInput(tcListChild.getChildText("input"));
						testcase.setOutput(tcListChild.getChildText("output"));

						if(tcListChild.getChild("rallydev") != null){
							Element rallyChild = tcListChild.getChild("rallydev");

							if(rallyChild.getChild("updateTestCase")!=null){
								Element upChild = rallyChild.getChild("updateTestCase");
								rallydev.setUpdateTestCase(Boolean.parseBoolean(upChild.getAttributeValue("run")));
							}

							if(rallyChild.getChild("registerTestResult")!=null){
								Element regChild = rallyChild.getChild("registerTestResult");
								rallydev.setRegisterTestResult(Boolean.parseBoolean(regChild.getAttributeValue("run")));
							}
						}
						if(tcListChild.getChild("postmail")!=null){
							Element postmailListChild = tcListChild.getChild("postmail");
							testcase.setPostMail(Boolean.parseBoolean(postmailListChild.getAttributeValue("run")));
						}
						if(tcListChild.getChild("proxy")!=null){
							Element proxyListChild = tcListChild.getChild("proxy");
							testcase.setProxy(Boolean.parseBoolean(proxyListChild.getAttributeValue("set")));
						}
					}
					if (element.getChild("defectreport") != null){
						Element defListChild = element.getChild("defectreport");
						defect.setRun(Boolean.parseBoolean(defListChild.getAttributeValue("run")));
						defect.setOutput(defListChild.getChildText("output"));

						if (defListChild.getChild("state-list") != null){
							int countStates = 0;
							Element stateListChild = defListChild.getChild("state-list");
							String[] stateList = {};
							if (stateListChild.getChild("state") != null){
								List stateChildrens = stateListChild.getChildren();
								stateList = new String [stateChildrens.size()];
								Iterator it = stateChildrens.iterator();
								stateList = new String[stateChildrens.size()];
								while(it.hasNext()){
									Element string = (Element) it.next();
									stateList[countStates++] = string.getText();
								}
							}
							defect.setStateFilter(stateList);
						}
						if(defListChild.getChild("postmail")!=null){
							Element postmailListChild = defListChild.getChild("postmail");
							defect.setPostMail(Boolean.parseBoolean(postmailListChild.getAttributeValue("run")));
						}

					}	

					if (element.getChild("report") != null){
						Element repListChild = element.getChild("report");
						report.setRun(Boolean.parseBoolean(repListChild.getAttributeValue("run")));
						report.setOutput(repListChild.getChildText("output"));
						report.setSheet(repListChild.getChildText("sheet"));
						report.setType(repListChild.getChildText("type"));

						if (repListChild.getChild("field-list") != null){
							int countFields = 0;
							Element fieldListChild = repListChild.getChild("field-list");
							String[] reportList = {};
							if (fieldListChild.getChild("field") != null){
								List reportChildrens = fieldListChild.getChildren();
								reportList = new String [reportChildrens.size()];
								Iterator it = reportChildrens.iterator();
								reportList = new String[reportChildrens.size()];
								while(it.hasNext()){
									Element string = (Element) it.next();
									reportList[countFields++] = string.getText();
								}
							}
							report.setFields(reportList);
						}
						
						if (repListChild.getChild("filter-list") != null){
							int countfilters = 0;
							Element filterListChild = repListChild.getChild("filter-list");
							String[] reportList = {};
							if (filterListChild.getChild("filter") != null){
								List reportChildrens = filterListChild.getChildren();
								reportList = new String [reportChildrens.size()];
								Iterator it = reportChildrens.iterator();
								reportList = new String[reportChildrens.size()];
								while(it.hasNext()){
									Filter filter = new Filter();
									Element string = (Element) it.next();
									filter.setId(string.getAttributeValue("id"));
									filter.setField(string.getAttributeValue("field"));
									filter.setOperator(string.getAttributeValue("operator"));
									filter.setValue(string.getAttributeValue("value"));
									filters.add(filter);
								}
							}
							report.setFilters(filters);
						}
						if(repListChild.getChild("postmail")!=null){
							Element postmailListChild = repListChild.getChild("postmail");
							report.setPostMail(Boolean.parseBoolean(postmailListChild.getAttributeValue("run")));
						}
					}	
				}

				if(element.getName().equalsIgnoreCase("queue")){

					queue.setUsername(element.getChildText("username"));
					queue.setPassword(element.getChildText("password"));
					queue.setVhost(element.getChildText("vhost"));
					queue.setHost(element.getChildText("host"));
				}

				if(element.getName().equalsIgnoreCase("restURL-list")){
					if (element.getChildren("restURL") != null){
						int countURL = 0;
						List restListChildrens = element.getChildren("restURL");
						String[] restList = {};
						restList = new String [restListChildrens.size()];
						
						Iterator it = restListChildrens.iterator();
						while(it.hasNext()){
							Element string = (Element) it.next();
							restList[countURL++] = string.getText();
						}
						rest.setUrl(restList);
					}
				}
				if(element.getName().equalsIgnoreCase("rallydev")){
					rallydev.setUrl(element.getChildText("url"));
					rallydev.setUsername(element.getChildText("username"));
					rallydev.setPassword(element.getChildText("password"));
					rallydev.setProject(element.getChildText("project"));

					if(element.getChild("proxy")!=null){
						Element proxyListChild = element.getChild("proxy");
						rallydev.setProxy(Boolean.parseBoolean(proxyListChild.getAttributeValue("set")));
					}
				}
				if(element.getName().equalsIgnoreCase("proxy")){
					proxy.setHost(element.getChildText("host"));
					proxy.setPort(element.getChildText("port"));
				}

				if(element.getName().equalsIgnoreCase("email")){
					email.setSmtpName(element.getChildText("smtpName"));
					email.setMsgTxt(element.getChildText("msgTxt"));
					email.setSubjectTxt(element.getChildText("subjectTxt"));
					email.setFromAddress(element.getChildText("fromAddress"));

					if (element.getChild("toAddress-list") != null){
						int countAddress = 0;
						Element toAddressListChild = element.getChild("toAddress-list");
						String[] toAddressList = {};
						if (toAddressListChild.getChild("toAddress") != null){
							List toAddressChildrens = toAddressListChild.getChildren();
							toAddressList = new String [toAddressChildrens.size()];
							Iterator it = toAddressChildrens.iterator();
							while(it.hasNext()){
								Element string = (Element) it.next();
								toAddressList[countAddress++] = string.getText();
							}
						}
						email.setToAddressList(toAddressList);
					}

				}
			}
		}
		catch (JDOMException e) {
			log.error("Error reading configuration XML: "+e.getMessage());
			System.exit(1);
		}
		catch (IOException e) {
			log.error("Error reading configuration XML: "+e.getMessage());
			System.exit(1);
		}

		Config config = new Config(testcase, rallydev, defect, proxy, email, queue, rest, report);

		log.trace("Listing configuration from xml...");

		if(config.getTestCase().isRun()){
			log.trace("testcase run: "+new Boolean(config.getTestCase().isRun()).toString());
			log.trace("testcase input: "+config.getTestCase().getInput());
			log.trace("testcase output: "+config.getTestCase().getOutput());
			log.trace("testcase postmail: "+new Boolean(config.getTestCase().isPostMail()).toString());

			log.trace("Rest_Url: "+config.getRest().getUrl());

			log.trace("Queue_Username: "+config.getQueue().getUsername());
			log.trace("Queue_Password: "+config.getQueue().getPassword());
			log.trace("Queue_VHost: "+config.getQueue().getVhost());
			log.trace("Queue_Host: "+config.getQueue().getHost());

			log.trace("Rally UpdateTestCase: "+new Boolean(config.getRallyDev().isUpdateTestCase()).toString());
			log.trace("Rally RegisterTestResult: "+new Boolean(rallydev.isRegisterTestResult()).toString());
		}

		if(config.getDefect().isRun()){

			log.trace("defect run"+new Boolean(config.getDefect().isRun()).toString());
			log.trace("defect postmail: "+new Boolean(config.getDefect().isPostMail()).toString());
			log.trace(("defect output"+config.getDefect().getOutput()));
			String stateFilter[] = config.getDefect().getStateFilter();
			for(int i=0; i<stateFilter.length; i++)
				log.trace("defect stateFilter: "+stateFilter[i]);
		}
		if(config.getReport().isRun()){
			log.trace("report run"+new Boolean(config.getReport().isRun()).toString());
			log.trace("report postmail: "+new Boolean(config.getReport().isPostMail()).toString());
			log.trace(("report output"+config.getReport().getOutput()));
			log.trace(("report type"+config.getReport().getType()));
			String fields[] = config.getReport().getFields();
			for(int i=0; i<fields.length; i++)
				log.trace("report fields: "+fields[i]);
		}

		log.trace("Rally_Url: "+config.getRallyDev().getUrl());
		log.trace("Rally_Username: "+config.getRallyDev().getUsername());
		log.trace("Rally_Password: "+config.getRallyDev().getPassword());
		log.trace("Rally_Project: "+config.getRallyDev().getProject());

		log.trace("Proxy_Host: "+config.getProxy().getHost());
		log.trace("Proxy_port: "+config.getProxy().getPort());

		log.trace("email smtpName: "+config.getEmail().getSmtpName());
		log.trace("email msgTxt: "+config.getEmail().getMsgTxt());
		log.trace("email subjectTxt: "+config.getEmail().getSubjectTxt());
		log.trace("email fromAddress: "+config.getEmail().getFromAddress());
		String toAddress[] = config.getEmail().getToAddressList();
		if(toAddress!=null) for(int i=0; i<toAddress.length; i++)
			log.trace("email toAddress: "+toAddress[i]);

		return config;
	}
}

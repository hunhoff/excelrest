
package com.hp.excelrest.src.tools;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartBase;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.hp.excelrest.src.config.Config;
import com.hp.excelrest.src.testcase.TestCase;
import com.hp.excelrest.src.testcase.Variable;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.QueueingConsumer;


public class Connections {


	private class JsonPart extends PartBase {

		private byte[] bytes;

		public JsonPart(String name, String json) throws IOException {
			super(name, "application/json", "UTF-8", null);
			this.bytes = json.getBytes("UTF-8");
		}

		@Override
		protected void sendData(OutputStream os) throws IOException {
			os.write(bytes);
		}

		@Override
		protected long lengthOfData() throws IOException {
			return bytes.length;
		}
	}

	private String baseUrl;     // Initialization not shown here
	private String sessionId;   // Initialization not shown here

	static Logger log = Logger.getLogger(Connections.class);

	private Config config;
	private String queue_username;
	private String queue_password;
	private String queue_vhost;
	private String queue_host;
	private String[] rest_url;
	private LinkedList<Variable> headerList;


	public Connections(Config config) throws KeyManagementException {
		super();

		/************************* IGNORE CERTIFICATES *****************************************************/
		SSLContext ctx;
		try {
			ctx = SSLContext.getInstance("TLS");
			ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
			SSLContext.setDefault(ctx);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/**************************************************************************************************/

		this.config = config;

		if(config.getQueue()!=null){
			queue_username = config.getQueue().getUsername();
			queue_password = config.getQueue().getPassword();
			queue_vhost = config.getQueue().getVhost();
			queue_host = config.getQueue().getHost();
		}

		if(config.getRest()!=null){
			rest_url = config.getRest().getUrl();
		}
	}


	public void writeByteArray(byte[] responseBody, String file){ 

		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(responseBody);
			fos.close(); 
		}
		catch(FileNotFoundException ex)	{
			log.error("FileNotFoundException : " + ex);
		}
		catch(IOException ioe)	{
			log.error("IOException : " + ioe);
		}
	}

	public LinkedList<Variable> parseHeader(String testCaseID, String header){

		log.debug("Test case ID: "+testCaseID);
		log.debug("Parsing header: "+ header);

		headerList = new LinkedList<Variable>();
		if(!header.isEmpty()){
			String values[] = header.split("\n");
			for(int j=0; j<values.length; j++) {
				String div[] = values[j].split(": ");
				if(div.length==2){
					Variable variable = new Variable("header",div[0].toString().trim(),div[1].toString().trim());
					headerList.add(variable);
				} else {
					log.error("Error parsing header of test case ID: "+testCaseID);
				}
			}
		}
		return headerList;
	}

	/**
	 *************************************************************************************************
	 * if String file is empty write response body in the TestCase response as String else, write an 
	 * file and set the location in the TestCase response.
	 * @return TestCase updated with response and result_status
	 *************************************************************************************************
	 **/
	public TestCase get(TestCase tc, String fileLocation) {

		tc.setResponse("empty");
		headerList = parseHeader(tc.getId(), tc.getResttc().getHeaders());

		GetMethod method = null;
		try {

			HttpClient client = new HttpClient();
			if(config.getTestCase().isProxy()){
				client.getHostConfiguration().setProxy(config.getProxy().getHost(), Integer.parseInt(config.getProxy().getPort().trim()));	
			}

			log.info("rest_url: "+tc.getResttc().getPath());
			method = new GetMethod(tc.getResttc().getPath().trim());
			//method = new GetMethod(tc.getResttc().getPath().trim());

			//setting request header
			log.info("Setting request header:");
			Iterator<Variable> itHeader = headerList.iterator();
			while(itHeader.hasNext()){
				Variable var = (Variable) itHeader.next();
				log.info(var.getName()+":"+var.getContent());	
				method.addRequestHeader(var.getName(), var.getContent());
			}

			method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,new DefaultHttpMethodRetryHandler(3, true));
			client.getHttpConnectionManager().getParams().setConnectionTimeout(10000);

			int returnCode = client.executeMethod(method);
			tc.setResult_status(returnCode);
			log.info(returnCode);

			Header[] header = method.getResponseHeaders();
			tc.setResponseHeaders(header);

			if (returnCode != HttpStatus.SC_OK) {
				log.error("Method failed: " + method.getStatusLine());
				tc.setResponse(method.getStatusLine().toString());
			}

			byte[] responseBody = method.getResponseBody();

			if(!(responseBody == null)){

				//if the fileLocation is not empty create a new file with the response content
				if(!fileLocation.isEmpty()){
					log.info("write file at: "+fileLocation);
					writeByteArray(responseBody, fileLocation);
					tc.setResponse(fileLocation);
				} else {
					String respStr = new String(responseBody, "UTF-8");
					tc.setResponse(respStr);
					log.info(respStr);
				}
			} else {
				log.error("Response body is NULL!");
				tc.setResponse("");
			}
			method.releaseConnection();

		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			log.error(e.getStackTrace());
			tc.setResponse(e.getMessage());
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			log.error(e.getStackTrace());
			tc.setResponse(e.getMessage());
		} catch (NullPointerException e) {
			log.error("NullPointerException: " + e.getMessage());
			log.error(e.getStackTrace());
			tc.setResponse(e.getMessage());
		} catch (IllegalArgumentException e) {
			log.error("IllegalArgumentException: " + e.getMessage());
			log.error(e.getStackTrace());
			tc.setResponse(e.getMessage());
		} 
		return tc;
	}

	public TestCase delete(TestCase tc) {

		headerList = parseHeader(tc.getId(), tc.getResttc().getHeaders());
		DeleteMethod method = null;

		try {

			HttpClient client = new HttpClient();
			if(config.getTestCase().isProxy()){
				client.getHostConfiguration().setProxy(config.getProxy().getHost(), Integer.parseInt(config.getProxy().getPort().trim()));	
			}

			log.info("rest_url: "+tc.getResttc().getPath());
			method = new DeleteMethod(tc.getResttc().getPath().trim());

			//setting request header
			log.info("Setting request header:");
			Iterator itHeader = headerList.iterator();
			while(itHeader.hasNext()){
				Variable var = (Variable) itHeader.next();
				log.info(var.getName()+":"+var.getContent());	
				method.addRequestHeader(var.getName(), var.getContent());
			}

			method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,new DefaultHttpMethodRetryHandler(3, true));
			client.getHttpConnectionManager().getParams().setConnectionTimeout(10000);

			int returnCode = client.executeMethod(method);
			tc.setResult_status(returnCode);
			log.info(returnCode);

			Header[] header = method.getResponseHeaders();
			tc.setResponseHeaders(header);

			if ((returnCode != HttpStatus.SC_OK)||(returnCode != HttpStatus.SC_NO_CONTENT)) {
				log.error("Method failed: " + method.getStatusLine());
				tc.setResponse(method.getStatusLine().toString());
			}
			tc.setResponse(method.getResponseBodyAsString());
			method.releaseConnection();

		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			log.error(e.getStackTrace());
			tc.setResponse(e.getMessage());
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			log.error(e.getStackTrace());
			tc.setResponse(e.getMessage());
		} catch (NullPointerException e) {
			log.error("NullPointerException: " + e.getMessage());
			log.error(e.getStackTrace());
			tc.setResponse(e.getMessage());
		} catch (IllegalArgumentException e) {
			log.error("IllegalArgumentException: " + e.getMessage());
			log.error(e.getStackTrace());
			tc.setResponse(e.getMessage());
		} 
		return tc;
	}

	public TestCase simplePut(TestCase tc) {

		headerList = parseHeader(tc.getId(), tc.getResttc().getHeaders());

		try{
			HttpClient client = new HttpClient();
			if(config.getTestCase().isProxy()){
				client.getHostConfiguration().setProxy(config.getProxy().getHost(), Integer.parseInt(config.getProxy().getPort().trim()));	
			}

			//client.getParams().setParameter("http.useragent", "Test Client");
			BufferedReader br = null;

			log.info("rest_url: "+tc.getResttc().getPath());
			PutMethod method = new PutMethod(tc.getResttc().getPath().trim());

			//setting request header
			log.info("Setting request header:");
			Iterator itHeader = headerList.iterator();
			while(itHeader.hasNext()){
				Variable var = (Variable) itHeader.next();
				log.info(var.getName()+":"+var.getContent());	
				method.addRequestHeader(var.getName(), var.getContent());
			}

			//String auth = "token";
			//method.setRequestHeader("Authentication", auth);
			//method.addRequestHeader("Authentication", auth);
			//method.setRequestEntity(new StringRequestEntity(body, content-type, charset));

			method.setRequestEntity(new StringRequestEntity(tc.getResttc().getBody(), null, null));

			int returnCode = client.executeMethod(method);
			tc.setResult_status(returnCode);
			log.info(returnCode);

			Header[] header = method.getResponseHeaders();
			tc.setResponseHeaders(header);

			if(returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
				log.error("The Put method is not implemented by this URI.");
				tc.setResponse(method.getResponseBodyAsString());
				log.error(method.getResponseBodyAsString());
			} else {
				if(method.getResponseBodyAsStream()!=null){
					br = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
					String readLine, tmp="";
					while(((readLine = br.readLine()) != null)) {
						log.info(readLine);
						tmp = tmp + readLine+" ";
					}
					tc.setResponse(tmp);
				} else {
					tc.setResponse("");
				}
			}
			method.releaseConnection();
			if(br != null) try { br.close(); } catch (Exception fe) {}
		} catch (Exception e) {
			log.error(e.getStackTrace());
			log.error(e.getMessage());
			tc.setResponse(e.getMessage());
		}
		return tc;
	}

	public TestCase deleteWBody(TestCase tc) {

		headerList = parseHeader(tc.getId(), tc.getResttc().getHeaders());

		try{
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
			schemeRegistry.register(new Scheme("https", 443, new MockSSLSocketFactory()));
			ClientConnectionManager cm = new SingleClientConnManager(schemeRegistry);
			HttpHost proxy = new HttpHost("web-proxy.corp.hp.com", 8080, "http");
			DefaultHttpClient httpclient = new DefaultHttpClient(cm);
			if(config.getTestCase().isProxy()){
				httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
				//httpClient.getHostConfiguration().setProxy(config.getProxy().getHost(), Integer.parseInt(config.getProxy().getPort().trim()));	
			}
			log.info("rest_url: "+tc.getResttc().getPath());
			HttpDeleteWithBody method = new HttpDeleteWithBody(tc.getResttc().getPath().trim());

			//setting request header
			log.info("Setting request header:");
			Iterator itHeader = headerList.iterator();
			while(itHeader.hasNext()){
				Variable var = (Variable) itHeader.next();
				log.info(var.getName()+":"+var.getContent());	
				method.addHeader(var.getName(), var.getContent());
			}
			HttpEntity entity = new StringEntity(tc.getResttc().getBody());
			method.setEntity(entity);

			HttpResponse response = httpclient.execute(method);

			int returnCode = response.getStatusLine().getStatusCode();
			tc.setResult_status(returnCode);
			log.info(response.getStatusLine());

			org.apache.http.Header[] header = response.getAllHeaders();

			Header[] headerlist = new Header[header.length];

			for (int i = 0; i < header.length; i++) {
				Header h = new Header(header[i].getName(), header[i].getValue());
				headerlist[i] = h;
			}

			tc.setResponseHeaders(headerlist);

			if(returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
				log.error("The delete method is not implemented by this URI.");
				tc.setResponse(response.getStatusLine().getReasonPhrase());
				log.error(response.getStatusLine().getReasonPhrase());
			} else {
				if(response!=null){
					HttpEntity entity2 = response.getEntity();
					String responseString = EntityUtils.toString(entity2, "UTF-8");
					log.info(responseString);
					tc.setResponse(responseString);
				} else {
					tc.setResponse("");
				}
			}
		} catch (Exception e) {
			System.out.println(e.getCause());
			log.error(e.getStackTrace());
			log.error(e.getMessage());
			tc.setResponse(e.getMessage());
		}
		return tc;
	}

	public void deleteWBod(){
		try {
			HttpEntity entity = new StringEntity("teste");
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpDeleteWithBody httpDeleteWithBody = new HttpDeleteWithBody("http://10.17.1.72:8080/contacts");
			httpDeleteWithBody.setEntity(entity);
			HttpResponse response = httpClient.execute(httpDeleteWithBody);

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public TestCase simplePost(TestCase tc) {

		String path = "";
		headerList = parseHeader(tc.getId(), tc.getResttc().getHeaders());

		try{

			HttpClient client = new HttpClient();		
			if(config.getTestCase().isProxy()){
				client.getHostConfiguration().setProxy(config.getProxy().getHost(), Integer.parseInt(config.getProxy().getPort().trim()));	
			}

			//client.getParams().setParameter("http.useragent", "Test Client");
			BufferedReader br = null;

			log.info(tc.getResttc().getPath());
			PostMethod method = new PostMethod(tc.getResttc().getPath().trim());

			//setting request header
			log.info("Setting request header:");
			Iterator itHeader = headerList.iterator();
			while(itHeader.hasNext()){
				Variable var = (Variable) itHeader.next();
				log.info(var.getName()+":"+var.getContent());	
				method.addRequestHeader(var.getName(), var.getContent());
			}

			//set header
			//method.addRequestHeader("", tc.getResttc().getHeaders());
			//method.setRequestHeader(new Header("Content-type", "text/xml; charset=\"utf-8\"")); 

			//set body
			//method.addParameter("name", "value");
			method.setRequestEntity(new StringRequestEntity(tc.getResttc().getBody(), null, null));
			log.info(tc.getResttc().getBody());		

			int returnCode = 0, count=0;

			//do {
			returnCode = client.executeMethod(method);
			tc.setResult_status(returnCode);
			log.info(returnCode);

			/*
			 * KLUDGE: to avoid invitation issue HTTP500
			 *
				TimeController ExcelRestTime = new TimeController();
				ExcelRestTime.startTime();
				ExcelRestTime.waitFor(1000);
				count++;

			} while (((returnCode == 500) || (returnCode == 503)) && (count < 5));*/

			Header[] header = method.getResponseHeaders();
			tc.setResponseHeaders(header);

			if(returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
				log.error("The Post method is not implemented by this URI.");
				tc.setResponse(method.getResponseBodyAsString());
				log.error(method.getResponseBodyAsString());
			} else {
				br = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
				String readLine, tmp="";
				while(((readLine = br.readLine()) != null)) {
					log.info(readLine);
					tmp = tmp + readLine+" ";
				}
				tc.setResponse(tmp);
			}
			method.releaseConnection();
			if(br != null) try { br.close(); } catch (Exception fe) {}
		} catch (Exception e) {
			log.error(e.getStackTrace());
			log.error(e.getMessage());
			tc.setResponse(e.getMessage());
		}
		return tc;
	}


	private static class DefaultTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}

	public TestCase simpleOptions(TestCase tc) {

		headerList = parseHeader(tc.getId(), tc.getResttc().getHeaders());

		try{
			HttpClient client = new HttpClient();		
			if(config.getTestCase().isProxy()){
				client.getHostConfiguration().setProxy(config.getProxy().getHost(), Integer.parseInt(config.getProxy().getPort().trim()));	
			}

			BufferedReader br = null;

			log.info("rest_url: "+tc.getResttc().getPath());
			PostMethod method = new PostMethod(tc.getResttc().getPath().trim()){
				@Override public String getName() { return "OPTIONS"; }
			};

			//setting request header
			log.info("Setting request header:");
			Iterator itHeader = headerList.iterator();
			while(itHeader.hasNext()){
				Variable var = (Variable) itHeader.next();
				log.info(var.getName()+":"+var.getContent());	
				method.addRequestHeader(var.getName(), var.getContent());
			}
			method.addRequestHeader("X-Http-Method-Override", "patch");

			method.setRequestEntity(new StringRequestEntity(tc.getResttc().getBody(), null, null));
			log.info(tc.getResttc().getBody());			

			int returnCode = client.executeMethod(method);
			tc.setResult_status(returnCode);
			log.info(returnCode);

			Header[] header = method.getResponseHeaders();
			tc.setResponseHeaders(header);

			if(returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
				log.error("The Options method is not implemented by this URI.");
				tc.setResponse(method.getResponseBodyAsString());
				log.error(method.getResponseBodyAsString());
			} else {
				br = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
				String readLine, tmp="";
				while(((readLine = br.readLine()) != null)) {
					log.info(readLine);
					tmp = tmp + readLine+" ";
				}
				tc.setResponse(tmp);
			}
			method.releaseConnection();
			if(br != null) try { br.close(); } catch (Exception fe) {}
		} catch (Exception e) {
			log.error(e.getStackTrace());
			log.error(e.getMessage());
			tc.setResponse(e.getMessage());
		}
		return tc;
	}	

	public TestCase simplePatch(TestCase tc) {

		headerList = parseHeader(tc.getId(), tc.getResttc().getHeaders());

		try{
			HttpClient client = new HttpClient();		
			if(config.getTestCase().isProxy()){
				client.getHostConfiguration().setProxy(config.getProxy().getHost(), Integer.parseInt(config.getProxy().getPort().trim()));	
			}

			//client.getParams().setParameter("http.useragent", "Test Client");
			BufferedReader br = null;

			log.info("rest_url: "+tc.getResttc().getPath());
			PostMethod method = new PostMethod(tc.getResttc().getPath().trim()){
				@Override public String getName() { return "PATCH"; }
			};

			//setting request header
			log.info("Setting request header:");
			Iterator itHeader = headerList.iterator();
			while(itHeader.hasNext()){
				Variable var = (Variable) itHeader.next();
				log.info(var.getName()+":"+var.getContent());	
				method.addRequestHeader(var.getName(), var.getContent());
			}
			method.addRequestHeader("X-Http-Method-Override", "patch");

			//set header
			//method.addRequestHeader("", tc.getResttc().getHeaders());
			//method.setRequestHeader(new Header("Content-type", "text/xml; charset=\"utf-8\"")); 

			//set body
			//method.addParameter("name", "value");
			method.setRequestEntity(new StringRequestEntity(tc.getResttc().getBody(), null, null));
			log.info(tc.getResttc().getBody());			

			int returnCode = client.executeMethod(method);
			tc.setResult_status(returnCode);
			log.info(returnCode);

			Header[] header = method.getResponseHeaders();
			tc.setResponseHeaders(header);

			if(returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
				log.error("The Post method is not implemented by this URI.");
				tc.setResponse(method.getResponseBodyAsString());
				log.error(method.getResponseBodyAsString());
			} else {
				br = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
				String readLine, tmp="";
				while(((readLine = br.readLine()) != null)) {
					log.info(readLine);
					tmp = tmp + readLine+" ";
				}
				tc.setResponse(tmp);
			}
			method.releaseConnection();
			if(br != null) try { br.close(); } catch (Exception fe) {}
		} catch (Exception e) {
			log.error(e.getStackTrace());
			log.error(e.getMessage());
			tc.setResponse(e.getMessage());
		}
		return tc;
	}

	public TestCase postFile(TestCase tc) {

		headerList = parseHeader(tc.getId(), tc.getResttc().getHeaders());

		try{
			BufferedReader br = null;
			HttpClient client = new HttpClient();

			if(config.getTestCase().isProxy()){
				client.getHostConfiguration().setProxy(config.getProxy().getHost(), Integer.parseInt(config.getProxy().getPort().trim()));	
			}

			log.info("file: "+tc.getResttc().getBody().replace("file=", ""));
			File f = new File(tc.getResttc().getBody().replace("file=", "").trim());

			log.info("rest_url: "+tc.getResttc().getPath());
			PostMethod method = new PostMethod(tc.getResttc().getPath().trim());

			//setting request header
			log.info("Setting request header:");
			Iterator itHeader = headerList.iterator();
			while(itHeader.hasNext()){
				Variable var = (Variable) itHeader.next();
				log.info(var.getName()+":"+var.getContent());	
				method.addRequestHeader(var.getName(), var.getContent());
			}

			//set header
			//method.addRequestHeader("", tc.getResttc().getHeaders());

			log.info("File Length = " + f.length());

			//DEPRECATED - replaced by RequestEntity
			//method.setRequestBody(new FileInputStream(f));

			if(f.exists()) {

				//RequestEntity entity = new FileRequestEntity(f, "application/x-www-form-urlencoded");
				RequestEntity entity = new FileRequestEntity(f, "");
				method.setRequestEntity(entity);

				int returnCode = client.executeMethod(method);
				tc.setResult_status(returnCode);
				log.info(returnCode);

				Header[] header = method.getResponseHeaders();
				tc.setResponseHeaders(header);

				if(returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
					log.error("The Post method is not implemented by this URI.");
					tc.setResponse(method.getResponseBodyAsString());
				} else {
					br = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
					String readLine, tmp="";
					while(((readLine = br.readLine()) != null)) {
						log.info(readLine);
						tmp = tmp + readLine+" ";
					}
					tc.setResponse(tmp);
				}
				method.releaseConnection();
				if(br != null) try { br.close(); } catch (Exception fe) {}

			} else {
				log.error("File not found: "+f.getAbsolutePath());
				System.exit(1);
			}
		} catch (Exception e) {
			log.error(e.getStackTrace());
			log.error(e.getMessage());
			tc.setResponse(e.getMessage());
		}
		return tc;
	}

	public TestCase postMultipartJson(TestCase tc) {

		headerList = parseHeader(tc.getId(), tc.getResttc().getHeaders());

		PostMethod method = new PostMethod(tc.getResttc().getPath().trim());
		method.getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, true);

		try{

			List <Part> parts = new ArrayList<Part>();

			String bodyLines[] = tc.getResttc().getBody().split("\n");
			String fileNames[] = new String[bodyLines.length];  
			File files[] = new File[bodyLines.length];

			for(int i=0; i < bodyLines.length; i++){
				String values[] = bodyLines[i].split("=");
				if(values.length==2){
					files[i] = new File(values[1].trim());
					fileNames[i] = new String(values[0].trim());

					if(files[i].getName().contains(".js")) {
						log.info("JSON-FILE "+i+": Name: "+values[0].trim()+", Path: "+values[1].trim());
						parts.add(new FilePart(fileNames[i], files[i], "application/json", "utf-8"));
					}
					else {
						log.info("FILE "+i+": Name: "+values[0].trim()+", Path: "+values[1].trim());
						parts.add(new FilePart(fileNames[i], files[i]));
					}
				}
			}

			BufferedReader br = null;
			HttpClient client = new HttpClient();

			if(config.getTestCase().isProxy()){
				client.getHostConfiguration().setProxy(config.getProxy().getHost(), Integer.parseInt(config.getProxy().getPort().trim()));	
			}

			log.info("rest_url: "+tc.getResttc().getPath());
			method = new PostMethod(tc.getResttc().getPath().trim());

			//setting request header
			log.info("Setting request header:");
			Iterator itHeader = headerList.iterator();
			while(itHeader.hasNext()){
				Variable var = (Variable) itHeader.next();
				log.info(var.getName()+":"+var.getContent());	
				method.addRequestHeader(var.getName(), var.getContent());
			}

			Part allParts[] = new Part[parts.size()];
			allParts = parts.toArray(allParts);

			MultipartRequestEntity mpEntity = new MultipartRequestEntity(allParts, method.getParams());
			String boundary = mpEntity.getContentType();
			boundary = boundary.substring(boundary.indexOf("boundary=") + 9);

			Header header = new Header("Content-type", "multipart/form-data; type=application/json; boundary=" + boundary);
			method.addRequestHeader(header);
			method.setRequestEntity(mpEntity);

			int returnCode = client.executeMethod(method);

			tc.setResult_status(returnCode);
			log.info(returnCode);

			Header[] headers = method.getResponseHeaders();
			tc.setResponseHeaders(headers);

			if(returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
				log.error("The Post method is not implemented by this URI.");
				tc.setResponse(method.getResponseBodyAsString());
			} else {
				br = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
				String readLine, tmp="";
				while(((readLine = br.readLine()) != null)) {
					log.info(readLine);
					tmp = tmp + readLine+" ";
				}
				tc.setResponse(tmp);
			}
			//method.releaseConnection();
			if(br != null) try { br.close(); } catch (Exception fe) {}

		} catch (Exception e) {
			log.error(e.getStackTrace());
			log.error(e.getMessage());
			tc.setResponse(e.getMessage());
		}
		finally {
			method.releaseConnection();
		}
		return tc;
	}

	public TestCase postMultipleFiles(TestCase tc) {

		headerList = parseHeader(tc.getId(), tc.getResttc().getHeaders());

		PostMethod method = new PostMethod(tc.getResttc().getPath().trim());
		method.getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, true);

		try{
			BufferedReader br = null;
			HttpClient client = new HttpClient();

			if(config.getTestCase().isProxy()){
				client.getHostConfiguration().setProxy(config.getProxy().getHost(), Integer.parseInt(config.getProxy().getPort().trim()));	
			}

			String values[] = tc.getResttc().getBody().split("\n");
			if(values.length==2){

				File f1 = new File(values[0].replace("XML=", "").trim());
				log.info("XML File Length = " + f1.length());
				log.info("XML File Path = "+f1.getAbsolutePath());


				File f2 = new File(values[1].replace("FILE=", "").trim());
				log.info("FILE Length = " + f2.length());
				log.info("FILE Path = "+f2.getAbsolutePath());

				log.info("rest_url: "+tc.getResttc().getPath());
				//method = new PostMethod(tc.getResttc().getPath());

				//setting request header
				log.info("Setting request header:");
				Iterator itHeader = headerList.iterator();
				while(itHeader.hasNext()){
					Variable var = (Variable) itHeader.next();
					log.info(var.getName()+":"+var.getContent());	
					method.addRequestHeader(var.getName(), var.getContent());
				}

				if((f1.exists()) && (f2.exists())) {

					Part[] parts = {
							new FilePart("XML", f1),
							new FilePart("FILE", f2)
					};

					method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));

					client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);

					int returnCode = client.executeMethod(method);
					tc.setResult_status(returnCode);
					log.info(returnCode);

					Header[] header = method.getResponseHeaders();
					tc.setResponseHeaders(header);

					if(returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
						log.error("The Post method is not implemented by this URI.");
						tc.setResponse(method.getResponseBodyAsString());
					} else {
						br = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
						String readLine, tmp="";
						while(((readLine = br.readLine()) != null)) {
							log.info(readLine);
							tmp = tmp + readLine+" ";
						}
						tc.setResponse(tmp);
					}
					//method.releaseConnection();
					if(br != null) try { br.close(); } catch (Exception fe) {}

				} else {
					log.error("One of the files was not found: \n"+f1.getAbsolutePath()+"\n"+f2.getAbsolutePath());
					System.exit(1);
				}
			}
		} catch (Exception e) {
			log.error(e.getStackTrace());
			log.error(e.getMessage());
			tc.setResponse(e.getMessage());
		}
		finally {
			method.releaseConnection();
		}
		return tc;
	}

	public TestCase postKraken(TestCase tc) {

		try {
			final String TRUSTSTORE = "file:src/main/resources/java-trust-store";
			final String TRUSTSTORE_PASS = "ottoprint";
			final String KEYSTORE = "file:src/main/resources/java-trust-store";
			final String KEYSTORE_PASS = "ottoprint";
			BufferedReader br = null;

			File file2 = new File("src/main/resources/java-trust-store");

			Protocol.registerProtocol("https", new Protocol("https", new org.apache.commons.httpclient.contrib.ssl.AuthSSLProtocolSocketFactory(file2.toURI().toURL(), TRUSTSTORE_PASS, file2.toURI().toURL(), KEYSTORE_PASS), 443));

			//final String JSON_STRING = "{\"id\":\"\",\"comics\":[{\"comicId\":\"Pearls\",\"clientId\":\"123\",\"date\":\"2013-09-25\"}]}";

			final String JSON_STRING = tc.getResttc().getBody();
			log.info(tc.getResttc().getBody());	

			HttpClient httpclient = new HttpClient();

			if(config.getTestCase().isProxy()){
				httpclient.getHostConfiguration().setProxy(config.getProxy().getHost(), Integer.parseInt(config.getProxy().getPort().trim()));	
			}

			StringRequestEntity requestEntity = new StringRequestEntity(JSON_STRING, "application/json", "UTF-8");

			//PostMethod postMethod = new PostMethod("https://vrapi.vault-load.cloudpublish.com/vrapi/comics/get");
			log.info("rest_kraken_url: "+tc.getResttc().getPath());
			PostMethod postMethod = new PostMethod(tc.getResttc().getPath().trim());
			postMethod.setRequestEntity(requestEntity);

			int statusCode = httpclient.executeMethod(postMethod);
			tc.setResult_status(statusCode);
			log.info(statusCode);

			if(statusCode == HttpStatus.SC_NOT_IMPLEMENTED) {
				log.error("The Post method is not implemented by this URI.");
				tc.setResponse(postMethod.getResponseBodyAsString());
				log.error(postMethod.getResponseBodyAsString());
			} else {
				br = new BufferedReader(new InputStreamReader(postMethod.getResponseBodyAsStream()));
				String readLine, tmp="";
				while(((readLine = br.readLine()) != null)) {
					log.info(readLine);
					tmp = tmp + readLine+" ";
				}
				tc.setResponse(tmp);
			}	

			//System.out.println("STATUS CODE: " + statusCode);
			//System.out.println(postMethod.getResponseBodyAsString());
			postMethod.releaseConnection();

		} catch (Exception e) {
			log.error(e.getStackTrace());
			log.error(e.getMessage());
			tc.setResponse(e.getMessage());
		}
		return tc;
	}

	public boolean downloadFile(TestCase tc, String urlFile, String localfile){

		int read = 0;
		long write = 0;

		OutputStream OS = null;
		URLConnection connection = null;
		InputStream  IS = null;

		try {
			URL url = new URL(urlFile);

			if(config.getTestCase().isProxy()){
				System.setProperty("http.proxyHost",config.getProxy().getHost().trim());
				System.setProperty("http.proxyPort", config.getProxy().getPort().trim());
				System.setProperty("https.proxyHost",config.getProxy().getHost().trim());
				System.setProperty("https.proxyPort", config.getProxy().getPort().trim());
			}

			connection = url.openConnection();

			log.info("File downloaded at: "+localfile);
			IS = connection.getInputStream();
			OS = new BufferedOutputStream(new FileOutputStream(localfile));
			byte[] buffer = new byte[1024];
			while ((read = IS.read(buffer)) != -1){
				OS.write(buffer, 0, read);
				write += read;
			}

		} catch (Exception e) {
			log.error("Cannot download file."+e.getMessage());
			return false;
		} finally {
			try {
				if (IS != null) { IS.close(); }
				if (OS != null) { OS.close(); }
			} catch (IOException e) {
				log.error("Warning, the connection could not be closed."+e.getMessage());
			}
		}
		return true;
	}

	public void publishMessage(TestCase tc) {

		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUsername(queue_username);
			factory.setPassword(queue_password);
			factory.setVirtualHost(queue_vhost);
			factory.setHost(queue_host);

			Connection conn = factory.newConnection();
			Channel channel = conn.createChannel();

			BasicProperties bp = new BasicProperties();

			log.debug("tc_queue_to_publish: "+tc.getQueuetc().getQueue_to_publish());
			log.debug("tc_message_to_publish: "+tc.getQueuetc().getMessage_to_publish());
			byte[] messageBody = tc.getQueuetc().getMessage_to_publish().getBytes();
			channel.basicPublish("", tc.getQueuetc().getQueue_to_publish(), null, messageBody);

			channel.close();
			conn.close();

		} catch (Exception e) {
			log.error("publishMessage caught exception: " + e);
			log.error(e.getStackTrace());
			System.exit(1);
		}
	}

	public void queuePurge(String queueName) {

		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUsername(queue_username);
			factory.setPassword(queue_password);
			factory.setVirtualHost(queue_vhost);
			factory.setHost(queue_host);
			Connection conn = factory.newConnection();
			Channel channel = conn.createChannel();

			channel.queuePurge(queueName);

			channel.close();
			conn.close();

		} catch (Exception e) {
			log.error("queuePurge caught exception: " + e);
			log.error(e.getStackTrace());
			System.exit(1);
		}
	}

	public TestCase consumeMessage(TestCase tc) {

		try {
			//String uri ="amqp://"+qConfig.getHost();

			ConnectionFactory factory = new ConnectionFactory();
			factory.setUsername(queue_username);
			factory.setPassword(queue_password);
			factory.setVirtualHost(queue_vhost);
			factory.setHost(queue_host);
			Connection conn = factory.newConnection();

			final Channel channel = conn.createChannel();
			QueueingConsumer consumer = new QueueingConsumer(channel);

			log.debug("tc_queue_to_consume: "+tc.getQueuetc().getQueue_to_consume());
			channel.basicConsume(tc.getQueuetc().getQueue_to_consume(), consumer);
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();

			log.debug("tc_message_consumed: "+new String(delivery.getBody()));
			//update the QueueTestCase with the consumed value
			tc.setResponse(new String(delivery.getBody()));
			channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

			channel.close();
			conn.close();

			return tc;

		} catch (Exception e) {
			log.error("consumeMessage caught exception: " + e);
			log.error(e.getStackTrace());
			System.exit(1);
			return tc;
		}
	}

	public boolean isEmpty(String queueName) {

		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUsername(queue_username);
			factory.setPassword(queue_password);
			factory.setVirtualHost(queue_vhost);
			factory.setHost(queue_host);

			Connection conn = factory.newConnection();
			Channel channel = conn.createChannel();

			boolean autoAck = false;
			GetResponse response = channel.basicGet(queueName, autoAck);

			if (response == null) {
				System.out.println("Queue is empty.");
				return true;
			} else {
				AMQP.BasicProperties props = response.getProps();
				System.out.println("The Queue is not Empty. Message: " + new String(response.getBody()));

				long deliveryTag = response.getEnvelope().getDeliveryTag();
				System.out.println(props.toString());
				return false;
			}

		} catch (Exception e) {
			System.err.println("isEmpty caught exception: " + e);
			e.printStackTrace();
			System.exit(1);
			return false;
		}

	}
	public int getNumberOfMessages(String queueName) {

		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUsername(queue_username);
			factory.setPassword(queue_password);
			factory.setVirtualHost(queue_vhost);
			factory.setHost(queue_host);

			Connection conn = factory.newConnection();
			Channel channel = conn.createChannel();

			int queuemessageSize = channel.queueDeclarePassive(queueName).getMessageCount();

			channel.close();
			conn.close();

			return queuemessageSize;

		} catch (Exception e) {
			log.error("getNumberOfMessages caught exception: " + e);
			log.error(e.getStackTrace());
			System.exit(1);
			return 1;
		}

	}


	public Config getConfig() {
		return config;
	}


	public void setConfig(Config config) {
		this.config = config;
	}
}





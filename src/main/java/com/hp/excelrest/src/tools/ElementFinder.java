package com.hp.excelrest.src.tools;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


public class ElementFinder {

	LinkedList<String> elementTextList;
	String elementText = "";

	public ElementFinder() {}
	static Logger log = Logger.getLogger(ElementFinder.class);

	private Element convertStringtoXML(String str) throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
		Reader in = new StringReader(str);
		Document doc = builder.build(in);
		Element rootElement = doc.getRootElement();
		return rootElement;
	}

	public void search(Element element, String keyElement){

		List childrens = element.getChildren();
		Iterator c = childrens.iterator();
		while(c.hasNext()) {  
			Element children = (Element) c.next();
			if(children.getName().equalsIgnoreCase(keyElement)){
				//elementTextList.add(children.getText());
				elementText = children.getText();
				
			}
			search(children, keyElement);
		}
	}

	public String finder(String str, String keyElement) {

		//elementTextList = new LinkedList<String>();
		try {
			Element rootElement = convertStringtoXML(str);
			search(rootElement, keyElement);
		}
		catch (JDOMException e) {
			log.error("Error reading XML: "+e.getMessage());
			return "Error reading XML: "+e.getMessage();
		}
		catch (IOException e) {
			log.error("Error reading XML: "+e.getMessage());
			return "Error reading XML: "+e.getMessage();
			
		}
		//System.out.println(elementTextList.toString());
		return elementText;

	}

	public static void main(String[] args) {

		ElementFinder main = new ElementFinder();
		main.finder("<email><smtpName>16.234.33.148</smtpName><smtpName>16.234.33.148</smtpName></email>", "smtpName");

	}
}


package com.hp.excelrest.src.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;

import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.format.UnderlineStyle;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableHyperlink;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import org.apache.commons.httpclient.Header;
import org.apache.log4j.Logger;

import com.hp.excelrest.src.config.Config;
import com.hp.excelrest.src.rallydev.Defect;
import com.hp.excelrest.src.testcase.QueueTestCase;
import com.hp.excelrest.src.testcase.RestTestCase;
import com.hp.excelrest.src.testcase.TestCase;
import com.hp.excelrest.src.testcase.Variable;


public class JXLRead_Write {

	private RestTestCase rest;
	private QueueTestCase qtc;
	private TestCase tc;
	private LinkedList<TestCase> tcs = new LinkedList<TestCase>();
	static Logger log = Logger.getLogger(JXLRead_Write.class);

	public WritableCellFormat formatResult(String result){
		WritableCellFormat format = null;
		WritableFont arial12 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD,false, UnderlineStyle.NO_UNDERLINE, Colour.BLACK);

		try {
			if(result.equalsIgnoreCase("PASS")) {
				format = new WritableCellFormat(arial12);
				format.setBackground(Colour.GREEN);
				format.setWrap(true);
				format.setBorder(Border.ALL, BorderLineStyle.THIN,Colour.BLACK); //table border style
				format.setAlignment(Alignment.CENTRE);
			}
			//if FAIL set cell background red
			else if(result.equalsIgnoreCase("FAIL")) { 
				format = new WritableCellFormat(arial12);
				format.setWrap(true);
				format.setBorder(Border.ALL, BorderLineStyle.THIN,Colour.BLACK); //table border style
				format.setBackground(Colour.RED);
				format.setAlignment(Alignment.CENTRE);
			} 		
			else { 
				format = new WritableCellFormat(arial12);
				format.setWrap(true);
				format.setBorder(Border.ALL, BorderLineStyle.THIN,Colour.BLACK); //table border style
				format.setBackground(Colour.WHITE);
				format.setAlignment(Alignment.LEFT);
			}

		} catch(Exception e) {
			log.error(e.getMessage());
			log.error(e.getStackTrace());
			System.exit(1);
		}
		return format;
	}

	public void writeTestResultsReduced(String fileLocation, LinkedList<TestCase> tcs) {

		try {
			log.info("Result File Location: "+fileLocation);
			File file = new File(fileLocation);
			WritableWorkbook ww;
			Workbook workbook;
			boolean fileExist = file.exists();

			if(!file.exists())  {
				ww = Workbook.createWorkbook(file);
			} else {
				workbook = Workbook.getWorkbook(file);
				ww = Workbook.createWorkbook(new File("./temp-reduced.xls"), workbook);
			}

			Utils utils = new Utils();
			ww.createSheet(utils.getCurrentTimeStamp(), 0);
			WritableSheet excelSheet = ww.getSheet(0);

			//define font style
			WritableFont arial12 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD,false, UnderlineStyle.NO_UNDERLINE, Colour.BLACK);

			//write sheet header
			WritableCellFormat headerFormat = new WritableCellFormat(arial12);
			headerFormat.setBackground(Colour.GRAY_25) ; //Table background
			headerFormat.setBorder(Border.ALL, BorderLineStyle.THIN,Colour.BLACK); //table border style
			//arial.setFont(arial12); //set the font
			headerFormat.setAlignment(Alignment.CENTRE);// set alignment left
			headerFormat.setWrap(true);
			//CellView cv = new CellView();
			//cv.setFormat(arial);

			//writing header in the result xls file
			log.debug("writing header...");
			String header[] = {
					"tc_id",
					"tc_name",
					"tc_full_path",
					"tc_method",
					"tc_body",
					"tc_header",
					"response_status",
					"response",
					"result",
					"result_description (only failures)",
					"defect_number",
			"dependencies"};

			for(int i=0; i<header.length; i++) {
				log.debug("Column: "+i+1);
				log.debug("Row: 0");
				log.debug("Cel Content: "+header[i]);

				Label l = new Label(i, 0, header[i], headerFormat);
				excelSheet.addCell(l);
			}

			//write test case results into the sheet
			WritableCellFormat tcFormat;
			String result, assertion;

			log.debug("writing results...");
			for(int row=0; row<tcs.size(); row++) {

				if(tcs.get(row).isResult_success()) result = "PASS";
				else result = "FAIL";

				if(!tcs.get(row).isExecutable()) result="N/A";

				assertion="";
				for(int i=0; i < tcs.get(row).getAssertions().size(); i++){
					assertion = assertion + tcs.get(row).getAssertions().get(i).getType() +"="+ tcs.get(row).getAssertions().get(i).getContent();
					assertion = assertion+"\n";
				}

				String hdrs = "";
				if(tcs.get(row).getResponseHeaders()!=null)
					for (Header h : tcs.get(row).getResponseHeaders()) {
						hdrs += h.getName()+": "+h.getValue()+"\n";
					} 

				if(!tcs.get(row).getResponseHeadersStr().isEmpty()){
					hdrs+= tcs.get(row).getResponseHeadersStr();
				}

				String dependencies = "";
				if(tcs.get(row).getVars()!=null)
					for (Variable v : tcs.get(row).getVars()) {
						dependencies += v.getContent()+"\n";
					}

				String reducedResponseBody = tcs.get(row).getResponse();
				if(reducedResponseBody != null) 
					if(reducedResponseBody.length() > 1000) 
						reducedResponseBody = reducedResponseBody.substring(0, 1000);
				
				String qtc[] = {
						tcs.get(row).getId(),
						tcs.get(row).getName(),
						tcs.get(row).getResttc().getPath(),
						tcs.get(row).getResttc().getMethod(),
						tcs.get(row).getResttc().getBody(),
						tcs.get(row).getResttc().getHeaders(),
						Integer.toString(tcs.get(row).getResult_status()),
						reducedResponseBody,
						result,
						tcs.get(row).getResultDescriptionFailures(),
						tcs.get(row).getDefect_number(),
						dependencies};

				for(int column=0; column<qtc.length; column++) {
					if(qtc[column]!=null) {
						log.debug("Column: "+column+1);
						log.debug("Row: "+row+1);
						log.debug("Cel Content: "+qtc[column]);

						if(qtc[column].length() > 32000)
							qtc[column] = qtc[column].substring(0, 32000);

						Label l = new Label(column, row+1, qtc[column], formatResult(qtc[column]));
						excelSheet.addCell(l);
					}

				}
			}

			ww.write();
			ww.close();

			if(fileExist){

				workbook = Workbook.getWorkbook(new File("./temp-reduced.xls"));
				ww = Workbook.createWorkbook(file, workbook);
				ww.write();
				ww.close();
				workbook.close();

				boolean success = (new File("./temp-reduced.xls")).delete();
				if (!success) {
					log.warn("Deletion of temp file failed!");
				}
			}

		} catch(Exception e) {
			log.error(e.getMessage());
			log.error(e.getStackTrace());
			log.error(e.fillInStackTrace());
			log.error(e.getCause());
			System.exit(1);
		}
	}

	public void writeTestFailures(String fileLocation, LinkedList<TestCase> tcs, LinkedList<String> tcsToWrite) {

		try {
			log.info("Result File Location: "+fileLocation);
			File file = new File(fileLocation);
			WritableWorkbook ww;
			Workbook workbook;
			boolean fileExist = file.exists();

			if(!file.exists())  {
				ww = Workbook.createWorkbook(file);
			} else {
				workbook = Workbook.getWorkbook(file);
				ww = Workbook.createWorkbook(new File("./temp-failures.xls"), workbook);
			}

			Utils utils = new Utils();
			ww.createSheet(utils.getCurrentTimeStamp(), 0);
			WritableSheet excelSheet = ww.getSheet(0);

			//define font style
			WritableFont arial12 = new WritableFont(WritableFont.ARIAL, 12, WritableFont.BOLD,false, UnderlineStyle.NO_UNDERLINE, Colour.BLACK);

			//write sheet header
			WritableCellFormat headerFormat = new WritableCellFormat(arial12);
			headerFormat.setBackground(Colour.GRAY_25) ; //Table background
			headerFormat.setBorder(Border.ALL, BorderLineStyle.THIN,Colour.BLACK); //table border style
			//arial.setFont(arial12); //set the font
			headerFormat.setAlignment(Alignment.CENTRE);// set alignment left
			headerFormat.setWrap(true);
			//CellView cv = new CellView();
			//cv.setFormat(arial);

			//writing header in the result xls file
			log.debug("writing header...");
			String header[] = {
					"tc_id",
					"tc_name",
					"tc_full_path",
					"tc_method",
					"tc_body",
					"tc_header",
					"resp_status",
					"response_header",
					"response (5000 chars)",
					"result",
					"result_description",
					"defect",
			"dependencies"};

			for(int i=0; i<header.length; i++) {
				log.debug("Column: "+i+1);
				log.debug("Row: 0");
				log.debug("Cel Content: "+header[i]);

				Label l = new Label(i, 0, header[i], headerFormat);
				excelSheet.addCell(l);
			}

			//write test case results into the sheet
			WritableCellFormat tcFormat;
			String result, assertion;

			log.debug("writing results...");

			int line = 0;
			for(int row=0; row<tcs.size(); row++) {

				if(tcsToWrite.contains(tcs.get(row).getId())){

					if(tcs.get(row).isResult_success()) result = "PASS";
					else result = "FAIL";

					if(!tcs.get(row).isExecutable()) result="N/A";

					assertion="";
					for(int i=0; i < tcs.get(row).getAssertions().size(); i++){
						assertion = assertion + tcs.get(row).getAssertions().get(i).getType() +"="+ tcs.get(row).getAssertions().get(i).getContent();
						assertion = assertion+"\n";
					}

					String hdrs = "";
					if(tcs.get(row).getResponseHeaders()!=null)
						for (Header h : tcs.get(row).getResponseHeaders()) {
							hdrs += h.getName()+": "+h.getValue()+"\n";
						} 

					if(!tcs.get(row).getResponseHeadersStr().isEmpty()){
						hdrs+= tcs.get(row).getResponseHeadersStr();
					}

					String dependencies = "";
					if(tcs.get(row).getVars()!=null)
						for (Variable v : tcs.get(row).getVars()) {
							dependencies += v.getContent()+"\n";
						} 

					String qtc[] = {tcs.get(row).getId(),
							tcs.get(row).getName(),
							tcs.get(row).getResttc().getPath(),
							tcs.get(row).getResttc().getMethod(),
							tcs.get(row).getResttc().getBody(),
							tcs.get(row).getResttc().getHeaders(),
							Integer.toString(tcs.get(row).getResult_status()),
							hdrs,
							tcs.get(row).getResponse(),
							result,
							tcs.get(row).getResultDescriptionFailures(),
							tcs.get(row).getDefect_number(),
							dependencies};

					for(int column=0; column<qtc.length; column++) {
						if(qtc[column]!=null) {
							log.debug("Column: "+column+1);
							log.debug("Row: "+line+1);
							log.debug("Cel Content: "+qtc[column]);

							if(qtc[column].length() > 32000)
								qtc[column] = qtc[column].substring(0, 32000);

							Label l = new Label(column, line+1, qtc[column], formatResult(qtc[column]));
							excelSheet.addCell(l);
						}

					}
					line++;
				}

			}

			ww.write();
			ww.close();

			if(fileExist){

				workbook = Workbook.getWorkbook(new File("./temp-failures.xls"));
				ww = Workbook.createWorkbook(file, workbook);
				ww.write();
				ww.close();
				workbook.close();

				boolean success = (new File("./temp-failures.xls")).delete();
				if (!success) {
					log.warn("Deletion of temp file failed!");
				}
			}

		} catch(Exception e) {
			log.error(e.getMessage());
			log.error(e.getStackTrace());
			log.error(e.fillInStackTrace());
			log.error(e.getCause());
			System.exit(1);
		}
	}

	public void writeTestResults(String fileLocation, LinkedList<TestCase> tcs) {

		try {
			log.info("Result File Location: "+fileLocation);
			File file = new File(fileLocation);
			WritableWorkbook ww;
			Workbook workbook;
			boolean fileExist = file.exists();

			if(!file.exists())  {
				ww = Workbook.createWorkbook(file);
			} else {
				workbook = Workbook.getWorkbook(file);
				ww = Workbook.createWorkbook(new File("./temp.xls"), workbook);
			}

			Utils utils = new Utils();
			ww.createSheet(utils.getCurrentTimeStamp(), 0);
			WritableSheet excelSheet = ww.getSheet(0);

			//define font style
			WritableFont arial12 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD,false, UnderlineStyle.NO_UNDERLINE, Colour.BLACK);

			//write sheet header
			WritableCellFormat headerFormat = new WritableCellFormat(arial12);
			headerFormat.setBackground(Colour.GRAY_25) ; //Table background
			headerFormat.setBorder(Border.ALL, BorderLineStyle.THIN,Colour.BLACK); //table border style
			//arial.setFont(arial12); //set the font
			headerFormat.setAlignment(Alignment.CENTRE);// set alignment left
			headerFormat.setWrap(true);
			//CellView cv = new CellView();
			//cv.setFormat(arial);

			//writing header in the result xls file
			log.debug("writing header...");
			String header[] = {
					"rallydev_id",
					"tc_id",
					"tc_name",
					"tc_description",
					"tc_full_path",
					"tc_method",
					"tc_body",
					"tc_header",
					// "tc_queue_to_publish",
					// "tc_message_to_publish",
					// "tc_queue_to_consume",
					"tc_assertions",
					"response_status",
					"response_header",
					"response",
					"result",
					"result_description",
					"result_date",
					"executed",
					"defect_number",
			"dependencies"};

			for(int i=0; i<header.length; i++) {
				log.debug("Column: "+i+1);
				log.debug("Row: 0");
				log.debug("Cel Content: "+header[i]);

				Label l = new Label(i, 0, header[i], headerFormat);
				excelSheet.addCell(l);
			}

			//write test case results into the sheet
			WritableCellFormat tcFormat;
			String result, assertion;

			log.debug("writing results...");
			for(int row=0; row<tcs.size(); row++) {

				if(tcs.get(row).isResult_success()) result = "PASS";
				else result = "FAIL";

				if(!tcs.get(row).isExecutable()) result="N/A";

				assertion="";
				for(int i=0; i < tcs.get(row).getAssertions().size(); i++){
					assertion = assertion + tcs.get(row).getAssertions().get(i).getType() +"="+ tcs.get(row).getAssertions().get(i).getContent();
					assertion = assertion+"\n";
				}

				String hdrs = "";
				if(tcs.get(row).getResponseHeaders()!=null)
					for (Header h : tcs.get(row).getResponseHeaders()) {
						hdrs += h.getName()+": "+h.getValue()+"\n";
					} 

				if(!tcs.get(row).getResponseHeadersStr().isEmpty()){
					hdrs+= tcs.get(row).getResponseHeadersStr();
				}

				String dependencies = "";
				if(tcs.get(row).getVars()!=null)
					for (Variable v : tcs.get(row).getVars()) {
						dependencies += v.getContent()+"\n";
					} 

				String qtc[] = {tcs.get(row).getRallyDevId(),
						tcs.get(row).getId(),
						tcs.get(row).getName(),
						tcs.get(row).getDescription(),
						tcs.get(row).getResttc().getPath(),
						tcs.get(row).getResttc().getMethod(),
						tcs.get(row).getResttc().getBody(),
						tcs.get(row).getResttc().getHeaders(),
						// tcs.get(row).getQueuetc().getQueue_to_publish(), 
						// tcs.get(row).getQueuetc().getMessage_to_publish(),
						// tcs.get(row).getQueuetc().getQueue_to_consume(),
						assertion,
						Integer.toString(tcs.get(row).getResult_status()),
						//tcs.get(row).getResponseHeaders().toString(),
						hdrs,
						tcs.get(row).getResponse(),
						result,
						tcs.get(row).getResult_description(),
						tcs.get(row).getResult_date(),
						new Boolean(tcs.get(row).isExecutable()).toString(),
						tcs.get(row).getDefect_number(),
						dependencies};

				for(int column=0; column<qtc.length; column++) {
					if(qtc[column]!=null) {
						log.debug("Column: "+column+1);
						log.debug("Row: "+row+1);
						log.debug("Cel Content: "+qtc[column]);

						if(qtc[column].length() > 32000)
							qtc[column] = qtc[column].substring(0, 32000);

						Label l = new Label(column, row+1, qtc[column], formatResult(qtc[column]));
						excelSheet.addCell(l);
					}

				}
			}

			ww.write();
			ww.close();

			if(fileExist){

				workbook = Workbook.getWorkbook(new File("./temp.xls"));
				ww = Workbook.createWorkbook(file, workbook);
				ww.write();
				ww.close();
				workbook.close();

				boolean success = (new File("./temp.xls")).delete();
				if (!success) {
					log.warn("Deletion of temp file failed!");
				}
			}

		} catch(Exception e) {
			log.error(e.getMessage());
			log.error(e.getStackTrace());
			System.exit(1);
		}
	}

	public void writeDefectReport(Config config, String projectId, LinkedList<Defect> defList){

		Utils util = new Utils();
		try {
			log.info("Result File Location: "+config.getDefect().getOutput());
			File file = new File(config.getDefect().getOutput());
			WritableWorkbook ww = Workbook.createWorkbook(file);
			ww.createSheet("Defect_Report", 0);
			WritableSheet excelSheet = ww.getSheet(0);

			WritableFont arial12 = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD,false, UnderlineStyle.NO_UNDERLINE, Colour.BLACK);

			/********************************************* Writing sheet header *********************************************/
			WritableCellFormat headerFormat = new WritableCellFormat(arial12);
			headerFormat.setBackground(Colour.GRAY_25);
			headerFormat.setBorder(Border.ALL, BorderLineStyle.THIN,Colour.BLACK);
			headerFormat.setAlignment(Alignment.CENTRE);
			headerFormat.setWrap(true);
			log.debug("writing header...");
			String header[] = {"ObjectID",
					"FormattedID",
					"Name",
					"State",
					"Priority",
					"Severity",
					"Owner",
					"Submitter",
					"Blocked",
					"Submitter",
					"Release",
					"Iteration",
					"FixedInBuild",
					"VerifiedInBuild",
					"FoundInBuild",
					"CreationDate",
					"Environment",
			"Resolution"};


			for(int i=0; i<header.length; i++) {
				log.debug("Column: "+i+1);
				log.debug("Row: 0");
				log.debug("Cel Content: "+header[i]);

				Label l = new Label(i, 0, header[i], headerFormat);
				excelSheet.addCell(l);
			}
			/**************************************************************************************************************/

			/****************************************** Writing sheet content *********************************************/
			WritableCellFormat tcFormat;
			WritableFont newFont;
			log.debug("writing defect into the sheet...");
			for(int row=0; row<defList.size(); row++) {

				Date d = util.parse(defList.get(row).getCreationDate());

				String defect[] = {defList.get(row).getObjectID(),
						defList.get(row).getFormattedID(),
						defList.get(row).getName(),
						defList.get(row).getState(),
						defList.get(row).getPriority(),
						defList.get(row).getSeverity(),
						defList.get(row).getOwner(),
						defList.get(row).getSubmitter(),
						defList.get(row).getBlocked(),
						defList.get(row).getSubmitter(),
						defList.get(row).getRelease(),
						defList.get(row).getIteration(),
						defList.get(row).getFixedInBuild(),
						defList.get(row).getVerifiedInBuild(),
						defList.get(row).getFoundInBuild(),
						util.format(d),
						defList.get(row).getEnvironment(),
						defList.get(row).getResolution()};

				for(int column=0; column<defect.length; column++) {

					if(defect[column]!=null) {

						if((defect[column].equalsIgnoreCase("true"))||(defect[column].equalsIgnoreCase("Crash/Data Loss"))){ 
							newFont = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD,false, UnderlineStyle.NO_UNDERLINE, Colour.RED);
						} else { 
							newFont = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD,false, UnderlineStyle.NO_UNDERLINE, Colour.BLACK);
						} 

						tcFormat = new WritableCellFormat(newFont);
						tcFormat.setWrap(false);
						tcFormat.setBorder(Border.ALL, BorderLineStyle.THIN,Colour.BLACK);
						tcFormat.setBackground(Colour.WHITE);
						tcFormat.setAlignment(Alignment.LEFT);

						if(column==0){
							log.debug("Column: "+column+1);
							log.debug("Row: "+row+1);
							log.debug("Cel Content: "+defect[column]);
							URL  url = new URL(config.getRallyDev().getUrl()+"#/"+projectId+"/detail/defect/"+defect[column]);
							WritableHyperlink wh = new WritableHyperlink(column, row+1, column, row+1, url);
							log.debug("URL: "+url.toString());
							excelSheet.addHyperlink(wh);
						}
						else{
							log.debug("Column: "+column+1);
							log.debug("Row: "+row+1);
							log.debug("Cel Content: "+defect[column]);
							Label l = new Label(column, row+1, defect[column], tcFormat);
							excelSheet.addCell(l);
						}
					}

				}
			}
			/**************************************************************************************************************/

			ww.write();
			ww.close();

		} catch(Exception e) {
			log.error(e.getMessage());
			log.error(e.getStackTrace());
			System.exit(1);
		}
	}

}
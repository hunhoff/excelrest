
package com.hp.excelrest.src.tools;

import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingDeque;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.apache.log4j.Logger;
import org.json.simple.parser.JSONParser;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;

public class Utils {

	public Utils(){}
	static Logger log = Logger.getLogger(Utils.class);
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	public String getCurrentTimeStamp() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

	public String getCurrentimeStampISO8601() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}
	
	public String dateToXsdDatetimeFormatter (String timeZone)  {
		Calendar date = Calendar.getInstance();
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		return(format(date.getTime()));
	}
	public String dateToXsdDatetimeFormatter ()  {
		Calendar date = Calendar.getInstance();
		return(format(date.getTime()));
	}

	public synchronized Date parse(String xmlDateTime) throws ParseException  {
		if ( xmlDateTime.length() != 25 )  {
			throw new ParseException("Date not in expected xml datetime format", 0);
		}

		StringBuilder sb = new StringBuilder(xmlDateTime);
		sb.deleteCharAt(22);
		return simpleDateFormat.parse(sb.toString());
	}

	public synchronized String format(Date xmlDateTime) throws IllegalFormatException  {
		String s =  simpleDateFormat.format(xmlDateTime);
		StringBuilder sb = new StringBuilder(s);
		sb.insert(22, ':');
		return sb.toString();
	}

	public String jsonKeyFinder(String json, String key, int listPosition){

		JSONParser parser = new JSONParser();  
		KeyFinder finder = new KeyFinder();  
		finder.setMatchKey(key);

		//if the listPosition is not informed the value should be -1, else try to match with the listPosition 
		int count = -1;
		if(listPosition < 0) count=listPosition-1;

		try{    
			while(!finder.isEnd()){      
				parser.parse(json, finder, true);     
				if(finder.isFound()){ 
					count++;
					if(count==listPosition) {
						return ""+finder.getValue();
					}
					finder.setFound(false);        
					log.info("found "+key+":"+finder.getValue());
				}    
			}             
		}  catch (org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		}
		return ""+finder.getValue();
	}

	public String xmlElementFinder(String str, String keyElement){

		ElementFinder ef = new ElementFinder();
		return ""+ef.finder(str, keyElement);

	}
	public boolean compareTiffImages(String img1, String img2) throws IOException {

		File f1 = new File(img1);
		File f2 = new File(img2);

		if(f1.exists()&&f2.exists()){ 

			FileSeekableStream stream = new FileSeekableStream(img1);
			TIFFDecodeParam decodeParam = new TIFFDecodeParam();
			decodeParam.setDecodePaletteAsShorts(true);
			ParameterBlock params = new ParameterBlock();
			params.add(stream);
			RenderedOp img = JAI.create("tiff", params);
			BufferedImage image1  = img.getAsBufferedImage();

			stream = new FileSeekableStream(img2);
			decodeParam = new TIFFDecodeParam();
			decodeParam.setDecodePaletteAsShorts(true);
			params = new ParameterBlock();
			params.add(stream);
			img = JAI.create("tiff", params);
			BufferedImage image2  = img.getAsBufferedImage();

			if(image1.getWidth()  != image2.getWidth() || image1.getHeight() != image2.getHeight()){
				log.error("The image '"+img1+"' is different of the expected ('"+img2+"')");
				return false;
			}
			else {
				for(int x = 0; x < image1.getWidth(); x++){
					for(int y = 0; y < image1.getHeight(); y++){
						if(image1.getRGB(x, y)!= image2.getRGB(x, y)){
							log.error("The image '"+img1+"' is different of the expected ('"+img2+"')");
							y = image1.getHeight();
							x = image1.getWidth();
							return false;
						}
					}
				}
			}
			log.info("The image '"+img1+"' is equal of the expected ('"+img2+"')");
			return true;
		} else {
			log.error("One of the files were not found!");
			return false;
		}
	}

	public boolean compareImages(String img1, String img2) throws IOException {

		File f1 = new File(img1);
		File f2 = new File(img2);

		if(f1.exists()&&f2.exists()){ 

			BufferedImage image1 = ImageIO.read(f1); 
			BufferedImage image2 = ImageIO.read(f2); 

			if(image1.getWidth()  != image2.getWidth() || image1.getHeight() != image2.getHeight()){
				log.error("The image '"+img1+"' is different of the expected ('"+img2+"')");
				return false;
			}
			else {
				for(int x = 0; x < image1.getWidth(); x++){
					for(int y = 0; y < image1.getHeight(); y++){
						if(image1.getRGB(x, y)!= image2.getRGB(x, y)){
							log.error("The image '"+img1+"' is different of the expected ('"+img2+"')");
							y = image1.getHeight();
							x = image1.getWidth();
							return false;
						}
					}
				}
			}
			log.info("The image '"+img1+"' is equal of the expected ('"+img2+"')");
			return true;
		} else {
			log.error("One of the files were not found!");
			return false;
		}
	}

	public String subStringBetween(String sentence, String before, String after) {

		sentence = sentence.replaceAll(" ", "").replaceAll("\n", "").replaceAll("\t", "").trim();
		
		int startSub = subStringStartIndex(sentence, before);
		int stopSub = subStringEndIndex(sentence, after);

		String newWord = sentence.substring(startSub, stopSub);
		return newWord;
	}

	public int subStringStartIndex(String sentence, String delimiterBeforeWord) {

		int startIndex = 0;
		String newWord = "";
		int x = 0, y = 0;

		for (int i = 0; i < sentence.length(); i++) {
			newWord = "";

			if (sentence.charAt(i) == delimiterBeforeWord.charAt(0)) {
				startIndex = i;
				for (int j = 0; j < delimiterBeforeWord.length(); j++) {
					try {
						if (sentence.charAt(startIndex) == delimiterBeforeWord.charAt(j)) {
							newWord = newWord + sentence.charAt(startIndex);
						}
						startIndex++;
					} catch (Exception e) {
					}

				}
				if (newWord.equals(delimiterBeforeWord)) {
					x = startIndex;
				}
			}
		}
		return x;
	}

	public int subStringEndIndex(String sentence, String delimiterAfterWord) {

		int startIndex = 0;
		String newWord = "";
		int x = 0;

		for (int i = 0; i < sentence.length(); i++) {
			newWord = "";

			if (sentence.charAt(i) == delimiterAfterWord.charAt(0)) {
				startIndex = i;
				for (int j = 0; j < delimiterAfterWord.length(); j++) {
					try {
						if (sentence.charAt(startIndex) == delimiterAfterWord.charAt(j)) {
							newWord = newWord + sentence.charAt(startIndex);
						}
						startIndex++;
					} catch (Exception e) {
					}

				}
				if (newWord.equals(delimiterAfterWord)) {
					x = startIndex;
					x = x - delimiterAfterWord.length();
				}
			}
		}
		return x;
	}

	public static void main(String[] args){

		Utils util = new Utils();
		
		System.out.println(util.getCurrentimeStampISO8601());
		
		
		//String json = "{\"invitationOrgTypeList\":[{\"invitationType\":\"PSP_ADMIN\",\"organizationType\":\"PSP\",\"gbuUnits\":[\"Indigo\",\"Scitex\",\"IHPS\",\"Designjet\",\"Latex\"],\"systemRoles\":null},{\"invitationType\":\"RESELLER_ADMIN\",\"organizationType\":\"Channel\",\"gbuUnits\":null,\"systemRoles\":null},{\"invitationType\":\"USER_IN_ORG\",\"organizationType\":\"HP\",\"gbuUnits\":null,\"systemRoles\":[{\"id\":\"3\",\"name\":\"HP Operation\",\"description\":\"GBUs operation personnel\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/3\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/3\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/3\"}}},{\"id\":\"9\",\"name\":\"HP Sales and Marketing\",\"description\":\"GBUs sales and marketing personnel\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/9\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/9\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/9\"}}},{\"id\":\"12\",\"name\":\"Genesis Admin\",\"description\":\"Handle administration activities of genesis\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/12\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/12\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/12\"}}},{\"id\":\"15\",\"name\":\"Genesis Support\",\"description\":\"Handle service calls related directly for Genesis (i.e., not presses)\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/15\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/15\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/15\"}}},{\"id\":\"18\",\"name\":\"Genesis R&D\",\"description\":\"Handle development of Genesis components\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/18\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/18\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/18\"}}},{\"id\":\"21\",\"name\":\"CE Level 1\",\"description\":\"GBUs service personnel\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/21\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/21\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/21\"}}},{\"id\":\"24\",\"name\":\"CE Level 2\",\"description\":\"GBUs service personnel\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/24\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/24\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/24\"}}},{\"id\":\"27\",\"name\":\"Application Engineer\",\"description\":\"GBUs service personnel\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/27\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/27\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/27\"}}},{\"id\":\"30\",\"name\":\"Ramp Up Engineer\",\"description\":\"GBUs service personnel\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/30\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/30\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/30\"}}},{\"id\":\"33\",\"name\":\"Support Engineer\",\"description\":\"GBUs service personnel\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/33\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/33\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/33\"}}},{\"id\":\"36\",\"name\":\"Remote Support Engineer\",\"description\":\"GBUs service personnel\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/36\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/36\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/36\"}}},{\"id\":\"39\",\"name\":\"Specialist\",\"description\":\"GBUs service personnel\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/39\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/39\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/39\"}}},{\"id\":\"42\",\"name\":\"Certification Centers\",\"description\":\"Personnel supporting GBU marketing\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/42\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/42\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/42\"}}},{\"id\":\"45\",\"name\":\"Supplies and Media Personnel\",\"description\":\"GBU marketing personnel\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/45\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/45\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/45\"}}},{\"id\":\"134\",\"name\":\"HP Service\",\"description\":\"Service Administration teams (GBU and WW)\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/134\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/134\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/134\"}}},{\"id\":\"137\",\"name\":\"HP Training\",\"description\":\"Training personnel (GBU and WW training centers)\",\"pspRole\":false,\"hpRole\":true,\"channelRole\":false,\"deviceRole\":false,\"pspHMACRole\":false,\"hpHMACRole\":false,\"channelHMACRole\":false,\"_links\":{\"self\":{\"op\":\"GET\",\"href\":\"/api/aaa/v1/systemroles/137\"},\"update\":{\"op\":\"PUT\",\"href\":\"/api/aaa/v1/systemroles/137\"},\"delete\":{\"op\":\"DELETE\",\"href\":\"/api/aaa/v1/systemroles/137\"}}}]}],\"size\":3,\"_links\":null}";
		//String s = util.jsonKeyFinder(json, "id", 2);
		//System.out.println(s);
		//System.out.println(util.dateToXsdDatetimeFormatter());
		//System.out.println(util.dateToXsdDatetimeFormatter("GMT"));
		//String url = "https://region-a.geo-1.objects.hpcloudsvc.com/v1/10026054891113/production/265040e8-b140-4ee0-ad94-808c347a6e52?temp_url_sig=10026054891113%3AF45PS3GCK3YRGHY6L6VX%3A19c4bb62a982695265fa5fb049ede40affc28c6b&temp_url_expires=1396479134825";
		//util.downloadFile(url, "c:/test", "web-proxy", "8088");
		/*String url1 = "https://region-a.geo-1.objects.hpcloudsvc.com/v1/10026054891113/TestingArea/09222866-4e54-4ee5-9b12-5f93f802d468?temp_url_sig=10026054891113%3AF45PS3GCK3YRGHY6L6VX%3Aac6a52f6d96d186b82545299eac23008889eeb21&temp_url_expires=1395435802";
		util.downloadFile(url1,"c:/test1.pdf", "web-proxy", "8088");

		 */
	}
}

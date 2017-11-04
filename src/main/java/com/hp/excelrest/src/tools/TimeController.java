
package com.hp.excelrest.src.tools;

import org.apache.log4j.Logger;

/**
 *******************************************************************************
 *Class responsible to calculate the Test Server execution time. 
 *@author      Alessandro Hunhoff 
 *@author 	   alessandro.hunhoff@hp.com 
 *@since       1.0.0
 *@version     1.0.23
 *******************************************************************************
 **/

public class TimeController {

	private double startTime;
	private double stopTime;
	private double elapsedTime;
	static Logger log = Logger.getLogger(TimeController.class);

	/**
	 *************************************************************************************************
	 * Starts a timer.
	 *************************************************************************************************
	 **/
	public void startTime(){
		this.startTime = 0.0;
		this.startTime = (double)System.currentTimeMillis() / 1000D;
	}

	/**
	 *************************************************************************************************
	 * calculates the time expended between the start and now. 
	 * @return double value containing the elapsed time.
	 *************************************************************************************************
	 **/
	public double getElapsedTime(){
		this.stopTime = 0.0;
		this.stopTime = (double)System.currentTimeMillis() / 1000D;
		elapsedTime = (this.stopTime - this.startTime);
		if(elapsedTime >= 3600.0) elapsedTime = elapsedTime/3600.0;
		return elapsedTime;
	}

	public void waitFor(long milliseconds){
		long t0, t1, elapsed;
		t0 =  System.currentTimeMillis();
		do{
			t1 = System.currentTimeMillis();
			elapsed = t1-t0;
		}
		while (elapsed < milliseconds);
	}
}

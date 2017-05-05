package com.hh.videoviewplayer.utils;

public class DateTools {

	/**
	 * 将时间int类型转换成string
	 * @param milliSecond
	 * @return
     */
	public static String getTimeStr(int milliSecond){
		int second = milliSecond/1000;
		int hh = second/3600;
		int mm = second%3600/60;
		int ss = second%60;

		String timeStr = null;
		if (hh != 0){
			timeStr = String.format("%02d:%02d:%02d" , hh , mm , ss);
		}
		else{
			timeStr = String.format("%02d:%02d" , mm , ss);
		}
		return timeStr;
	}
}

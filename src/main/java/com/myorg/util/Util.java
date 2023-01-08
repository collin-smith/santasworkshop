package com.myorg.util;

public class Util 
{

	
  //Get the contents in a string between two delimeters
  public static String getContents(String inputString, String delimeter1, String delimeter2)
  {
    String result = "";
    int a = inputString.indexOf(delimeter1);
    int b = inputString.indexOf(delimeter2);
    if (a!= -1 && b!=-1 && a<b)
    {
      result = inputString.substring(a+delimeter1.length(),b);
    }
    return result;
  }


  //Get the contents in a string between two delimeters
  public static String getContentsAfterDelimeter(String inputString, String delimeter1)
  {
    String result = "";
    int a = inputString.indexOf(delimeter1);
    int b = inputString.length();
    if (a!= -1 && b!=-1 && a<b)
    {
      result = inputString.substring(a+delimeter1.length(),b);
    }
    return result;
  }


  public static String getContentsBeforeDelimeter(String inputString, String delimeter1)
  {
	  	String result = "";
		int a = inputString.indexOf(delimeter1);
		if (a!= -1)
		{
			result = inputString.substring(0,a);
		}
		return result;
  }

}

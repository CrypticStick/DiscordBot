package com.Stickles.Discord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JSONObject {

	String jsonString = "";
	public JSONObject(String json) {
		jsonString = json;
	}
	
	public int countChar(String str, char c)
	{
	    int count = 0;
	
	    for(int i=0; i < str.length(); i++)
	    {    if(str.charAt(i) == c)
	            count++;
	    }
	
	    return count;
	}
	
	String getBracketSet(String data) {
		String result = data.trim();
		int possibleEndpoint = -1;
		char startChar;
		char endChar;
		
		if (data.trim().startsWith("{")) {
			startChar = '{';
			endChar = '}';
		} else if (data.trim().startsWith("[")) {
			startChar = '[';
			endChar = ']';
		} else 
			return data;
		
		possibleEndpoint = result.indexOf(endChar)+1;
		while (countChar(result.substring(0,possibleEndpoint),startChar) != 
				countChar(result.substring(0,possibleEndpoint),endChar))
			possibleEndpoint = result.indexOf(endChar, possibleEndpoint)+1;
		result = result.substring(0,possibleEndpoint);
		
		return result;
	}
	
	String[] getArrayInBrackets(String data) {
		String result = data.trim();
		int possibleEndpoint = -1;
		List<String> resultArray = new ArrayList<String>();
		char startChar;
		char endChar;
		
		if (result.substring(1).trim().startsWith("{")) {
			startChar = '{';
			endChar = '}';
		} else if (result.substring(1).trim().startsWith("[")) {
			startChar = '[';
			endChar = ']';
		} else 
			return new String[] {result};
		
		result = result.substring(1,result.length()-1).trim();
		
		while (result.startsWith(String.valueOf(startChar))) {
			possibleEndpoint = result.indexOf(endChar)+1;
			while (countChar(result.substring(0,possibleEndpoint),startChar) != 
					countChar(result.substring(0,possibleEndpoint),endChar))
				possibleEndpoint = result.indexOf(endChar, possibleEndpoint)+1;
			resultArray.add(result.substring(0,possibleEndpoint));
			result = result.substring(possibleEndpoint);
			
			if (result.trim().startsWith(","))	
				result = result.substring(result.indexOf(',')+1).trim();	//remove comma if it exists
		}
		
		return Arrays.copyOf(resultArray.toArray(), resultArray.size(), String[].class);
	}
	
	public String[] get(String... name) throws NumberFormatException, ArrayIndexOutOfBoundsException{
		
		String result = jsonString;
		String[] resultArray = new String[] {""};
		
		for (int i = 0; i < name.length; i++) {
			
			resultArray = getArrayInBrackets(result);	//if current data is an array, convert to array
			if (resultArray.length > 1) {	//if array exists
				result = resultArray[Integer.parseInt(name[i])];	//hope next value from user is an index number
				continue;
			} else {	//if no array
				result = resultArray[0];	//set current result to current data
			}
			
			if (!result.contains(name[i]))	//if value from user doesn't exist
				return new String[] {""};	//return empty value
			
			result = result.substring(result.indexOf(name[i])+name[i].length()+1);	//cuts json near location of content
			result = result.substring(result.indexOf(':')+1).trim();	//takes result straight to first character of data
			
			if (result.startsWith("{") || result.startsWith("["))	//if data starts with brackets
				result = getBracketSet(result);	//get data within brackets

			else {	//if data does not contain brackets
				result = result.substring(0, result.indexOf('\n'));	// cut off data to end of line
				if (result.trim().endsWith(","))	
					result = result.substring(0, result.lastIndexOf(','));	//remove comma if it exists
				break;
			}
		}
		resultArray = getArrayInBrackets(result);
			return resultArray;
	}
}

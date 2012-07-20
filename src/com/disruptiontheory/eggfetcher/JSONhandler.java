package com.disruptiontheory.eggfetcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class JSONhandler {
	public static ArrayList<String> getJSONValues(String jsonString, List<String> objectKeys) {
		ArrayList<String> result = new ArrayList<String>();

		// convert the given string to a json object
		JSONObject jobj = null;
		try {
			jobj = (JSONObject) JSONValue.parse(jsonString);
		} catch (Exception ex) {
			JSONArray jray = null;
			try {
				// jobj = (JSONObject) JSONValue.parse(jsonString.substring(1, jsonString.length() - 1));
				jray = (JSONArray) JSONValue.parse(jsonString);
				if (jray.size() == 1) {
					jobj = (JSONObject) jray.get(0);
				} else {
					return null;
				}
			} catch (Exception excep) {
				System.err.println("Couldn't convert the following to a JSON object...");
				System.err.println(jsonString);
				System.out.println(ex.toString());
			}
		}

		// loop through all of the given keys and extract their values as a string
		for (String jsonKey : objectKeys) {
			// System.out.println("Getting " + jsonKey);
			// use a try statement inside the loop iteration so that breaking only skips this key
			try {
				Object objectValue = jobj.get(jsonKey);  
				if(objectValue != null) {
					result.add(objectValue.toString());					
				}
			} catch (Exception ex) {
				System.err.println("Couldn't get " + jsonKey + " from...");
				System.err.println(jsonString);
				System.out.println(ex.toString());
			}
		}

		return result;
	}

	public static String getJSONValue(String jsonString, String objectKey) {
		ArrayList<String> jsonResults = JSONhandler.getJSONValues(jsonString, Arrays.asList(objectKey));
		if (jsonResults.size() == 0) {
			System.out.println(objectKey + " was useless.");
			return "";
		}

		String result = jsonResults.get(0).toString();
		return result;
	}
}

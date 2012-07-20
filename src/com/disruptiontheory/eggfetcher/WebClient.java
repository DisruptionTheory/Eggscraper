package com.disruptiontheory.eggfetcher;

import java.io.*;
import java.net.*;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class WebClient {
	/**
	 * Query a URL with post data and get a result as a JSON-formatted string
	 * @param url The URL to query
	 * @param queryData A JSON-formatted string of data to post to the query
	 * @return JSON-formatted string
	 */
	public static String QueryAndGetJSON(String url, String queryData) {
		try {			
			//create a http client
			HttpClient httpClient = new DefaultHttpClient();
			//create the post object and assign its data
			HttpPost pagePost = new HttpPost(new URL(url).toURI());		    
		    pagePost.setEntity(new StringEntity(queryData));
		    
		    //using the http client, execute the post and get the  response
			HttpResponse response = httpClient.execute(pagePost);
			
		    //process the response. Since this is JSON, we're only expecting one long line
		    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));		    
		    return rd.readLine();

		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}			
	}
	
	//if you don't need to post
	public static String GetJSON(String url) {
		try {			
			//create a http client
			HttpClient httpClient = new DefaultHttpClient();
			//create the post object and assign its data
			HttpGet pageGet = new HttpGet(new URL(url).toURI());
		    
		    //using the http client, execute the post and get the  response
			HttpResponse response = httpClient.execute(pageGet);
			
		    //process the response. Since this is JSON, we're only expecting one long line
		    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));		    
		    return rd.readLine();

		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}					
	}
}

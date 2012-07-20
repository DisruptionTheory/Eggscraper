package com.disruptiontheory.eggfetcher;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class WebSrv {
	public static void main() {
		try {
			// bind the server to listen on port 366
			//I chose "366" because it looks a little like "EGG", and it's normally used for "On Demand Mail Relay", 
			//Which I do not forsee the server ever needing to use.
			InetSocketAddress addr = new InetSocketAddress(366);
			HttpServer server = HttpServer.create(addr, 0);

			// when a "query" is made to the root folder path, execute a new BrandHandler class in a new cached thread pool
			server.createContext("/", new BrandHandler());
			server.setExecutor(Executors.newCachedThreadPool());
			server.start();
			System.out.println("Server is listening on port 366");
		} catch (Exception ex) {
			System.err.println("Failed to start server because of " + ex.getMessage());
		}
	}
}

class BrandHandler implements HttpHandler {
	public void handle(HttpExchange exchange) throws IOException {
		Headers responseHeaders = exchange.getResponseHeaders();
		responseHeaders.set("Content-Type", "text/plain");
		responseHeaders.set("Access-Control-Allow-Origin", "*");
		exchange.sendResponseHeaders(200, 0);
		OutputStream responseBody = exchange.getResponseBody();
		//Headers requestHeaders = exchange.getRequestHeaders();

		//get the GET variables from the request
        Map<String, Object> parameters = new HashMap<String, Object>();
        URI requestedUri = exchange.getRequestURI();
        String query = requestedUri.getRawQuery();
        parseQuery(query, parameters);
        
        //convert the brand from a get var to an int
        int brand = Integer.parseInt(parameters.get("Brand").toString()); 
        //check if that brand exists in the database
        double brandRating = DatabaseHandler.GetBrandRating(brand);
        //if it does not, it will return -1
        if(brandRating == -1) {
            //execute the query based on that brand
        	//this will also "compile" its results
            new EggFetcher().GetProductsByBrand(brand);      
            brandRating = DatabaseHandler.GetBrandRating(brand);
        }
        
        //send the resulting brand value back to the client
        //responseBody.write(("Brand rating is " + brand).getBytes());
        responseBody.write(("" + brandRating).getBytes());

        //responseBody.write(("Brand is " + brand).getBytes());
		responseBody.close();
	}

    @SuppressWarnings("unused")
	private void parseGetParameters(HttpExchange exchange) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        URI requestedUri = exchange.getRequestURI();
        String query = requestedUri.getRawQuery();
        parseQuery(query, parameters);
        exchange.setAttribute("parameters", parameters);
    }

    @SuppressWarnings("unchecked")
	private void parseQuery(String query, Map<String, Object> parameters) {
        if (query != null) {
            String pairs[] = query.split("[&]");

            for (String pair : pairs) {
                String param[] = pair.split("[=]");

                String key = null;
                String value = null;
                if (param.length > 0) {
                    try {
						key = URLDecoder.decode(param[0],
						    System.getProperty("file.encoding"));

		                if (param.length > 1) {
		                    value = URLDecoder.decode(param[1],
		                        System.getProperty("file.encoding"));
		                }
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
                }

                if (parameters.containsKey(key)) {
                    Object obj = parameters.get(key);
                    if(obj instanceof List<?>) {
                        List<String> values = (List<String>)obj;
                        values.add(value);
                    } else if(obj instanceof String) {
                        List<String> values = new ArrayList<String>();
                        values.add((String)obj);
                        values.add(value);
                        parameters.put(key, values);
                    }
                } else {
                    parameters.put(key, value);
                }
            }
        }
   }
}

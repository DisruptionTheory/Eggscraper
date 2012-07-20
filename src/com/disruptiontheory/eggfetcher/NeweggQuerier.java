package com.disruptiontheory.eggfetcher;
import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.simple.*;


public class NeweggQuerier {
	//the number of products returned this query
	int queryProductCount = 0;
	
	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	public static void BuildIndicies() {
		//List<Integer> stores = Arrays.asList();
		ArrayList stores = new ArrayList();
		List<Integer> categories = Arrays.asList();
		//indexed by category
		HashMap<Integer, List<Integer>> subcategories = new HashMap<Integer, List<Integer>>();

		//stores
		//I'm just going to do computer hardware (store ID 1) for now, and not push it
		//String stores =  NeweggQuerier.QueryAndGetJSON("http://www.ows.newegg.com/Stores.egg/Menus", "");
		stores.add(1);

		//categories
		for(Object store : stores) {
			//String strStoreJSON = NeweggQuerier.QueryAndGetJSON("http://www.ows.newegg.com/Stores.egg/Categories/" + store.toString(), "");
			String strStoreJSON = NeweggQuerier.GetJSON("http://www.ows.newegg.com/Stores.egg/Categories/" + store.toString());
			//System.out.println("=== Query Result ===");
			//System.out.println(strStoreJSON);
			JSONArray storeJSON = (JSONArray) JSONValue.parse(strStoreJSON);
			for (Object jCat : storeJSON) {
				//System.out.println("=== Category ===");
				//System.out.println(jCat);
				ArrayList lstrStoreCats = NeweggQuerier.getJSONValues(jCat.toString(), Arrays.asList("CategoryID", "Description"));
				System.out.println(lstrStoreCats.toString());
			}
		}
		
		//subcategories
	}
	
	public static String GetProductsByBrand(int brandId) {
		//queryProductCount = 0;
		
		//the current page we're on
		int pageNum = 1;
		//the number of products per page
		int pageSize = 0;
		//the number of products that there are
		int totalProductCount = 0;
		//the number of products returned with this query
		int returnedProductCount = 0;
		//the result of the query, in string format
		String queryResult = "";
		
		ArrayList<NeweggProduct> returnedProducts = new ArrayList<NeweggProduct>(); 
		
		//query newegg for the first set of information
		queryResult = NeweggQuerier.QueryNewegg(NeweggQuerier.SlimData(brandId, pageNum));
		
		//we need to get the paginationInfo from the result, and then the number of items per page from that
		String paginationInfo = (String) NeweggQuerier.getJSONValues(queryResult,  Arrays.asList("PaginationInfo")).get(0);
		pageSize = Integer.parseInt((String) NeweggQuerier.getJSONValues(paginationInfo,  Arrays.asList("PageSize")).get(0));

		//NavigationContentList [0] 
		//String navigateContentList = NeweggQuerier.getJSONValue(queryResult, "NavigationContentList");
		//ArrayList<Integer> brandCats = NeweggQuerier.GetBrandCats(NeweggQuerier.getJSONValue(queryResult, "NavigationItemList"));
		//do a query for each category in brandcats...
		
		totalProductCount = Integer.parseInt((String) NeweggQuerier.getJSONValues(paginationInfo,  Arrays.asList("TotalCount")).get(0));
		System.out.println("Brand " + brandId + " has " + totalProductCount + " products, showing " + pageSize + " products per page.");

		//while returnedProducts == pageSize
		while(1 == 1) {
			String results = NeweggQuerier.getJSONValue(queryResult, "ProductListItems");
			JSONArray productList = (JSONArray) JSONValue.parse(results);
			returnedProductCount = productList.size();
			ArrayList<NeweggProduct> queryProducts = NeweggQuerier.GetProducts(brandId, results);
			System.out.println(returnedProductCount + " products returned this query.");
			//loop through all of the products in the query
			
			returnedProducts.addAll(queryProducts);
			
			if(1==1) {
				break;
			}
			
			//we're assuming the last page won't be exactly 20 products long. A dangerous game.
			if(returnedProductCount < pageSize) {
				break;
			} else {
				//run the query again
				pageNum = pageNum + 1;
				queryResult = NeweggQuerier.QueryNewegg(NeweggQuerier.SlimData(brandId, pageNum));
				break;
			}
		}
		
		System.out.println("==========================PRODUCT LIST==========================");
		try {
			//OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream("test.txt"), "UTF-8");
			String outputString = "INSERT INTO eggscraper.NeweggProducts (`NeweggItemNumber`, `model`, `reviewcount`, `averagerating`) VALUES ";
			for(NeweggProduct product : returnedProducts) {
				outputString += product.toMySQLInsert() + ",";
				//System.out.println(product.toString());
			}
			DatabaseHandler dbHandler = new DatabaseHandler();
			dbHandler.InsertNeweggProducts(outputString);
			/*
			outputString = outputString.substring(0, outputString.length() - 1);
			out.write(outputString);
			out.close();
			*/
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		return "";
	}
	
	//need to return an array of categories and an array of subcategories...
	//let's just do categories for now...
	public static ArrayList<Integer> GetBrandCats(String navigationItemList) {
		ArrayList<Integer> resultList = new ArrayList<Integer>();

		//NavigationItemList array contains "ItemCount" and "CategoryId" and "SubCategoryId" for what need to be searched for each brand...
		JSONArray navItems = (JSONArray) JSONValue.parse(navigationItemList);
		for(Object navEntry : navItems) {
			JSONObject navItem = (JSONObject) navEntry;
			resultList.add(Integer.parseInt(navItem.get("CategoryId").toString()));
		}		
		
		return resultList;
	}
	
	public static String QueryNewegg(String queryData) {
		System.out.println("Querying with");
		System.out.println(queryData);
		return NeweggQuerier.QueryAndGetJSON("http://www.ows.newegg.com/Search.egg/Advanced", queryData);
	}
	
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

	@SuppressWarnings("unchecked")
	public static String NeweggQueryData(int brand, int page, int category, int subcat) {
		JSONObject obj = new JSONObject();

		obj.put("IsUPCCodeSearch", false);
		obj.put("isGuideAdvanceSearch", false);
		obj.put("StoreDepaId", 1);
		obj.put("CategoryId", category);
		obj.put("SubCategoryId", subcat);
		obj.put("NodeId", -1);
		obj.put("BrandId", brand);
		obj.put("NValue", "");
		obj.put("Keyword", "");
		obj.put("Sort", "FEATURED");
		obj.put("PageNumber", page);
		
		if(subcat > -1) {
			obj.put("IsSubCategorySearch", true);
		} else {
			obj.put("IsSubCategorySearch", false);
		}

		return obj.toJSONString();		
	}
	
	public static String SlimData(int brand) {
		return NeweggQuerier.NeweggQueryData(brand, 0, -1, -1);
	}
	
	public static String SlimData(int brand, int page) {
		return NeweggQuerier.NeweggQueryData(brand, page, -1, -1);
	}
	
	public static String SlimData(int brand, int page, int catId) {
		return NeweggQuerier.NeweggQueryData(brand, page, catId, -1);
	}
	
	public static ArrayList<NeweggProduct> GetProducts(int brandId, String productsJSON) {
		int reviewCount = 0;
		int productRating = 0;
		String itemNumber = "";
		String productModel = "";
		
		ArrayList<NeweggProduct> resultProducts = new ArrayList<NeweggProduct>();
		
		JSONArray jarr = (JSONArray) JSONValue.parse(productsJSON);
		for (Object productEntry : jarr) { 
			JSONObject product = (JSONObject) productEntry;
			JSONObject productReviews = (JSONObject) JSONValue.parse(product.get("ReviewSummary").toString());
			String revCnt = productReviews.get("TotalReviews").toString().replace("[", "").replace("]", "").replace(",", "");
			if (revCnt.equalsIgnoreCase("") == false) {
				reviewCount = Integer.parseInt(revCnt);
				if(reviewCount <= 0) {
					continue;
				}
				productRating = Integer.parseInt(productReviews.get("Rating").toString());
				itemNumber = product.get("NeweggItemNumber").toString();
				productModel = product.get("Model").toString();
				resultProducts.add(new NeweggProduct(itemNumber, productModel, brandId, reviewCount, productRating));
			}			
		}		
		
		return resultProducts;
	}
	  
	  public static void GetProductAverages(String s) {
			try {
				// TotalReviews":"[33]","Rating":4},"Model":"ST310005N1A1AS-RK

				Object obj = JSONValue.parse(s);
				JSONObject jobj = (JSONObject) obj;
				s = jobj.get("ProductListItems").toString();
				obj = JSONValue.parse(s);
				JSONArray jarr = (JSONArray) obj;

				int iterateCount = 0;
				for (Object obby : jarr) {
					iterateCount++;
					
					JSONObject product = (JSONObject) obby;
					JSONObject productReviews = (JSONObject) JSONValue.parse(product.get("ReviewSummary").toString());

					//if there are no reviews, it comes up empty rather than 0, so we have to do a little dance
					int reviewCount = 0;
					try {
						reviewCount = Integer.parseInt(productReviews.get("TotalReviews").toString().replace("[", "").replace("]", "").replace(",", ""));
					}catch (Exception ex) {
						//System.err.println(productReviews.toString());
					}
					int productRating = Integer.parseInt(productReviews.get("Rating").toString());
					if (reviewCount > 0) {
						System.out.println(product.get("NeweggItemNumber").toString() + " : " + productRating);
					}
					
					//System.out.println("==== Product " + product.get("Model").toString() + " ====");					
					//System.out.println("Review Summary: " + product.get("ReviewSummary").toString());
					//AverageRating
					//NeweggItemNumber
				}	
				System.out.println("Processed " + iterateCount + " products.");
			} catch (Exception ex) {
				System.err.println(ex.toString());
			}  
	  }
	  
	  //Get the entires at specified keys for a given jsonString
	  public static ArrayList getJSONValues(String jsonString, List<String> objectKeys) {
		  ArrayList result = new ArrayList();

		  //convert the given string to a json object
		  JSONObject jobj = null;
		  try {
			  jobj = (JSONObject) JSONValue.parse(jsonString);
		  } catch(Exception ex) {
			  JSONArray jray = null;
			  try {
				  //jobj = (JSONObject) JSONValue.parse(jsonString.substring(1, jsonString.length() - 1));
				  jray = (JSONArray) JSONValue.parse(jsonString);
				  if(jray.size() == 1) {
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
		  
		  //loop through all of the given keys and extract their values as a string
		  for(String jsonKey : objectKeys) {
			  //System.out.println("Getting " + jsonKey);
			  //use a try statement inside the loop iteration so that breaking only skips this key
			  try {
				  result.add(jobj.get(jsonKey).toString());
			  } catch (Exception ex) {
				  System.err.println("Couldn't get " + jsonKey + " from...");
				  System.err.println(jsonString);
				  System.out.println(ex.toString());
			  }
		  }
		  
		  return result;
	  }
	  
	  public static String getJSONValue(String jsonString, String objectKey) {
		  ArrayList jsonResults = NeweggQuerier.getJSONValues(jsonString, Arrays.asList(objectKey));
		  if(jsonResults.size() == 0) {
			  System.out.println(objectKey + " was useless.");
			  return "";
		  }
		  
		  String result = jsonResults.get(0).toString();		  
		  return result;
	  }
		        
	
	  //this is a mess, probly don't need it any more
	@SuppressWarnings("rawtypes")
	public static void FromJSON(String s) {
		// Object obj=JSONValue.parse(s);
		// JSONObject jobj = (JSONObject) obj;

		try {
			// TotalReviews":"[33]","Rating":4},"Model":"ST310005N1A1AS-RK

			Object obj = JSONValue.parse(s);
			JSONObject jobj = (JSONObject) obj;
			// System.out.println(jobj.get("ProductListItems").toString());
			s = jobj.get("ProductListItems").toString();
			obj = JSONValue.parse(s);
			/*
			JSONArray jarr = (JSONArray) obj;
			for (Object obby : jarr) {
				// System.out.println(obby);
				JSONObject product = (JSONObject) obby;
				System.out.println("==== Product " + product.get("Model").toString() + " ====");
				System.out.println("Review Summary: " + product.get("ReviewSummary").toString());
				//AverageRating
				//N82E16822148321

				Iterator iter = product.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry) iter.next();
					System.out.println(entry.getKey() + "=>" + entry.getValue());
				}

				// for now, only do one
				break;
			}

			if (1 == 1) {
				return;
			}
*/
			// Map json = (Map)parser.parse(s, containerFactory);
			// Map ojson = (Map)parser.parse(s, containerFactory);
			// Map json = (Map) ojson.get("ProductListItems");

			// Iterator iter = json.entrySet().iterator();
			Iterator iter = jobj.entrySet().iterator();

			System.out.println("==iterate result==");
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				System.out.println(entry.getKey() + "=>" + entry.getValue());
			}
		} catch (Exception ex) {
			System.err.println(ex.toString());
		}

		  /*
		  JSONArray array = (JSONArray)obj;
		  System.out.println("======the 2nd element of array======");
		  System.out.println(array.get(1));
		  System.out.println();
		                
		  JSONObject obj2=(JSONObject)array.get(1);
		  System.out.println("======field \"1\"==========");
		  System.out.println(obj2.get("1"));
		  */    		
	}
}

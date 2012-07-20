package com.disruptiontheory.eggfetcher;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


public class EggFetcher {
	//the number of products returned this query
	int returnedProductCount = 0;
	
	public void GetProductsByBrand(int brandId) {		
		//the current page we're on
		int pageNum = 1;
		//the number of products per page
		int pageSize = 0;
		//the number of products that there are for this query
			//this was only used for output / debug purposes, so I've since commented it out. But it can be restored for use as needed
		//int totalProductCount = 0;
		//the result of the query, in string format
		String queryResult = "";	
		//the products returned in this query
		ArrayList<NeweggProduct> returnedProducts = new ArrayList<NeweggProduct>(); 
		
		//query newegg for the first set of information, passing only the brand we want
			//(page number should just be "1" or default at this point)
		queryResult = EggFetcher.QueryNewegg(QueryData.SlimData(brandId, pageNum));
		
		//we need to get the paginationInfo from the result, and then the number of items per page from that
		String paginationInfo = (String) JSONhandler.getJSONValue(queryResult,  "PaginationInfo");
		pageSize = Integer.parseInt(JSONhandler.getJSONValue(paginationInfo, "PageSize").toString());
		
		//totalProductCount = Integer.parseInt(JSONhandler.getJSONValue(paginationInfo, "TotalCount").toString());		
		//System.out.println("Brand " + brandId + " has " + totalProductCount + " products, showing " + pageSize + " products per page.");

		//the NavigationContentList holds a NavigationItemList, which we need in order to get the categories
		String navigateContentList = JSONhandler.getJSONValue(queryResult, "NavigationContentList");		
		//But if it returns empty, or null, then this query is likely unsupported, and we're not going to get any information, so we have to return empty-handed
		if(navigateContentList.equalsIgnoreCase("")) {
			return;
		}
		
		//get the categories that this brand has products in
		//ArrayList<Integer> brandCats = GetBrandCats(JSONhandler.getJSONValue(navigateContentList, "NavigationItemList"));
		ArrayList<Brand> brandCats = GetBrandCats(JSONhandler.getJSONValue(navigateContentList, "NavigationItemList"));
		//loop through each of those categories, get the products in it, and then add those products to the list of returned products for this query
		for(Brand brand : brandCats) {
			returnedProducts.addAll(GetProductsFromCategory(brand, pageSize, brandId));
		}
		
		//System.out.println("==========================PRODUCT LIST==========================");
		try {
			//prep a mysql string for execution
				//it would be better to pass cleaner data to the databasehandler, which may get done in a future update
				//when doing so, should also change to INSERT / UPDATE, rather than ignoring existing entries
			String outputString = "INSERT IGNORE INTO `NeweggProducts` (`NeweggItemNumber`, `brand`, `model`, `reviewcount`, `averagerating`) VALUES ";
			//loop through each product and format it to a VALUE entry for the list, according to a defined function
			for(NeweggProduct product : returnedProducts) {
				outputString += product.toMySQLInsert() + ",";
			}
			//remove the trailing delimiter
			outputString = outputString.substring(0, outputString.length() - 1);
			//call the function to insert this data to the database
			if(DatabaseHandler.InsertNeweggProducts(outputString) == true)
			{
				//if the product insert went off without erroring,
				//take the finished list of products and compute the average brand rating from it
				DatabaseHandler.CompileBrandRating(brandId);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//get all of the products in a given category (by category id)
	//pass the brand ID in so that the prouducts don't have to do extra queries when attaching that information to themselves
	public ArrayList<NeweggProduct> GetProductsFromCategory(Brand brand, int pageSize, int brandId) {
		//the id of the category we're working with
		int catId = brand.id;
		//the number of products in the current category
		int productCount = brand.ItemCount;
		
		//the number of products that we have queried, in total, during this query
		returnedProductCount = 0;
		
		//the products returned in this query
		ArrayList<NeweggProduct> returnedProducts = new ArrayList<NeweggProduct>();
		//the current page number of this query
		int pageNum = 1;
		//the JSON-formatted string we get back from Newegg
		String queryResult;
		
		//while there are still products to be queried in this category
		while(returnedProductCount < productCount) {			
			//get the products on this page in this category from newegg
			queryResult = EggFetcher.QueryNewegg(QueryData.SlimData(brandId, pageNum, catId));
			
			//get just the products from the results of the query
			String results = JSONhandler.getJSONValue(queryResult, "ProductListItems");
			//if there is no productlistitems, or the json query doesn't work for any other reason, close out of this gracefully
			if(results == "") {
				break;
			}
			//extract the products from the result data
			//the returnedProductCount will be incremented by however many products this returns:
			ArrayList<NeweggProduct> queryProducts = GetProducts(brandId, results);
			
			//add the resulting products to the list of products to return
			returnedProducts.addAll(queryProducts);
			
			//incremement the number of the page to be queried
			pageNum++;
		}	
		
		return returnedProducts;
	}
	
	public ArrayList<NeweggProduct> GetProducts(int brandId, String productsJSON) {
		int reviewCount = 0;
		int productRating = 0;
		String itemNumber = "";
		String productModel = "";
		
		ArrayList<NeweggProduct> resultProducts = new ArrayList<NeweggProduct>();
		
		JSONArray jarr = (JSONArray) JSONValue.parse(productsJSON);
		for (Object productEntry : jarr) { 
			returnedProductCount++;
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
		//System.out.println("Sending back " + resultProducts.size() + " products.");
		
		return resultProducts;
	}
	
	public static ArrayList<Brand> GetBrandCats(String navigationItemList) {		
		ArrayList<Brand> resultList = new ArrayList<Brand>();

		//NavigationItemList array contains "ItemCount" and "CategoryId" and "SubCategoryId" for what need to be searched for each brand...
		JSONArray navItems = (JSONArray) JSONValue.parse(navigationItemList);
		for(Object navEntry : navItems) {
			JSONObject navItem = (JSONObject) navEntry;
			int catId = Integer.parseInt(navItem.get("CategoryId").toString());
			int itemCount = Integer.parseInt(navItem.get("ItemCount").toString());
			resultList.add(new Brand(catId, itemCount));
		}		
		
		return resultList;
	} 
	
	public static String QueryNewegg(String queryData) {
		return WebClient.QueryAndGetJSON("http://www.ows.newegg.com/Search.egg/Advanced", queryData);
	}
	
	public static String GetBrandName(int brandId) {
		//get an item number from the database by doing 
		//"SELECT NeweggItemNumber FROM NeweggProducts WHERE brand = " + brandId + " LIMIT 1"
		
		//do a (get?) query against
		//http://www.ows.newegg.com/Products.egg/{ItemNumber}/Specification
		//using the grabbed item number
		return "Unknown Name";
	}
}

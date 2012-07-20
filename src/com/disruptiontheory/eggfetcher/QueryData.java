package com.disruptiontheory.eggfetcher;

import org.json.simple.*;

public class QueryData {
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
		return QueryData.NeweggQueryData(brand, 0, -1, -1);
	}
	
	public static String SlimData(int brand, int page) {
		return QueryData.NeweggQueryData(brand, page, -1, -1);
	}
	
	public static String SlimData(int brand, int page, int catId) {
		return QueryData.NeweggQueryData(brand, page, catId, -1);
	}
}

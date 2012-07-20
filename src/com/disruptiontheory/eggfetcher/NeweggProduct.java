package com.disruptiontheory.eggfetcher;
import java.util.ArrayList;


public class NeweggProduct {
	private String productId = "";
	private String model = "";
	private int brandId = 0;
	private String manufactureDate = "";
	private int ratingCount = 0;
	private int averageRating = 0;
	
	public static ArrayList<NeweggProduct> NeweggProducts = new ArrayList<NeweggProduct>();
	
	public NeweggProduct(String prodId, String modelNum, int brand, int numRatings, int avgRating) {
		this.productId = prodId;
		this.model = modelNum;
		this.brandId = brand;
		this.ratingCount = numRatings;
		this.averageRating = avgRating;
		
		NeweggProducts.add(this);
	}
	
	public String GetProductId() {
		return this.productId;
	}
	
	public void SetProductId(String newId) {
		this.productId = newId;
	}
	
	public int GetBrand() {
		return this.brandId;
	}
	
	public void SetBrand(int newBrand) {
		this.brandId = newBrand;
	}
	
	public String GetModel() {
		return this.model;
	}
	
	public void SetModel(String newModel) {
		this.model = newModel;
	}
	
	public String GetManufactureDate() {
		return this.manufactureDate;
	}
	
	public void SetManufactureDate(String newDate) {
		this.manufactureDate = newDate;
	}
	
	public int GetRatingCount() {
		return this.ratingCount;
	}
	
	public void SetRatingCount(int newCount) {
		this.ratingCount = newCount;
	}
	
	public int GetAverageRating() {
		return this.averageRating;
	}
	
	public void SetAverageRating(int newRating) {
		this.averageRating = newRating;
	}
	
	public String toMySQLInsert() {
		//get rid of all of the double quotes, because they're breaking the SQL and santization / regex is failing us
		this.SetModel(this.GetModel().replaceAll("\"", ""));
		
		String queryResult = "(\"" + DatabaseHandler.Sanitize(this.GetProductId()) + "\"";
		queryResult += ", \"" + this.GetBrand() + "\"";
		queryResult += ", \"" + DatabaseHandler.Sanitize(this.GetModel()) + "\"";
		queryResult += ", \"" + this.GetRatingCount() + "\"";
		queryResult += ", \"" + this.GetAverageRating() + "\"";
		
		queryResult += ") " + "\r\n";
		return queryResult;
	}
}

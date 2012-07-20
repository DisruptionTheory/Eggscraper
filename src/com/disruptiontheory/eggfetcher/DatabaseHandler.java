package com.disruptiontheory.eggfetcher;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

public class DatabaseHandler {

	private Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;
	
	public static boolean InsertNeweggProducts(String queryString) {
		return new DatabaseHandler().ExecuteQuery(queryString);
	}
	
	public boolean ExecuteQuery(String queryString) {
		try
		{
		// This will load the MySQL driver, each DB has its own driver
		Class.forName("com.mysql.jdbc.Driver");
		
		//Thankfully, I remembered to edit the below before uploading to Github. You'll need to put in actual values to get it working on your server
		connect = DriverManager.getConnection(
				"jdbc:mysql://<SERVER_ADDRESS>?"
				+ "user=<USER>&password=<PASSWORD>");

		statement = connect.createStatement();
		statement.execute(queryString);
		return true;
		} catch (Exception ex) {
			System.out.println("Couldn't execute MySQL Query:");
			System.out.println(ex.getMessage());
			System.out.println(queryString);
			return false;
		}	
	}
	
	public ResultSet SelectQuery(String queryString) {
		try
		{
		// This will load the MySQL driver, each DB has its own driver
		Class.forName("com.mysql.jdbc.Driver");
		
		connect = DriverManager.getConnection(
				"jdbc:mysql://173.255.243.88/eggscraper?"
				+ "user=eggy&password=descrambling");
		

		preparedStatement = connect.prepareStatement(queryString);
		resultSet = preparedStatement.executeQuery();
		return resultSet;
		//writeResultSet(resultSet);
		
		} catch (Exception ex) {
			System.out.println("Couldn't execute MySQL Query:");
			System.out.println(ex.getMessage());
			System.out.println(queryString);
			return null;
		}
	}
	
	public static double GetBrandRating(int brandId) {		
		ResultSet resultSet = new DatabaseHandler().SelectQuery("SELECT rating FROM BrandRatings WHERE brandid = " + brandId);
		try {
			if(resultSet.next()) {
				double rating = resultSet.getFloat("rating");
				return rating;
			}
			return -1;
		} catch (Exception ex) {
			System.out.println("Couldn't get rating: " + ex.getMessage());
			return -1;			
		}
	}
	
	public static double CompileBrandRating(int brandId) {
		String queryString = "INSERT IGNORE INTO BrandRatings (brandid, brand_name, rating) " + 
			"VALUES (" + brandId + ", \"Name Unknown\", (SELECT AVG(averagerating) FROM `NeweggProducts` WHERE brand = " + brandId + "))";
		
		new DatabaseHandler().ExecuteQuery(queryString);
		
		return DatabaseHandler.GetBrandRating(brandId);
	}

	public void readDataBase() throws Exception {
		try {
			// This will load the MySQL driver, each DB has its own driver
			Class.forName("com.mysql.jdbc.Driver");
			// Setup the connection with the DB
			connect = DriverManager
					.getConnection("jdbc:mysql://localhost/feedback?"
							+ "user=sqluser&password=sqluserpw");

			// Statements allow to issue SQL queries to the database
			statement = connect.createStatement();
			// Result set get the result of the SQL query
			resultSet = statement
					.executeQuery("select * from FEEDBACK.COMMENTS");
			writeResultSet(resultSet);

			// PreparedStatements can use variables and are more efficient
			preparedStatement = connect
					.prepareStatement("insert into  FEEDBACK.COMMENTS values (default, ?, ?, ?, ? , ?, ?)");
			// "myuser, webpage, datum, summery, COMMENTS from FEEDBACK.COMMENTS");
			// Parameters start with 1
			preparedStatement.setString(1, "Test");
			preparedStatement.setString(2, "TestEmail");
			preparedStatement.setString(3, "TestWebpage");
			preparedStatement.setString(5, "TestSummary");
			preparedStatement.setString(6, "TestComment");
			preparedStatement.executeUpdate();

			preparedStatement = connect
					.prepareStatement("SELECT myuser, webpage, datum, summery, COMMENTS from FEEDBACK.COMMENTS");
			resultSet = preparedStatement.executeQuery();
			writeResultSet(resultSet);

			// Remove again the insert comment
			preparedStatement = connect
			.prepareStatement("delete from FEEDBACK.COMMENTS where myuser= ? ; ");
			preparedStatement.setString(1, "Test");
			preparedStatement.executeUpdate();
			
			resultSet = statement
			.executeQuery("select * from FEEDBACK.COMMENTS");
			writeMetaData(resultSet);
			
		} catch (Exception e) {
			throw e;
		} finally {
			close();
		}

	}

	private void writeMetaData(ResultSet resultSet) throws SQLException {
		// 	Now get some metadata from the database
		// Result set get the result of the SQL query
		
		System.out.println("The columns in the table are: ");
		
		System.out.println("Table: " + resultSet.getMetaData().getTableName(1));
		for  (int i = 1; i<= resultSet.getMetaData().getColumnCount(); i++){
			System.out.println("Column " +i  + " "+ resultSet.getMetaData().getColumnName(i));
		}
	}

	private void writeResultSet(ResultSet resultSet) throws SQLException {
		// ResultSet is initially before the first data set
		while (resultSet.next()) {
			// It is possible to get the columns via name
			// also possible to get the columns via the column number
			// which starts at 1
			// e.g. resultSet.getSTring(2);
			String user = resultSet.getString("myuser");
			String website = resultSet.getString("webpage");
			String summery = resultSet.getString("summery");
			Date date = resultSet.getDate("datum");
			String comment = resultSet.getString("comments");
			System.out.println("User: " + user);
			System.out.println("Website: " + website);
			System.out.println("Summery: " + summery);
			System.out.println("Date: " + date);
			System.out.println("Comment: " + comment);
		}
	}

	// You need to close the resultSet
	private void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}

			if (statement != null) {
				statement.close();
			}

			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {

		}
	}
	
	public static String Sanitize(String inputString) {
		return inputString
			.replaceAll("\\x00", "\\\\x00")
			.replaceAll("\\x1a", "\\\\x1a")
			.replaceAll("\\r", "\\\\r")
			.replaceAll("\\n", "\\\\n")
			//.replaceAll("\\", "\\\\")
			.replaceAll("\'", "\\\'")
			.replaceAll("\"", "\\\"");
	}

}

package com.myorg.dynamodb;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.myorg.util.Util;

public class LetterFacade 
{
  private AmazonDynamoDB client = null;
  private String tableName = "";
  private LetterBuilder builder = null;
  private DynamoDBMapper mapper = null;
	
  public LetterFacade(String dynamoDBTableName)
  {
    client = AmazonDynamoDBClientBuilder.standard()
	                .withRegion(System.getenv("AWS_REGION"))
	                .build();
	        
    //Overriding the City DynamoDB table name (See JavaCdkStack)
    tableName = dynamoDBTableName;
    DynamoDBMapperConfig mapperConfig = DynamoDBMapperConfig.builder()
	                .withTableNameOverride(TableNameOverride.withTableNameReplacement(tableName))
	                .build();

    //Use of the Partition Index (state) and the sort key(city) for querying data
    //Let's just see if there is any data (for Alberta in this case)
    //We are only going to add data once, so if there is data we will not do again
    mapper = new DynamoDBMapper(client, mapperConfig);
    builder = new LetterBuilder(mapper, mapperConfig);	
  }
    
  public LetterBuilder getLetterBuilder()
  {
    return builder;
  }

	
  public List<Letter> getLettersByPK(String partitionKey) 
  {
    // Set up an alias for the partition key name in case it's a reserved word
	//Map<String, String> attrNameAlias = Map.of("#state", "state");

	// Set up mapping of the partition name with the value
	// Map<String, AttributeValue> attrValues = Map.of(":pk", new AttributeValue().withS(partitionKey));
	Map<String, AttributeValue> attrValues = new HashMap<String, AttributeValue>();
	attrValues.put(":pk", new AttributeValue().withS(partitionKey));

	DynamoDBQueryExpression<Letter> queryExpression = new DynamoDBQueryExpression<>();
	queryExpression.withKeyConditionExpression("PK = :pk");
	queryExpression.setExpressionAttributeValues(attrValues);
	return mapper.query(Letter.class, queryExpression);
  }

  public List<Letter> getLettersByPKandSK(String partitionKey, String sortKey) 
  {
	// Set up an alias for the partition key name in case it's a reserved word
	//Map<String, String> attrNameAlias = Map.of("#state", "state");

	// Set up mapping of the partition name with the value
	Map<String, AttributeValue> attrValues = new HashMap<String, AttributeValue>();
	attrValues.put(":pk", new AttributeValue().withS(partitionKey));
	attrValues.put(":sk", new AttributeValue().withS(sortKey));

	DynamoDBQueryExpression<Letter> queryExpression = new DynamoDBQueryExpression<>();
	queryExpression.withKeyConditionExpression("PK = :pk AND SK = :sk");
	queryExpression.setExpressionAttributeValues(attrValues);
	return mapper.query(Letter.class, queryExpression);
  }

  public List<Letter> getLettersByPKandBegins_WithSK(String partitionKey, String skStart) 
  {
	Map<String, AttributeValue> attrValues = new HashMap<String, AttributeValue>();
	attrValues.put(":pk", new AttributeValue().withS(partitionKey));
	attrValues.put(":sk", new AttributeValue().withS(skStart));

	DynamoDBQueryExpression<Letter> queryExpression = new DynamoDBQueryExpression<>();
	queryExpression.withKeyConditionExpression("PK = :pk AND begins_with(SK, :sk) ");
	queryExpression.setExpressionAttributeValues(attrValues);
        
	return mapper.query(Letter.class, queryExpression);
  }
    
  public List<Letter> getLettersForAToy(String toyId) 
  {
	//Please note that for GSI you can only use the equal operator and not greater than or less than
	//or you will get an message like : Query key condition not supported (Service: AmazonDynamoDBv2; Status Code: 400; Error Code: ValidationException;
	String gsiIndex = "String1GSIIndex";
	Map<String, AttributeValue> eav = Map.of(":v_string1", new AttributeValue().withS("TOY#"+toyId));
	String filterExpression = "string1 = :v_string1";
        
	//setting sorting to be forward by the GSI sort key of elevationMetres
	//withScanIndexForward (false is descending, true is ascending) based on the sort key(elevationMetres) of the GSI
	DynamoDBQueryExpression<Letter> queryExpression = new DynamoDBQueryExpression<Letter>()
                .withIndexName(gsiIndex)
                .withConsistentRead(false)
                .withKeyConditionExpression(filterExpression)
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(true);
        
	return mapper.query(Letter.class, queryExpression);
  }
      
  public List<Letter> getAllLetters() 
  {
    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
    return this.mapper.scan(Letter.class, scanExpression);
  }

  public String getLetterHistoryJSON(String message)
  {
    //message will be of format "LETTER#ABCD"
	String letterHistoryJson ="";
    	
	//Let's Get the data required
	LetterFacade letterFacade = new LetterFacade(System.getenv("LETTERSDYNAMODBTABLE"));
	//Get the Letter Rows
	List<Letter> letterRows = letterFacade.getLettersByPKandBegins_WithSK("LETTER", message);
	    
	//Let's get the Toy Rows
	List<Letter> products = letterFacade.getLettersByPK("TOY");


	//Let's persist that in the LETTER Entry
	String summaryRowMessage = "";
	Letter summaryRow = null;
	List<Letter> letterToys = new ArrayList<Letter>();
	for (int i=0;i<letterRows.size();i++)
	{
	  Letter row = letterRows.get(i);
	  if (row.getSK().equals(message+"SUMMARY"))
	  {
	    summaryRow = row;
	    summaryRowMessage += "Found summaryRow ("+summaryRow+")";
	  }
	  else
      {
	    letterToys.add(row);
	  }
	}
	
	// GENERATE THE LETTERHISTORY JSON
	//Create Response
	JSONObject letterHistory = new JSONObject();
	//Create Letter
	JSONObject letter = new JSONObject();
	letterHistory.put("letter", letter);

	letter.put("id", Util.getContents(summaryRow.getSK(), "#", "SUMMARY"));
	letter.put("address", summaryRow.getString2());
	letter.put("total", ""+summaryRow.getDouble1());
	letter.put("dateOrdered", ""+summaryRow.getDate1());
		  
	JSONArray letterToyArray = new JSONArray();
	letter.put("toys", letterToyArray);
		  
	for (int i=0;i<letterToys.size();i++)
	{
	  Letter letterToy = letterToys.get(i);
	  JSONObject product = new JSONObject();
	  product.put("id", letterToy.getString1());
	  product.put("quantity", letterToy.getDouble1());
	  letterToyArray.add(product);
	}
		    
	//Create Products
	JSONArray productArray = new JSONArray();
	letterHistory.put("toys", productArray);
		  
	for (int i=0;i<products.size();i++)
	{
	  Letter dbProduct = products.get(i);
	  JSONObject product = new JSONObject();
	  product.put("id", Util.getContentsAfterDelimeter(dbProduct.getSK(), "#"));				  
	  product.put("name", dbProduct.getString2());
	  product.put("description", dbProduct.getString3());
	  product.put("price", dbProduct.getDouble1());
	  productArray.add(product);
    }

    letterHistoryJson = letterHistory.toJSONString();    	
    return letterHistoryJson;
  }
    	
}

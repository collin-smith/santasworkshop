package com.myorg.lambda;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.simple.JSONObject;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.myorg.dynamodb.Letter;
import com.myorg.dynamodb.LetterFacade;
import com.myorg.util.Util;

public class GetLetterToysLambdaHandler implements RequestHandler<Object, Object>{

/*
 * 		{
			"letters":[
			{
  				"id": "9383",
  				"address":"123 Candycane lane",
  				"toys":
  				[
     				{"id": "A", "quantity":"1"},
     				{"id": "B", "quantity":"2"},
     				{"id": "C", "quantity":"3"}
  				]
			},	
						{
  				"id": "9383",
  				"address":"Rudolph",
  				"toys":
  				[
     				{"id": "A", "quantity":"1"},
     				{"id": "C", "quantity":"2"}
  				]
			},		
]
 */
  @Override
  public Object handleRequest(Object event, Context context)
  {	  
    JSONObject response = new JSONObject();
	Connection connection = null;
	try
	{
      String letterId = Util.getContents(event.toString(), "pathParameters={id=", "}, stageVariables");
		    
      //GET FROM RDS
      String JDBC_PREFIX = "jdbc:postgres://";
      String dbEndpoint = System.getenv("DBENDPOINT");
      String portNumber = "5432";
      String databasename = System.getenv("DATABASENAME");
      String username = System.getenv("USERNAME");
      String password = System.getenv("PASSWORD");
      String url = JDBC_PREFIX + dbEndpoint+":"+portNumber+"/"+databasename;
      response.put("url", url);
		    
      context.getLogger().log("Get LetterToysLambda url="+url);
      context.getLogger().log("username="+username);
      context.getLogger().log("password="+password);

      try 
      {
        context.getLogger().log("Loading Driver class");
        Class.forName("org.postgresql.Driver");
        context.getLogger().log("Getting connection");
        connection = DriverManager.getConnection(url, username, password); 
        context.getLogger().log("Completed getConnection call");
      } 
      catch (Exception e) 
      {
        context.getLogger().log("Exception:"+e.getMessage()+":"+e.getStackTrace());
      }

      if (connection!=null)
      {
        context.getLogger().log("** We have a connection!");
        ResultSet rs;
        Statement statement = connection.createStatement();
        rs = statement.executeQuery("SELECT LETTERHISTORYJSON FROM LETTERHISTORY ORDER BY ID DESC");
        context.getLogger().log("E");
        String letterHistoryJsonRDS = "{}";
        if (rs.next())
        {
	      letterHistoryJsonRDS = rs.getString("LETTERHISTORYJSON");
	    }
		response.put("Letter History from RDS", letterHistoryJsonRDS);
      }

		    
		    // GET FROM DYNAMODB
		  	LetterFacade letterFacade = new LetterFacade(System.getenv("LETTERSDYNAMODBTABLE"));
		  	List<Letter> letterRows = new ArrayList<Letter>();
		    if (letterId.length()>0)
		    {
		      //Get a specific Letter SUMMARY Row based on the incoming id
		      letterRows = letterFacade.getLettersByPKandBegins_WithSK("LETTER", "LETTER#"+letterId+"SUMMARY");
		    }
		    
		    String message = "";
		    String letterHistory = "";
		    if (letterRows.size()==1)
		    {
		      Letter row = letterRows.get(0);
		      letterHistory = row.getString3();
		      message = "Successfully retrieved the letter history.";
		    }
		    else
		    {
		      message = "Error: Did not find Letter Summary Row ("+letterRows.size() + " rowsfound.)";
		    	
		    }
		    response.put("letterId", letterId);
		    response.put("message", message);
		    response.put("Letter History from DynamoDB", letterHistory);
	  }
	  catch (Exception exc)
	  {
		  return error(response, exc);
	  }
	  finally
	  {
		  try {
			  if (connection!=null) 
			  {
			    connection.close();
			  }
		  }
		  catch (Exception exc)
		  {
			  context.getLogger().log("Exc:"+exc.getMessage()+":"+exc.getStackTrace());
		  }
	  }

	  return ok(response);
  }
  
    private APIGatewayProxyResponseEvent ok(JSONObject response) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(response.toJSONString())
                .withIsBase64Encoded(false);
    }

    private APIGatewayProxyResponseEvent error(JSONObject response, Exception exc) {
        String exceptionString = String.format("error: %s: %s", exc.getMessage(), Arrays.toString(exc.getStackTrace()));
        response.put("Exception", exceptionString);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withBody(response.toJSONString())
                .withIsBase64Encoded(false);
    }
}

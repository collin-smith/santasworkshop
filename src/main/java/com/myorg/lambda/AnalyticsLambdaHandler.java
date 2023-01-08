package com.myorg.lambda;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.myorg.dynamodb.LetterFacade;


//public class AnalyticsLambdaHandler implements RequestHandler<Object, Object>{
public class AnalyticsLambdaHandler implements RequestHandler<SQSEvent, Object>{
	
	
  @Override
  public Object handleRequest(SQSEvent event, Context context)
  
  {	  
    JSONObject response = new JSONObject();
	Connection connection = null;
	try
	{
		  
	  //SQS(simple)
	  String message = "";
	  for(SQSMessage msg : event.getRecords())
	  {
	    message+= msg.getBody();        
	  }
	        
	        
	  //SNS Message from SQS
	  for(SQSMessage msg : event.getRecords())
	  {
	    JSONParser parser = new JSONParser();
	    String sqsBody = msg.getBody();
	    context.getLogger().log("AA sqsBody="+sqsBody);
	        	
	    Object obj = parser.parse(sqsBody);
	    context.getLogger().log("Object type"+ obj.getClass());
	    JSONObject jsonObject = (JSONObject)obj;
	    context.getLogger().log("jsonarray message = "+jsonObject.get("Message"));
	  	message = jsonObject.get("Message").toString();
	  	context.getLogger().log("message="+message);
	  }


	        
	  context.getLogger().log("A");
	  context.getLogger().log("Message: "+message);	

	  response.put("lambda", "GetRDS");
      response.put("instructions", "Create the Aurora");
		    
      String JDBC_PREFIX = "jdbc:postgresql://";
      String dbEndpoint = System.getenv("DBENDPOINT");
	  String portNumber = "5432";
	  String databasename = System.getenv("DATABASENAME");
	  String username = System.getenv("USERNAME");
	  String password = System.getenv("PASSWORD");
	  response.put("JDBC_PREFIX", JDBC_PREFIX);
	  response.put("dbEndpoint", dbEndpoint);
	  response.put("databasename", databasename);
	  response.put("username", username);
		    
	  context.getLogger().log("B");

	  String url = JDBC_PREFIX + dbEndpoint+":"+portNumber+"/"+databasename;
	  response.put("url", url);    
      context.getLogger().log("Analystics url="+url);
      context.getLogger().log("username="+username);
	  context.getLogger().log("password="+password);
		    
	  try 
      {
	    context.getLogger().log("2 Loading Driver class");
	    Class.forName("org.postgresql.Driver");
	    context.getLogger().log("Getting connection");
        connection = DriverManager.getConnection(url, username, password); 
	    context.getLogger().log("Completed getConnection call");
	  } 
	  catch (Exception e) 
	  {
	    context.getLogger().log("Exception:"+e.getMessage()+":"+e.getStackTrace());
		message = "Exception:"+e.getMessage()+":"+e.getStackTrace();
	  }
		    
	  context.getLogger().log("C");
    
	  String reads = "";
	  if (connection!=null)
	  {
	    context.getLogger().log("** We have a connection!");
		ResultSet rs;
		Statement statement = connection.createStatement();
		rs = statement.executeQuery("SELECT LETTERHISTORYJSON FROM LETTERHISTORY");
	    context.getLogger().log("E");
	    int numberRecords = 0;
	    String description = "";
		while ( rs.next() ) 
		{
		  context.getLogger().log("F");
		  numberRecords++;
		  description = rs.getString("LETTERHISTORYJSON");
		  reads+="*"+description+"*";
		}
		response.put("numberRecords", numberRecords);
		response.put("reads", reads);
	    context.getLogger().log("G description="+description);

	    LetterFacade letterFacade = new LetterFacade(System.getenv("LETTERSDYNAMODBTABLE"));
	    context.getLogger().log("H");
	    String letterHistoryJson = letterFacade.getLetterHistoryJSON(message);
		context.getLogger().log("I");
		
		//Let's insert a record
	    String insertStatement = "INSERT INTO LETTERHISTORY (LETTERID, LETTERHISTORYJSON, CREATED) VALUES (?,?, now())";
	    context.getLogger().log("J");

		// create the insert preparedstatement
		PreparedStatement preparedStatement = connection.prepareStatement(insertStatement);
		preparedStatement.setString (1, message);
		preparedStatement.setString (2, letterHistoryJson);		        
		preparedStatement.execute();
	
     }
		    
	 context.getLogger().log("J");
		    
   }
   catch (Exception exc)
   {
     context.getLogger().log("Exception: "+exc.getMessage()+":"+exc.getStackTrace());
     return error(response, exc);
   }
   finally
   {
     try
     {
	   if (connection!=null)
	   {
	     connection.close();
	   }	  
     }
     catch (Exception exc)
     {
	    context.getLogger().log("Connection exc"+exc.getMessage());
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

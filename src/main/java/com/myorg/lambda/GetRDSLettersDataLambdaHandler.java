package com.myorg.lambda;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import org.json.simple.JSONObject;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;


public class GetRDSLettersDataLambdaHandler implements RequestHandler<Object, Object>{


  @Override
  public Object handleRequest(Object event, Context context)
  {	  
    JSONObject response = new JSONObject();
    Connection connection = null;
    try
    {
			  		    		    
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
      String url = JDBC_PREFIX + dbEndpoint+":"+portNumber+"/"+databasename;
      response.put("url", url);

      context.getLogger().log("url="+url);
      context.getLogger().log("username="+username);
      context.getLogger().log("password="+password);
			  
      String message = "success";

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
          message = "Exception:"+e.getMessage()+":"+e.getStackTrace();
          }
      response.put("message", message);
      context.getLogger().log("Connection="+connection);
			    
      String jsons = "";
      if (connection!=null)
      {
          //Try to read from the rds database
          ResultSet rs;
          Statement statement = connection.createStatement();
          rs = statement.executeQuery("SELECT LETTERHISTORYJSON FROM LETTERHISTORY");
          int numberRecords = 0;
          while ( rs.next() ) 
          {
            context.getLogger().log("C");
            numberRecords++;
            String description = rs.getString("LETTERHISTORYJSON");
            jsons+= description+",";
          }
          context.getLogger().log("D");
          response.put("numberRecordsRead", numberRecords);
          response.put("jsons", jsons);
				        
          //Let's insert a record
          String insertStatement = " insert into LETTERHISTORY (LETTERID, LETTERHISTORYJSON, CREATED) values (?,?, now())";
          context.getLogger().log("Preparing the SQL Statement");

          // create the postgres insert preparedstatement
          PreparedStatement preparedStatement = connection.prepareStatement(insertStatement);
          preparedStatement.setString (1, "LETTER#12345");
          preparedStatement.setString (2, "{\"LetterHistory Json"+new Date()+"\"}");
          preparedStatement.execute();
          context.getLogger().log("Completed SQL Statement");
			               
          connection.close();
			    
          context.getLogger().log("Closed the connection");
        }    
      }
	  catch (Exception exc)
	  {
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

  private APIGatewayProxyResponseEvent ok(JSONObject response) 
  {
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
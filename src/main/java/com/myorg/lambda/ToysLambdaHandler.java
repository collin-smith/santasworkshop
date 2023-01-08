package com.myorg.lambda;

import java.util.Arrays;
import java.util.Date;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.myorg.dynamodb.LetterFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class ToysLambdaHandler implements RequestHandler<Object, Object>{


	/*
* Expected request:
{
   "id":"D",
   "price":"89",
   "name":"Santa Hat",
   "description":"official version"
}

Expected response:
{
"id": "D",
"message" : "successfully saved"
}

	 */
    
    
  @Override
  public Object handleRequest(Object event, Context context)
  {	  
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    JSONObject response = new JSONObject();

    try
    {

      //Process Request {the request body}
      String eventJson = gson.toJson(event);			
      Object obj = new JSONParser().parse(eventJson);
      JSONObject jsonParser = (JSONObject) obj;
      String bodyJson = (String) jsonParser.get("body");  
      obj = new JSONParser().parse(bodyJson);
      jsonParser = (JSONObject) obj;
      String id = (String) jsonParser.get("id");
      Double price = Double.parseDouble(jsonParser.get("price").toString());
      String name = (String) jsonParser.get("name");
      String description = (String) jsonParser.get("description");
		  
      //Persist the Toy to the Database
      LetterFacade letterFacade = new LetterFacade(System.getenv("LETTERSDYNAMODBTABLE"));
      letterFacade.getLetterBuilder().PK("TOY").SK("TOY#"+id).string1("SUMMARY").double1(price).string2(name).string3(description).date1(new Date()).save();

      //Build the response
      response.put("id", id);
      response.put("message", "successfully saved");
		  		  
	}
	catch (Exception exc)
	{
      return error(response, exc);
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

  private APIGatewayProxyResponseEvent error(JSONObject response, Exception exc) 
  {
    String exceptionString = String.format("error: %s: %s", exc.getMessage(), Arrays.toString(exc.getStackTrace()));
    response.put("Exception", exceptionString);
    return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withBody(response.toJSONString())
                .withIsBase64Encoded(false);
  }
}

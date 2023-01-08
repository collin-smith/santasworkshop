package com.myorg.lambda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.myorg.dynamodb.Letter;
import com.myorg.dynamodb.LetterFacade;
import com.myorg.util.Util;

public class GetToysLambdaHandler implements RequestHandler<Object, Object>{

/*
 * 
 * {"toys":
			  [
			     {"id": "A", "name":"Chromebook Laptop", "description":"Student edition","quantity":"1"},
			     {"id": "B", "name":"Classic Headphones","description":"1","White":"2"},
			     {"id": "C", "name":"Black Mouse pad","description":"Black rubber","quantity":"3"}
			  ]
			  }
 */
  @Override
  public Object handleRequest(Object event, Context context)
  {	  
    JSONObject response = new JSONObject();
	try
	{
      String toyId = Util.getContents(event.toString(), "pathParameters={id=", "}, stageVariables");
      LetterFacade letterFacade = new LetterFacade(System.getenv("LETTERSDYNAMODBTABLE"));
      List<Letter> toyRows = new ArrayList<Letter>();
      if (toyId.length()>0)
      {
	    //Get a specific Toy based on the incoming id
		toyRows = letterFacade.getLettersByPKandBegins_WithSK("TOY", "TOY#"+toyId);
      }
      else
      {
        //return them all
		toyRows = letterFacade.getLettersByPKandBegins_WithSK("TOY", "TOY#");
	  }
		    
	  JSONArray toysArray = new JSONArray();			    
	  response.put("toys", toysArray);
		    
	  for (int j=0;j<toyRows.size();j++)
      {
        JSONObject letter = new JSONObject();
        toysArray.add(letter);
        Letter letterToy = toyRows.get(j);
        letter.put("id", Util.getContentsAfterDelimeter(letterToy.getSK(),"TOY#"));
        letter.put("name", ""+letterToy.getString2());
        letter.put("description", ""+letterToy.getString3());
        letter.put("price", ""+letterToy.getDouble1());  
      }
		    
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
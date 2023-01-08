package com.myorg.lambda;

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

public class GetToyLettersLambdaHandler implements RequestHandler<Object, Object>{

  @Override
  public Object handleRequest(Object event, Context context)
  {	  
    JSONObject response = new JSONObject();

	  try
	  {
	    //Get all the letters for a given toy
		  
		context.getLogger().log("A ");
		String toyId = Util.getContents(event.toString(), "pathParameters={id=", "}, stageVariables");
		LetterFacade letterFacade = new LetterFacade(System.getenv("LETTERSDYNAMODBTABLE"));
	    context.getLogger().log("B toyId= "+toyId);

		List<Letter> letters = letterFacade.getLettersForAToy(toyId);
	    context.getLogger().log("Got the results");
	    context.getLogger().log("Number returned="+letters.size());
			
			
		//Create letter Array
		JSONArray letterArray = new JSONArray();			    
		response.put("toyId", toyId);
		response.put("letters", letterArray);
	    context.getLogger().log("D ");
		    
	    double totalNumberOfToysOrdered = 0;
	    for (int i=0;i<letters.size();i++)
		{
	      context.getLogger().log("E ");

	      Letter letter = letters.get(i);
	      JSONObject jsonToy = new JSONObject();
	      letterArray.add(jsonToy);
	      jsonToy.put("letterId", Util.getContents(letter.getSK(), "LETTER#", "TOY"));
	      jsonToy.put("quantity", letter.getDouble1());
	      totalNumberOfToysOrdered += letter.getDouble1();
	    }
	    context.getLogger().log("F ");

	    response.put("totalNumberOfToysOrdered", totalNumberOfToysOrdered);
	    context.getLogger().log("G ");
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

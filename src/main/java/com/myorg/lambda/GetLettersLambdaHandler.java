package com.myorg.lambda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.myorg.dynamodb.Letter;
import com.myorg.dynamodb.LetterFacade;
import com.myorg.util.Util;

public class GetLettersLambdaHandler implements RequestHandler<Object, Object>{

/*
 *Expected Response
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
	boolean scanForward = true;

	try
	{
      String letterId = Util.getContents(event.toString(), "pathParameters={id=", "}, stageVariables");
	  LetterFacade letterFacade = new LetterFacade(System.getenv("LETTERSDYNAMODBTABLE"));
	  List<Letter> allLetterRows = new ArrayList<Letter>();
	  if (letterId.length()>0)
	  {
	    //Get a specific Letter based on the incoming id
		allLetterRows = letterFacade.getLettersByPKandBegins_WithSK("LETTER", "LETTER#"+letterId);
	  }
	  else
	  {
	    //return them all
	    allLetterRows = letterFacade.getAllLetters();
      }
		    
      HashMap<String,String> ids = new HashMap<String,String>(); 
	  //Go through and determine all the unique ids
	  for (int i=0;i<allLetterRows.size();i++)
	  {
	    Letter currentRow = allLetterRows.get(i);
		String id = Util.getContents(currentRow.getSK(),"LETTER#","SUMMARY");
		if (id.length()>0 && ids.containsKey(id)==false)
		{
	      context.getLogger().log("found a new: "+id);
		  ids.put(id, id);
		}
      }
		    
		   
	  //Create letter Array
	  JSONArray letterArray = new JSONArray();			    
      response.put("letters", letterArray);
		    	    
	  //Generate all of the JSON By looping through all of the ids
	  for (String currentId : ids.keySet()) 
	  {    	
	    context.getLogger().log("looking at currentId= "+currentId);
		    	
		//Get the rows the currentId
		List<Letter> letterRows = new ArrayList<Letter>();
		for (int i=0;i<allLetterRows.size();i++)
	    {
	      Letter currentRow = allLetterRows.get(i);
		  if (currentRow.getSK().startsWith("LETTER#"+currentId))
		  {
		    letterRows.add(currentRow);
		  }
	    }
			   
		Letter summaryRow = null;
	    List<Letter> letterToys = new ArrayList<Letter>();
	    for (int i=0;i<letterRows.size();i++)
	    {
	      Letter row = letterRows.get(i);
		  if (row.getSK().equals("LETTER#"+currentId+"SUMMARY"))
		  {
		    summaryRow = row;
		  }
		  else
		  {
		    letterToys.add(row);
		  }
	    }
			    
	    JSONObject letter = new JSONObject();
	    letterArray.add(letter);

	    context.getLogger().log("summaryRow: "+summaryRow);
	    context.getLogger().log("summaryRow: "+ summaryRow.getPK() +":"+summaryRow.getSK());
	    letter.put("id", Util.getContents(summaryRow.getSK(), "#", "SUMMARY"));
	    letter.put("address", summaryRow.getString2());
	    letter.put("total", ""+summaryRow.getDouble1());
	    letter.put("dateReceived", ""+summaryRow.getDate1());
				
	    JSONArray letterToyArray = new JSONArray();
	    letter.put("toys", letterToyArray);
				  
		for (int i=0;i<letterToys.size();i++)
	    {
		  Letter toyLetter = letterToys.get(i);
		  JSONObject product = new JSONObject();
		  product.put("id", Util.getContentsAfterDelimeter(toyLetter.getString1(),"TOY#"));  
		  product.put("quantity", ""+toyLetter.getDouble1());
		  letterToyArray.add(product);
	    }

	  }  

	}
	catch (Exception exc)
	{
	  return error(response, exc);
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

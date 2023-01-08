package com.myorg.lambda;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.myorg.dynamodb.Letter;
import com.myorg.dynamodb.LetterFacade;

public class SaveLetterHistoryLambdaHandler implements RequestHandler<SQSEvent, Object>
{

/*
		{
			"letter":
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
			"toys":
			  [
			     {"id": "A", "name":"Chromebook Laptop", "description":"Student edition","quantity":"1"},
			     {"id": "B", "name":"Classic Headphones","description":"1","White":"2"},
			     {"id": "C", "name":"Black Mouse pad","description":"Black rubber","quantity":"3"}
			  ]
		}		
	*/	
		
		
		
  @Override
  public Object handleRequest(SQSEvent event, Context context)
  {	  
    try
	{
      //Get the Letter ID off of the message
      String message = "";
	  context.getLogger().log("Event: "+event.toString());
	  context.getLogger().log("Records: "+event.getRecords().toString());		  
	  context.getLogger().log("Running at "+new Date());
	  //Read a message 
	  String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
	  String started= "Start Time: " + timeStamp +":";

		  
	  //SQS(simple)
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
	     
	  context.getLogger().log("Message: "+message);		  
		  
	  LetterFacade letterFacade = new LetterFacade(System.getenv("LETTERSDYNAMODBTABLE"));
	  context.getLogger().log("Loading Letter Information "+ message);
	  List<Letter> summaryLetterRows = letterFacade.getLettersByPKandBegins_WithSK("LETTER", message+"SUMMARY");

	  if (summaryLetterRows!=null)
	  {
	    context.getLogger().log("Number SummaryLetter Rows="+summaryLetterRows.size());    	
      }

      String letterHistoryJson = letterFacade.getLetterHistoryJSON(message);
      context.getLogger().log("LetterHistoryJson="+letterHistoryJson);

			  
      //PERSIST THE LETTERHISTORY JSON TO THE SUMMARY ROW in STRING3
      if (summaryLetterRows != null)
      {
        Letter summaryRow = summaryLetterRows.get(0);
        context.getLogger().log("Saving summaryRow");
        summaryRow.setString3(letterHistoryJson);
        summaryRow.setDate1(new Date());
        context.getLogger().log("row="+summaryRow);
        letterFacade.getLetterBuilder().setLetter(summaryRow);
        letterFacade.getLetterBuilder().save();
        context.getLogger().log("should be saved");	
      }
      context.getLogger().log("Completed at "+ new Date());
      return null;	 		  
	}
	catch (Exception exc)
	{
	  context.getLogger().log("ZZZ");
	  context.getLogger().log("Exception "+ exc.getMessage() +":"+exc.getStackTrace());
	}
	return null;
  }
}

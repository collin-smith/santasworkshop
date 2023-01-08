package com.myorg.lambda;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.myorg.dynamodb.Letter;
import com.myorg.dynamodb.LetterFacade;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

public class LettersLambdaHandler implements RequestHandler<Object, Object>{
    
/*
 * 
 *     Expected Request:
{
  "id": "6666",
  "address":"123 Candycane lane",
  "toys":
  [
     {"id": "A", "quantity":"45"},
     {"id": "B", "quantity":"55"},
     {"id": "C", "quantity":"65"}
  ]
}

Expected Response:
{
"id": "6666",
"message" : "Successfully saved"
}

 *     
 */
    
    
  @Override
  public Object handleRequest(Object event, Context context)
  {	  
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    JSONObject response = new JSONObject();

    try
    {
      String message = "";
      context.getLogger().log("Reading JSON");
      //Get Letter Information from Request
      String eventJson = gson.toJson(event);			
      Object obj = new JSONParser().parse(eventJson);
      JSONObject jsonParser = (JSONObject) obj;
      String bodyJson = (String) jsonParser.get("body"); 
      Object jsonBody = new JSONParser().parse(bodyJson);
      jsonParser = (JSONObject) jsonBody;
      String letterId = (String) jsonParser.get("id");
      String address = (String) jsonParser.get("address");

			
      JSONArray toyArray = (JSONArray) jsonParser.get("toys");

      int numberOfToys = toyArray.size();
      if (numberOfToys==0)
      {
        message = "**Error**: At least one toy is required";
      }
      else
	  {
	    context.getLogger().log("Persisting");
	    //Persist the Letter records to the Database
	    LetterFacade letterFacade = new LetterFacade(System.getenv("LETTERSDYNAMODBTABLE"));
	    //Saving the letters as a whole (Product rows first and summary row after)
	    context.getLogger().log("Saving Product rows");
	
	    double totalPrice = 0.0D;
	    //Save the Product information
	    for (int i=0;i<toyArray.size();i++)
		{
		  JSONObject productObj = (JSONObject)toyArray.get(i);
		  String productId= (String)productObj.get("id");
		  Double quantity= (Double)Double.parseDouble(""+productObj.get("quantity"));

		  //Let's look up the toy to get the current price
		  List<Letter> toys = letterFacade.getLettersByPKandSK("TOY", "TOY#"+productId);
		  Double productPrice = 0.0D;
		  if (toys.size()>0)
		  {
	         //Found the letter
		     productPrice = toys.get(0).getDouble1();
		  }
				  			  
		  letterFacade.getLetterBuilder().PK("LETTER").SK("LETTER#"+letterId+"LETTER#"+productId).string1("TOY#"+productId).string2("").double1(quantity).date1(new Date()).save();		
		  totalPrice += (quantity * productPrice);		
		  context.getLogger().log("totalPrice:"+totalPrice+" quantity="+quantity+" productPrice="+productPrice);
		}
	
	    context.getLogger().log("Saving Summary row");
	
	    //Save the summary Record
	    letterFacade.getLetterBuilder().PK("LETTER").SK("LETTER#"+letterId+"SUMMARY").string1("SUMMARY").double1(totalPrice).string2(address).string3("{}").date1(new Date()).save();
	
	    message = "Successfully saved";
			  	
	    String messageBody = "LETTER#"+letterId;
	          
	    String topicArn = System.getenv("LETTERSSNSTOPICARN");
	    String topicName = System.getenv("LETTERSSNSTOPICNAME");

	    context.getLogger().log("D");
	    //Let's publish a message to the SNS Topic
	    //These should go to the 2 queues, email and text
	          
	    SnsClient snsClient  = SnsClient.create();
	    PublishRequest request = PublishRequest.builder()
	                  .message(messageBody)
	                  .topicArn(topicArn)
	                  .build();
	          
	    context.getLogger().log("E");
	          
	    PublishResponse result = snsClient.publish(request);
	    context.getLogger().log(result.messageId() + " Message sent. Status is " + result.sdkHttpResponse().statusCode());


		        
	  }

      response.put("id", letterId);
      response.put("message", message);
	        
	}
	catch (Exception exc)
	{
      context.getLogger().log("Exception="+exc.toString());
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

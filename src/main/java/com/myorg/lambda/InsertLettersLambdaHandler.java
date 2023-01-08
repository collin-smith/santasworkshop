package com.myorg.lambda;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONObject;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.myorg.dynamodb.Letter;
import com.myorg.dynamodb.LetterFacade;

public class InsertLettersLambdaHandler implements RequestHandler<Object, Object> {

  @SuppressWarnings({"unchecked"})
  @Override
  public Object handleRequest(Object event, Context context) 
  {
    JSONObject response = new JSONObject();
    String defaultLetter = "LETTER#3476";
    try 
    {
        	
      //Check to see if there are any DynamoDB entries
        LetterFacade letterFacade = new LetterFacade(System.getenv("LETTERSDYNAMODBTABLE"));
        List<Letter> letters = letterFacade.getLettersByPKandBegins_WithSK("LETTER", defaultLetter);
        response.put("region:", System.getenv("AWS_REGION"));
        response.put("tableName:", System.getenv("LETTERSDYNAMODBTABLE"));
        response.put("numberOfLetters already in Table", letters.size());
        for (int i=0;i<letters.size();i++)
        {
          response.put("letter["+i+"]", letters.get(i));
        }
            
            
        if (letters.size() > 0) return ok(response);

        //Only create some entries if they haven't been created already
        letterFacade.getLetterBuilder().PK("LETTER").SK("LETTER#3476SUMMARY").string1("SUMMARY").double1(150.00D).string2("123 Candycane lane").string3("{}").date1(new Date()).save();
        letterFacade.getLetterBuilder().PK("LETTER").SK("LETTER#3476TOY#A").string1("TOY#A").double1(1).date1(new Date()).save();
        letterFacade.getLetterBuilder().PK("LETTER").SK("LETTER#3476TOY#B").string1("TOY#B").double1(2).date1(new Date()).save();
        letterFacade.getLetterBuilder().PK("LETTER").SK("LETTER#3476TOY#C").string1("TOY#C").double1(3).date1(new Date()).save();
            
        letterFacade.getLetterBuilder().PK("LETTER").SK("LETTER#3728SUMMARY").string1("SUMMARY").double1(150.00D).string2("453 Grinch Avenue").string3("{}").date1(new Date()).save();
        letterFacade.getLetterBuilder().PK("LETTER").SK("LETTER#3728TOY#A").string1("TOY#A").double1(4).date1(new Date()).save();
        letterFacade.getLetterBuilder().PK("LETTER").SK("LETTER#3728TOY#B").string1("TOY#B").double1(3).date1(new Date()).save();

        letterFacade.getLetterBuilder().PK("LETTER").SK("LETTER#0393SUMMARY").string1("SUMMARY").double1(190.00D).string2("984 Rudolph Crescent").string3("{}").date1(new Date()).save();
        letterFacade.getLetterBuilder().PK("LETTER").SK("LETTER#0393TOY#B").string1("TOY#B").double1(3).date1(new Date()).save();
        letterFacade.getLetterBuilder().PK("LETTER").SK("LETTER#0393TOY#C").string1("TOY#C").double1(4).date1(new Date()).save();


        letterFacade.getLetterBuilder().PK("TOY").SK("TOY#A").string1("SUMMARY").double1(499).string2("Rubik's Cube").string3("Original Editin").date1(new Date()).save();
        letterFacade.getLetterBuilder().PK("TOY").SK("TOY#B").string1("SUMMARY").double1(50).string2("Cabbage Patch Doll").string3("Updated").date1(new Date()).save();
        letterFacade.getLetterBuilder().PK("TOY").SK("TOY#C").string1("SUMMARY").double1(10).string2("Lego").string3("Basic set").date1(new Date()).save();

        response.put("insertedData", true);
        response.put("lettersInserted", letterFacade.getLetterBuilder().count());
        response.put("log:", letterFacade.getLetterBuilder().getLog());
            
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
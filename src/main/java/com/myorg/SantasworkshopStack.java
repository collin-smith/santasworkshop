package com.myorg;

import software.constructs.Construct;

import java.util.HashMap;
import org.json.simple.JSONArray;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.Method;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.lambda.CfnFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;

public class SantasworkshopStack extends Stack 
{
  public SantasworkshopStack(final Construct scope, final String id) 
  {
    this(scope, id, null);
  }

  public SantasworkshopStack(final Construct scope, final String id, final StackProps props) 
  {
    super(scope, id, props);

    // Environment variable to separate the environments
    String environment = "dev";
        
    String tableName = environment + "-"+ "Letter";
    //Create a DynamoDB Table
    TableProps tableProps = TableProps.builder()
      .partitionKey(Attribute.builder()
      .name("PK")
      .type(AttributeType.STRING)
      .build())
      .sortKey(Attribute.builder()
      .name("SK")
      .type(AttributeType.STRING)
      .build())        	    
      .readCapacity(1)
      .writeCapacity(1)
      .removalPolicy(RemovalPolicy.DESTROY)
      .tableName(tableName)
      .build();

    Table lettersDynamoDbTable = new Table(this, tableName, tableProps);
    //Global Secondary Index
    //purpose of this GSI is to implement an Inverted index so that
    //We want to find the LETTERS that exist for a given TOY(STRING1)
    //Use the GSI on STRING1 to get all of the LETTER rows for a given toy  (Don't really need to use the SK now)
    //Once we have all those rows, we can use DOUBLE1 to get the Quantity requested for them.
    //We can use this to get the total quantity requested across all of the letters
    //Remember to update your Java class (Letter.java in this case) to note which attributes are part of the GSI
    lettersDynamoDbTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
      .indexName("String1GSIIndex")
      .partitionKey(Attribute.builder()
       .name("string1")
       .type(AttributeType.STRING)
       .build())
       //.sortKey(Attribute.builder()
       //    	 .name("SK")
       //    	 .type(AttributeType.NUMBER)
       //    	 .build())
      .readCapacity(1)
      .writeCapacity(1)
      .build());
        
    // SQS Queues
    Queue lettersHistoryQueue = Queue.Builder.create(this, environment + "-LetterHistory").visibilityTimeout(Duration.seconds(300)).build();
    Queue analyticsQueue = Queue.Builder.create(this, environment + "-Analytics").visibilityTimeout(Duration.seconds(300)).build();

    //SNS Topic
    Topic snsTopic = new Topic(this, "lettersSNSTopic");


    //Lambda Environment Variables to pass to the Lambdas
    HashMap<String, String> env = new HashMap <String, String>();
    env.put("ENVIRONMENT", environment);
    env.put("LETTERSDYNAMODBTABLE", lettersDynamoDbTable.getTableName());
    env.put("LETTERSHISTORYSQS", lettersHistoryQueue.getQueueName());
    env.put("LETTERSHISTORYSQSURL", lettersHistoryQueue.getQueueUrl());
    env.put("ANALYTICSSQS", analyticsQueue.getQueueName());
    env.put("ANALYTICSSQSURL", analyticsQueue.getQueueUrl());
    env.put("LETTERSSNSTOPICARN", snsTopic.getTopicArn());
    env.put("LETTERSSNSTOPICNAME", snsTopic.getTopicName());
        
    //PostgreSQL RDS Configuration
    env.put("DBENDPOINT", "mypostgres_dbendpoint");
    env.put("DATABASENAME", "santasworkshop");
    env.put("USERNAME", "myusername");
    env.put("PASSWORD", "mypassword");

        
    //SNS Topic subscriptions
    snsTopic.addSubscription(new SqsSubscription(lettersHistoryQueue));
    snsTopic.addSubscription(new SqsSubscription(analyticsQueue));
   
    //graviton2 lambda configuration
    String key = "Architectures";
    JSONArray values = new JSONArray();
    values.add("arm64");
        
        
    int memorySize = 1024;
    Function insertLettersLambdaFunction = Function.Builder.create(this, "insertLettersLambda")
                .runtime(Runtime.JAVA_11)
                .functionName("insertLettersLambda")
                .timeout(Duration.seconds(50))
                .memorySize(memorySize)
                .environment(env)
                .code(Code.fromAsset("target/santasworkshop-0.1.jar"))
                .handler("com.myorg.lambda.InsertLettersLambdaHandler::handleRequest")
                .build();
    //configure to run as a graviton2 lambda
    CfnFunction cfnFunction = (CfnFunction)insertLettersLambdaFunction.getNode().getDefaultChild();
    cfnFunction.addPropertyOverride(key, values);        
    lettersDynamoDbTable.grantFullAccess(insertLettersLambdaFunction);
        
    Function getLettersLambdaFunction = Function.Builder.create(this, "getLettersLambda")
                .runtime(Runtime.JAVA_11)
                .functionName("getLettersLambda")
                .timeout(Duration.seconds(50))
                .memorySize(memorySize)
                .environment(env)
                .code(Code.fromAsset("target/santasworkshop-0.1.jar"))
                .handler("com.myorg.lambda.GetLettersLambdaHandler::handleRequest")
                .build();
    //configure to run as a graviton2 lambda
    cfnFunction = (CfnFunction)getLettersLambdaFunction.getNode().getDefaultChild();
    cfnFunction.addPropertyOverride(key, values);        
    lettersDynamoDbTable.grantFullAccess(getLettersLambdaFunction);

    Function getToysLambdaFunction = Function.Builder.create(this, "getToysLambda")
                .runtime(Runtime.JAVA_11)
                .functionName("getToysLambda")
                .timeout(Duration.seconds(50))
                .memorySize(memorySize)
                .environment(env)
                .code(Code.fromAsset("target/santasworkshop-0.1.jar"))
                .handler("com.myorg.lambda.GetToysLambdaHandler::handleRequest")
                .build();
    //configure to run as a graviton2 lambda
    cfnFunction = (CfnFunction)getToysLambdaFunction.getNode().getDefaultChild();
    cfnFunction.addPropertyOverride(key, values);        
    lettersDynamoDbTable.grantFullAccess(getToysLambdaFunction);

        
    Function ToysLambdaFunction = Function.Builder.create(this, "ToysLambda")
                .runtime(Runtime.JAVA_11)
                .functionName("ToysLambda")
                .timeout(Duration.seconds(50))
                .memorySize(memorySize)
                .environment(env)
                .code(Code.fromAsset("target/santasworkshop-0.1.jar"))
                .handler("com.myorg.lambda.ToysLambdaHandler::handleRequest")
                .build();
    //configure to run as a graviton2 lambda
    cfnFunction = (CfnFunction)ToysLambdaFunction.getNode().getDefaultChild();
    cfnFunction.addPropertyOverride(key, values); 
    lettersDynamoDbTable.grantFullAccess(ToysLambdaFunction);

    Function LettersLambdaFunction = Function.Builder.create(this, "LettersLambda")
                .runtime(Runtime.JAVA_11)
                .functionName("LettersLambda")
                .timeout(Duration.seconds(50))
                .memorySize(memorySize)
                .environment(env)
                .code(Code.fromAsset("target/santasworkshop-0.1.jar"))
                .handler("com.myorg.lambda.LettersLambdaHandler::handleRequest")
                .build();
    //configure to run as a graviton2 lambda
    cfnFunction = (CfnFunction)LettersLambdaFunction.getNode().getDefaultChild();
    cfnFunction.addPropertyOverride(key, values); 
    lettersDynamoDbTable.grantFullAccess(LettersLambdaFunction);
    lettersHistoryQueue.grantSendMessages(LettersLambdaFunction);
    lettersHistoryQueue.grantConsumeMessages(LettersLambdaFunction);
    analyticsQueue.grantSendMessages(LettersLambdaFunction);
    analyticsQueue.grantConsumeMessages(LettersLambdaFunction);
    snsTopic.grantPublish(LettersLambdaFunction);
        
        
    Function saveLetterHistoryLambdaFunction = Function.Builder.create(this, "saveLetterHistoryLambda")
                .runtime(Runtime.JAVA_11)
                .functionName("saveLetterHistoryLambda")
                .timeout(Duration.seconds(50))
                .memorySize(memorySize)
                .environment(env)
                .code(Code.fromAsset("target/santasworkshop-0.1.jar"))
                .handler("com.myorg.lambda.SaveLetterHistoryLambdaHandler::handleRequest")
                .build();
    //configure to run as a graviton2 lambda
    cfnFunction = (CfnFunction)saveLetterHistoryLambdaFunction.getNode().getDefaultChild();
    cfnFunction.addPropertyOverride(key, values); 
    lettersDynamoDbTable.grantFullAccess(saveLetterHistoryLambdaFunction);
    lettersHistoryQueue.grantSendMessages(saveLetterHistoryLambdaFunction);
    lettersHistoryQueue.grantConsumeMessages(saveLetterHistoryLambdaFunction);
    saveLetterHistoryLambdaFunction.addEventSource(new SqsEventSource(lettersHistoryQueue));

    Function getLetterToysLambdaFunction = Function.Builder.create(this, "getLetterToysLambda")
                .runtime(Runtime.JAVA_11)
                .functionName("getLetterToysLambda")
                .timeout(Duration.seconds(50))
                .memorySize(memorySize)
                .environment(env)
                .code(Code.fromAsset("target/santasworkshop-0.1.jar"))
                .handler("com.myorg.lambda.GetLetterToysLambdaHandler::handleRequest")
                .build();
    //configure to run as a graviton2 lambda
    cfnFunction = (CfnFunction)getLetterToysLambdaFunction.getNode().getDefaultChild();
    cfnFunction.addPropertyOverride(key, values); 
    lettersDynamoDbTable.grantFullAccess(getLetterToysLambdaFunction);

    Function getToyLettersLambdaFunction = Function.Builder.create(this, "getToyLettersLambda")
                .runtime(Runtime.JAVA_11)
                .functionName("getToyLettersLambda")
                .timeout(Duration.seconds(50))
                .memorySize(memorySize)
                .environment(env)
                .code(Code.fromAsset("target/santasworkshop-0.1.jar"))
                .handler("com.myorg.lambda.GetToyLettersLambdaHandler::handleRequest")
                .build();
        
    //configure to run as a graviton2 lambda
    cfnFunction = (CfnFunction)getLetterToysLambdaFunction.getNode().getDefaultChild();
    cfnFunction.addPropertyOverride(key, values); 
    lettersDynamoDbTable.grantFullAccess(getToyLettersLambdaFunction);

        
        
    Function getRDSDataLambdaFunction = Function.Builder.create(this, "getRDSLettersDataLambda")
                .runtime(Runtime.JAVA_11)
                .functionName("getRDSLettersDataLambda")
                .timeout(Duration.seconds(50))
                .memorySize(memorySize)
                .environment(env)
                .code(Code.fromAsset("target/santasworkshop-0.1.jar"))
                .handler("com.myorg.lambda.GetRDSLettersDataLambdaHandler::handleRequest")
                .build();
    //configure to run as a graviton2 lambda
    cfnFunction = (CfnFunction)getRDSDataLambdaFunction.getNode().getDefaultChild();
    cfnFunction.addPropertyOverride(key, values); 

    Function analyticsLambdaFunction = Function.Builder.create(this, "analyticsHistoryLambda")
                .runtime(Runtime.JAVA_11)
                .functionName("analyticsLambda")
                .timeout(Duration.seconds(50))
                .memorySize(memorySize)
                .environment(env)
                .code(Code.fromAsset("target/santasworkshop-0.1.jar"))
                .handler("com.myorg.lambda.AnalyticsLambdaHandler::handleRequest")
                .build();
    //configure to run as a graviton2 lambda
    cfnFunction = (CfnFunction)analyticsLambdaFunction.getNode().getDefaultChild();
    cfnFunction.addPropertyOverride(key, values); 
    lettersDynamoDbTable.grantFullAccess(analyticsLambdaFunction);
    analyticsQueue.grantSendMessages(analyticsLambdaFunction);
    analyticsQueue.grantConsumeMessages(analyticsLambdaFunction);
    analyticsLambdaFunction.addEventSource(new SqsEventSource(analyticsQueue));

                
    //API Gateway Configuration (Allowing Lambdas to be called via the API Gateway
    RestApi api = RestApi.Builder.create(this, "Letters")
                .restApiName("Letters").description("Letters")
                .build();
        
    LambdaIntegration insertLettersIntegration = LambdaIntegration.Builder.create(insertLettersLambdaFunction) 
                .requestTemplates(new HashMap<String, String>() {{
                    put("application/json", "{ \"statusCode\": \"200\" }");
                }}).build();
        

    LambdaIntegration getLettersIntegration = LambdaIntegration.Builder.create(getLettersLambdaFunction) 
                .requestTemplates(new HashMap<String, String>() {{
                    put("application/json", "{ \"statusCode\": \"200\" }");
                }}).build();
        
    LambdaIntegration lettersIntegration = LambdaIntegration.Builder.create(LettersLambdaFunction) 
                .requestTemplates(new HashMap<String, String>() {{
                    put("application/json", "{ \"statusCode\": \"200\" }");
                }}).build();
        
    LambdaIntegration toysIntegration = LambdaIntegration.Builder.create(ToysLambdaFunction) 
                .requestTemplates(new HashMap<String, String>() {{
                    put("application/json", "{ \"statusCode\": \"200\" }");
                }}).build();
        
    LambdaIntegration getToysIntegration = LambdaIntegration.Builder.create(getToysLambdaFunction) 
                .requestTemplates(new HashMap<String, String>() {{
                    put("application/json", "{ \"statusCode\": \"200\" }");
                }}).build();
        
    LambdaIntegration getLetterToysIntegration = LambdaIntegration.Builder.create(getLetterToysLambdaFunction) 
                .requestTemplates(new HashMap<String, String>() {{
                    put("application/json", "{ \"statusCode\": \"200\" }");
                }}).build();
        
    LambdaIntegration getRDSDataIntegration = LambdaIntegration.Builder.create(getRDSDataLambdaFunction) 
                .requestTemplates(new HashMap<String, String>() {{
                    put("application/json", "{ \"statusCode\": \"200\" }");
                }}).build();        
        
    LambdaIntegration getToyLettersIntegration = LambdaIntegration.Builder.create(getToyLettersLambdaFunction) 
                .requestTemplates(new HashMap<String, String>() {{
                    put("application/json", "{ \"statusCode\": \"200\" }");
                }}).build();
        
        
         
        
    //It is up to you if you want to structure your lambdas in separate APIGateway APIs (RestApi)

    //Option 1: Adding at the top level of the APIGateway API
    //api.getRoot().addMethod("POST", helloIntegration);

    //Option 2: Or break out resources under one APIGateway API as follows     
        
    Resource insertLettersResource = api.getRoot().addResource("insertletters");
    Method insertLettersMethod = insertLettersResource.addMethod("POST", insertLettersIntegration);

    //Get Letters
    Resource lettersResource = api.getRoot().addResource("letters");
    Method getLettersMethod = lettersResource.addMethod("GET", getLettersIntegration);        

    //Individual Letters
    Resource getLetterResource = lettersResource.addResource("{id}");
    Method getLetterMethod = getLetterResource.addMethod("GET", getLettersIntegration);        

    //POST Letters
    Method putLettersMethod = lettersResource.addMethod("POST", lettersIntegration);        

    //Get Toys
    Resource ToysResource = api.getRoot().addResource("toys");
    Method getToysMethod = ToysResource.addMethod("GET", getToysIntegration);        

    //Individual Toys
    Resource getToyResource = ToysResource.addResource("{id}");
    Method getToyMethod = getToyResource.addMethod("GET", getToysIntegration);        
        
    //POST Toys
    Method ToysMethod = ToysResource.addMethod("POST", toysIntegration);        

    //Get Letter/{id}/Toys
    Resource letterResource = api.getRoot().addResource("letter");
    Resource letterIdResource = letterResource.addResource("{id}");
    Resource letterIdProductResource = letterIdResource.addResource("toys");
    Method getLetterToysMethod = letterIdProductResource.addMethod("GET", getLetterToysIntegration);        
        
    //Get RDS
    Resource rdsResource = api.getRoot().addResource("rds");
    Method getRDSDataMethod = rdsResource.addMethod("GET", getRDSDataIntegration);        

    //Get toyletters/{id}
    Resource getToyLettersResource = api.getRoot().addResource("toyletters");
    Resource toyLetterIdResource = getToyLettersResource.addResource("{id}");
    Method getToyLettersMethod = toyLetterIdResource .addMethod("GET", getToyLettersIntegration);        
   
    //CDK Output to display to the user the resultant information and urls
    CfnOutput.Builder.create(this, "ZA RegionOutput")
        .description("")
         .value("Region:"+ this.getRegion())
         .build();

    CfnOutput.Builder.create(this, "ZB DynamoDB Table")
        .description("")
        .value("DynamoDBTable:"+ lettersDynamoDbTable.getTableName())
        .build();

    CfnOutput.Builder.create(this, "ZC SQS Queue")
        .description("")
        .value("SQS Queue:"+ lettersHistoryQueue.getQueueName())
        .build();
    
    String urlPrefix = api.getUrl().substring(0, api.getUrl().length()-1);

    CfnOutput.Builder.create(this, "ZD POST /insertletters Lambda")
        .description("")
        .value("POST INSERTLETTERS Lambda:"+urlPrefix + insertLettersMethod.getResource().getPath())
        .build();
        
    CfnOutput.Builder.create(this, "ZE GET /letters Lambda")
        .description("")
        .value("Get Letters Lambda:"+urlPrefix + getLettersMethod.getResource().getPath())
        .build();

    CfnOutput.Builder.create(this, "ZF GET /letters/{id} Lambda")
       .description("")
       .value("Get Letters<id> Lambda:"+urlPrefix + getLetterMethod.getResource().getPath())
       .build();
       
    CfnOutput.Builder.create(this, "ZG POST /letter/ Lambda")
       .description("")
       .value("POST Letter Lambda:"+urlPrefix + putLettersMethod.getResource().getPath())
       .build();

    CfnOutput.Builder.create(this, "ZH GET /toys/ Lambda")
       .description("")
       .value("GET Toys Lambda:"+urlPrefix + getToysMethod.getResource().getPath())
       .build();
       
    CfnOutput.Builder.create(this, "ZI GET /toys/{id} Lambda")
       .description("")
       .value("GET Toys<id> Lambda:"+urlPrefix + getToyMethod.getResource().getPath())
       .build();
       
    CfnOutput.Builder.create(this, "ZJ POST /toys/ Lambda")
       .description("")
       .value("POST Toys Lambda:"+urlPrefix + ToysMethod.getResource().getPath())
       .build();

    CfnOutput.Builder.create(this, "ZK GET /letter/{id}/toys/ Lambda")
       .description("")
       .value("GET Letter Toys Lambda:"+urlPrefix + getLetterToysMethod.getResource().getPath())
       .build();
       
    CfnOutput.Builder.create(this, "ZM GET RDS Lambda")
       .description("")
       .value("RDS Lambda:"+urlPrefix + getRDSDataMethod.getResource().getPath())
       .build();
       
    CfnOutput.Builder.create(this, "ZM GET toyletters/{id} Lambda")
       .description("")
       .value("ToyLetters Lambda:"+urlPrefix + getToyLettersMethod.getResource().getPath())
       .build();
         
  }
  
}

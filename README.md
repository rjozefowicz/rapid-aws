# rapid-aws
Example lightweight client for AWS services that limits AWS Lambda cold starts for Java. It is pure-Java8 implementation without third-part libraries that increases fat JAR size and cold starts time.

It uses AWS Low-Level API described on https://docs.aws.amazon.com/index.html

Current implementation allows integration with:
* DynamoDB
* Lambda function
* SQS
* Comprehend

# Getting Started

## Example AWS Lambda

This example assumes that there is DynamoDB Table **rapid-aws-data** created with Primary Key **uuid** with String type. It uses **org.json** as JSON parser for better readibility during preparing JSON requests.

JSON request in the following format:
```javascript
{
  "input": "Text to be persisted in DynamoDB and analyzed by Comprehend"
}
```

Maven dependencies:
```xml
<dependencies>
         <dependency>
            <groupId>pl.r6lab.aws</groupId>
            <artifactId>rapid-aws</artifactId>
            <version>0.1.0</version>
        </dependency>
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-core</artifactId>
            <version>1.2.0</version>
        </dependency>
        <dependency>
            <groupId>org.json</groupId>
            <artifactId>json</artifactId>
            <version>20180813</version>
        </dependency>
    </dependencies>
```

Example AWS Lambda handler architecture:
![Lambda function architecture diagram](http://saltbae.s3-website-eu-west-1.amazonaws.com/rapid-aws-diagram.png)

Function code:
```java
public class TestFunction implements RequestHandler<Map<String, String>, Void> {

    private static final String RAPID_AWS_DATA_TABLE_NAME = "rapid-aws-data";
    private static final String DETECTED_ENTITIES_PROCESSOR_FUNCTION_NAME = "detected-entities-processor";

    private final RapidClient client = RapidClient.envAware();
    private final RapidLambdaInvocationClient lambdaClient = RapidLambdaInvocationClient.envAware();
    private final RapidSQSClient sqsClient = RapidSQSClient.envAware();

    public Void handleRequest(Map<String, String> data, Context context) {
        String input = data.get("input");
        if (nonNull(input)) {

            // 1. PutItem in DynamoDB
            String putItem = putItem(input);
            Response putItemResponse = client.execute(BasicRequest.of(ServiceName.DYNAMODB, "PutItem", HttpMethod.POST, putItem));

            if (putItemResponse.isSuccess()) {

                // 2. DetectEntities in Comprehend
                String detectEntities = detectEntities(input);
                Response detectEntitiesResponse = client.execute(BasicRequest.of(ServiceName.COMPREHEND, "DetectEntities", HttpMethod.POST, detectEntities));

                if (detectEntitiesResponse.isSuccess()) {

                    // 3. Send message to SQS queue
                    Response sendMessageResponse = sqsClient.execute(SQSRequest.of("SendMessage", detectEntitiesResponse.getPayload(), new HashMap<>(), getQueueUrl(), HttpMethod.POST));
                    if (!sendMessageResponse.isSuccess()) {
                        logError(ServiceName.SQS, detectEntitiesResponse.getPayload());
                    }

                    // 4. Invoke another lambda function
                    Response invokeResponse = lambdaClient.execute(LambdaInvokeRequest.of(DETECTED_ENTITIES_PROCESSOR_FUNCTION_NAME, detectEntitiesResponse.getPayload(), HttpMethod.POST));
                    if (!invokeResponse.isSuccess()) {
                        logError(ServiceName.LAMBDA, invokeResponse.getPayload());
                    }
                } else {
                    logError(ServiceName.COMPREHEND, detectEntitiesResponse.getPayload());
                }

            } else {
                logError(ServiceName.DYNAMODB, putItemResponse.getPayload());
            }
        }
        return null;
    }

    private String putItem(String input) {
        JSONObject request = new JSONObject();
        request.put("TableName", RAPID_AWS_DATA_TABLE_NAME);
        JSONObject item = new JSONObject();
        JSONObject uuid = new JSONObject();
        uuid.put("S", UUID.randomUUID());
        JSONObject value = new JSONObject();
        value.put("S", input);
        item.put("uuid", uuid);
        item.put("value", value);
        request.put("Item", item);
        String jsonValue = request.toString();
        System.out.println(jsonValue);
        return jsonValue;
    }

    private String detectEntities(String input) {
        JSONObject request = new JSONObject();
        request.put("LanguageCode", "en");
        request.put("Text", input);
        String jsonValue = request.toString();
        System.out.println(jsonValue);
        return jsonValue;
    }

    private void logError(ServiceName serviceName, String payload) {
        System.out.println("Something went wrong while accessing " + serviceName.getName());
        System.out.println(payload);
    }

    private String getQueueUrl() {
        return "YOUR_QUEUE_URL";
    }
```

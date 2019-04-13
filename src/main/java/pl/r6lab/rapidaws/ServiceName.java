package pl.r6lab.rapidaws;

public enum ServiceName {
    DYNAMODB("dynamodb", "DynamoDB_20120810", "application/x-amz-json-1.0"),
    COMPREHEND("comprehend", "Comprehend_20171127", "application/x-amz-json-1.1"),
    LAMBDA("lambda", "Lambda_2015_0331", "application/x-amz-json-1.1"),
    SNS("sns", "SNS_20100331", "application/x-amz-json-1.1"),
    SQS("sqs", "AmazonSQSv20121105", "application/x-www-form-urlencoded");

    private final String name;
    private final String version;
    private final String contentType;

    ServiceName(String name, String version, String contentType) {
        this.name = name;
        this.version = version;
        this.contentType = contentType;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getContentType() {
        return contentType;
    }
}

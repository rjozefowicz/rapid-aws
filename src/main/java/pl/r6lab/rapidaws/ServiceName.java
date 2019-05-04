package pl.r6lab.rapidaws;

public enum ServiceName {
    DYNAMODB("dynamodb", "DynamoDB_20120810", "application/x-amz-json-1.0", false),
    COMPREHEND("comprehend", "Comprehend_20171127", "application/x-amz-json-1.1", true),
    LAMBDA("lambda", "Lambda_2015_0331", "application/x-amz-json-1.1", true),
    SQS("sqs", "AmazonSQSv20121105", "application/x-www-form-urlencoded", false);

    private final String name;
    private final String version;
    private final String contentType;
    private final boolean httpsRequired;

    ServiceName(String name, String version, String contentType, boolean httpsRequired) {
        this.name = name;
        this.version = version;
        this.contentType = contentType;
        this.httpsRequired = httpsRequired;
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

    public boolean isHttpsRequired() {
        return httpsRequired;
    }
}

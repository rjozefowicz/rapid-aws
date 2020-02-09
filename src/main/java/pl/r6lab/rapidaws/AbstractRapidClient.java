package pl.r6lab.rapidaws;

import java.time.format.DateTimeFormatter;

public abstract class AbstractRapidClient implements RapidClient {

    private static final String FORCE_HTTPS = "FORCE_HTTPS";

    protected static final String SEPARATOR = "---------------";

    protected static final String X_AMZ_SECURITY_TOKEN = "x-amz-security-token";
    protected static final String AUTHORIZATION_HEADER = "Authorization";
    protected static final String SIGNATURE_KEY_DATE_PATTERN = "YYYYMMdd";
    protected static final String AWS_DATE_PATTERN = "YYYYMMdd'T'HHmmss'Z'";
    protected static final DateTimeFormatter AWS_DATE_FORMATTER = DateTimeFormatter.ofPattern(AWS_DATE_PATTERN);
    protected static final DateTimeFormatter SIGNATURE_KEY_DATE_FORMATTER = DateTimeFormatter.ofPattern(SIGNATURE_KEY_DATE_PATTERN);


    private final String region;
    private final Boolean forceHttps;

    private boolean printHeaders;

    protected AbstractRapidClient(String region) {
        this.region = region;
        this.printHeaders = false;
        this.forceHttps = Boolean.valueOf(System.getenv(FORCE_HTTPS));
        System.out.println("Force HTTPS: " + this.forceHttps);
    }

    public abstract Response execute(Request request);

    public void setPrintHeaders(boolean printHeaders) {
        this.printHeaders = printHeaders;
    }

    @Override
    public String getRegion() {
        return region;
    }

    protected boolean isPrintHeaders() {
        return printHeaders;
    }

    protected void printHeader(String header, String value) {
        System.out.println(header);
        System.out.println(value);
        System.out.println(SEPARATOR);
    }

}

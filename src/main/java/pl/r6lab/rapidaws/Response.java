package pl.r6lab.rapidaws;

public final class Response {
    private final boolean success;
    private final String payload;

    private Response(boolean success, String payload) {
        this.success = success;
        this.payload = payload;
    }

    public static Response success(String response) {
        return new Response(true, response);
    }

    public static Response fail(String response) {
        return new Response(false, response);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getPayload() {
        return payload;
    }
}

package pl.r6lab.rapidaws;

import spark.Spark;

public class DummyServer {
    public static void main(String[] args) {
        Spark.port(9000);
        Spark.post("/test", (request, response) -> {
            return "abcd";
        });
    }
}

package pl.r6lab.rapidaws;

public interface RapidClient {

    Response execute(Request request);

    String getRegion();

}

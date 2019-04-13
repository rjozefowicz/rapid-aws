package pl.r6lab.rapidaws;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class SignatureVersion4 {

    private static final String HMAC_ALGORITHM = "hmacSHA256";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("YYYYMMdd");

    private SignatureVersion4() {
    }

    public static final byte[] sha256(String toEncode) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(
                toEncode.getBytes(StandardCharsets.UTF_8));
    }

    public final static byte[] hmacSHA256(String data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param key
     * @param signatureDate just date without time
     * @param regionName
     * @param serviceName
     * @return
     * @throws Exception
     */
    public final static byte[] getSignatureKey(String key, LocalDate signatureDate, String regionName, String serviceName) throws Exception {
        String date = signatureDate.format(FORMATTER);
        return signatureKey(key, date, regionName, serviceName);
    }

    public final static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        return signatureKey(key, dateStamp, regionName, serviceName);
    }

    private static byte[] signatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kSecret = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSHA256(dateStamp, kSecret);
        byte[] kRegion = hmacSHA256(regionName, kDate);
        byte[] kService = hmacSHA256(serviceName, kRegion);
        return hmacSHA256("aws4_request", kService);
    }
}

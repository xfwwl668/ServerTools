import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.StringJoiner;

final class SubscriptionComposer {
    private SubscriptionComposer() {
    }

    static String build(String vlessUrl, String trojanUrl, String ssUrl, String tunnelUrl) {
        StringJoiner subscription = new StringJoiner("\n");
        subscription.add(vlessUrl);
        subscription.add(trojanUrl);
        subscription.add(ssUrl);
        if (tunnelUrl != null && !tunnelUrl.isBlank()) {
            subscription.add(tunnelUrl.trim());
        }
        return Base64.getEncoder().encodeToString(subscription.toString().getBytes(StandardCharsets.UTF_8));
    }
}

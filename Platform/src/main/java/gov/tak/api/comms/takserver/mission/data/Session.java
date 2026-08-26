package gov.tak.api.comms.takserver.mission.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Outlines an OAuth response: <a href="https://www.rfc-editor.org/rfc/rfc6749#section-5.1">Specification 5.1</a>
 *
 * @param accessToken OAuth access token.
 * @param tokenType Type of OAuth token.
 * @param expireTime Timestamp in epoch millis where the session expires at.
 * @param scope Scope of OAuth token.
 * @param jti Used as the {@code JSESSIONID} value.
 *
 * @since 3.5
 */
public record Session(@JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") @JsonDeserialize(using = RelativeToAbsoluteTimeConverter.class) long expireTime,
        String scope,
        String jti)
{

    /**
     * @return {@code true} when the session has expired.
     */
    public boolean isExpired()
    {
        return System.currentTimeMillis() >= expireTime();
    }

    /**
     * The OAuth session's duration of validity is given by the number of seconds provided by the {@code expires_in} field we get in the response.
     * This converter takes that duration and converts it to an absolute timestamp in epoch millis of when the session will no longer be valid.
     * <p/>
     * Since we do this at deserialization time, the timestamp is computed from when we get the response back from the server.
     * There is some degree of time lost in transit, along with any request we send going to the server.
     * We can prevent having to waste a request re-try by cutting the duration a bit short and pre-emptively re-authenticating early.
     * Realistically 5 seconds should be plenty of time as no request should take more than that amount of time to go through from our client
     * to the TAK Server.
     */
    private static class RelativeToAbsoluteTimeConverter extends StdDeserializer<Long>
    {
        /**
         * We'll want to cut the duration a bit short so that we don't trip the
         * expiration near the end of our allocated session.
         */
        private static final long LEEWAY_MILLIS = 5_000;

        protected RelativeToAbsoluteTimeConverter()
        {
            super(Long.class);
        }

        @Override
        public Long deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException
        {
            long durationSeconds = jsonParser.getValueAsLong();
            long now = System.currentTimeMillis();
            return (durationSeconds * 1000) + now - LEEWAY_MILLIS;
        }
    }
}

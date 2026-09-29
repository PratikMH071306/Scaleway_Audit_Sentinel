package com.secure_monitor.scaleway.iam;

import org.springframework.http.HttpStatusCode;

public class ScalewayApiException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String safeMessage;

    public ScalewayApiException(HttpStatusCode statusCode, String safeMessage, Throwable cause) {
        super(safeMessage, cause);
        this.statusCode = statusCode;
        this.safeMessage = safeMessage;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getSafeMessage() {
        return safeMessage;
    }
}

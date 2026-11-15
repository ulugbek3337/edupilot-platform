package com.eduflow.exception;

import org.springframework.http.HttpStatus;

public class EduFlowException extends RuntimeException {

    private final HttpStatus status;
    private final String messageCode;

    public EduFlowException(int status, String messageCode) {
        this(HttpStatus.valueOf(status), messageCode);
    }

    public EduFlowException(HttpStatus status, String messageCode) {
        super(messageCode);
        this.status = status;
        this.messageCode = messageCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageCode() {
        return messageCode;
    }
}

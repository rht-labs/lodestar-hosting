package com.redhat.labs.lodestar.hosting.exception;

public class HostingException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    public HostingException(String message) {
        super(message);
    }
    
    public HostingException(String message, Exception ex) {
        super(message, ex);
    }

}

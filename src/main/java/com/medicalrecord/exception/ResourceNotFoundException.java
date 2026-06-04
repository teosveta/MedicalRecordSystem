package com.medicalrecord.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s с %s '%s' не е намерен", resourceName, fieldName, fieldValue));
    }
}

package com.shoplite.api;

/** Error body returned by the REST API. */
public record ErrorDto(int status, String error) {
}

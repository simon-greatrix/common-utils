package com.pippsford.common;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * An exception with parameters that may be used to generate a detailed error message.
 *
 * @author Simon Greatrix on 19/01/2021.
 */
public class ParameterisedException extends Exception {

  /** Message parameters. */
  private final transient JsonObject parameters;

  /** Template used to render the parameters. */
  private final String template;


  public ParameterisedException(String message, String template, JsonObject parameters) {
    this(message, template, parameters, null);
  }


  /**
   * New instance.
   *
   * @param message    The error message.
   * @param template   The error template.
   * @param parameters parameters to the error response
   * @param cause      optional causative exception
   */
  public ParameterisedException(String message, String template, JsonObject parameters, Throwable cause) {
    super(message);
    this.template = template;
    this.parameters = parameters != null ? Json.createObjectBuilder(parameters).build() : JsonValue.EMPTY_JSON_OBJECT;
    if (cause != null) {
      initCause(cause);
    }
  }


  public JsonObject getParameters() {
    return parameters;
  }


  public String getTemplate() {
    return template;
  }

}

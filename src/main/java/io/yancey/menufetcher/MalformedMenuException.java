package io.yancey.menufetcher;

import java.io.*;

public class MalformedMenuException extends IOException {
  public MalformedMenuException() {
    super();
  }

  public MalformedMenuException(String message) {
    super(message);
  }

  public MalformedMenuException(Throwable cause) {
    super(cause);
  }

  public MalformedMenuException(String message, Throwable cause) {
    super(message, cause);
  }
}

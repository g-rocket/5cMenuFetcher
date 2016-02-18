package io.yancey.menufetcher;

import java.io.*;

public class MenuNotAvailableException extends IOException {
	public MenuNotAvailableException() {
		super();
	}
	
	public MenuNotAvailableException(String message) {
		super(message);
	}
	
	public MenuNotAvailableException(Throwable cause) {
		super(cause);
	}
	
	public MenuNotAvailableException(String message, Throwable cause) {
		super(message, cause);
	}
}

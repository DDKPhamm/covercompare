package com.covercompare.quote.http;

import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

public final class RestClients {

	private RestClients() {
	}

	/**
	 * Every outbound call gets explicit timeouts. The JDK default is to wait forever, which turns one
	 * hung downstream service into a hung quote service.
	 */
	public static RestClient create(RestClient.Builder builder, HttpConnection connection) {
		HttpClient httpClient = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(connection.connectTimeout())
			.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(connection.readTimeout());
		return builder.clone().baseUrl(connection.baseUrl().toString()).requestFactory(requestFactory).build();
	}

	/**
	 * Worth retrying: the service answered 5xx, or refused the connection. Not worth retrying: a
	 * timeout (the service is slow, so another attempt doubles the wait) or a 4xx (our request is
	 * wrong and will be wrong again).
	 */
	public static boolean isRetryable(Throwable failure) {
		if (failure instanceof HttpServerErrorException) {
			return true;
		}
		return failure instanceof ResourceAccessException && hasCause(failure, ConnectException.class);
	}

	public static boolean isTimeout(Throwable failure) {
		return hasCause(failure, HttpTimeoutException.class) || hasCause(failure, SocketTimeoutException.class);
	}

	private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			if (type.isInstance(cause)) {
				return true;
			}
		}
		return false;
	}

}

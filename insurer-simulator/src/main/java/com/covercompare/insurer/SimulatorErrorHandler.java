package com.covercompare.insurer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.covercompare.insurer.FaultInjector.SimulatedOutageException;

@RestControllerAdvice
class SimulatorErrorHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(SimulatedOutageException.class)
	ProblemDetail handleOutage(SimulatedOutageException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
	}

}

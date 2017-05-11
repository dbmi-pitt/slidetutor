package edu.pitt.dbmi.tutor.model;

public class TutorException extends Exception {
	public TutorException(String msg,Throwable cause){
		super(msg,cause);
	}
	public TutorException(String msg){
		super(msg);
	}
}

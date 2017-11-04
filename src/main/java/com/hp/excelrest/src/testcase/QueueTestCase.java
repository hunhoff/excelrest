package com.hp.excelrest.src.testcase;

public class QueueTestCase {

	public QueueTestCase(){}

	private String queue_to_publish = "";
	private String message_to_publish = "";
	private String queue_to_consume = "";

	public String getQueue_to_publish() {
		return queue_to_publish;
	}
	public void setQueue_to_publish(String queue_to_publish) {
		this.queue_to_publish = queue_to_publish;
	}
	public String getMessage_to_publish() {
		return message_to_publish;
	}
	public void setMessage_to_publish(String message_to_publish) {
		this.message_to_publish = message_to_publish;
	}
	public String getQueue_to_consume() {
		return queue_to_consume;
	}
	public void setQueue_to_consume(String queue_to_consume) {
		this.queue_to_consume = queue_to_consume;
	}
}
package edu.sjsu.cmpe.procurement.domain;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BookOrder {
	@JsonProperty("id")
	private String id;
	@JsonProperty("order_book_isbns")
	private ArrayList<Integer> order_book_isbns = new ArrayList<Integer>();
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the order_book_isbns
	 */
	public ArrayList<Integer> getOrder_book_isbns() {
		return order_book_isbns;
	}
	/**
	 * @param order_book_isbns the order_book_isbns to set
	 */
	public void setOrder_book_isbns(ArrayList<Integer> order_book_isbns) {
		this.order_book_isbns = order_book_isbns;
	}
}

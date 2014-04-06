/**
 * 
 */
package edu.sjsu.cmpe.library.domain;

import com.yammer.dropwizard.jersey.params.LongParam;

/**
 * @author parthkathuria
 *
 */
public class NewBookOrder {
	private String libraryName;
	private LongParam isbn;
	/**
	 * @return the libraryName
	 */
	public String getLibraryName() {
		return libraryName;
	}
	/**
	 * @param libraryName the libraryName to set
	 */
	public void setLibraryName(String libraryName) {
		this.libraryName = libraryName;
	}
	/**
	 * @return the isbn
	 */
	public LongParam getIsbn() {
		return isbn;
	}
	/**
	 * @param isbn the isbn to set
	 */
	public void setIsbn(LongParam isbn) {
		this.isbn = isbn;
	}

}

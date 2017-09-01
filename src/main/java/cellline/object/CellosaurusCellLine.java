/**
 * @file CellosaurusCellLineBean.java
 * @author Edison Ong
 * @since Jul 18, 2017
 * @version 1.0
 * @comment 
 */
package cellline.object;

import java.util.Arrays;
import java.util.HashSet;

/**
 * 
 */
public class CellosaurusCellLine extends CellLine {
	
	private String secondarAccession;
	/**
	 * @return the secondarAccession
	 */
	public String getSecondarAccession() {
		return secondarAccession;
	}
	/**
	 * @param secondarAccession the secondarAccession to set
	 */
	public void setSecondarAccession(String secondarAccession) {
		this.secondarAccession = secondarAccession;
	}
	
	private String category;
	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}
	/**
	 * @param category the category to set
	 */
	public void setCategory(String category) {
		this.category = category;
	}
	
	public CellosaurusCellLine() {
		super();
		this.database = "Cellosaurus";
	}
	
	public CellosaurusCellLine( String accession ) {
		super( accession );
		this.database = "Cellosaurus";
	}
	
	public CellosaurusCellLine( String database, String identifier ) {
		super( database, identifier );
		this.database = "Cellosaurus";
	}
}

/**
 * @file CellLineEfo.java
 * @author Edison Ong
 * @since Jul 20, 2017
 * @version 1.0
 * @comment 
 */
package cellline.object;

/**
 * 
 */
public class EFOCellLine extends CellLine {
	
	private String iri;
	/**
	 * @return the iri
	 */
	public String getIri() {
		return iri;
	}
	/**
	 * @param iri the iri to set
	 */
	public void setIri(String iri) {
		this.iri = iri;
	}
	
	public EFOCellLine() {
		super();
		this.database = "EFO";
	}
	
	public EFOCellLine( String accession ) {
		super( accession );
		this.database = "EFO";
	}
	
	public EFOCellLine( String database, String identifier ) {
		super( database, identifier );
		this.database = "EFO";
	}

}
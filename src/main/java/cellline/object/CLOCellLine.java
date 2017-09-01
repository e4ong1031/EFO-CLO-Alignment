/**
 * @file CLOCellLine.java
 * @author Edison Ong
 * @since Aug 4, 2017
 * @version 1.0
 * @comment 
 */
package cellline.object;

import java.util.HashSet;

/**
 * 
 */
public class CLOCellLine extends CellLine {
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
	
	public CLOCellLine() {
		super();
		this.database = "CLO";
	}
	
	public CLOCellLine( String accession ) {
		super( accession );
		this.database = "CLO";
	}
	
	public CLOCellLine( String database, String identifier ) {
		super( database, identifier );
		this.database = "CLO";
	}
	/**
	 * @param cloCellLine
	 */
	public CLOCellLine( CLOCellLine cloCellLine ) {
		super();
		this.accession = cloCellLine.getAccession();
		this.database = cloCellLine.getDatabase();
		this.identifier = cloCellLine.getIdentifier();
		this.iri = cloCellLine.getIri();
		this.name = cloCellLine.getName();
		this.synonyms = cloCellLine.getSynonyms();
		this.crossReferences = cloCellLine.getCrossReferences();
		this.citations = cloCellLine.getCitations();
		this.diseases = cloCellLine.getDiseases();
		this.species = cloCellLine.getSpecies();
		this.organismParts = cloCellLine.getOrganismParts();
		this.cellTypes = cloCellLine.getCellTypes();
	}
}

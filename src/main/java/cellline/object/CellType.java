/**
 * @file CellType.java
 * @author Edison Ong
 * @since Aug 15, 2017
 * @version 1.0
 * @comment 
 */
package cellline.object;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.validation.AccessionValidator;
import cellline.validation.CrossReferenceValidator;

/**
 * 
 */
public class CellType implements Comparable<CellType> {
	
	static final Logger logger = LoggerFactory.getLogger( CellType.class );
	
	private String database;
	/**
	 * @return the database
	 */
	public String getDatabase() {
		if ( this.database != null ) return database;
		else return this.accession.split( "_" )[0];
	}
	/**
	 * @param database the database to set
	 */
	public void setDatabase(String database) {
		database = database.trim();
		if ( !database.isEmpty() ) {
			this.database = database;
			if ( this.accession != null ) {
				logger.trace( "Update cellType database" );
				String[] tokens = this.accession.split( "_" );
				this.setAccession( database + "_" + tokens[1] );
			}
		} else {
			logger.warn( "Empty database" );
		}
	}
	
	private String identifier;
	/**
	 * @return the identifier
	 */
	public String getIdentifier() {
		if ( this.identifier != null ) return this.identifier;
		else return this.accession.split( "_" )[1];
	}
	/**
	 * @param identifer the identifier to set
	 */
	public void setIdentifier(String identifier) {
		identifier = identifier.trim();
		if ( !identifier.isEmpty() ) {
			this.identifier = identifier;
			if ( this.accession != null ) {
				logger.trace( "Update cellType identifier" );
				String[] tokens = this.accession.split( "_" );
				this.setAccession( tokens[0] + "_" + identifier );
			}
		} else {
			logger.warn( "Empty identifier" );
		}
	}
	
	private String accession;
	/**
	 * @return the accession
	 */
	public String getAccession() {
		if ( this.accession == null && this.database != null && this.identifier != null ) {
			String accession  = this.database + '_' + this.identifier;
			if ( new AccessionValidator().isValid( accession ) ) return accession;
		}
		return this.accession;
	}
	/**
	 * @param accession the accession to set
	 */
	public void setAccession( String accession ) {
		if ( new AccessionValidator().isValid( accession ) ) {
			this.accession = accession;
			logger.trace( "Update cellType database and identifier");
			String[] tokens = accession.split( "_" );
			if ( tokens.length == 2 ) {
				this.database = tokens[0];
				this.identifier = tokens[1];
			}
		} else this.accession = null;
	}
	
	private String name;
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	private HashSet<CrossReference> crossReferences;
	/**
	 * @return the crossReferences
	 */
	public Set<CrossReference> getCrossReferences() {
		return crossReferences;
	}
	/**
	 * @param crossReferences the crossReferences to set
	 */
	public void setCrossReferences(Set<CrossReference> crossReferences) {
		for ( CrossReference crossReference : crossReferences ) {
			CrossReferenceValidator validator = new CrossReferenceValidator( this.crossReferences );
			if ( validator.isValid( crossReference ) && validator.isUnique( crossReference ) )
				this.crossReferences.add( crossReference );
		}
	}
	/*
	 * @param crossReference the crossReference to add to the set
	 */
	public void addCrossReferences(CrossReference crossReference) {
		CrossReferenceValidator validator = new CrossReferenceValidator( this.crossReferences );
		if ( validator.isValid( crossReference ) && validator.isUnique( crossReference ) )
			this.crossReferences.add( crossReference );
	}
	/*
	 * @param crossReferences the crossReference to all add to the set
	 */
	public void addAllCrossReferences(Set<CrossReference> crossReferences) {
		if ( crossReferences != null ) {
			for ( CrossReference crossReference : crossReferences ) {
				CrossReferenceValidator validator = new CrossReferenceValidator( this.crossReferences );
				if ( validator.isValid( crossReference ) && validator.isUnique( crossReference ) )
					this.crossReferences.add( crossReference );
			}
		}
	}
	
	private Set<String> synonyms;
	/**
	 * @return the synonyms
	 */
	public Set<String> getSynonyms() {
		return synonyms;
	}
	/**
	 * @param synonyms the synonyms to set
	 */
	public void setSynonyms(Set<String> synonyms) {
		this.synonyms = synonyms;
	}
	/**
	 * @param synonym the synonym to add to the set
	 */
	public void addSynonyms(String synonym) {
		this.synonyms.add(synonym);
	}
	/*
	 * @param synonyms the synonyms to all add to the set
	 */
	public void addAllSynonyms(Set<String> synonyms) {
		this.synonyms.addAll(synonyms);
	}
	
	public CellType() {
		this.synonyms = new HashSet<String>();
		this.crossReferences = new HashSet<CrossReference>();
	}
	
	public CellType( String accession ) {
		this.synonyms = new HashSet<String>();
		this.crossReferences = new HashSet<CrossReference>();
		String[] tokens = accession.split( "_" );
		if ( tokens.length == 2 ) {
			this.setDatabase( tokens[0] );
			this.setIdentifier( tokens[1] );
			this.setAccession( accession );
		} else if ( tokens.length == 1 ) {
			this.setIdentifier( tokens[0] );
		}
	}
	
	public CellType( String database, String identifier ) {
		this.synonyms = new HashSet<String>();
		this.crossReferences = new HashSet<CrossReference>();
		this.setDatabase( database );
		this.setIdentifier( identifier );
		this.setAccession( database + '_' + identifier );
	}
	
	@Override
	public int compareTo( CellType cellType ) {
		// Check accession
		if ( this.accession.contentEquals( cellType.getAccession() ) )
			return 0;
		// Check name and synonyms exact match
		Set<String> sourceSynonyms = new HashSet<String>( this.synonyms );
		sourceSynonyms.add( this.name );
		Set<String> targetSynonyms = new HashSet<String>( cellType.getSynonyms() );
		targetSynonyms.add( cellType.getName() );
		for ( String synonym : targetSynonyms ) {
			if ( sourceSynonyms.contains( synonym ) ) return 0;
		}
		// Check cross reference mapping
		Set<CrossReference> sourceXRefSet = new HashSet<CrossReference>( this.crossReferences );
		sourceXRefSet.add( new CrossReference( this.accession ) );
		Set<CrossReference> targetXRefSet = new HashSet<CrossReference>( cellType.getCrossReferences() );
		targetXRefSet.add( new CrossReference( cellType.getAccession() ) );
		for ( CrossReference targetXRef : targetXRefSet ) {
			for ( CrossReference sourceXRef : sourceXRefSet ) {
				if ( sourceXRef.equals( targetXRef ) ) return 0; 
			}
		}
		return -1;
	}
	
	@Override
	public boolean equals( Object object ) {
		if ( object instanceof CellType ) {
			CellType cellType = (CellType) object;
			if ( this.compareTo( cellType ) == 0 ) return true;
		}
		return false;
	}
	
	public void merge( CellType cellTypeToMerge ) {
		if ( this.equals( cellTypeToMerge ) ) {
			Set<CrossReference> xRefToMerge = new HashSet<CrossReference>( cellTypeToMerge.getCrossReferences() );
			if ( !this.accession.contentEquals( cellTypeToMerge.getAccession() ) ) {
				xRefToMerge.add( new CrossReference( cellTypeToMerge.getAccession() ) );
			}
			this.addAllCrossReferences( xRefToMerge );
			if ( !this.name.contentEquals( cellTypeToMerge.getName() ) )
					this.synonyms.add( cellTypeToMerge.getName() );
			this.synonyms.addAll( cellTypeToMerge.getSynonyms() );
		} else if ( cellTypeToMerge != null )
			logger.warn( "Unable to merge two non-equal cellTypes" );
		else
			logger.trace( "Unable to merge null cellType object" );
	}
}


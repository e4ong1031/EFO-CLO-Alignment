/**
 * @file OrganismPart.java
 * @author Edison Ong
 * @since Jul 25, 2017
 * @version 1.0
 * @comment 
 */
package cellline.object;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.validation.AccessionValidator;

/**
 * 
 */
public class OrganismPart implements Comparable<OrganismPart> {
	
	static final Logger logger = LoggerFactory.getLogger( OrganismPart.class );
	
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
				logger.trace( "Update organism part database" );
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
				logger.trace( "Update organism part identifier" );
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
			logger.trace( "Update organism part database and identifier");
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
	
	private Set<CrossReference> crossReferences;
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
		this.crossReferences = crossReferences;
	}
	/*
	 * @param crossReference the crossReference to add to the set
	 */
	public void addCrossReferences(CrossReference crossReference) {
		this.crossReferences.add(crossReference);
	}
	/*
	 * @param crossReferences the crossReference to all add to the set
	 */
	public void addAllCrossReferences(Set<CrossReference> crossReferences) {
		this.crossReferences.addAll(crossReferences);
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
	
	public OrganismPart() {
		this.synonyms = new HashSet<String>();
		this.crossReferences = new HashSet<CrossReference>();
	}
	
	public OrganismPart( String accession ) {
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
	
	public OrganismPart( String database, String identifier ) {
		this.synonyms = new HashSet<String>();
		this.crossReferences = new HashSet<CrossReference>();
		this.setDatabase( database );
		this.setIdentifier( identifier );
		this.setAccession( database + '_' + identifier );
	}
	
	@Override
	public int compareTo( OrganismPart organismPart ) {
		// Check accession
		if ( this.accession.contentEquals( organismPart.getAccession() ) )
			return 0;
		// Check name and synonyms exact match
		Set<String> sourceSynonyms = new HashSet<String>( this.synonyms );
		sourceSynonyms.add( this.name );
		Set<String> targetSynonyms = new HashSet<String>( organismPart.getSynonyms() );
		targetSynonyms.add( organismPart.getName() );
		for ( String synonym : targetSynonyms ) {
			if ( sourceSynonyms.contains( synonym ) ) return 0;
		}
		// Check cross reference mapping
		Set<CrossReference> sourceXRefSet = new HashSet<CrossReference>( this.crossReferences );
		sourceXRefSet.add( new CrossReference( this.accession ) );
		Set<CrossReference> targetXRefSet = new HashSet<CrossReference>( organismPart.getCrossReferences() );
		targetXRefSet.add( new CrossReference( organismPart.getAccession() ) );
		for ( CrossReference targetXRef : targetXRefSet ) {
			for ( CrossReference sourceXRef : sourceXRefSet ) {
				if ( sourceXRef.equals( targetXRef ) ) return 0; 
			}
		}
		return -1;
	}
	
	@Override
	public boolean equals( Object object ) {
		if ( object instanceof OrganismPart ) {
			OrganismPart organismPart = (OrganismPart) object;
			if ( this.compareTo( organismPart ) == 0 ) return true;
		}
		return false;
	}
	
	public void merge( OrganismPart organismPartToMerge ) {
		if ( this.equals( organismPartToMerge ) ) {
			Set<CrossReference> xRefToMerge = new HashSet<CrossReference>( organismPartToMerge.getCrossReferences() );
			if ( !this.accession.contentEquals( organismPartToMerge.getAccession() ) ) {
				xRefToMerge.add( new CrossReference( organismPartToMerge.getAccession() ) );
			}
			this.addAllCrossReferences( xRefToMerge );
			if ( !this.name.contentEquals(organismPartToMerge.getName() ) )
					this.synonyms.add( organismPartToMerge.getName() );
			this.synonyms.addAll( organismPartToMerge.getSynonyms() );
		} else if ( organismPartToMerge != null )
			logger.warn( "Unable to merge two non-equal organismParts" );
		else
			logger.trace( "Unable to merge null organismPart object" );
	}
}

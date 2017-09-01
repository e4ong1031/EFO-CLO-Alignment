/**
 * @file CellLine.java
 * @author Edison Ong
 * @since Jul 20, 2017
 * @version 1.0
 * @comment 
 */
package cellline.object;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.validation.AccessionValidator;
import cellline.validation.CellTypeValidator;
import cellline.validation.CrossReferenceValidator;
import cellline.validation.DiseaseValidator;
import cellline.validation.OrganismPartValidator;
import cellline.validation.SpeciesValidator;

/**
 * 
 */
public class CellLine {
	
	protected static final Logger logger = LoggerFactory.getLogger( CellLine.class );
	
	protected String name;
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
	
	protected String database;
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
				logger.trace( "Update cell line database" );
				String[] tokens = this.accession.split( "_" );
				this.setAccession( database + "_" + tokens[1] );
			}
		} else {
			logger.warn( "Empty database" );
		}
	}
	
	protected String identifier;
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
				logger.trace( "Update cell line identifier" );
				String[] tokens = this.accession.split( "_" );
				this.setAccession( tokens[0] + "_" + identifier );
			}
		} else {
			logger.warn( "Empty identifier" );
		}
	}

	protected String accession;
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
			logger.trace( "Update cell line database and identifier");
			String[] tokens = accession.split( "_" );
			if ( tokens.length == 2 ) {
				this.database = tokens[0];
				this.identifier = tokens[1];
			}
		} else this.accession = null;
	}
	
	protected Set<String> synonyms;
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
	
	protected Set<CrossReference> crossReferences;
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
	
	protected Set<Citation> citations;
	/**
	 * @return the citations
	 */
	public Set<Citation> getCitations() {
		return citations;
	}
	/**
	 * @param citations the citations to set
	 */
	public void setCitations(Set<Citation> citations) {
		this.citations = citations;
	}
	/*
	 * @param citation the citation to add to the set
	 */
	public void addCitation(Citation citation) {
		this.citations.add(citation);
	}
	/*
	 * @param citations the citations to all add to the set
	 */
	public void addAllCitation(Set<Citation> citations) {
		this.citations.addAll(citations);
	}
	
	protected Set<Disease> diseases;
	/**
	 * @return the diseases
	 */
	public Set<Disease> getDiseases() {
		return diseases;
	}
	/**
	 * @param diseases the diseases to set
	 */
	public void setDiseases(Set<Disease> diseases) {
		for ( Disease disease : diseases ) {
			DiseaseValidator validator = new DiseaseValidator( this.diseases );
			if ( validator.isValid( disease ) && validator.isUnique( disease ) )
				this.diseases.add( disease );
		}
	}
	/*
	 * @param disease the disease to add
	 */
	public void addDisease(Disease disease) {
		DiseaseValidator validator = new DiseaseValidator( this.diseases );
		if ( validator.isValid( disease ) && validator.isUnique( disease ) )
			this.diseases.add( disease );
	}
	/*
	 * @param diseases the diseases to all add to the set
	 */
	public void addAllDiseases(Set<Disease> diseases) {
		if ( diseases != null ) {
			for ( Disease disease : diseases ) {
				DiseaseValidator validator = new DiseaseValidator( this.diseases );
				if ( validator.isValid( disease ) && validator.isUnique( disease ) )
					this.diseases.add( disease );
			}
		}
	}
	
	protected Set<Species> species;
	/**
	 * @return the species
	 */
	public Set<Species> getSpecies() {
		return species;
	}
	/**
	 * @param species the species to set
	 */
	public void setSpecies(Set<Species> speciesSet) {
		for ( Species species : speciesSet ) {
			SpeciesValidator validator = new SpeciesValidator( this.species );
			if ( validator.isValid( species ) && validator.isUnique( species ) )
				this.species.add( species );
		}
	}
	/*
	 * @param species the species to add to the set
	 */
	public void addSpecies(Species species) {
		SpeciesValidator validator = new SpeciesValidator( this.species );
		if ( validator.isValid( species ) && validator.isUnique( species ) )
			this.species.add( species );
	}
	/*
	 * @param species the species to all add to the set
	 */
	public void addAllSpecies(Set<Species> speciesSet) {
		if ( species != null ) {
			for ( Species species : speciesSet ) {
				SpeciesValidator validator = new SpeciesValidator( this.species );
				if ( validator.isValid( species ) && validator.isUnique( species ) )
					this.species.add( species );
			}
		}
	}
	
	protected Set<OrganismPart> organismParts;
	/**
	 * @return the organismParts
	 */
	public Set<OrganismPart> getOrganismParts() {
		return organismParts;
	}
	/**
	 * @param organismParts the organismParts to set
	 */
	public void setOrganismParts(Set<OrganismPart> organismParts) {
		for ( OrganismPart organismPart : organismParts ) {
			OrganismPartValidator validator = new OrganismPartValidator( this.organismParts );
			if ( validator.isValid( organismPart ) && validator.isUnique( organismPart ) )
				this.organismParts.add( organismPart );
		}
	}
	/*
	 * @param organismPart the organism part to add to the set
	 */
	public void addOrganismPart(OrganismPart organismPart) {
		OrganismPartValidator validator = new OrganismPartValidator( this.organismParts );
		if ( validator.isValid( organismPart ) && validator.isUnique( organismPart ) )
			this.organismParts.add( organismPart );
	}
	/*
	 * @param organismParts the organism parts to all add to the set
	 */
	public void addAllOrganismParts(Set<OrganismPart> organismParts) {
		if ( organismParts != null ) {
			for ( OrganismPart organismPart : organismParts ) {
				OrganismPartValidator validator = new OrganismPartValidator( this.organismParts );
				if ( validator.isValid( organismPart ) && validator.isUnique( organismPart ) )
					this.organismParts.add( organismPart );
			}
		}
	}
	
	protected Set<CellType> cellTypes;
	/**
	 * @return the cellTypes
	 */
	public Set<CellType> getCellTypes() {
		return cellTypes;
	}
	/**
	 * @param cellTypes the cellTypes to set
	 */
	public void setCellTypes(Set<CellType> cellTypes) {
		for ( CellType cellType : cellTypes ) {
			CellTypeValidator validator = new CellTypeValidator( this.cellTypes );
			if ( validator.isValid( cellType ) && validator.isUnique( cellType ) )
				this.cellTypes.add( cellType );
		}
	}
	/*
	 * @param cellType the organism part to add to the set
	 */
	public void addCellType(CellType cellType) {
		CellTypeValidator validator = new CellTypeValidator( this.cellTypes );
		if ( validator.isValid( cellType ) && validator.isUnique( cellType ) )
			this.cellTypes.add( cellType );
	}
	/*
	 * @param cellTypes the organism parts to all add to the set
	 */
	public void addAllCellTypes(Set<CellType> cellTypes) {
		if ( cellTypes != null ) {
			for ( CellType cellType : cellTypes ) {
				CellTypeValidator validator = new CellTypeValidator( this.cellTypes );
				if ( validator.isValid( cellType ) && validator.isUnique( cellType ) )
					this.cellTypes.add( cellType );
			}
		}
	}
	
	
	
	public CellLine() {
		this.synonyms = new HashSet<String>();
		this.crossReferences = new HashSet<CrossReference>();
		this.citations = new HashSet<Citation>();
		this.diseases = new HashSet<Disease>();
		this.species = new HashSet<Species>();
		this.organismParts = new HashSet<OrganismPart>();
		this.cellTypes = new HashSet<CellType>();
	}
	
	public CellLine( String accession ) {
		this.synonyms = new HashSet<String>();
		this.crossReferences = new HashSet<CrossReference>();
		this.citations = new HashSet<Citation>();
		this.diseases = new HashSet<Disease>();
		this.species = new HashSet<Species>();
		this.organismParts = new HashSet<OrganismPart>();
		this.cellTypes = new HashSet<CellType>();
		String[] tokens = accession.split( "_" );
		if ( tokens.length == 2 ) {
			this.setDatabase( tokens[0] );
			this.setIdentifier( tokens[1] );
			this.setAccession( accession );
		} else if ( tokens.length == 1 ) {
			this.setIdentifier( tokens[0] );
		}
	}
	
	public CellLine( String database, String identifier ) {
		this.synonyms = new HashSet<String>();
		this.crossReferences = new HashSet<CrossReference>();
		this.citations = new HashSet<Citation>();
		this.diseases = new HashSet<Disease>();
		this.species = new HashSet<Species>();
		this.organismParts = new HashSet<OrganismPart>();
		this.cellTypes = new HashSet<CellType>();
		this.setDatabase( database );
		this.setIdentifier( identifier );
		this.setAccession( database + '_' + identifier );
	}

	public Set<String> getCrossReferenceAccessionsFromSource( String source ) {
		Set<String> accessions = new HashSet<String>();
		for ( CrossReference xRef : this.crossReferences ) {
			if ( xRef.getSource().equalsIgnoreCase( source ) ) accessions.add( xRef.getAccession() );
		}
		return accessions;
	}
}
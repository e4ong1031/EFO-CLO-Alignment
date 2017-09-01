/**
 * @file SpeciesValidator.java
 * @author Edison Ong
 * @since Aug 9, 2017
 * @version 1.0
 * @comment 
 */
package cellline.validation;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.object.Species;

/**
 * 
 */
public class SpeciesValidator {
	private static final Logger logger = LoggerFactory.getLogger( SpeciesValidator.class ); 
	
	private Set<Species> speciesSet;
	
	public SpeciesValidator() {
		this.speciesSet = new HashSet<Species>();
	}
	
	public SpeciesValidator( Set<Species> species ) {
		this.speciesSet = species;
	}
	
	public Boolean isValid( Species  species ) {
		if ( species == null )
			return false;
		if ( new AccessionValidator().isValid( species.getAccession() ) ) {
			String[] tokens = species.getAccession().split( "_" );
			if ( !species.getDatabase().contentEquals( tokens[0] ) ) {
				logger.warn( "Unmatched species database to accesion: " + species.getAccession() );
				return false;
			}
			if ( !species.getIdentifier().contentEquals( tokens[1] ) ) {
				logger.warn( "Unmatched species identifier to accession: " + species.getAccession() );
				return false;
			}
			return true;
		} else {
			logger.warn( "Invalid species accession: " + species.getAccession() );
			return false;
		}
	}
	
	public Boolean isUnique( Species speciesToCheck ) {
		for ( Species species : this.speciesSet ) {
			if ( species.equals( speciesToCheck ) ) {
				logger.trace( "Duplicated species: " + species.getAccession() );
				return false;
			}
		}
		return true;
	}
}

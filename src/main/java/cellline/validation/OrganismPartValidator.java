/**
 * @file OrganismPartValidator.java
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

import cellline.object.OrganismPart;

/**
 * 
 */
public class OrganismPartValidator {
	private static final Logger logger = LoggerFactory.getLogger( OrganismPartValidator.class ); 
	
	private Set<OrganismPart> organismPartSet;
	
	public OrganismPartValidator() {
		this.organismPartSet = new HashSet<OrganismPart>();
	}
	
	public OrganismPartValidator( Set<OrganismPart> organismParts ) {
		this.organismPartSet = organismParts;
	}
	
	public Boolean isValid( OrganismPart  organismPart) {
		if ( organismPart == null )
			return false;
		if ( new AccessionValidator().isValid( organismPart.getAccession() ) ) {
			String[] tokens = organismPart.getAccession().split( "_" );
			if ( !organismPart.getDatabase().contentEquals( tokens[0] ) ) {
				logger.warn( "Unmatched organismPart database to accesion: " + organismPart.getAccession() );
				return false;
			}
			if ( !organismPart.getIdentifier().contentEquals( tokens[1] ) ) {
				logger.warn( "Unmatched organismPart identifier to accession: " + organismPart.getAccession() );
				return false;
			}
			return true;
		} else {
			logger.warn( "Invalid organismPart accession: " + organismPart.getAccession() );
			return false;
		}
	}
	
	public Boolean isUnique( OrganismPart organismPartToCheck ) {
		for ( OrganismPart organismPart : this.organismPartSet ) {
			if ( organismPart.equals( organismPartToCheck ) ) {
				logger.trace( "Duplicated organismPart: " + organismPart.getAccession() );
				return false;
			}
		}
		return true;
	}
}

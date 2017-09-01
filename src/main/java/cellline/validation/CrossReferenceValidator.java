/**
 * @file CrossReferenceValidator.java
 * @author Edison Ong
 * @since Aug 1, 2017
 * @version 1.0
 * @comment 
 */
package cellline.validation;

import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.object.CrossReference;

/**
 * 
 */
public class CrossReferenceValidator {
	
	private static final Logger logger = LoggerFactory.getLogger( CrossReferenceValidator.class ); 
	
	private Set<CrossReference> xRefSet;
	
	public CrossReferenceValidator() {
		this.xRefSet = new HashSet<CrossReference>();
	}
	
	public CrossReferenceValidator( Set<CrossReference> crossReferences ) {
		this.xRefSet = crossReferences;
	}
	
	public Boolean isValid( CrossReference xRef ) {
		if ( xRef == null )
			return false;
		if ( new AccessionValidator().isValid( xRef.getAccession() ) ) {
			String[] tokens = xRef.getAccession().split( "_" );
			if ( !xRef.getSource().contentEquals( tokens[0] ) ) {
				logger.warn( "Unmatched cross reference source to accesion: " + xRef.getAccession() );
				return false;
			}
			if ( !xRef.getIdentifier().contentEquals( tokens[1] ) ) {
				logger.warn( "Unmatched cross reference identifier to accession: " + xRef.getAccession() );
				return false;
			}
			return true;
		} else {
			logger.warn( "Invalid cross reference accession: " + xRef.getAccession() );
			return false;
		}
	}
	
	public Boolean isUnique( CrossReference xRefToCheck ) {
		for ( CrossReference xRef : this.xRefSet ) {
			if ( xRef.equals( xRefToCheck) ) {
				logger.trace( "Duplicated cross reference: " + xRefToCheck.getAccession() );
				return false;
			}
		}
		return true;
	}
}

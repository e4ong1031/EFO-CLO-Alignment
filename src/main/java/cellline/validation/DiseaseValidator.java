/**
 * @file DiseaseValidator.java
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

import cellline.object.Disease;

/**
 * 
 */
public class DiseaseValidator {
	private static final Logger logger = LoggerFactory.getLogger( DiseaseValidator.class ); 
	
	private Set<Disease> diseaseSet;
	
	public DiseaseValidator() {
		this.diseaseSet = new HashSet<Disease>();
	}
	
	public DiseaseValidator( Set<Disease> diseases ) {
		this.diseaseSet = diseases;
	}
	
	public Boolean isValid( Disease  disease) {
		if ( disease == null )
			return false;
		if ( new AccessionValidator().isValid( disease.getAccession() ) ) {
			String[] tokens = disease.getAccession().split( "_" );
			if ( !disease.getDatabase().contentEquals( tokens[0] ) ) {
				logger.warn( "Unmatched disease database to accesion: " + disease.getAccession() );
				return false;
			}
			if ( !disease.getIdentifier().contentEquals( tokens[1] ) ) {
				logger.warn( "Unmatched disease identifier to accession: " + disease.getAccession() );
				return false;
			}
			return true;
		} else {
			logger.warn( "Invalid disease accession: " + disease.getAccession() );
			return false;
		}
	}
	
	public Boolean isUnique( Disease diseaseToCheck ) {
		for ( Disease disease : this.diseaseSet ) {
			if ( disease.equals( diseaseToCheck ) ) {
				logger.trace( "Duplicated disease: " + disease.getAccession() );
				return false;
			}
		}
		return true;
	}
}

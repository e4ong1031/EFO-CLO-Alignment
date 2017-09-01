/**
 * @file AccessionValidator.java
 * @author Edison Ong
 * @since Jul 26, 2017
 * @version 1.0
 * @comment 
 */
package cellline.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class AccessionValidator {
	
	private static final Logger logger = LoggerFactory.getLogger( AccessionValidator.class ); 

	public AccessionValidator() {};
	
	public Boolean isValid( String accession ) {
		if ( accession == null ) return false;
		Pattern pattern = Pattern.compile( "[^_]*_[^ ][^_]*" );
		Matcher matcher = pattern.matcher( accession );
		if ( matcher.matches() ) return true;
		else {
			logger.warn( "Incorrect accession format: " + accession );
			return false;
		}
	}
}

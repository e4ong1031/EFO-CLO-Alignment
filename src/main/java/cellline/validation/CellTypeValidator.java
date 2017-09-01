/**
 * @file CellTypeValidator.java
 * @author Edison Ong
 * @since Aug 15, 2017
 * @version 1.0
 * @comment 
 */
package cellline.validation;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.object.CellType;

/**
 * 
 */
public class CellTypeValidator {
	private static final Logger logger = LoggerFactory.getLogger( CellTypeValidator.class ); 
	
	private Set<CellType> cellTypeSet;
	
	public CellTypeValidator() {
		this.cellTypeSet = new HashSet<CellType>();
	}
	
	public CellTypeValidator( Set<CellType> cellTypes ) {
		this.cellTypeSet = cellTypes;
	}
	
	public Boolean isValid( CellType  cellType) {
		if ( cellType == null )
			return false;
		if ( new AccessionValidator().isValid( cellType.getAccession() ) ) {
			String[] tokens = cellType.getAccession().split( "_" );
			if ( !cellType.getDatabase().contentEquals( tokens[0] ) ) {
				logger.warn( "Unmatched cellType database to accesion: " + cellType.getAccession() );
				return false;
			}
			if ( !cellType.getIdentifier().contentEquals( tokens[1] ) ) {
				logger.warn( "Unmatched cellType identifier to accession: " + cellType.getAccession() );
				return false;
			}
			return true;
		} else {
			logger.warn( "Invalid cellType accession: " + cellType.getAccession() );
			return false;
		}
	}
	
	public Boolean isUnique( CellType cellTypeToCheck ) {
		for ( CellType cellType : this.cellTypeSet ) {
			if ( cellType.equals( cellTypeToCheck ) ) {
				logger.trace( "Duplicated cellType: " + cellType.getAccession() );
				return false;
			}
		}
		return true;
	}
}

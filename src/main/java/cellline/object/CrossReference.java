/**
 * @file CrossReference.java
 * @author Edison Ong
 * @since Jul 20, 2017
 * @version 1.0
 * @comment 
 */
package cellline.object;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.validation.AccessionValidator;

/**
 * 
 */
public class CrossReference implements Comparable<CrossReference> {
	
	static final Logger logger = LoggerFactory.getLogger( CrossReference.class );
	
	private String source;
	/**
	 * @return the source
	 */
	public String getSource() {
		if ( this.source != null ) return source;
		else if ( this.accession != null ) return this.accession.split( "_" )[0];
		else return null;
	}
	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		source = source.trim();
		if ( !source.isEmpty() ) {
			this.source = source;
			if ( this.accession != null ) {
				logger.trace( "Update cross reference source" );
				String[] tokens = this.accession.split( "_" );
				this.setAccession( source + "_" + tokens[1] );
			} else if ( this.identifier != null && ! this.identifier.isEmpty() ) {
				String accession = this.source + "_" + this.identifier;
				if ( new AccessionValidator().isValid( accession ) ) {
					logger.trace( "Auto-generate cross reference accession" );
					this.accession = accession;
				}
			}
		} else {
			logger.warn( "Empty source" );
		}
	}
	
	private String identifier;
	/**
	 * @return the identifier
	 */
	public String getIdentifier() {
		if ( this.identifier != null ) return this.identifier;
		else if ( this.accession != null ) return this.accession.split( "_" )[1];
		else return null;
	}
	/**
	 * @param identifer the identifier to set
	 */
	public void setIdentifier(String identifier) {
		identifier = identifier.trim();
		if ( !identifier.isEmpty() ) {
			this.identifier = identifier;
			if ( this.accession != null ) {
				logger.trace( "Update cross reference identifier" );
				String[] tokens = this.accession.split( "_" );
				this.setAccession( tokens[0] + "_" + identifier );
			} else if ( this.source != null && ! this.source.isEmpty() ) {
				String accession = this.source + "_" + this.identifier;
				if ( new AccessionValidator().isValid( accession ) ) {
					logger.trace( "Auto-generate cross reference accession" );
					this.accession = accession;
				}
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
		return this.accession;
	}
	/**
	 * @param accession the accession to set
	 */
	public void setAccession( String accession ) {
		if ( new AccessionValidator().isValid( accession ) ) {
			this.accession = accession;
			logger.trace( "Update cross reference source and identifier");
			String[] tokens = accession.split( "_" );
			if ( tokens.length == 2 ) {
				this.source = tokens[0];
				this.identifier = tokens[1];
			}
		} else this.accession = null;
	}
	
	public CrossReference( String accession ) {
		if ( new AccessionValidator().isValid( accession ) ) {
			String[] tokens = accession.split( "_" );
			this.source = tokens[0];
			this.identifier = tokens[1];
			this.accession = accession;
		} else {
			this.accession = null;
		}
	}
	
	public CrossReference( String source, String identifier ) {
		String accession = source + "_" + identifier;
		if ( new AccessionValidator().isValid( accession ) ) {
			this.source = source;
			this.identifier = identifier;
			this.accession = accession;
		} else
			this.accession = null;
	}
	
	@Override
	public int compareTo( CrossReference xRef ) {
		if ( this.accession.contentEquals( xRef.getAccession() ) )
			return 0;
		else if ( this.source.equalsIgnoreCase( xRef.getSource() ) && this.identifier.equalsIgnoreCase( xRef.getIdentifier() ) )
			return 0;
		else
			return -1;
	}
	
	@Override
	public boolean equals( Object object ) {
		if ( ( object instanceof CrossReference) ) {
			CrossReference xRef = (CrossReference) object;
			if ( this.compareTo( xRef ) == 0 ) return true;
		}
		return false;
	}
}

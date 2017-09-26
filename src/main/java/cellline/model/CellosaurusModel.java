/**
 * @file CellosaurusCellLinesjava
 * @author Edison Ong
 * @since Jul 18, 2017
 * @version 1.0
 * @comment 
 */
package cellline.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.object.CLOCellLine;
import cellline.object.CellosaurusCellLine;
import cellline.object.CrossReference;
import cellline.object.Disease;
import cellline.object.Species;

/**
 * 
 */
public class CellosaurusModel {
	
	static final Logger logger = LoggerFactory.getLogger( CellosaurusModel.class );

	private HashMap<String, CellosaurusCellLine> cellLines = new HashMap<String, CellosaurusCellLine>();
	/**
	 * @return the cellLines
	 */
	public HashMap<String, CellosaurusCellLine> getCellLines() {
		return cellLines;
	}
	
	public CellosaurusModel() {}
	
	public CellosaurusModel( File cellosaurusFile ) {
		this.loadFromFile( cellosaurusFile );
	}
	
	public void loadFromFile( File cellosaurusFile ) {
		logger.info( "Reading Cellosaurus from text file: " + cellosaurusFile.getAbsolutePath() );
		BufferedReader buffer = null;
		ArrayList <String> lines = new ArrayList <String> ();
		try {
			buffer = new BufferedReader( new FileReader( cellosaurusFile ) );
			String line = null;

			Boolean startFlag = false;
			while ( !startFlag && ( line = buffer.readLine() ) != null) {
				if ( line.startsWith( "_" ) ) startFlag = true;
			}
			while( ( line = buffer.readLine() ) != null ) {
				if ( line.trim().length()>0 && !line.trim().startsWith( "#" ) )	{
					if ( line.trim().startsWith( "//" ) ) {
						CellosaurusCellLine cellLine = parseCellLine( lines );
						logger.trace( "Adding cell line to the collection" );
						this.cellLines.put( cellLine.getAccession(), cellLine );
						lines.clear();
					} else
						lines.add( line.trim() );
				}
				
			}
		} catch ( FileNotFoundException e ) {
			logger.error( "", e );
		} catch ( IOException e ) {
			logger.error( "", e );
		} finally {
			try {
				if ( buffer != null ) buffer.close();
			}
			catch ( IOException ex ) {
				logger.error( "", ex );
			}
		}
	}
	
 	private CellosaurusCellLine parseCellLine( ArrayList<String> lines ) {
		CellosaurusCellLine cellLine = new CellosaurusCellLine();
		for ( String line : lines ) {
			String code = line.substring( 0, 2 );
			String value = line.substring( 5 );
			String[] tokens;
			switch ( code ) {
				case "ID":
					logger.trace( "Found cell line name" );
					cellLine.setName( value );
					break;
				case "AC":
					logger.trace( "Found cell line accession" );
					cellLine.setAccession( value );
					break;
				case "AS":
					logger.trace( "Found cell line secondary accession" );
					cellLine.setSecondarAccession( value );
					break;
				case "SY":
					logger.trace( "Found cell line synonyms" );
					cellLine.addAllSynonyms( new HashSet<String> ( Arrays.asList( value.split( "; " ) ) ) );
					break;
				case "DR":
					logger.trace( "Found cell line cross reference" );
					tokens = value.split( "; " );
					String source = tokens[0];
					Pattern pattern = Pattern.compile( String.format( "(%s)[^:_]*[:_][ ]?([^:_]*)", source ) );
					Matcher matcher = pattern.matcher( tokens[1] );
					if ( matcher.matches() ) {
						source = matcher.group( 1 ).replaceAll( "_", "-" );
						String identifier = matcher.group( 2 ).replaceAll( "_", " " );
						CrossReference xRef = new CrossReference( source, identifier );		
						cellLine.addCrossReferences( xRef );
					} else {
						source = source.replaceAll( "_", "-" );
						String identifier = tokens[1].replaceAll( "_", " " );
						CrossReference xRef = new CrossReference( source, identifier );		
						cellLine.addCrossReferences( xRef );
					}
					break;
				case "DI":
					logger.trace( "Found cell line disease" );
					tokens = value.split( "; " );
					Disease disease = new Disease( tokens[0], tokens[1] );
					disease.setName( tokens[2] );
					cellLine.addDisease( disease );
					break;
				case "OX":
					logger.trace( "Found cell line species" );
					tokens = value.split( "; ! " );
					Species species = new Species( "NCBITaxon", tokens[0].split( "=" )[1] );
					species.setName( tokens[1] );
					cellLine.addSpecies( species );
					break;
				case "CA":
					logger.trace( "Found cell line category" );
					cellLine.setCategory( value );
					break;
				default:
					logger.trace( "Unknown/Unspecified line code" );
					break;
			}
		}
		return cellLine;
	}
 	
 	public CellosaurusCellLine getCellLineFromAccession( String accession ) {
		if ( this.cellLines.containsKey( accession ) )
			return this.cellLines.get( accession );
		else
			return new CellosaurusCellLine( accession );
	}
 	
 	public Set<CellosaurusCellLine> getCellLinesFromCrossReferenceSource( String source ) {
 		Set<CellosaurusCellLine> output = new HashSet<CellosaurusCellLine>();
 		for ( CellosaurusCellLine cellLine : this.cellLines.values() ) {
 			Set<CrossReference> xRefs = cellLine.getCrossReferences();
 			for ( CrossReference xRef : xRefs ) {
 				if ( source.equalsIgnoreCase( xRef.getSource() ) ) output.add( cellLine );
 			}
 		}
 		return output;
 	}
 	
 	public Set<CellosaurusCellLine> getCellLinesFromCrossReferenceAccession( String accession ) {
 		Set<CellosaurusCellLine> output = new HashSet<CellosaurusCellLine>();
 		for ( CellosaurusCellLine cellLine : this.cellLines.values() ) {
 			Set<CrossReference> xRefs = cellLine.getCrossReferences();
 			for ( CrossReference xRef : xRefs ) {
 				if ( accession.equalsIgnoreCase( xRef.getAccession() ) ) output.add( cellLine );
 			}
 		}
 		return output;
 	}
}



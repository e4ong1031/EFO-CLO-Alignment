/**
 * @file EFOInternalProcess.java
 * @author Edison Ong
 * @since Aug 23, 2017
 * @version 1.0
 * @comment 
 */
package cellline.application;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import au.com.bytecode.opencsv.CSVWriter;
import cellline.model.CLOOntologyModel;
import cellline.model.CellosaurusModel;
import cellline.model.DiseaseOntologyModel;
import cellline.model.EFOOntologyModel;
import cellline.object.CellosaurusCellLine;
import cellline.object.EFOCellLine;

/**
 * 
 */
public class EFOInternalProcess {
	/*
	 * args4j options declaration
	 */
	@Option(
			name = "-s",
			usage = "Source[EFO] txt data file path.",
			required = true,
			aliases = {"--source-file", "--source"}
			)
	private String efoFileName;
	@Option(
			name = "-i",
			usage = "Intermediate[Cellosaurus] txt data file path.",
			required = true,
			aliases = {"--intermediate-file", "--intermediate"}
			)
	private String clsFileName;
	@Option(
			name = "-o",
			usage = "Mapping output directory path.",
			required = true,
			aliases = {"--output-path"}
			)
	private String outputDirectory;
	
	static final Logger logger = LoggerFactory.getLogger( EFOInternalProcess.class );
	
	private EFOOntologyModel efoModel;
	private CLOOntologyModel cloModel;
	private CellosaurusModel clsModel;
	private DiseaseOntologyModel doidModel;
	
	HashMap<String, String> efoCLSMap;
	HashMap<String, String> cloCLSMap;
	Set<String> efoMappedSet;
	
	public static void main( String[] args ) {
		new EFOInternalProcess().run( args );
	}
	
	public void run( String[] args ) {
		CmdLineParser parser = new CmdLineParser( this );
		
		// Load arguments
		try {
			parser.parseArgument( args );
		} catch( CmdLineException e ) {
			logger.error( "Incorrect arguments" );
            parser.printUsage( System.err );
            System.exit( -1 );
		}
		
		// Load Cellosaurus
		this.clsModel = new CellosaurusModel( new File( this.clsFileName ) );
		this.efoCLSMap = new HashMap<String, String>();
		for ( CellosaurusCellLine efoCLSCellLine : clsModel.getCellLinesFromCrossReferenceSource( "EFO" ) ) {
			Set<String> efoAccessions = efoCLSCellLine.getCrossReferenceAccessionsFromSource( "EFO" );
			if ( efoAccessions.size() == 1 ) {
				efoCLSMap.put( efoAccessions.iterator().next(),  efoCLSCellLine.getAccession() );
			}
		}
		logger.info( String.format( "Found %d EFO mapping in Cellosaurus", efoCLSMap.size() ) );
		
		// Load EFO
		this.efoModel = new EFOOntologyModel( new File( this.efoFileName ) );
		
		HashMap<String, Set<String>> nonNativeDb = new HashMap<String,Set<String>>();
		for ( Map.Entry<String, EFOCellLine> efo : this.efoModel.getCellLines().entrySet() ) {
			String efoAccession = efo.getKey();
			EFOCellLine efoCellLine = efo.getValue();
			
			if ( !efoCellLine.getDatabase().equalsIgnoreCase( "EFO" ) ) {
				String db = efoCellLine.getDatabase();
				if ( !nonNativeDb.containsKey( db ) )
					nonNativeDb.put( db, new HashSet<String>() );
				nonNativeDb.get( db ).add( efoCellLine.getAccession() );
			}
		}
		
		for ( Map.Entry<String, Set<String>> entry : nonNativeDb.entrySet() ) {
			String db = entry.getKey();
				CSVWriter writer;
				try {
					writer = new CSVWriter( new FileWriter( outputDirectory + db + "_in_EFO.csv" ) );
					for ( String accession : entry.getValue() ) {
						EFOCellLine cellLine = efoModel.getCellLineFromAccession( accession );
						String[] output =  {cellLine.getIri(), cellLine.getName()};
						writer.writeNext( output );
					}
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
				
	}
	
}

/**
 * @file EFOCellosaurusMapping.java
 * @author Edison Ong
 * @since Aug 24, 2017
 * @version 1.0
 * @comment 
 */
package cellline.application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.bean.CsvToBean;
import cellline.model.CLOOntologyModel;
import cellline.model.CellosaurusModel;
import cellline.model.DiseaseOntologyModel;
import cellline.model.EFOOntologyModel;
import cellline.object.CellosaurusCellLine;
import cellline.object.EFOCellLine;

/**
 * 
 */
public class EFOCellosaurusMapping {
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
			usage = "Intermediate[Mapped EFO-CLO] txt data file path.",
			required = true,
			aliases = {"--intermediate-file", "--intermediate"}
			)
	private String mapFileName;
	@Option(
			name = "-t",
			usage = "Target[Cellosaurus] txt data file path.",
			required = true,
			aliases = {"--target-file", "--target"}
			)
	private String clsFileName;
	@Option(
			name = "-o",
			usage = "Mapping output directory path.",
			required = true,
			aliases = {"--output-path"}
			)
	private String outputDirectory;
	
	static final Logger logger = LoggerFactory.getLogger( EFOCellosaurusMapping.class );
	
	private EFOOntologyModel efoModel;
	private CellosaurusModel clsModel;
	
	Set<String> efoMappedSet;
	
	public static void main( String[] args ) {
		new EFOCellosaurusMapping().run( args );
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
		
		efoMappedSet = new HashSet<String>();
		try {
			CSVReader reader = new CSVReader( new FileReader( mapFileName  ) );
			List<String[]> lines = reader.readAll();
			for ( String[] line : lines.subList(1, lines.size() ) ) {
				efoMappedSet.add( line[0] );
				
			}
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Load Cellosaurus
		this.clsModel = new CellosaurusModel( new File( this.clsFileName ) );
		
		// Load EFO
		this.efoModel = new EFOOntologyModel( new File( this.efoFileName ) );
		
		Set<String> efoToAdd = new HashSet<String>();
		Set<String> efoStem = new HashSet<String>();
		Set<String> efoNA = new HashSet<String>();
		for ( Map.Entry<String, EFOCellLine> efo : this.efoModel.getCellLines().entrySet() ) {
			String efoAccession = efo.getKey();
			EFOCellLine efoCellLine = efo.getValue();
			
			if ( efoCellLine.getDatabase().equalsIgnoreCase( "EFO" ) && !efoMappedSet.contains( efoAccession ) ) {
				Set<CellosaurusCellLine> cellLines = clsModel.getCellLinesFromCrossReferenceAccession( efoAccession );
				if ( !cellLines.isEmpty() ) {
					boolean isStem = false;
					for ( CellosaurusCellLine cellLine : cellLines ) {
						if ( cellLine.getCategory().matches( "(?i:.*stem cell.*)" ) )
							isStem = true;
					}
					if ( !isStem )
						efoToAdd.add( efoAccession );
					else
						efoStem.add( efoAccession );
				} else
					efoNA.add( efoAccession );
			}
		}
		
		logger.info( String.format( "Found %d EFO non-stem cell cell lines avaiable in Cellosaurus and will add to CLO", efoToAdd.size() ) );
		
		try {
			CSVWriter addWriter = new CSVWriter( new FileWriter( outputDirectory + "EFO_AddTo_CLO.csv" ) );
			for ( String accession : efoToAdd ) {
				EFOCellLine cellLine = efoModel.getCellLineFromAccession( accession );
				String[] entry = {
						accession,
						cellLine.getName(),
						MapWriter.writeSetToString( cellLine.getDiseases() ),
						MapWriter.writeSetToString( cellLine.getSpecies() ),
						MapWriter.writeSetToString( cellLine.getOrganismParts() ),
						MapWriter.writeSetToString( cellLine.getCellTypes() ),
				};
				addWriter.writeNext( entry );
			}
			addWriter.close();
			
			CSVWriter stemWriter = new CSVWriter( new FileWriter( outputDirectory + "EFO_Stem.csv" ) );
			for ( String accession : efoStem ) {
				EFOCellLine cellLine = efoModel.getCellLineFromAccession( accession );
				String[] entry = {
						accession,
						cellLine.getName(),
						MapWriter.writeSetToString( cellLine.getDiseases() ),
						MapWriter.writeSetToString( cellLine.getSpecies() ),
						MapWriter.writeSetToString( cellLine.getOrganismParts() ),
						MapWriter.writeSetToString( cellLine.getCellTypes() ),
				};
				stemWriter.writeNext( entry );
			}
			stemWriter.close();
			
			CSVWriter naWriter = new CSVWriter( new FileWriter( outputDirectory + "EFO_NA.csv" ) );
			for ( String accession : efoNA ) {
				EFOCellLine cellLine = efoModel.getCellLineFromAccession( accession );
				String[] entry = {
						accession,
						cellLine.getName(),
						MapWriter.writeSetToString( cellLine.getDiseases() ),
						MapWriter.writeSetToString( cellLine.getSpecies() ),
						MapWriter.writeSetToString( cellLine.getOrganismParts() ),
						MapWriter.writeSetToString( cellLine.getCellTypes() ),
				};
				naWriter.writeNext( entry );
			}
			naWriter.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}
	
}
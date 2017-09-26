package cellline.application;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import au.com.bytecode.opencsv.CSVWriter;
import cellline.match.CellLineMatcher;
import cellline.match.DiseaseMatcher;
import cellline.match.OrganismPartMatcher;
import cellline.match.SpeciesMatcher;
import cellline.model.CLOOntologyModel;
import cellline.model.CellosaurusModel;
import cellline.model.DiseaseOntologyModel;
import cellline.model.EFOOntologyModel;
import cellline.object.CLOCellLine;
import cellline.object.CellosaurusCellLine;
import cellline.object.Disease;
import cellline.object.EFOCellLine;
import cellline.object.OrganismPart;
import cellline.object.Species;
import rationals.transformations.Union;

/**
 * @file EFOCellosaurusCLOMapping.java
 * @author Edison Ong
 * @since Aug 8, 2017
 * @version 1.0
 * @comment 
 */

/**
 * 
 */
public class EFOCellosaurusCLOMapping {
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
			name = "-t",
			usage = "Target[CLO] ontology owl file path.",
			required = true,
			aliases = {"--target-file"}
			)
	private String cloFileName;
	@Option(
			name = "-o",
			usage = "Mapping output directory path.",
			required = true,
			aliases = {"--output-path"}
			)
	private String outputDirectory;
	
	static final Logger logger = LoggerFactory.getLogger( EFOCellosaurusCLOMapping.class );
	
	public static void main( String[] args ) {
		new EFOCellosaurusCLOMapping().run( args );
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
		CellosaurusModel clsModel = new CellosaurusModel( new File( this.clsFileName ) );
		HashMap<String, String> clsEFOMap = new HashMap<String, String>();
		for ( CellosaurusCellLine efoCLSCellLine : clsModel.getCellLinesFromCrossReferenceSource( "EFO" ) ) {
			Set<String> efoAccessions = efoCLSCellLine.getCrossReferenceAccessionsFromSource( "EFO" );
			if ( efoAccessions.size() == 1 ) {
				clsEFOMap.put( efoCLSCellLine.getAccession(), efoAccessions.iterator().next() );
			}
		}
		logger.info( String.format( "Found %d EFO mapping in Cellosaurus", clsEFOMap.size() ) );
		HashMap<String, String> clsCLOMap = new HashMap<String, String>();
		for ( CellosaurusCellLine cloCLSCellLine : clsModel.getCellLinesFromCrossReferenceSource( "CLO" ) ) {
			Set<String> cloAccessions = cloCLSCellLine.getCrossReferenceAccessionsFromSource( "CLO" );
			if ( cloAccessions.size() == 1 ) {
				clsCLOMap.put( cloCLSCellLine.getAccession(), cloAccessions.iterator().next() );
			}
		}
		logger.info( String.format( "Found %d CLO mapping in Cellosaurus", clsCLOMap.size() ) );
		
		// Load EFO
		EFOOntologyModel efoModel = new EFOOntologyModel( new File( this.efoFileName ) );
		
		// Load CLO
		CLOOntologyModel cloModel = new CLOOntologyModel( new File ( this.cloFileName ) );
		
		// Load DOID
		DiseaseOntologyModel doidModel = new DiseaseOntologyModel();
		
		// Expand disease cross reference from DOID and EFO
		for ( Map.Entry<String, EFOCellLine> efo : efoModel.getCellLines().entrySet() )
			for ( Disease efoDisease : efo.getValue().getDiseases() ) {
				for ( Disease sourceDOID : doidModel.getDiseasesFromCrossReferenceAccession( efoDisease.getAccession() ) )
					efoDisease.merge( sourceDOID );
				for ( Disease sourceEFO : efoModel.getDiseasesFromCrossReferenceAccession( efoDisease.getAccession() ) )
					efoDisease.merge( sourceEFO );
			}
		for ( Map.Entry<String, CLOCellLine> clo : cloModel.getCellLines().entrySet() )
			for ( Disease cloDisease : clo.getValue().getDiseases() ) {
				for ( Disease targetDOID : doidModel.getDiseasesFromCrossReferenceAccession( cloDisease.getAccession() ) )
					cloDisease.merge( targetDOID );
				for ( Disease targetEFO : efoModel.getDiseasesFromCrossReferenceAccession( cloDisease.getAccession() ) )
					cloDisease.merge( targetEFO );
			}
		for ( Map.Entry<String, CellosaurusCellLine> cls : clsModel.getCellLines().entrySet() )
			for ( Disease clsDisease : cls.getValue().getDiseases() ) {
				for ( Disease intermediateDOID : doidModel.getDiseasesFromCrossReferenceAccession( clsDisease.getAccession() ) )
					clsDisease.merge( intermediateDOID );
				for ( Disease intermediateEFO : efoModel.getDiseasesFromCrossReferenceAccession( clsDisease.getAccession() ) )
					clsDisease.merge( intermediateEFO );
			}
		
		CellLineMatcher cellLineMatcher = new CellLineMatcher();
		DiseaseMatcher diseaseMatcher = new DiseaseMatcher( doidModel, efoModel );
		SpeciesMatcher speciesMatcher = new SpeciesMatcher();
		OrganismPartMatcher organismPartMatcher = new OrganismPartMatcher();
		Set<String> validMapping = new HashSet<String>();
		Set<String> invalidMapping = new HashSet<String>();
		try {
			CSVWriter validwriter = new CSVWriter( new FileWriter( outputDirectory + "Valid_EFO-CLS-CLO_Mapping.tsv" ) );
			CSVWriter invalidwriter = new CSVWriter( new FileWriter( outputDirectory + "Invalid_EFO-CLS-CLO_Mapping.tsv" ), '\t');
			String[] colName = { 
					"EFO Accession",
					"CLO Accession",
					"Cellosaurus Accession",
					"EFO-CLO Name/Synonyms Match?",
					"EFO-CLS-CLO Name/Synonyms Reover?",
					"Name Edit Distance",
					"EFO Name",
					"CLO Name",
					"Cellosaurus Name",
					"Shorest Synonyms Edit Distance",
					"EFO Synonyms",
					"CLO Synonyms",
					"Cellosaurus Synonyms",
					"EFO-CLO Disease Match?",
					"EFO-CLS-CLO Disease Recover?",
					"EFO Disease",
					"CLO Disease",
					"Cellosaurus Disease",
					"EFO-CLO Species Match?",
					"EFO-CLS-CLO Species Recover?",
					"EFO Species",
					"CLO Species",
					"Cellosaurus Species",
			};
			validwriter.writeNext( colName );
			invalidwriter.writeNext( colName );
			
			for ( String clsAccession : Sets.intersection( clsEFOMap.keySet(), clsCLOMap.keySet() ) ) {
				CellosaurusCellLine clsCellLine = clsModel.getCellLineFromAccession( clsAccession );
				
				String efoAccession = clsEFOMap.get( clsAccession );
				EFOCellLine efoCellLine = efoModel.getCellLineFromAccession( efoAccession );
				
				String cloAccession = clsCLOMap.get( clsAccession );
				CLOCellLine cloCellLine = cloModel.getCellLineFromAccession( cloAccession );
				
				Boolean[] mappingFlag = {null,null,null,null,null,null};
				ArrayList<String> entry = new ArrayList<String>();
				Boolean flag;
				Boolean efoFlag;
				Boolean cloFlag;
				
				// Accession
				entry.add( efoAccession );
				entry.add( cloAccession );
				entry.add( clsAccession );
				
				// Name Match (EFO-CLO)
				flag = null;
				String nameDist = "not available";
				String synonymDist = "not available";
				if ( efoCellLine.getName() != null && cloCellLine.getName() != null ) {
					cellLineMatcher.setSource( efoCellLine );
					cellLineMatcher.setTarget( cloCellLine );
					flag = cellLineMatcher.match();
					nameDist = String.valueOf( cellLineMatcher.getNameEditDistance() );
					synonymDist = String.valueOf( cellLineMatcher.getShortestSynonymEditDistance() );
				}
				mappingFlag[0] = flag;
				entry.add( String.valueOf( flag ) );
				
				// Name Match (EFO-CLS, CLO-CLS)
				flag = null;
				efoFlag = null;
				cloFlag = null;
				if ( efoCellLine.getName() != null && cloCellLine.getName() != null && clsCellLine.getName() != null ) {
					cellLineMatcher.setSource( clsCellLine );
					cellLineMatcher.setTarget( efoCellLine );
					efoFlag = cellLineMatcher.match();
					cellLineMatcher.setTarget( cloCellLine );
					cloFlag = cellLineMatcher.match();
					flag = efoFlag && cloFlag;
				}
				mappingFlag[1] = ( flag );
				entry.add( String.valueOf( flag ) );
				
				// Name
				entry.add( nameDist );
				entry.add( efoCellLine.getName() );
				entry.add( cloCellLine.getName() );
				entry.add( clsCellLine.getName() );
				
				// Synonyms
				entry.add( synonymDist );
				entry.add( String.valueOf( efoCellLine.getSynonyms() ) );
				entry.add( String.valueOf( cloCellLine.getSynonyms() ) );
				entry.add( String.valueOf( clsCellLine.getSynonyms() ) );
				
				// Disease Match (EFO-CLO)
				flag = null;
				if ( !efoCellLine.getDiseases().isEmpty() && !cloCellLine.getDiseases().isEmpty() ) {
					for ( Disease efoDisease : efoCellLine.getDiseases() ) {
						diseaseMatcher.setSource( efoDisease );
						for ( Disease cloDisease : cloCellLine.getDiseases() ) {
							diseaseMatcher.setTarget( cloDisease );
							if ( diseaseMatcher.match() )
								flag = true;
						}
					}
				}
				mappingFlag[2] = flag;
				entry.add( String.valueOf( flag ) );
				
				// Disease Match (EFO-CLS, CLS-CLS)
				flag = null;
				efoFlag = null;
				cloFlag = null;
				if ( !efoCellLine.getDiseases().isEmpty() && !cloCellLine.getDiseases().isEmpty() && !clsCellLine.getDiseases().isEmpty() ) {
					efoFlag = false;
					cloFlag = false;
					for ( Disease clsDisease : clsCellLine.getDiseases() ) {
						diseaseMatcher.setSource( clsDisease );
						
						for ( Disease efoDisease : efoCellLine.getDiseases() ) {
							diseaseMatcher.setTarget( efoDisease );
							if ( diseaseMatcher.match() )
								efoFlag = true;
						}
						
						for ( Disease cloDisease : cloCellLine.getDiseases() ) {
							diseaseMatcher.setTarget( cloDisease );
							if ( diseaseMatcher.match() )
								cloFlag = true;
						}
					}
					flag = efoFlag && cloFlag;
				}
				mappingFlag[3] = ( flag );
				entry.add( String.valueOf( flag ) );
				
				// Disease
				ArrayList<String> efoDiseasesMsgs = new ArrayList<String>();
				for ( Disease disease : efoCellLine.getDiseases() )
					efoDiseasesMsgs.add( disease.getAccession() + "#" + disease.getName() );
				ArrayList<String> cloDiseasesMsgs = new ArrayList<String>();
				for ( Disease disease : cloCellLine.getDiseases() )
					cloDiseasesMsgs.add( disease.getAccession() + "#" + disease.getName() );
				ArrayList<String> clsDiseasesMsgs = new ArrayList<String>();
				for ( Disease disease : clsCellLine.getDiseases() )
					clsDiseasesMsgs.add( disease.getAccession() + "#" + disease.getName() );
				entry.add( String.valueOf( efoDiseasesMsgs ) );
				entry.add( String.valueOf( cloDiseasesMsgs ) );
				entry.add( String.valueOf( clsDiseasesMsgs ) );
				
				// Species Match (EFO-CLO)
				flag = null;
				if ( !efoCellLine.getSpecies().isEmpty() && !cloCellLine.getSpecies().isEmpty() ) {
					for ( Species efoSpecies : efoCellLine.getSpecies() ) {
						speciesMatcher.setSource( efoSpecies );
						for ( Species cloSpecies : cloCellLine.getSpecies() ) {
							speciesMatcher.setTarget( cloSpecies );
							if ( speciesMatcher.match() )
								flag = true;
						}
					}
				}
				mappingFlag[4] = flag;
				entry.add( String.valueOf( flag ) );
				
				// Species Match(EFO-CLS, CLO-CLS)
				flag = null;
				efoFlag = null;
				cloFlag = null;
				if ( !efoCellLine.getSpecies().isEmpty() && !cloCellLine.getSpecies().isEmpty() && !clsCellLine.getSpecies().isEmpty() ) {
					efoFlag = false;
					cloFlag = false;
					for ( Species clsSpecies : clsCellLine.getSpecies() ) {
						speciesMatcher.setSource( clsSpecies );
						
						for ( Species efoSpecies : efoCellLine.getSpecies() ) {
							speciesMatcher.setTarget( efoSpecies );
							if ( speciesMatcher.match() )
								efoFlag = true;
						}
						
						for ( Species cloSpecies : cloCellLine.getSpecies() ) {
							speciesMatcher.setTarget( cloSpecies );
							if ( speciesMatcher.match() )
								cloFlag = true;
						}
					}
					flag = efoFlag && cloFlag;
				}
				mappingFlag[5] = ( flag );
				entry.add( String.valueOf( flag ) );
				
				// Species
				ArrayList<String> efoSpeciesMsgs = new ArrayList<String>();
				for ( Species species : efoCellLine.getSpecies() )
					efoSpeciesMsgs.add( species.getAccession() + "#" + species.getName() );
				ArrayList<String> clsSpeciesMsgs = new ArrayList<String>();
				for ( Species species : clsCellLine.getSpecies() )
					clsSpeciesMsgs.add( species.getAccession() + "#" + species.getName() );
				ArrayList<String> cloSpeciesMsgs = new ArrayList<String>();
				for ( Species species : cloCellLine.getSpecies() )
					cloSpeciesMsgs.add( species.getAccession() + "#" + species.getName() );
				entry.add( String.valueOf( efoSpeciesMsgs ) );
				entry.add( String.valueOf( cloSpeciesMsgs ) );
				entry.add( String.valueOf( clsSpeciesMsgs ) );
				
				boolean[] finalDecision = {false,false,false};
				if ( mappingFlag[0] == null ) {
					if ( mappingFlag[1] != null && mappingFlag[1] )
						finalDecision[0] = true;
				} else if ( mappingFlag[0] ) {
					finalDecision[0] = true;
				} else {
					if ( mappingFlag[1] != null && mappingFlag[1] )
						finalDecision[0] = true;
				}
				if ( mappingFlag[2] == null ) {
					if ( mappingFlag[3] == null )
						finalDecision[1] = true;
					else if ( mappingFlag[3] )
						finalDecision[1] = true;
				} else if ( mappingFlag[2] ) {
					finalDecision[1] = true;
				}
				if ( mappingFlag[4] == null ) {
					if ( mappingFlag[5] == null )
						finalDecision[2] = true;
					else if ( mappingFlag[5] )
						finalDecision[2] = true;
				} else if ( mappingFlag[4] ) {
					finalDecision[2] = true;
				}
				
				if ( !finalDecision[0] ) {
					if ( ( mappingFlag[2] != null && mappingFlag[2] ) 
							|| ( mappingFlag[4] != null && mappingFlag[4] ) ) {
						if ( mappingFlag[1] != null && mappingFlag[1] )
							finalDecision[0] = true;
						if ( mappingFlag[1] || Integer.getInteger( nameDist ) < 2 || Integer.getInteger( synonymDist ) < 2 )
							finalDecision[0] = true;
					}
				}
				

				String[] output = new String[entry.size()];
				if ( finalDecision[0] && finalDecision[1] && finalDecision[2] ) {
					validMapping.add( efoAccession );
					validwriter.writeNext( entry.toArray( output ) );
				} else {
					invalidMapping.add( efoAccession );
					invalidwriter.writeNext( entry.toArray( output ) );
				}
			}
			validwriter.close();
			invalidwriter.close();
			logger.info( String.format( "Found %d one-to-one cell line mapping from EFO to Cellosaurus", validMapping.size()  ) );
			logger.info( String.format( "Found %d cell lines with invalid mapping from EFO to Cellosaurus", invalidMapping.size() ) );
		} catch (IOException e) {
			logger.error( e.getMessage() );
			System.exit( -1 );
		}
	}
}

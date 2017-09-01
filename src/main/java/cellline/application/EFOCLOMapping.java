package cellline.application;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import au.com.bytecode.opencsv.CSVWriter;
import cellline.match.CellLineMatcher;
import cellline.match.CellTypeMatcher;
import cellline.match.DiseaseMatcher;
import cellline.match.OrganismPartMatcher;
import cellline.match.SpeciesMatcher;
import cellline.model.CLOOntologyModel;
import cellline.model.CellosaurusModel;
import cellline.model.DiseaseOntologyModel;
import cellline.model.EFOOntologyModel;
import cellline.object.CLOCellLine;
import cellline.object.CellLine;
import cellline.object.CellType;
import cellline.object.CellosaurusCellLine;
import cellline.object.Disease;
import cellline.object.EFOCellLine;
import cellline.object.OrganismPart;
import cellline.object.Species;

/**
 * @file EFOCLOMapping.java
 * @author Edison Ong
 * @since Aug 9, 2017
 * @version 1.0
 * @comment 
 */

/**
 * 
 */
public class EFOCLOMapping {
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
	@Option(
			name = "-d",
			usage = "Debug mode.",
			required = false,
			aliases = {"--debug"}
			)
	private boolean debug;
	
	static final Logger logger = LoggerFactory.getLogger( EFOCLOMapping.class );
	
	private EFOOntologyModel efoModel;
	private CLOOntologyModel cloModel;
	private CellosaurusModel clsModel;
	private DiseaseOntologyModel doidModel;
	
	HashMap<String, String> efoCLSMap;
	HashMap<String, String> cloCLSMap;
	Set<String> efoMappedSet;
	
	Set<String> exactValidMapping;
	Set<String> exactInvalidMapping;
	Set<String> multiMapping;
	Set<String> fuzzyMapping;
	
	SynchronizedEntrySet exactValidEntrySet;
	SynchronizedEntrySet exactInvalidEntrySet;
	SynchronizedEntrySet multiEntrySet;
	SynchronizedEntrySet fuzzyEntrySet;
	
	public static void main( String[] args ) {
		new EFOCLOMapping().run( args );
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
		this.cloCLSMap = new HashMap<String, String>();
		for ( CellosaurusCellLine cloCLSCellLine : clsModel.getCellLinesFromCrossReferenceSource( "CLO" ) ) {
			Set<String> cloAccessions = cloCLSCellLine.getCrossReferenceAccessionsFromSource( "CLO" );
			if ( cloAccessions.size() == 1 ) {
				cloCLSMap.put( cloAccessions.iterator().next(), cloCLSCellLine.getAccession() );
			}
		}
		logger.info( String.format( "Found %d CLO mapping in Cellosaurus", cloCLSMap.size() ) );
		this.efoMappedSet = Sets.intersection( new HashSet<String>( efoCLSMap.values() ), new HashSet<String>( cloCLSMap.values() ) );
		logger.info( String.format( "Mapped %d EFO to CLO through Cellosaurus", efoMappedSet.size() ) );
		
		// Load EFO
		this.efoModel = new EFOOntologyModel( new File( this.efoFileName ) );
		
		// Load CLO
		this.cloModel = new CLOOntologyModel( new File ( this.cloFileName ) );
		
		// Load DOID
		this.doidModel = new DiseaseOntologyModel();
		
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
		
		this.map();
	}
	
	public void map() {
		this.exactValidMapping = new HashSet<String>();
		this.exactInvalidMapping = new HashSet<String>();
		this.multiMapping = new HashSet<String>();
		this.fuzzyMapping = new HashSet<String>();
		
		this.exactValidEntrySet = new SynchronizedEntrySet();
		this.exactInvalidEntrySet = new SynchronizedEntrySet();
		this.multiEntrySet = new SynchronizedEntrySet();
		this.fuzzyEntrySet = new SynchronizedEntrySet();
		
		int numCPU = Runtime.getRuntime().availableProcessors();
		logger.info( String.format( "%d CPU available in the system", numCPU ) );
		int maxThread = (int) Math.round( numCPU * 0.9 );
		logger.info( "Setting max thread to " + String.valueOf( maxThread ) );
		//ExecutorService executor = new ThreadPoolExecutor( 1, maxThread, 100000, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>() );
		ExecutorService executor = Executors.newFixedThreadPool( maxThread );
		
		int counter = 0;
		for ( Map.Entry<String, EFOCellLine> efo : this.efoModel.getCellLines().entrySet() ) {
			String efoAccession = efo.getKey();
			EFOCellLine efoCellLine = efo.getValue();
			
			if ( !efoCellLine.getDatabase().equalsIgnoreCase( "EFO" ) )
				continue;
			if ( efoMappedSet.contains( efoCLSMap.get( efoAccession ) ) )
				continue;
			counter ++;
			logger.info( String.format( "Submit EFO Cell Line to queue#%d: %s", counter, efoAccession ) );
			MatchThread match = new MatchThread();
			match.setEFO( efo );
			match.setName( String.format( "Queue#%d: %s", counter, efoAccession ) );
			Thread thread = new Thread( match );
			executor.execute( thread );
			if ( debug && counter == ( maxThread * 2 ) ) break;
		}
		executor.shutdown();
		
		try {
			boolean finished = executor.awaitTermination(10, TimeUnit.HOURS);
			if ( finished ) {
				logger.info( String.format( "Found %d exact match valid cell line mapping from EFO to CLO", exactValidMapping.size()  ) );
				MapWriter exactValidWriter = new MapWriter( outputDirectory + "Exact_Valid_Mapping_EFO-CLO.tsv" );
				for ( String[] entry : this.exactValidEntrySet.value() ) {
					exactValidWriter.writeEntry( entry );
				}
				exactValidWriter.close();
				
				logger.info( String.format( "Found %d exact match invalid cell line mapping from EFO to CLO", exactInvalidMapping.size()  ) );
				MapWriter exactInvalidWriter = new MapWriter( outputDirectory + "Exact_Invalid_Mapping_EFO-CLO.tsv" );
				for ( String[] entry : this.exactInvalidEntrySet.value() ) {
					exactInvalidWriter.writeEntry( entry );
				}
				exactInvalidWriter.close();
				
				logger.info( String.format( "Found %d multi-mapping cell line mapping from EFO to CLO", multiMapping.size()  ) );
				MapWriter multiWriter = new MapWriter( outputDirectory + "Multi_Mapping_EFO-CLO.tsv" );
				for ( String[] entry : this.multiEntrySet.value() ) {
					multiWriter.writeEntry( entry );
				}
				multiWriter.close();
				
				logger.info( String.format( "Found %d inexact cell line mapping from EFO to CLO", fuzzyMapping.size()  ) );
				MapWriter fuzzyWriter = new MapWriter( outputDirectory + "Inexact_Mapping_EFO-CLO.tsv" );
				for ( String[] entry : this.fuzzyEntrySet.value() ) {
					fuzzyWriter.writeEntry( entry );
				}
				fuzzyWriter.close();
				
				logger.info( "Finsihed EFO-CLO mapping" );
			}
		} catch (IOException e) {
			logger.error( e.getMessage() );
			System.exit( -1 );
		} catch (InterruptedException e) {
			logger.error( e.getMessage() );
		}
		
		System.gc();
	}
	
	private class MatchThread implements Runnable {
		
		private Logger threadLogger = LoggerFactory.getLogger( MatchThread.class );
		
		private Entry<String, EFOCellLine> efo;
		public void setEFO( Entry<String, EFOCellLine> efo ) {
			this.efo = efo;
		}
		
		private String name;
		public void setName( String name ) {
			this.name = name;
		}
		
		public void run() {
			// Set up matchers
			CellLineMatcher cellLineMatcher = new CellLineMatcher();
			DiseaseMatcher diseaseMatcher = new DiseaseMatcher( doidModel, efoModel );
			SpeciesMatcher speciesMatcher = new SpeciesMatcher();
			OrganismPartMatcher organismPartMatcher = new OrganismPartMatcher();
			CellTypeMatcher cellTypeMatcher = new CellTypeMatcher();
			
			Stopwatch timer = Stopwatch.createStarted();
			
			String efoAccession = efo.getKey();
			EFOCellLine efoCellLine = efo.getValue();
			
			TreeSet<CellLineMatch> matches = new TreeSet<CellLineMatch>();
			boolean exact = false;
			boolean multi = false;
			int counter = 0;
			int process = -1;
			int size = cloModel.getCellLines().size();
			
			threadLogger.info( "Starting " + this.name );
			
			Set<String> keySet = new HashSet<String>( cloModel.getCellLines().keySet() );
			for ( String cloAccession : keySet ) {
				CLOCellLine cloCellLine = cloModel.getCellLineFromAccession( cloAccession );
				synchronized ( cloCellLine ) {
					if ( efoMappedSet.contains( cloCLSMap.get( cloAccession ) ) )
						continue;
					counter++;
					int percent = (int) Math.floor( (double) counter / (double) size * 20 );
					if ( percent > process ) {
						process = percent;
						threadLogger.info( String.format( "[%s] Processing <%s%s> elapsed time: %s", 
								efoAccession,
								new String( new char[process] ).replace( "\0", "#" ),
								new String( new char[(20-process)] ).replace( "\0", "-" ),
								timer.toString() ) );
					}
					
					CellLineMatch match = new CellLineMatch();
					match.setCellLine( cloCellLine );
					int score = 0;
					boolean valid = true;
					
					// Cell Line Name/Synonyms/XREF matching
					cellLineMatcher.setSource( efoCellLine );
					cellLineMatcher.setTarget( cloCellLine );
					match.setEditDistance( cellLineMatcher.getNameEditDistanceIgnoreCase() );
					match.setSynonymsDistance( cellLineMatcher.getShortestSynonymEditDistanceIgnoreCase() );
					if ( cellLineMatcher.matchIgnoreCase() ) {
						threadLogger.info( String.format( "[%s] Found exact name/synonyms/cross-reference exact matched Cell Line: %s", efoAccession, cloAccession ) );
						if ( exact )
							multi = true;
						exact = true;
						score = 8;
					} else {
						score -= match.getSynonymsDistance();
					}
					
					// Cell Line Disease matching
					if ( !efoCellLine.getDiseases().isEmpty() && !cloCellLine.getDiseases().isEmpty() ) {
						match.setDiseaseMatch( false );
						for ( Disease efoDisease : efoCellLine.getDiseases() ) {
							for ( Disease cloDisease : cloCellLine.getDiseases() ) {
								diseaseMatcher.setSource( efoDisease );
								diseaseMatcher.setTarget( cloDisease );
								if ( diseaseMatcher.match() )
									match.setDiseaseMatch( true );
							}
						}
						if ( match.isDiseaseMatch() )
							score ++;
						else {
							valid = false;
							score --;
						}
					} else {
						match.setDiseaseMatch( null );
					}
					
					// Cell Line Species matching
					if ( !efoCellLine.getSpecies().isEmpty() && !cloCellLine.getSpecies().isEmpty() ) {
						match.setSpeciesMatch( false );
						for ( Species efoSpecies : efoCellLine.getSpecies() ) {
							for ( Species cloSpecies : cloCellLine.getSpecies() ) {
								speciesMatcher.setSource( efoSpecies );
								speciesMatcher.setTarget( cloSpecies );
								if ( speciesMatcher.match() )
									match.setSpeciesMatch( true );
							}
						}
						if ( match.isSpeciesMatch() )
							score ++;
						else {
							valid = false;
							score --;
						}
					} else {
						match.setSpeciesMatch( null );
					}
					
					// Cell Line OrganismPart matching
					if ( !efoCellLine.getOrganismParts().isEmpty() && !cloCellLine.getOrganismParts().isEmpty() ) {
						match.setOrganismMatch( false );
						for ( OrganismPart efoOrganismPart : efoCellLine.getOrganismParts() ) {
							for ( OrganismPart cloOrganismPart : cloCellLine.getOrganismParts() ) {
								organismPartMatcher.setSource( efoOrganismPart );
								organismPartMatcher.setTarget( cloOrganismPart );
								if ( organismPartMatcher.match() )
									match.setOrganismMatch( true );;
							}
						}
						if ( match.isOrganismMatch() )
							score ++;
						else {
							valid = false;
							score --;
						}
					} else {
						match.setOrganismMatch( null );
					}
					
					// Cell Line CellType matching
					if ( !efoCellLine.getCellTypes().isEmpty() && !cloCellLine.getCellTypes().isEmpty() ) {
						match.setCellMatch( false );
						for ( CellType efoCellType : efoCellLine.getCellTypes() ) {
							for ( CellType cloCellType : cloCellLine.getCellTypes() ) {
								cellTypeMatcher.setSource( efoCellType );
								cellTypeMatcher.setTarget( cloCellType );
								if ( cellTypeMatcher.match() )
									match.setCellMatch( true );;
							}
						}
						if ( match.isCellMatch() )
							score ++;
						else {
							valid = false;
							score --;
						}
					} else {
						match.setCellMatch( null );
					}
					
					// Update match score and suggested matches SortedSet
					match.setValid( valid );
					match.setScore( score );
					matches.add( match );
					if ( matches.size() > 3 ) {
						matches.pollFirst();
					}
				}
			}
			
			if ( exact ) {
				if ( !multi ) {
					CellLineMatch last = matches.last();
					if ( last.isValid() ) {//TODO Update value
						exactValidMapping.add( efoAccession );
						String[] entry = {
								efoAccession,
								String.valueOf( last.getScore() ),
								last.getCellLine().getAccession(),
								"exact match",
								"0",
								efoCellLine.getName(),
								last.getCellLine().getName(),
								"0",
								efoCellLine.getSynonyms().toString(),
								last.getCellLine().getSynonyms().toString(),
								String.valueOf( last.isDiseaseMatch() ),
								MapWriter.writeSetToString( efoCellLine.getDiseases() ),
								MapWriter.writeSetToString( last.getCellLine().getDiseases() ),
								String.valueOf( last.isSpeciesMatch() ),
								MapWriter.writeSetToString( efoCellLine.getSpecies() ),
								MapWriter.writeSetToString( last.getCellLine().getSpecies() ),
								String.valueOf( last.isOrganismMatch() ),
								MapWriter.writeSetToString( efoCellLine.getOrganismParts() ),
								MapWriter.writeSetToString( last.getCellLine().getOrganismParts() ),
								String.valueOf( last.isCellMatch() ),
								MapWriter.writeSetToString( efoCellLine.getCellTypes() ),
								MapWriter.writeSetToString( last.getCellLine().getCellTypes() ),
						};
						exactValidEntrySet.add( entry );
						threadLogger.info( String.format( "[%s] exact valid mapping with score (%d)%s", efoAccession, last.getScore(), last.getCellLine().getAccession() ) );
					} else {
						exactInvalidMapping.add( efoAccession );
						String[] entry = {
								efoAccession,
								String.valueOf( last.getScore() ),
								last.getCellLine().getAccession(),
								"exact match",
								"0",
								efoCellLine.getName(),
								last.getCellLine().getName(),
								"0",
								efoCellLine.getSynonyms().toString(),
								last.getCellLine().getSynonyms().toString(),
								String.valueOf( last.isDiseaseMatch() ),
								MapWriter.writeSetToString( efoCellLine.getDiseases() ),
								MapWriter.writeSetToString( last.getCellLine().getDiseases() ),
								String.valueOf( last.isSpeciesMatch() ),
								MapWriter.writeSetToString( efoCellLine.getSpecies() ),
								MapWriter.writeSetToString( last.getCellLine().getSpecies() ),
								String.valueOf( last.isOrganismMatch() ),
								MapWriter.writeSetToString( efoCellLine.getOrganismParts() ),
								MapWriter.writeSetToString( last.getCellLine().getOrganismParts() ),
								String.valueOf( last.isCellMatch() ),
								MapWriter.writeSetToString( efoCellLine.getCellTypes() ),
								MapWriter.writeSetToString( last.getCellLine().getCellTypes() ),
						};
						exactInvalidEntrySet.add( entry );
						threadLogger.info( String.format( "[%s] exact invalid mapping with score (%d)%s", efoAccession, last.getScore(), last.getCellLine().getAccession() ) );
					}					
				} else {
					multiMapping.add( efoAccession );
					Set<String[]> entries = new HashSet<String[]>();
					while ( matches.size() != 0 ) {
						CellLineMatch current = matches.last();
						if ( current.getSynonymsDistance() == 0 ) {
							String[] entry = {
									efoAccession,
									String.valueOf( current.getScore() ),
									current.getCellLine().getAccession(),
									"exact match",
									"0",
									efoCellLine.getName(),
									current.getCellLine().getName(),
									"0",
									efoCellLine.getSynonyms().toString(),
									current.getCellLine().getSynonyms().toString(),
									String.valueOf( current.isDiseaseMatch() ),
									MapWriter.writeSetToString( efoCellLine.getDiseases() ),
									MapWriter.writeSetToString( current.getCellLine().getDiseases() ),
									String.valueOf( current.isSpeciesMatch() ),
									MapWriter.writeSetToString( efoCellLine.getSpecies() ),
									MapWriter.writeSetToString( current.getCellLine().getSpecies() ),
									String.valueOf( current.isOrganismMatch() ),
									MapWriter.writeSetToString( efoCellLine.getOrganismParts() ),
									MapWriter.writeSetToString( current.getCellLine().getOrganismParts() ),
									String.valueOf( current.isCellMatch() ),
									MapWriter.writeSetToString( efoCellLine.getCellTypes() ),
									MapWriter.writeSetToString( current.getCellLine().getCellTypes() ),
							};
							entries.add( entry );
							threadLogger.info( String.format( "[%s] multi mapping with score (%d)%s", efoAccession, current.getScore(), current.getCellLine().getAccession() ) );
						}
						matches.remove( current );
					}
					multiEntrySet.addAll( entries );
				}
			} else {
				fuzzyMapping.add( efoAccession );
				Set<String[]> entries = new HashSet<String[]>();
				while ( matches.size() != 0 ) {
					CellLineMatch current = matches.last();
					String[] entry = {
							efoAccession,
							String.valueOf( current.getScore() ),
							current.getCellLine().getAccession(),
							String.valueOf( false ),
							String.valueOf( current.getEditDistance() ),
							efoCellLine.getName(),
							current.getCellLine().getName(),
							String.valueOf( current.getSynonymsDistance() ),
							efoCellLine.getSynonyms().toString(),
							current.getCellLine().getSynonyms().toString(),
							String.valueOf( current.isDiseaseMatch() ),
							MapWriter.writeSetToString( efoCellLine.getDiseases() ),
							MapWriter.writeSetToString( current.getCellLine().getDiseases() ),
							String.valueOf( current.isSpeciesMatch() ),
							MapWriter.writeSetToString( efoCellLine.getSpecies() ),
							MapWriter.writeSetToString( current.getCellLine().getSpecies() ),
							String.valueOf( current.isOrganismMatch() ),
							MapWriter.writeSetToString( efoCellLine.getOrganismParts() ),
							MapWriter.writeSetToString( current.getCellLine().getOrganismParts() ),
							String.valueOf( current.isCellMatch() ),
							MapWriter.writeSetToString( efoCellLine.getCellTypes() ),
							MapWriter.writeSetToString( current.getCellLine().getCellTypes() ),
					};
					entries.add( entry );
					threadLogger.info( String.format( "[%s] inexact mapping with score (%d)%s", efoAccession, current.getScore(), current.getCellLine().getAccession() ) );
					matches.remove( current );
				}
				fuzzyEntrySet.addAll( entries );
			}
			System.gc();
			threadLogger.info( String.format( "[%s] Finished in %s", efoAccession, timer.stop().toString() ) );
		}
	}
}

class MapWriter {
	private CSVWriter writer;
	private String[] entry;
	private String[] colName = { 
			"EFO Accession",
			"Score",
			"CLO Accession",
			"Name/Synonyms Match?",
			"Name Edit Distance",
			"EFO Name",
			"CLO Name",
			"Shorest Synonyms Edit Distance",
			"EFO Synonyms",
			"CLO Synonyms",
			"Disease Match?",
			"EFO Disease",
			"CLO Disease",
			"Species Match?",
			"EFO Species",
			"CLO Species",
			"Organism Part Match?",
			"EFO Organism Part",
			"CLO Organism Part",
			"Cell/Cell Type Match?",
			"EFO Cell Type",
			"CLO Cell",
	};
	
	public MapWriter( String fileName ) throws IOException {
		this.writer = new CSVWriter( new FileWriter( fileName ) );
		this.writer.writeNext( colName );
		this.entry = new String[colName.length];
	}
	
	public void clearEntry() {
		this.entry = new String[colName.length];
	}
	
	public void addEntry( int index, String input ) {
		this.entry[index] = input;
	}
	
	public boolean setEntry( String[] entry ) {
		if ( this.colName.length == entry.length ) {
			this.entry = entry;
			return true;
		}
		return false;
	}
	
	public boolean writeEntry( String[] entry ) {
		if ( this.setEntry( entry ) && this.writeNext() )
			return true;
		return false;
	}
	
	public boolean writeNext() {
		this.writer.writeNext( this.entry );
		this.clearEntry();
		return true;
	}
	
	public boolean close() {
		try {
			this.writer.close();
		} catch (IOException e) {
			EFOCLOMapping.logger.error( "Mapping writer error", e );
			return false;
		}
		return true;
	}
	
	public static String writeSetToString( Set<? extends Object> objectSet ) {
		ArrayList<String> msgList = new ArrayList<String>();
		for ( Object object : objectSet ) {
			if ( object instanceof Disease ) {
				Disease disease = (Disease) object;
				msgList.add( disease.getAccession() + "#" + disease.getName() );
			} else if ( object instanceof Species ) {
				Species species = (Species) object;
				msgList.add( species.getAccession() + "#" + species.getName() );
			} else if ( object instanceof OrganismPart ) {
				OrganismPart organismPart = (OrganismPart) object;
				msgList.add( organismPart.getAccession() + "#" + organismPart.getName() );
			} else if ( object instanceof CellType ) { 
				CellType cellType = (CellType) object;
				msgList.add( cellType.getAccession() + "#" + cellType.getName() );
			} else {
				msgList.add( object.toString() );
			}
		}
		return msgList.toString();
	}
}

class CellLineMatch implements Comparable<CellLineMatch> {
	private int score;
	/**
	 * @return the score
	 */
	public int getScore() {
		return score;
	}
	/**
	 * @param score the score to set
	 */
	public void setScore(int score) {
		this.score = score;
	}
	
	private CellLine cellLine;
	/**
	 * @return the cellLine
	 */
	public CellLine getCellLine() {
		return cellLine;
	}
	/**
	 * @param cellLine the cellLine to set
	 */
	public void setCellLine(CellLine cellLine) {
		this.cellLine = cellLine;
	}
	
	private int editDistance;
	/**
	 * @return the editDistance
	 */
	public int getEditDistance() {
		return editDistance;
	}
	/**
	 * @param editDistance the editDistance to set
	 */
	public void setEditDistance(int editDistance) {
		this.editDistance = editDistance;
	}
	
	private int synonymsDistance;
	/**
	 * @return the synonymsDistance
	 */
	public int getSynonymsDistance() {
		return synonymsDistance;
	}
	/**
	 * @param synonymsDistance the synonymsDistance to set
	 */
	public void setSynonymsDistance(int synonymsDistance) {
		this.synonymsDistance = synonymsDistance;
	}
	
	private Boolean diseaseMatch;
	/**
	 * @return the diseaseMatch
	 */
	public Boolean isDiseaseMatch() {
		return diseaseMatch;
	}
	/**
	 * @param diseaseMatch the diseaseMatch to set
	 */
	public void setDiseaseMatch(Boolean diseaseMatch) {
		this.diseaseMatch = diseaseMatch;
	}
	
	private Boolean speciesMatch;
	/**
	 * @return the speciesMatch
	 */
	public Boolean isSpeciesMatch() {
		return speciesMatch;
	}
	/**
	 * @param speciesMatch the speciesMatch to set
	 */
	public void setSpeciesMatch(Boolean speciesMatch) {
		this.speciesMatch = speciesMatch;
	}
	
	private Boolean organismMatch;
	/**
	 * @return the organismMatch
	 */
	public Boolean isOrganismMatch() {
		return organismMatch;
	}
	/**
	 * @param organismMatch the organismMatch to set
	 */
	public void setOrganismMatch(Boolean organismMatch) {
		this.organismMatch = organismMatch;
	}
	
	private Boolean cellMatch;
	/**
	 * @return the cellMatch
	 */
	public Boolean isCellMatch() {
		return cellMatch;
	}
	/**
	 * @param cellMatch the cellMatch to set
	 */
	public void setCellMatch(Boolean cellMatch) {
		this.cellMatch = cellMatch;
	}
	
	/**
	 * @return the valid
	 */
	public boolean isValid() {
		return valid;
	}
	/**
	 * @param valid the valid to set
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}
	private boolean valid;
	
	public CellLineMatch() {}
	
	@Override
	public int compareTo( CellLineMatch match ) {
		if ( this.getCellLine().getAccession().contentEquals( match.getCellLine().getAccession() ) )
			return 0;
		if ( this.score > match.score )
			return 1;
		else if ( this.score < match.score )
			return -1;
		else {
			if ( this.synonymsDistance < match.synonymsDistance )
				return 1;
			else if ( this.synonymsDistance > match.synonymsDistance )
				return -1;
			else {
				if ( this.isValid() && !match.isValid() )
					return 1;
				else if ( !this.isValid() && match.isValid() )
					return -1;
				else
					return 1;
			}
		}
	}
	
	@Override
	public boolean equals( Object obj ) {
		if ( obj instanceof CellLineMatch ) {
			CellLineMatch match = (CellLineMatch) obj;
			if ( this.compareTo( match ) == 0 )
				return true;
		}
		return false;
	}
}

class SynchronizedEntrySet {
    private Set<String[]> entrySet = new HashSet<String[]>();

    public synchronized void add( String[] entry ) {
        this.entrySet.add( entry );
    }
    
    public synchronized void addAll( Set<String[]> entries ) {
    	this.entrySet.addAll( entries );
    }

    public synchronized Set<String[]> value() {
        return this.entrySet;
    }
    
    public synchronized int size() {
    	return this.entrySet.size();
    }
}

/**
 * @file CLOOntologyModel.java
 * @author Edison Ong
 * @since Aug 4, 2017
 * @version 1.0
 * @comment 
 */
package cellline.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.UrlValidator;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.object.CrossReference;
import cellline.object.Disease;
import cellline.object.EFOCellLine;
import cellline.object.CLOCellLine;
import cellline.object.CellType;
import cellline.object.OrganismPart;
import cellline.object.Species;
import cellline.validation.AccessionValidator;

/**
 * 
 */
public class CLOOntologyModel extends OntologyModel {
	
	public static final String CLO_IRI_FORMAT = "http://purl.obolibrary.org/obo/%s_%s";
	public static final String[] CLO_SYNONYMS_IRI_STR = {
			"http://purl.obolibrary.org/obo/IAO_0000118",
			"http://www.ebi.ac.uk/efo/alternative_term",
			"http://www.geneontology.org/formats/oboInOwl#hasBroadSynonym",
			"http://www.geneontology.org/formats/oboInOwl#hasExactSynonym",
			"http://www.geneontology.org/formats/oboInOwl#hasNarrowSynonym",
			"http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym",
	};
	public static final String CLO_CELL_LINE_IRI_STR = "http://purl.obolibrary.org/obo/CLO_0000019";
	public static final String[] CLO_DB_XREF_IRI_STR = {
			"http://www.geneontology.org/formats/oboInOwl#hasDbXref",
			"http://purl.obolibrary.org/obo/IAO_0000119",
	};
	public static final String CLO_LINCS_ID_IRI_STR = "http://purl.obolibrary.org/obo/CLO_0000178";
	public static final String CLO_PUBCHEM_AID_IRI_STR = "http://purl.obolibrary.org/obo/CLO_0037244";
	public static final String CLO_SEEALSO_IRI_STR = "http://www.w3.org/2000/01/rdf-schema#seeAlso";
	public static final String CLO_LINCS_XREF_IRI_STR = "http://purl.obolibrary.org/obo/CLO_0000178";
	public static final String CLO_DOID_DISEASE_IRI_STR = "http://purl.obolibrary.org/obo/DOID_4";
	public static final String CLO_EFO_DISEASE_IRI_STR = "http://www.ebi.ac.uk/efo/EFO_0000408";
	public static final String[] CLO_DISEASE_PROPERTY_IRI_STR = {
			"http://purl.obolibrary.org/obo/CLO_0000015",
			"http://purl.obolibrary.org/obo/CLO_0000167",
			"http://purl.obolibrary.org/obo/CLO_0000179",
			"http://www.ebi.ac.uk/cellline#is_model_for"
	};
	public static final String CLO_DERIVES_FROM_IRI_STR = "http://purl.obolibrary.org/obo/RO_0001000";
	public static final String CLO_ORGANISM_IRI_STR = "http://purl.obolibrary.org/obo/OBI_0100026";
	public static final String CLO_ORGANISM_PART_IRI_STR = "http://purl.obolibrary.org/obo/UBERON_0001062";
	public static final String CLO_CELL_IRI_STR = "http://purl.obolibrary.org/obo/CL_0000003";
	
	static final Logger logger = LoggerFactory.getLogger( CLOOntologyModel.class );
	
	public CLOOntologyModel() {
		super();
	}
	
	public CLOOntologyModel( File cloFile ) {
		super( cloFile );
		this.parseCellTypesFromOntology();
		this.parseOrganismPartsFromOntology();
		this.parseDiseasesFromOntology();
		this.parseCellLinesFromOntology();
	}
	
	public CLOOntologyModel( IRI cloIri ) {
		super( cloIri );
		this.parseCellTypesFromOntology();
		this.parseOrganismPartsFromOntology();
		this.parseDiseasesFromOntology();
		this.parseCellLinesFromOntology();
	}
	
	@Override
	public CrossReference parseCrossReference( String xRefString ) {
		if ( xRefString.startsWith( "WEB") )
			xRefString = xRefString.replaceFirst( "WEB[^\\w]*", "" );
		if ( xRefString.startsWith( "PubMed" ) )
			xRefString = xRefString.replaceFirst( "PubMed[^\\w]*", "" );
		Pattern pattern;
		if ( new UrlValidator().isValid( xRefString ) ) {
			if ( xRefString.endsWith( ".jpg" ) || xRefString.endsWith( ".png" ) || xRefString.endsWith( ".svg" ) || xRefString.endsWith( ".gif" ) ) 
				return null;
			if ( xRefString.matches( "(?i:.*(wikipedia.org).*)") ) 
				pattern = Pattern.compile( "(wikipedia).*[\\/#](?!.*[\\/#])(.*)", Pattern.CASE_INSENSITIVE );
			else if ( xRefString.matches( "(?i:.*(pubmed)/(\\d+))" ) )
				pattern = Pattern.compile( "(pubmed)/(\\d+)", Pattern.CASE_INSENSITIVE );
			else if ( xRefString.matches( "(?i:.*(atcc).*)" ) )
				pattern = Pattern.compile( ".*(atcc).*[\\/#](?!.*[\\/#])([^.]*)", Pattern.CASE_INSENSITIVE );
			else if ( xRefString.matches( "(?i:.*(hypercldb|cldb).*)" ) )
				pattern = Pattern.compile( ".*(hypercldb|cldb).*[\\\\/#](?!.*[\\\\/#])([^.]*)", Pattern.CASE_INSENSITIVE );
			else if ( xRefString.matches( "(?i:.*(chembldb).*)" ) )
				pattern = Pattern.compile( "[\\/#](?!.*[\\/#])([a-zA-Z]+)(\\d+)", Pattern.CASE_INSENSITIVE );
			else if ( xRefString.matches( "(?i:.*(thermofisher).*)" ) )
				pattern = Pattern.compile( ".*(thermofisher).*[\\/#](?!.*[\\/#])([^.]*)", Pattern.CASE_INSENSITIVE );
			else if ( xRefString.matches( "(?i:.*(sigmaaldrich).*)" ) )
				pattern = Pattern.compile( ".*(sigmaaldrich).*[\\/#](?!.*[\\/#])([^.?]*)" );
			else 
				pattern = Pattern.compile( "[\\/#](?!.*[\\/#])([a-zA-Z0-9]+)[^:_]*[:_]?([^?=&%]*)" );
		} else {
			pattern = Pattern.compile( "([a-zA-Z0-9]+)[^:_]*[:_]?[ ]?(.*)" );
		}
		
		Matcher matcher = pattern.matcher( xRefString );
		if ( matcher.find() ) {
			String source = matcher.group( 1 );
			String identifier = matcher.group( 2 );
			
			// Special handling: [DB]: [DB]_[ID] format
			String[] tokens = identifier.split( "_" );
			if ( tokens.length > 1 && tokens[0].equalsIgnoreCase( source ) )
				return new CrossReference( identifier );
			
			// Special handling: RRID
			if ( source.equalsIgnoreCase( "RRID" ) ) {
				return new CrossReference( identifier );
			}
			
			// Special handling: Wikipedia
			if ( source.equalsIgnoreCase( "Wikipedia" ) ) {
				source = "Wikipedia";
				identifier = identifier.replaceAll( "_", " " );
				return new CrossReference( source, identifier );
			}
			
			// Special handling: NIFSTD
			if ( source.equalsIgnoreCase( "NIF_GrossAnatomy" ) )
				source = "NIFSTD";
			if ( source.equalsIgnoreCase( "NIFSTD" ) ) {
				if ( identifier.contains( "birnlex" ) )
					source = "NIFSTD:birnlex";
				else if ( identifier.contains( "nlx_dys" ) )
					source = "NIFSTD:nlx-dys";
				identifier = identifier.substring( identifier.lastIndexOf( "_" ) + 1 );
				return new CrossReference( source, identifier );
			}
			
			if ( source.equalsIgnoreCase( "ATCC" ) ) {
				source = "ATCC";
				return new CrossReference( source, identifier );
			}
			
			if ( source.equalsIgnoreCase( "CLDB" ) ) {
				source = "CLDB";
				return new CrossReference( source, identifier );
			}
			
			if ( source.equalsIgnoreCase( "ThermoFisher" ) ) {
				source = "ThermoFisher";
				return new CrossReference( source, identifier );
			}
			
			if ( source.equalsIgnoreCase( "SigmaAldrich" ) ) {
				source = "SigmaAldrich";
				return new CrossReference( source, identifier );
			}
			
			return new CrossReference( source, identifier );
		}
		return null;
	}
	
	// Cell Lines
	private HashMap<String, CLOCellLine> cellLines = new HashMap<String, CLOCellLine>();
	/**
	 * @return the cellLines
	 */
	public HashMap<String, CLOCellLine> getCellLines() {
		return cellLines;
	}

	private void parseCellLinesFromOntology() {
		NodeSet<OWLClass> cloCellLineNodeSet = this.reasoner.getSubClasses( this.dataFactory.getOWLClass( IRI.create( CLO_CELL_LINE_IRI_STR ) ), false );
		
		ArrayList<OWLClass> upperCellLineClasses = new ArrayList<OWLClass>(); //For Category
		
		for ( OWLClass cloCellLineClass : cloCellLineNodeSet.getFlattened() ) {
			// Ignore OWL:Nothing
			if ( cloCellLineClass.isOWLNothing() ) continue;
			Iterator<OWLAnnotation> labelIterator = EntitySearcher.getAnnotationObjects( cloCellLineClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( OntologyModel.RDF_LABEL_IRI_STR ) ) ).iterator();
			String cloCellLineLabel = labelIterator.next().getValue().asLiteral().get().getLiteral().toString();
			if ( cloCellLineLabel.endsWith( "cell line cell" ) ) {
				upperCellLineClasses.add( cloCellLineClass );
			} else {
				CLOCellLine cellLine = this.parseCellLine( cloCellLineClass );
				if ( new AccessionValidator().isValid( cellLine.getAccession() ) )
					this.cellLines.put( cellLine.getAccession(), cellLine );
				else
					logger.warn( String.format( "Incorrect CLO cell line accession for %s: %s", cellLine.getAccession(), cellLine.getIri() ) );
			}
		}
		logger.info( String.format( "Found %d cell lines in CLO ontology", this.cellLines.size() ) );
	}
	
	private CLOCellLine parseCellLine( OWLClass cellLineClass ) {
		CLOCellLine cellLine = new CLOCellLine();
		
		// IRI
		String iri = cellLineClass.getIRI().toString();
		cellLine.setIri( iri );
		
		// Accession
		cellLine.setAccession( this.parseAccessionFromIRI( iri ) );
		
		// Name
		Iterator<OWLAnnotation> labelIterator = EntitySearcher.getAnnotationObjects( cellLineClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( RDF_LABEL_IRI_STR ) ) ).iterator();
		String cellLineLabel = labelIterator.next().getValue().asLiteral().get().getLiteral().toString();
		Matcher labelMatcher = Pattern.compile( "(.*)[ ]?cell[ ]?$" ).matcher( cellLineLabel );
		if ( labelMatcher.find() ) {
			cellLine.addSynonyms( cellLineLabel );
			cellLine.setName( labelMatcher.group( 1 ).trim() );
		} else
			cellLine.setName( cellLineLabel );
		
		// Synonyms
		for ( String synonymIRIString : CLO_SYNONYMS_IRI_STR ) {
			Iterator<OWLAnnotation> synonymIterator = EntitySearcher.getAnnotationObjects( cellLineClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( synonymIRIString ) ) ).iterator();
			while ( synonymIterator.hasNext() ) cellLine.addSynonyms( synonymIterator.next().getValue().asLiteral().get().getLiteral().toString() );
		}
		
		// Database cross reference
		Iterator<OWLAnnotation> xRefIterator;
		for ( String xRefIriString : CLO_DB_XREF_IRI_STR ) {
			xRefIterator = EntitySearcher.getAnnotationObjects( cellLineClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( xRefIriString ) ) ).iterator();
			while ( xRefIterator.hasNext() ) {
				String xRefString = xRefIterator.next().getValue().asLiteral().get().getLiteral().toString();
				CrossReference xRef = this.parseCrossReference( xRefString );
				if ( xRef == null ) continue;
				if ( xRef.getAccession() != null )
					cellLine.addCrossReferences( xRef );
				else
					logger.warn( String.format( "Incorrect CLO cross reference for %s: %s", cellLine.getAccession(), xRefString ) );
			}
		}
		// Cell line LINCS ID
		xRefIterator = EntitySearcher.getAnnotationObjects( cellLineClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( CLO_LINCS_ID_IRI_STR ) ) ).iterator();
		while ( xRefIterator.hasNext() ) {
			String xRefString = xRefIterator.next().getValue().asLiteral().get().getLiteral().toString();
			CrossReference xRef = this.parseCrossReference( "LINCS_" + xRefString );
			if ( xRef == null ) continue;
			if ( xRef.getAccession() != null )
				cellLine.addCrossReferences( xRef );
			else
				logger.warn( String.format( "Incorrect CLO cross reference for %s: %s", cellLine.getAccession(), xRefString ) );
		}
		// PubChem AID
		xRefIterator = EntitySearcher.getAnnotationObjects( cellLineClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( CLO_PUBCHEM_AID_IRI_STR ) ) ).iterator();
		while ( xRefIterator.hasNext() ) {
			String xRefString = xRefIterator.next().getValue().asLiteral().get().getLiteral().toString();
			CrossReference xRef = this.parseCrossReference( "PUBCHEM-BioAssay_" + xRefString );
			if ( xRef == null ) continue;
			if ( xRef.getAccession() != null )
				cellLine.addCrossReferences( xRef );
			else
				logger.warn( String.format( "Incorrect CLO cross reference for %s: %s", cellLine.getAccession(), xRefString ) );
		}
		
		
		Collection<OWLAxiom> axiomCollection = EntitySearcher.getReferencingAxioms( cellLineClass, this.ontology );
		
		// Disease
		for ( String diseasePropertyIriString : CLO_DISEASE_PROPERTY_IRI_STR ) {
			OWLObjectProperty diseaseBearer = this.dataFactory.getOWLObjectProperty( IRI.create( diseasePropertyIriString ) );
			// Species
			OWLClass organismClass = this.dataFactory.getOWLClass( IRI.create( CLO_ORGANISM_IRI_STR ) );
			OWLObjectProperty derivesFrom = this.dataFactory.getOWLObjectProperty( IRI.create( CLO_DERIVES_FROM_IRI_STR ) );
			for ( OWLAxiom axiom : axiomCollection ) {
				if ( axiom.containsEntityInSignature( diseaseBearer ) ) {
					Set<OWLClass> diseasesClassSet = axiom.getClassesInSignature();
					for ( OWLClass diseaseClass : diseasesClassSet ) {
						String diseaseAccession = this.parseAccessionFromIRI( diseaseClass.getIRI().toString() );
						if ( this.diseases.containsKey( diseaseAccession ) ) {
							cellLine.addDisease( this.diseases.get( diseaseAccession ) );
						}
					}
				}
				if ( axiom.containsEntityInSignature( derivesFrom ) ) {
					Set<OWLClass> derivesFromClassSet = axiom.getClassesInSignature();
					for ( OWLClass derivesFromClass : derivesFromClassSet ) {
						for ( OWLClass tmpClass : this.reasoner.getSuperClasses( derivesFromClass, false ).getFlattened() ) {
							if ( tmpClass.equals( organismClass ) ) cellLine.addSpecies( this.parseSpecies( derivesFromClass ) );
						}
						String classAccession = this.parseAccessionFromIRI( derivesFromClass.getIRI().toString() );
						// Organism Parts
						if ( this.organismParts.containsKey( classAccession ) )
							cellLine.addOrganismPart( this.organismParts.get( classAccession ) );
						// Cell Types
						if ( this.cellTypes.containsKey( classAccession ) ) 
							cellLine.addCellType( this.cellTypes.get( classAccession ) );
					}
				}
			}
		}
		
		return cellLine;
	}
	
	public CLOCellLine getCellLineFromAccession( String accession ) {
		if ( this.cellLines.containsKey( accession ) )
			return this.cellLines.get( accession );
		else
			return new CLOCellLine();
	}
	
	public Boolean hasCellLineWithAccession( String accession ) {
		for ( String cellLineAccession : this.cellLines.keySet() ) {
			if ( cellLineAccession.contentEquals( accession ) ) return true;
		}
		return false;
	}
	
	// Diseases
	private HashMap<String, Disease> diseases = new HashMap<String, Disease>();
	private HashMap<String, IRI> diseaseIriMap = new HashMap<String, IRI>();
	private HashMap<String, String> diseaseOrganismPartMap = new HashMap<String, String>();
	
	public void parseDiseasesFromOntology() {
		//OWLObjectProperty hasDiseaseLocation = this.dataFactory.getOWLObjectProperty( IRI.create( EFO_HAS_DISEASE_LOCATION_IRI_STR ) );
		//OWLObjectProperty occurIn = this.dataFactory.getOWLObjectProperty( IRI.create( EFO_OCCURS_IN_IRI_STR ) );
		NodeSet<OWLClass> cloDiseaseNodeSet;
		cloDiseaseNodeSet = this.reasoner.getSubClasses( this.dataFactory.getOWLClass( IRI.create( CLO_DOID_DISEASE_IRI_STR ) ), false );
		for ( OWLClass cloDiseaseClass : cloDiseaseNodeSet.getFlattened() ) {
			// Ignore OWL:Nothing
			if ( cloDiseaseClass.isOWLNothing() ) continue;
			Disease disease = this.parseDisease( cloDiseaseClass );
			
			/* TODO Add organism parts from the disease axiom
			Collection<OWLAxiom> axiomCollection = EntitySearcher.getReferencingAxioms( cloDiseaseClass, this.ontology );
			for ( OWLAxiom axiom : axiomCollection ) {
				if ( axiom.containsEntityInSignature( hasDiseaseLocation ) || axiom.containsEntityInSignature( occurIn ) ) {
					Set<OWLClass> organismPartClassSet = axiom.getClassesInSignature();
					for ( OWLClass organismPartClass : organismPartClassSet ) {
						String organismPartAccession = this.parseAccessionFromIRI( organismPartClass.getIRI().toString() );
						if ( this.organismParts.containsKey( organismPartAccession ) ) {
							diseaseOrganismPartMap.put( disease.getAccession(), organismPartAccession );
						}
					}
				}
			}
			*/
			
			this.diseases.put( disease.getAccession(), disease );
			this.diseaseIriMap.put( disease.getAccession(), cloDiseaseClass.getIRI() );
		}
		
		cloDiseaseNodeSet = this.reasoner.getSubClasses( this.dataFactory.getOWLClass( IRI.create( CLO_EFO_DISEASE_IRI_STR ) ), false );
		for ( OWLClass cloDiseaseClass : cloDiseaseNodeSet.getFlattened() ) {
			// Ignore OWL:Nothing
			if ( cloDiseaseClass.isOWLNothing() ) continue;
			Disease disease = this.parseDisease( cloDiseaseClass );
			
			/* TODO Add organism parts from the disease axiom
			Collection<OWLAxiom> axiomCollection = EntitySearcher.getReferencingAxioms( cloDiseaseClass, this.ontology );
			for ( OWLAxiom axiom : axiomCollection ) {
				if ( axiom.containsEntityInSignature( hasDiseaseLocation ) || axiom.containsEntityInSignature( occurIn ) ) {
					Set<OWLClass> organismPartClassSet = axiom.getClassesInSignature();
					for ( OWLClass organismPartClass : organismPartClassSet ) {
						String organismPartAccession = this.parseAccessionFromIRI( organismPartClass.getIRI().toString() );
						if ( this.organismParts.containsKey( organismPartAccession ) ) {
							diseaseOrganismPartMap.put( disease.getAccession(), organismPartAccession );
						}
					}
				}
			}
			*/
			
			this.diseases.put( disease.getAccession(), disease );
			this.diseaseIriMap.put( disease.getAccession(), cloDiseaseClass.getIRI() );
		}
	}
	
	private Disease parseDisease( OWLClass diseaseClass ) {
		Disease disease = new Disease();
		
		if ( diseaseClass.getIRI().toString().contentEquals( CLO_DOID_DISEASE_IRI_STR )
				|| diseaseClass.getIRI().toString().contentEquals( CLO_EFO_DISEASE_IRI_STR ) ) {
			return disease;
		}
		
		// Accession
		disease.setAccession( this.parseAccessionFromIRI( diseaseClass.getIRI().toString() ) );
		
		// Name
		Iterator<OWLAnnotation> labelIterator = EntitySearcher.getAnnotationObjects( diseaseClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( RDF_LABEL_IRI_STR ) ) ).iterator();
		String diseaseLabel = labelIterator.next().getValue().asLiteral().get().getLiteral().toString();
		disease.setName( diseaseLabel );
		
		// Synonyms
		for ( String synonymIRIString : CLO_SYNONYMS_IRI_STR ) {
			Iterator<OWLAnnotation> synonymIterator = EntitySearcher.getAnnotationObjects( diseaseClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( synonymIRIString ) ) ).iterator();
			while ( synonymIterator.hasNext() ) disease.addSynonyms( synonymIterator.next().getValue().asLiteral().get().getLiteral().toString() );
		}
		
		// Database cross reference
		Iterator<OWLAnnotation> xRefIterator;
		for ( String xRefIriString : CLO_DB_XREF_IRI_STR ) {
			xRefIterator = EntitySearcher.getAnnotationObjects( diseaseClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( xRefIriString ) ) ).iterator();
			while ( xRefIterator.hasNext() ) {
				OWLAnnotationValue xRefValue = xRefIterator.next().getValue();
				String xRefString;
				if ( xRefValue instanceof IRI )
					xRefString = xRefValue.toString();
				else
					xRefString = xRefValue.asLiteral().get().getLiteral().toString();
				CrossReference xRef = this.parseCrossReference( xRefString );
				if ( xRef == null ) continue;
				if ( xRef.getAccession() != null )
					disease.addCrossReferences( xRef );
				else
					logger.warn( String.format( "Incorrect CLO cross reference for %s: %s", disease.getAccession(), xRefString ) );
			}
		}
		
		return disease;
	}
	
	public Disease getDiseaseFromAccession( String accession ) {
		if ( this.diseases.containsKey( accession ) ) {
			return this.diseases.get( accession );
		} else
			return null;
	}
	
	public Disease getDiseaseFromCrossReferenceAccession( String accession ) {
		if ( this.diseases.containsKey( accession ) )
			return this.diseases.get( accession );
		for ( Disease disease : this.diseases.values() ) {
			for ( CrossReference xRef : disease.getCrossReferences() ) {
				if ( xRef.getAccession().contentEquals( accession ) )
					return disease;
			}
		}
		return null;
	}
	
	public Boolean isSubClassOfRelation( Disease sourceDisease, Disease targetDisease ) {
		return this.isSubClassOfRelation( sourceDisease, targetDisease, 1 );
	}
	
	public Boolean isSubClassOfRelation( Disease sourceDisease, Disease targetDisease, int distance ) {
		if ( sourceDisease == null || targetDisease == null )
			return false;
		if ( !this.diseaseIriMap.containsKey( sourceDisease.getAccession() ) || !this.diseaseIriMap.containsKey( targetDisease.getAccession() ) )
			return false;
		OWLClass sourceClass = this.dataFactory.getOWLClass( this.diseaseIriMap.get( sourceDisease.getAccession() ) );
		OWLClass targetClass = this.dataFactory.getOWLClass( this.diseaseIriMap.get( targetDisease.getAccession() ) );
		return this.isSubClassOfRelation( sourceClass, targetClass );
	}
	
	public Boolean hasSameParent( Disease sourceDisease, Disease targetDisease ) {
		if ( sourceDisease == null || targetDisease == null )
			return false;
		OWLClass sourceClass = this.dataFactory.getOWLClass( this.diseaseIriMap.get( sourceDisease.getAccession() ) );
		OWLClass targetClass = this.dataFactory.getOWLClass( this.diseaseIriMap.get( targetDisease.getAccession() ) );
		OWLClass doidDiseaseClass = this.dataFactory.getOWLClass( IRI.create( CLO_EFO_DISEASE_IRI_STR ) );
		OWLClass efoDiseaseClass = this.dataFactory.getOWLClass( IRI.create( CLO_EFO_DISEASE_IRI_STR ) );
		return ( this.hasSameParent( sourceClass, targetClass, doidDiseaseClass ) 
				|| this.hasSameParent( sourceClass, targetClass, efoDiseaseClass ));
	}
	
	// Cell Types
	private HashMap<String, CellType> cellTypes = new HashMap<String, CellType>();
	private HashMap<String, IRI> cellTypeIriMap = new HashMap<String, IRI>();
	
	public void parseCellTypesFromOntology() {
		NodeSet<OWLClass> efoCellTypeNodeSet = this.reasoner.getSubClasses( this.dataFactory.getOWLClass( IRI.create( CLO_CELL_IRI_STR ) ), false );
		for ( OWLClass efoCellTypeClass : efoCellTypeNodeSet.getFlattened() ) {
			// Ignore OWL:Nothing
			if ( efoCellTypeClass.isOWLNothing() ) continue;
			CellType cellType = this.parseCellType( efoCellTypeClass );
			if ( cellType.getDatabase().equalsIgnoreCase( "CL" ) ) {
				this.cellTypes.put( cellType.getAccession(), cellType );
				this.cellTypeIriMap.put( cellType.getAccession(), efoCellTypeClass.getIRI() );
			}
		}
	}
	
	private CellType parseCellType( OWLClass cellTypeClass ) {
		CellType cellType = new CellType();
		
		if ( cellTypeClass.getIRI().toString().contentEquals( CLO_CELL_IRI_STR ) ) {
			return cellType;
		}
		
		// Accession
		cellType.setAccession( this.parseAccessionFromIRI( cellTypeClass.getIRI().toString() ) );
		
		// Name
		Iterator<OWLAnnotation> labelIterator = EntitySearcher.getAnnotationObjects( cellTypeClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( RDF_LABEL_IRI_STR ) ) ).iterator();
		String cellTypeLabel = labelIterator.next().getValue().asLiteral().get().getLiteral().toString();
		cellType.setName( cellTypeLabel );
		
		// Synonyms
		for ( String synonymIRIString : CLO_SYNONYMS_IRI_STR ) {
			Iterator<OWLAnnotation> synonymIterator = EntitySearcher.getAnnotationObjects( cellTypeClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( synonymIRIString ) ) ).iterator();
			while ( synonymIterator.hasNext() ) {
				cellType.addSynonyms( synonymIterator.next().getValue().asLiteral().get().getLiteral().toString() );
			}
		}
		
		// Database cross reference
		for ( String xRefIriString : CLO_DB_XREF_IRI_STR ) {
			Iterator<OWLAnnotation> xRefIterator;
			xRefIterator = EntitySearcher.getAnnotationObjects( cellTypeClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( xRefIriString ) ) ).iterator();
			while ( xRefIterator.hasNext() ) {
				OWLAnnotationValue xRefValue = xRefIterator.next().getValue();
				String xRefString;
				if ( xRefValue instanceof IRI )
					xRefString = xRefValue.toString();
				else
					xRefString = xRefValue.asLiteral().get().getLiteral().toString();
				CrossReference xRef = this.parseCrossReference( xRefString );
				if ( xRef == null ) continue;
				if ( xRef.getAccession() != null )
					cellType.addCrossReferences( xRef );
				else
					logger.warn( String.format( "Incorrect EFO cross reference for %s: %s", cellType.getAccession(), xRefString ) );
			}
		}
		
		return cellType;
	}
	
	public CellType getCellTypeFromAccession( String accession ) {
		if ( this.cellTypes.containsKey( accession ) ) {
			return this.cellTypes.get( accession );
		} else
			return null;
	}
	
	public CellType getCellTypeFromCrossReferenceAccession( String accession ) {
		if ( this.cellTypes.containsKey( accession ) )
			return this.cellTypes.get( accession );
		for ( CellType cellType : this.cellTypes.values() ) {
			for ( CrossReference xRef : cellType.getCrossReferences() ) {
				if ( xRef.getAccession().contentEquals( accession ) )
					return cellType;
			}
		}
		return null;
	}
	
	// Organism Parts
	private HashMap<String, OrganismPart> organismParts = new HashMap<String, OrganismPart>();
	private HashMap<String, IRI> organismPartIriMap = new HashMap<String, IRI>();
	
	public void parseOrganismPartsFromOntology() {
		NodeSet<OWLClass> efoOrganismPartNodeSet = this.reasoner.getSubClasses( this.dataFactory.getOWLClass( IRI.create( CLO_ORGANISM_PART_IRI_STR ) ), false );
		for ( OWLClass efoOrganismPartClass : efoOrganismPartNodeSet.getFlattened() ) {
			// Ignore OWL:Nothing
			if ( efoOrganismPartClass.isOWLNothing() ) continue;
			OrganismPart organismPart = this.parseOrganismPart( efoOrganismPartClass );
			this.organismParts.put( organismPart.getAccession(), organismPart );
			this.organismPartIriMap.put( organismPart.getAccession(), efoOrganismPartClass.getIRI() );
		}
	}
	
	private OrganismPart parseOrganismPart( OWLClass organismPartClass ) {
		OrganismPart organismPart = new OrganismPart();
		
		if ( organismPartClass.getIRI().toString().contentEquals( CLO_ORGANISM_PART_IRI_STR ) ) {
			return organismPart;
		}
		
		// Accession
		organismPart.setAccession( this.parseAccessionFromIRI( organismPartClass.getIRI().toString() ) );
		
		// Name
		Iterator<OWLAnnotation> labelIterator = EntitySearcher.getAnnotationObjects( organismPartClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( RDF_LABEL_IRI_STR ) ) ).iterator();
		String organismPartLabel = labelIterator.next().getValue().asLiteral().get().getLiteral().toString();
		organismPart.setName( organismPartLabel );
		
		// Synonyms
		for ( String synonymIRIString : CLO_SYNONYMS_IRI_STR ) {
			Iterator<OWLAnnotation> synonymIterator = EntitySearcher.getAnnotationObjects( organismPartClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( synonymIRIString ) ) ).iterator();
			while ( synonymIterator.hasNext() ) {
				organismPart.addSynonyms( synonymIterator.next().getValue().asLiteral().get().getLiteral().toString() );
			}
		}
		
		// Database cross reference
		for ( String xRefIriString : CLO_DB_XREF_IRI_STR ) {
			Iterator<OWLAnnotation> xRefIterator;
			xRefIterator = EntitySearcher.getAnnotationObjects( organismPartClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( xRefIriString ) ) ).iterator();
			while ( xRefIterator.hasNext() ) {
				OWLAnnotationValue xRefValue = xRefIterator.next().getValue();
				String xRefString;
				if ( xRefValue instanceof IRI )
					xRefString = xRefValue.toString();
				else
					xRefString = xRefValue.asLiteral().get().getLiteral().toString();
				CrossReference xRef = this.parseCrossReference( xRefString );
				if ( xRef == null ) continue;
				if ( xRef.getAccession() != null )
					organismPart.addCrossReferences( xRef );
				else
					logger.warn( String.format( "Incorrect EFO cross reference for %s: %s", organismPart.getAccession(), xRefString ) );
			}
		}
		
		return organismPart;
	}
	
	public OrganismPart getOrganismPartFromAccession( String accession ) {
		if ( this.organismParts.containsKey( accession ) ) {
			return this.organismParts.get( accession );
		} else
			return null;
	}
	
	public OrganismPart getOrganismPartFromCrossReferenceAccession( String accession ) {
		if ( this.organismParts.containsKey( accession ) )
			return this.organismParts.get( accession );
		for ( OrganismPart organismPart : this.organismParts.values() ) {
			for ( CrossReference xRef : organismPart.getCrossReferences() ) {
				if ( xRef.getAccession().contentEquals( accession ) )
					return organismPart;
			}
		}
		return null;
	}

	public OrganismPart getOrganismPartFromDiseaseAccession( String accession ) {
		if ( this.diseaseIriMap.containsKey( accession ) && this.diseaseOrganismPartMap.containsKey( accession ) ) {
			return this.organismParts.get( this.diseaseOrganismPartMap.get( accession ) );
		}
		return null;
	}
	
	public OrganismPart getOrganismPartFromDiseaseCrossReferenceAccession( String accession ) {
		Disease disease = this.getDiseaseFromCrossReferenceAccession( accession );
		if ( disease == null ) return null;
		return this.getOrganismPartFromDiseaseAccession( disease.getAccession() );
	}
	
	// Species
	private Species parseSpecies( OWLClass speciesClass ) {
		Species species = new Species();
		
		if ( speciesClass.getIRI().toString().contentEquals( CLO_ORGANISM_IRI_STR ) ) {
			return species;
		}
		
		// Accession
		species.setAccession( this.parseAccessionFromIRI( speciesClass.getIRI().toString() ) );
		
		// Name
		Iterator<OWLAnnotation> labelIterator = EntitySearcher.getAnnotationObjects( speciesClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( RDF_LABEL_IRI_STR ) ) ).iterator();
		String speciesLabel = labelIterator.next().getValue().asLiteral().get().getLiteral().toString();
		species.setName( speciesLabel );
		
		// Synonyms
		for ( String synonymIRIString : CLO_SYNONYMS_IRI_STR ) {
			Iterator<OWLAnnotation> synonymIterator = EntitySearcher.getAnnotationObjects( speciesClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( synonymIRIString ) ) ).iterator();
			while ( synonymIterator.hasNext() ) species.addSynonyms( synonymIterator.next().getValue().asLiteral().get().getLiteral().toString() );
		}
		
		// Database cross reference
		Iterator<OWLAnnotation> xRefIterator;
		for ( String xRefIriString : CLO_DB_XREF_IRI_STR ) {
			xRefIterator = EntitySearcher.getAnnotationObjects( speciesClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( xRefIriString ) ) ).iterator();
			while ( xRefIterator.hasNext() ) {
				OWLAnnotationValue xRefValue = xRefIterator.next().getValue();
				String xRefString;
				if ( xRefValue instanceof IRI )
					xRefString = xRefValue.toString();
				else
					xRefString = xRefValue.asLiteral().get().getLiteral().toString();
				CrossReference xRef = this.parseCrossReference( xRefString );
				if ( xRef == null ) continue;
				if ( xRef.getAccession() != null )
					species.addCrossReferences( xRef );
				else
					logger.warn( String.format( "Incorrect EFO cross reference for %s: %s", species.getAccession(), xRefString ) );
			}
		}
		
		return species;
	}
}

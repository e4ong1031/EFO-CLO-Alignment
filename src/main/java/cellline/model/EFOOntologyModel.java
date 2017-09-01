/**
 * @file EFOCellLines.java
 * @author Edison Ong
 * @since Jul 18, 2017
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
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.object.CellType;
import cellline.object.CrossReference;
import cellline.object.Disease;
import cellline.object.EFOCellLine;
import cellline.object.OrganismPart;
import cellline.object.Species;
import cellline.validation.AccessionValidator;

/**
 * 
 */
public class EFOOntologyModel extends OntologyModel {
	
	public static final String EFO_IRI_FORMAT = "http://www.ebi.ac.uk/efo/%s_%s";
	
	public static final String[] EFO_SYNONYMS_IRI_STR = {
			"http://www.ebi.ac.uk/efo/alternative_term",
			"http://www.geneontology.org/formats/oboInOwl#hasBroadSynonym",
			"http://www.geneontology.org/formats/oboInOwl#hasExactSynonym",
			"http://www.geneontology.org/formats/oboInOwl#hasNarrowSynonym",
			"http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym",
	};
	
	public static final String EFO_CELL_LINE_IRI_STR = "http://www.ebi.ac.uk/efo/EFO_0000322";
	
	public static final String EFO_DB_XREF_IRI_STR = "http://www.geneontology.org/formats/oboInOwl#hasDbXref";
	public static final String EFO_DEFINITION_CITATION_IRI_STR = "http://www.ebi.ac.uk/efo/definition_citation";
	
	public static final String EFO_DISEASE_IRI_STR = "http://www.ebi.ac.uk/efo/EFO_0000408";
	public static final String EFO_DISEASE_BEARER_OF_IRI_STR = "http://purl.org/obo/owl/OBO_REL#bearer_of";
	
	public static final String EFO_ORGANISM_PART_IRI_STR = "http://www.ebi.ac.uk/efo/EFO_0000635";
	
	public static final String EFO_ORGANISM_IRI_STR = "http://purl.obolibrary.org/obo/OBI_0100026";
	
	public static final String EFO_CELLTYPE_IRI_STR = "http://www.ebi.ac.uk/efo/EFO_0000324";
	
	public static final String EFO_DERIVES_FROM_IRI_STR = "http://www.obofoundry.org/ro/ro.owl#derives_from";
	public static final String EFO_HAS_DISEASE_LOCATION_IRI_STR = "http://www.ebi.ac.uk/efo/EFO_0000784";
	public static final String EFO_OCCURS_IN_IRI_STR = "http://purl.obolibrary.org/obo/BFO_0000066";
	
	static final Logger logger = LoggerFactory.getLogger( EFOOntologyModel.class );
	
	public EFOOntologyModel() {
		super();
	}
	
	public EFOOntologyModel( File efoFile ) {
		super( efoFile );
		this.parseCellTypesFromOntology();
		this.parseOrganismPartsFromOntology();
		this.parseDiseasesFromOntology();
		this.parseCellLinesFromOntology();
	}
	
	public EFOOntologyModel( IRI efoIri ) {
		super( efoIri );
		this.parseCellTypesFromOntology();
		this.parseOrganismPartsFromOntology();
		this.parseDiseasesFromOntology();
		this.parseCellLinesFromOntology();
	}
	
	@Override
	public CrossReference parseCrossReference( String xRefString ) {
		Pattern pattern;
		if ( new UrlValidator().isValid( xRefString ) ) {
			if ( xRefString.endsWith( ".jpg" ) || xRefString.endsWith( ".png" ) || xRefString.endsWith( ".svg" ) || xRefString.endsWith( ".gif" ) ) 
				return null;
			if ( xRefString.matches( "(?i:.*(wikipedia.org).*)") ) pattern = Pattern.compile( "(wikipedia).*[\\/#](?!.*[\\/#])(.*)", Pattern.CASE_INSENSITIVE );
			else if ( xRefString.matches( "(?i:.*(orpha.net).*)" ) ) pattern = Pattern.compile( "(ORDO)[\\\\/#](?!.*[\\\\/#])(?=Orphanet)?([^?=&%]*)" );
			else if ( xRefString.matches( "(?i:.*(neurolex.org).*)" ) ) pattern = Pattern.compile( "(neurolex).*[\\/#](?!.*[\\/#])(.*)", Pattern.CASE_INSENSITIVE );
			else pattern = Pattern.compile( "[\\/#](?!.*[\\/#])([a-zA-Z0-9]+)[^:_]*[:_]?([^?=&%]*)" );
		} else {
			if ( xRefString.matches( "(?i:.*OpenCyc.*)" ) ) return null;
			if ( xRefString.matches( "(?i:.*NIF_GrossAnatomy.*)" ) ) pattern = Pattern.compile( "([a-zA-Z0-9_]+)[^:]*[:]?[ ]?(.*)" );
			else pattern = Pattern.compile( "([a-zA-Z0-9]+)[^:_]*[:_]?[ ]?(.*)" );
		}
		
		Matcher matcher = pattern.matcher( xRefString );
		if ( matcher.find() ) {
			String source = matcher.group( 1 );
			String identifier = matcher.group( 2 );
			
			// Special handling: NCI Metathesaurus
			if ( source.startsWith( "NCI_Metathesaurus" ) ) {
				source = "NCIm";
				return new CrossReference( source, identifier );
			}
			
			// Special handling: MESH/MSH
			if ( source.startsWith( "MSH" ) ) {
				source = "MESH";
				return new CrossReference( source, identifier );
			}
			
			// Special handling: FBtc web link
			if ( source.startsWith( "FBtc" ) ) {
				identifier = new String ( source );
				source = "FBtc";
				return new CrossReference( source, identifier );
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
			
			// Special handling: ORDO_Orphanet
			if ( source.startsWith( "ORDO" ) && identifier.startsWith( "Orphanet_" ) ) {
				identifier = identifier.replace( "Orphanet_", "" );
				return new CrossReference( source, identifier );
			}
			
			// Special handling: neuroxlex
			if ( source.equalsIgnoreCase( "neurolex" ) ) {
				source = "Neurolex";
				identifier = identifier.replace( "Category:", "" ).replaceAll( "_", " " );
				return new CrossReference( source, identifier );
			}
			
			return new CrossReference( source, identifier );
		}
		return null;
	}
	
	// Cell Lines
	private HashMap<String, EFOCellLine> cellLines = new HashMap<String, EFOCellLine>();
	/**
	 * @return the cellLines
	 */
	public HashMap<String, EFOCellLine> getCellLines() {
		return cellLines;
	}
	
	private void parseCellLinesFromOntology() {
		NodeSet<OWLClass> efoCellLineNodeSet = this.reasoner.getSubClasses( this.dataFactory.getOWLClass( IRI.create( EFO_CELL_LINE_IRI_STR ) ), false );
		
		ArrayList<OWLClass> upperCellLineClasses = new ArrayList<OWLClass>(); //For Category
		
		for ( OWLClass efoCellLineClass : efoCellLineNodeSet.getFlattened() ) {
			// Ignore OWL:Nothing
			if ( efoCellLineClass.isOWLNothing() ) continue;
			Iterator<OWLAnnotation> labelIterator = EntitySearcher.getAnnotationObjects( efoCellLineClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( OntologyModel.RDF_LABEL_IRI_STR ) ) ).iterator();
			String efoCellLineLabel = labelIterator.next().getValue().asLiteral().get().getLiteral().toString();
			if ( efoCellLineLabel.endsWith( "cell line" ) ) {
				upperCellLineClasses.add( efoCellLineClass );
			} else {
				EFOCellLine cellLine = this.parseCellLine( efoCellLineClass );
				if ( new AccessionValidator().isValid( cellLine.getAccession() ) )
					this.cellLines.put( cellLine.getAccession(), cellLine );
				else
					logger.warn( String.format( "Incorrect EFO cell line accession for %s: %s", cellLine.getAccession(), cellLine.getIri() ) );
			}
		}
		logger.info( String.format( "Found %d cell lines in EFO ontology", this.cellLines.size() ) );
	}
	
	private EFOCellLine parseCellLine( OWLClass cellLineClass ) {
		EFOCellLine cellLine = new EFOCellLine();
		
		// IRI
		String iri = cellLineClass.getIRI().toString();
		cellLine.setIri( iri );
		
		// Accession
		cellLine.setAccession( this.parseAccessionFromIRI( iri ) );
		
		// Name
		Iterator<OWLAnnotation> labelIterator = EntitySearcher.getAnnotationObjects( cellLineClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( RDF_LABEL_IRI_STR ) ) ).iterator();
		String cellLineLabel = labelIterator.next().getValue().asLiteral().get().getLiteral().toString();
		cellLine.setName( cellLineLabel );
		
		// Synonyms
		for ( String synonymIRIString : EFO_SYNONYMS_IRI_STR ) {
			Iterator<OWLAnnotation> synonymIterator = EntitySearcher.getAnnotationObjects( cellLineClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( synonymIRIString ) ) ).iterator();
			while ( synonymIterator.hasNext() ) cellLine.addSynonyms( synonymIterator.next().getValue().asLiteral().get().getLiteral().toString() );
		}
		
		// Database cross reference
		Iterator<OWLAnnotation> xRefIterator;
		xRefIterator = EntitySearcher.getAnnotationObjects( cellLineClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( EFO_DB_XREF_IRI_STR ) ) ).iterator();
		while ( xRefIterator.hasNext() ) {
			String xRefString = xRefIterator.next().getValue().asLiteral().get().getLiteral().toString();
			CrossReference xRef = this.parseCrossReference( xRefString );
			if ( xRef == null ) continue;
			if ( xRef.getAccession() != null )
				cellLine.addCrossReferences( xRef );
			else
				logger.warn( String.format( "Incorrect EFO cross reference for %s: %s", cellLine.getAccession(), xRefString ) );
		}
		// Database cross reference from definition_citation
		Collection<OWLAnnotationProperty> definitionCitationCollection = EntitySearcher.getSubProperties( 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( EFO_DEFINITION_CITATION_IRI_STR ) ), this.ontology );
		for ( OWLAnnotationProperty definitionCitation : definitionCitationCollection ) {
			xRefIterator = EntitySearcher.getAnnotationObjects( cellLineClass.getIRI(), this.ontology, 
					definitionCitation ).iterator();
			while ( xRefIterator.hasNext() ) {
				String xRefString = xRefIterator.next().getValue().asLiteral().get().getLiteral().toString();
				CrossReference xRef = this.parseCrossReference( xRefString );
				if ( xRef == null ) continue;
				if ( xRef.getAccession() != null )
					cellLine.addCrossReferences( xRef );
				else
					logger.warn( String.format( "Incorrect EFO cross reference for %s: %s", cellLine.getAccession(), xRefString ) );
			}
		}
		
		Collection<OWLAxiom> axiomCollection = EntitySearcher.getReferencingAxioms( cellLineClass, this.ontology );
		
		// Disease
		OWLObjectProperty diseaseBearer = this.dataFactory.getOWLObjectProperty( IRI.create( EFO_DISEASE_BEARER_OF_IRI_STR ) );
		// Species
		OWLClass organismClass = this.dataFactory.getOWLClass( IRI.create( EFO_ORGANISM_IRI_STR ) );
		OWLObjectProperty derivesFrom = this.dataFactory.getOWLObjectProperty( IRI.create( EFO_DERIVES_FROM_IRI_STR ) );
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
					// Cell Type
					if ( this.cellTypes.containsKey( classAccession ) )
						cellLine.addCellType( this.cellTypes.get( classAccession ) );
				}
			}
		}
		
		return cellLine;
	}
	
	public EFOCellLine getCellLineFromAccession( String accession ) {
		if ( this.cellLines.containsKey( accession ) )
			return this.cellLines.get( accession );
		else
			return new EFOCellLine();
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
		OWLObjectProperty hasDiseaseLocation = this.dataFactory.getOWLObjectProperty( IRI.create( EFO_HAS_DISEASE_LOCATION_IRI_STR ) );
		OWLObjectProperty occurIn = this.dataFactory.getOWLObjectProperty( IRI.create( EFO_OCCURS_IN_IRI_STR ) );
		NodeSet<OWLClass> efoDiseaseNodeSet = this.reasoner.getSubClasses( this.dataFactory.getOWLClass( IRI.create( EFO_DISEASE_IRI_STR ) ), false );
		for ( OWLClass efoDiseaseClass : efoDiseaseNodeSet.getFlattened() ) {
			// Ignore OWL:Nothing
			if ( efoDiseaseClass.isOWLNothing() ) continue;
			Disease disease = this.parseDisease( efoDiseaseClass );
			
			Collection<OWLAxiom> axiomCollection = EntitySearcher.getReferencingAxioms( efoDiseaseClass, this.ontology );
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
			
			this.diseases.put( disease.getAccession(), disease );
			this.diseaseIriMap.put( disease.getAccession(), efoDiseaseClass.getIRI() );
		}
	}
	
	private Disease parseDisease( OWLClass diseaseClass ) {
		Disease disease = new Disease();
		
		if ( diseaseClass.getIRI().toString().contentEquals( EFO_DISEASE_IRI_STR ) ) {
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
		for ( String synonymIRIString : EFO_SYNONYMS_IRI_STR ) {
			Iterator<OWLAnnotation> synonymIterator = EntitySearcher.getAnnotationObjects( diseaseClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( synonymIRIString ) ) ).iterator();
			while ( synonymIterator.hasNext() ) disease.addSynonyms( synonymIterator.next().getValue().asLiteral().get().getLiteral().toString() );
		}
		
		// Database cross reference
		Iterator<OWLAnnotation> xRefIterator;
		xRefIterator = EntitySearcher.getAnnotationObjects( diseaseClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( EFO_DB_XREF_IRI_STR ) ) ).iterator();
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
				logger.warn( String.format( "Incorrect EFO cross reference for %s: %s", disease.getAccession(), xRefString ) );
		}
		// Database cross reference from definition_citation
		Collection<OWLAnnotationProperty> definitionCitationCollection = EntitySearcher.getSubProperties( 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( EFO_DEFINITION_CITATION_IRI_STR ) ), this.ontology );
		for ( OWLAnnotationProperty definitionCitation : definitionCitationCollection ) {
			xRefIterator = EntitySearcher.getAnnotationObjects( diseaseClass.getIRI(), this.ontology, 
					definitionCitation ).iterator();
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
					logger.warn( String.format( "Incorrect EFO cross reference for %s: %s", disease.getAccession(), xRefString ) );
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
	
	public Set<Disease> getDiseasesFromCrossReferenceAccession( String accession ) {
		Set<Disease> diseaseSet = new HashSet<Disease>();
		if ( this.diseases.containsKey( accession ) )
			diseaseSet.add( this.diseases.get( accession ) );
		for ( Disease disease : this.diseases.values() ) {
			for ( CrossReference xRef : disease.getCrossReferences() ) {
				if ( xRef.getAccession().contentEquals( accession ) )
					diseaseSet.add( disease );
			}
		}
		return diseaseSet;
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
		OWLClass diseaseClass = this.dataFactory.getOWLClass( IRI.create( EFO_DISEASE_IRI_STR ) );
		return this.hasSameParent( sourceClass, targetClass, diseaseClass );
	}
	
	// Cell Types
	private HashMap<String, CellType> cellTypes = new HashMap<String, CellType>();
	private HashMap<String, IRI> cellTypeIriMap = new HashMap<String, IRI>();
	
	public void parseCellTypesFromOntology() {
		NodeSet<OWLClass> efoCellTypeNodeSet = this.reasoner.getSubClasses( this.dataFactory.getOWLClass( IRI.create( EFO_CELLTYPE_IRI_STR ) ), false );
		for ( OWLClass efoCellTypeClass : efoCellTypeNodeSet.getFlattened() ) {
			// Ignore OWL:Nothing
			if ( efoCellTypeClass.isOWLNothing() ) continue;
			CellType cellType = this.parseCellType( efoCellTypeClass );
			this.cellTypes.put( cellType.getAccession(), cellType );
			this.cellTypeIriMap.put( cellType.getAccession(), efoCellTypeClass.getIRI() );
		}
	}
	
	private CellType parseCellType( OWLClass cellTypeClass ) {
		CellType cellType = new CellType();
		
		if ( cellTypeClass.getIRI().toString().contentEquals( EFO_CELLTYPE_IRI_STR ) ) {
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
		for ( String synonymIRIString : EFO_SYNONYMS_IRI_STR ) {
			Iterator<OWLAnnotation> synonymIterator = EntitySearcher.getAnnotationObjects( cellTypeClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( synonymIRIString ) ) ).iterator();
			while ( synonymIterator.hasNext() ) cellType.addSynonyms( synonymIterator.next().getValue().asLiteral().get().getLiteral().toString() );
		}
		
		// Database cross reference
		Iterator<OWLAnnotation> xRefIterator;
		xRefIterator = EntitySearcher.getAnnotationObjects( cellTypeClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( EFO_DB_XREF_IRI_STR ) ) ).iterator();
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
		// Database cross reference from definition_citation
		Collection<OWLAnnotationProperty> definitionCitationCollection = EntitySearcher.getSubProperties( 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( EFO_DEFINITION_CITATION_IRI_STR ) ), this.ontology );
		for ( OWLAnnotationProperty definitionCitation : definitionCitationCollection ) {
			xRefIterator = EntitySearcher.getAnnotationObjects( cellTypeClass.getIRI(), this.ontology, 
					definitionCitation ).iterator();
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
	
	public Set<CellType> getCellTypesFromCrossReferenceAccession( String accession ) {
		Set<CellType> cellTypeSet = new HashSet<CellType>();
		if ( this.cellTypes.containsKey( accession ) )
			cellTypeSet.add( this.cellTypes.get( accession ) );
		for ( CellType cellType : this.cellTypes.values() ) {
			for ( CrossReference xRef : cellType.getCrossReferences() ) {
				if ( xRef.getAccession().contentEquals( accession ) )
					cellTypeSet.add( cellType );
			}
		}
		return cellTypeSet;
	}
	
	// Organism Parts
	private HashMap<String, OrganismPart> organismParts = new HashMap<String, OrganismPart>();
	private HashMap<String, IRI> organismPartIriMap = new HashMap<String, IRI>();
	
	public void parseOrganismPartsFromOntology() {
		NodeSet<OWLClass> efoOrganismPartNodeSet = this.reasoner.getSubClasses( this.dataFactory.getOWLClass( IRI.create( EFO_ORGANISM_PART_IRI_STR ) ), false );
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
		
		if ( organismPartClass.getIRI().toString().contentEquals( EFO_ORGANISM_PART_IRI_STR ) ) {
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
		for ( String synonymIRIString : EFO_SYNONYMS_IRI_STR ) {
			Iterator<OWLAnnotation> synonymIterator = EntitySearcher.getAnnotationObjects( organismPartClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( synonymIRIString ) ) ).iterator();
			while ( synonymIterator.hasNext() ) {
				organismPart.addSynonyms( synonymIterator.next().getValue().asLiteral().get().getLiteral().toString() );
			}
		}
		
		// Database cross reference
		Iterator<OWLAnnotation> xRefIterator;
		xRefIterator = EntitySearcher.getAnnotationObjects( organismPartClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( EFO_DB_XREF_IRI_STR ) ) ).iterator();
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
		// Database cross reference from definition_citation
		Collection<OWLAnnotationProperty> definitionCitationCollection = EntitySearcher.getSubProperties( 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( EFO_DEFINITION_CITATION_IRI_STR ) ), this.ontology );
		for ( OWLAnnotationProperty definitionCitation : definitionCitationCollection ) {
			xRefIterator = EntitySearcher.getAnnotationObjects( organismPartClass.getIRI(), this.ontology, 
					definitionCitation ).iterator();
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
	
	/*
	public OrganismPart getOrganismPartFromDiseaseCrossReferenceAccession( String accession ) {
		Disease disease = this.getDiseaseFromCrossReferenceAccession( accession );
		if ( disease == null ) return null;
		return this.getOrganismPartFromDiseaseAccession( disease.getAccession() );
	}
	*/
	
	// Species
	private Species parseSpecies( OWLClass speciesClass ) {
		Species species = new Species();
		
		if ( speciesClass.getIRI().toString().contentEquals( EFO_ORGANISM_IRI_STR ) ) {
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
		for ( String synonymIRIString : EFO_SYNONYMS_IRI_STR ) {
			Iterator<OWLAnnotation> synonymIterator = EntitySearcher.getAnnotationObjects( speciesClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( synonymIRIString ) ) ).iterator();
			while ( synonymIterator.hasNext() ) species.addSynonyms( synonymIterator.next().getValue().asLiteral().get().getLiteral().toString() );
		}
		
		// Database cross reference
		Iterator<OWLAnnotation> xRefIterator;
		xRefIterator = EntitySearcher.getAnnotationObjects( speciesClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( EFO_DB_XREF_IRI_STR ) ) ).iterator();
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
		// Database cross reference from definition_citation
		Collection<OWLAnnotationProperty> definitionCitationCollection = EntitySearcher.getSubProperties( 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( EFO_DEFINITION_CITATION_IRI_STR ) ), this.ontology );
		for ( OWLAnnotationProperty definitionCitation : definitionCitationCollection ) {
			xRefIterator = EntitySearcher.getAnnotationObjects( speciesClass.getIRI(), this.ontology, 
					definitionCitation ).iterator();
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

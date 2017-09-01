/**
 * @file DiseaseOntologyModel.java
 * @author Edison Ong
 * @since Jul 27, 2017
 * @version 1.0
 * @comment 
 */
package cellline.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.obolibrary.oboformat.model.Xref;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;
import org.semanticweb.owlapi.util.OWLClassExpressionCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.object.CrossReference;
import cellline.object.Disease;
import cellline.object.OrganismPart;
import cellline.validation.AccessionValidator;

/**
 * 
 */
public class DiseaseOntologyModel extends OntologyModel {
	public final String DOID_OWL_IRI_STR = "http://purl.obolibrary.org/obo/doid.owl";
	public final String DOID_IRI_FORMAT = "http://purl.obolibrary.org/obo/%s_%s";
	public final String DOID_DISEASE_IRI_STR = "http://purl.obolibrary.org/obo/DOID_4";
	public final String DOID_ORGANISM_PART_IRI_STR = "http://purl.obolibrary.org/obo/UBERON_0001062";
	public final String DOID_DB_XREF_IRI_STR = "http://www.geneontology.org/formats/oboInOwl#hasDbXref";
	public final String DOID_LOCATED_IN_IRI_STR = "http://purl.obolibrary.org/obo/doid#located_in";
	public static final String[] DOID_SYNONYMS_IRI_STR = {
			"http://www.geneontology.org/formats/oboInOwl#hasExactSynonym",
			"http://www.geneontology.org/formats/oboInOwl#hasNarrowSynonym",
			"http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym",
	};
	
	public final String DISEASE_ORDO_IRI_FORMAT = "http://www.orpha.net/%s/Orphanet_%s";
	public final String DISEASE_UBERON_ONTOLOGY_IRI_STR = "http://purl.obolibrary.org/obo/doid/imports/uberon_import.owl";
	public final String DISEASE_ORGANISM_PART_LOCATED_IN_IRI_STR = "http://purl.obolibrary.org/obo/RO_0001025";
	public final String DISEASE_SPECIES_INHERES_IN_IRI_STR = "http://purl.obolibrary.org/obo/RO_0000052";
	public final String DISEASE_SPECIES_ONLY_IN_TAXON_IRI_STR = "http://purl.obolibrary.org/obo/RO_0002160";
	
	private Logger logger = LoggerFactory.getLogger( DiseaseOntologyModel.class );
	
	private String fileName;
	
	public DiseaseOntologyModel() {
		super();
		this.fileName = "doid";
		logger.info( "Loading disease ontology: " + this.fileName.toUpperCase() );
		InputStream doidFile = getClass().getClassLoader().getResourceAsStream( "disease/doid_merged.owl" );
		this.loadFromFile( doidFile );
		//this.LoadFromIri( IRI.create( DOID_OWL_IRI_STR ) );
		//TODO parse organism parts
		//this.parseOrganismPartsFromOntology();
		this.parseDiseasesFromOntology();
	}
	
	public DiseaseOntologyModel( File doidFile ) {
		super();
		this.fileName = "doid";
		logger.info( "Loading disease ontology: " + this.fileName.toUpperCase() );
		this.loadFromFile( doidFile );
		//TODO parse organism parts
		//this.parseOrganismPartsFromOntology();
		this.parseDiseasesFromOntology();
	}
	
	@Override
	public CrossReference parseCrossReference( String xRefString ) {
		Pattern pattern;
		if ( new UrlValidator().isValid( xRefString ) )
			pattern = Pattern.compile( "[\\/#](?!.*[\\/#])([a-zA-Z0-9]+)[^:_]*[:_]?([^?=&%]*)" );
		else
			pattern = Pattern.compile( "([^_:]*)[^:]*:[ ]?(.*)" );
		
		Matcher matcher = pattern.matcher( xRefString );
		if ( matcher.find() ) {
			String source = matcher.group( 1 );
			String identifier = matcher.group( 2 );
			if ( source.equalsIgnoreCase( "NCI" ) ) source = "NCIt";
			return new CrossReference( source, identifier );
		}
		return null;
	}
	
	// Diseases
	private HashMap<String, Disease> diseases = new HashMap<String, Disease>();
	private HashMap<String, IRI> diseaseIriMap = new HashMap<String, IRI>();
	private HashMap<String, String> diseaseOrganismPartMap = new HashMap<String, String>();
	
	private void parseDiseasesFromOntology() {
		OWLObjectProperty hasDiseaseLocation = this.dataFactory.getOWLObjectProperty( IRI.create( DOID_ORGANISM_PART_IRI_STR ) );
		OWLObjectProperty locatedIn = this.dataFactory.getOWLObjectProperty( IRI.create( DOID_LOCATED_IN_IRI_STR ) );
		NodeSet<OWLClass> diseaseNodeSet = this.reasoner.getSubClasses( this.dataFactory.getOWLClass( IRI.create( DOID_DISEASE_IRI_STR ) ), false );
		for ( OWLClass diseaseClass : diseaseNodeSet.getFlattened() ) {
			// Ignore OWL:Nothing
			if ( diseaseClass.isOWLNothing() ) continue;
			Disease disease = this.parseDisease( diseaseClass );
			
			/* TODO Add organism parts from the 
			Collection<OWLAxiom> axiomCollection = EntitySearcher.getReferencingAxioms( diseaseClass, this.ontology );
			for ( OWLAxiom axiom : axiomCollection ) {
				if ( axiom.containsEntityInSignature( hasDiseaseLocation ) || axiom.containsEntityInSignature( locatedIn ) ) {
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
			this.diseaseIriMap.put( disease.getAccession(), diseaseClass.getIRI() );
		}
	}
	
	private Disease parseDisease( OWLClass diseaseClass ) {
		Disease disease = new Disease();
		
		if ( diseaseClass.getIRI().toString().contentEquals( DOID_DISEASE_IRI_STR ) ) {
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
		for ( String synonymIRIString : DOID_SYNONYMS_IRI_STR ) {
			Iterator<OWLAnnotation> synonymIterator = EntitySearcher.getAnnotationObjects( diseaseClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( synonymIRIString ) ) ).iterator();
			while ( synonymIterator.hasNext() ) disease.addSynonyms( synonymIterator.next().getValue().asLiteral().get().getLiteral().toString() );
		}
		
		// Database cross reference
		Iterator<OWLAnnotation> xRefIterator;
		xRefIterator = EntitySearcher.getAnnotationObjects( diseaseClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( DOID_DB_XREF_IRI_STR ) ) ).iterator();
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
		
		return disease;
	}
	
	public Disease getDiseaseFromAccession( String accession ) {
		if ( this.diseases.containsKey( accession ) )
			return this.diseases.get( accession );
		else
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
		OWLClass diseaseClass = this.dataFactory.getOWLClass( IRI.create( DOID_DISEASE_IRI_STR ) );
		return this.hasSameParent( sourceClass, targetClass, diseaseClass );
	}
	
	// Organism Parts
	private HashMap<String, OrganismPart> organismParts = new HashMap<String, OrganismPart>();
	private HashMap<String, IRI> organismPartIriMap = new HashMap<String, IRI>();
	
	public void parseOrganismPartsFromOntology() {
		NodeSet<OWLClass> efoOrganismPartNodeSet = this.reasoner.getSubClasses( this.dataFactory.getOWLClass( IRI.create( DOID_ORGANISM_PART_IRI_STR ) ), false );
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
		
		if ( organismPartClass.getIRI().toString().contentEquals( DOID_ORGANISM_PART_IRI_STR ) ) {
			return organismPart;
		}
		
		// Accession
		organismPart.setAccession( this.parseAccessionFromIRI( organismPartClass.getIRI().toString() ) );
		
		// Name
		Iterator<OWLAnnotation> labelIterator = EntitySearcher.getAnnotationObjects( organismPartClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( RDF_LABEL_IRI_STR ) ) ).iterator();
		if ( labelIterator.hasNext() ) {
			String organismPartLabel = labelIterator.next().getValue().asLiteral().get().getLiteral().toString();
			organismPart.setName( organismPartLabel );
		}
		
		// Synonyms
		for ( String synonymIRIString : DOID_SYNONYMS_IRI_STR ) {
			Iterator<OWLAnnotation> synonymIterator = EntitySearcher.getAnnotationObjects( organismPartClass.getIRI(), this.ontology, 
					this.dataFactory.getOWLAnnotationProperty( IRI.create( synonymIRIString ) ) ).iterator();
			while ( synonymIterator.hasNext() ) {
				organismPart.addSynonyms( synonymIterator.next().getValue().asLiteral().get().getLiteral().toString() );
			}
		}
		
		// Database cross reference
		Iterator<OWLAnnotation> xRefIterator;
		xRefIterator = EntitySearcher.getAnnotationObjects( organismPartClass.getIRI(), this.ontology, 
				this.dataFactory.getOWLAnnotationProperty( IRI.create( DOID_DB_XREF_IRI_STR ) ) ).iterator();
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
}

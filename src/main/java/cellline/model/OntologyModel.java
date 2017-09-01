/**
 * @file OntologyModel.java
 * @author Edison Ong
 * @since Jul 19, 2017
 * @version 1.0
 * @comment 
 */
package cellline.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.UrlValidator;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.object.CrossReference;

/**
 * 
 */
public class OntologyModel {
	
	private static final Logger logger = LoggerFactory.getLogger( OntologyModel.class );
	
	public static final String RDF_LABEL_IRI_STR = "http://www.w3.org/2000/01/rdf-schema#label";
	
	protected OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	protected OWLOntology ontology;
	protected OWLDataFactory dataFactory = manager.getOWLDataFactory();
	protected StructuralReasoner reasoner;
	
	public void loadFromFile( File ontologyFile ) {
		try {
			logger.info( "Loading ontology from document: " + ontologyFile.getAbsolutePath() );
			this.ontology = this.manager.loadOntologyFromOntologyDocument( ontologyFile );
			this.reasoner = new StructuralReasoner( this.ontology, new SimpleConfiguration(), BufferingMode.NON_BUFFERING );
			this.reasoner.precomputeInferences();
			
		} catch ( OWLOntologyCreationException e ) {
			logger.error( "Fail to load ontology from document: " + ontologyFile.getAbsolutePath(), e );
			System.exit( -1 );
		}
		logger.info( "Loaded ontology");
	}
	
	public void loadFromFile( InputStream ontologyFile ) {
		try {
			logger.info( "Loading ontology from input stream" );
			this.ontology = this.manager.loadOntologyFromOntologyDocument( ontologyFile );
			this.reasoner = new StructuralReasoner( this.ontology, new SimpleConfiguration(), BufferingMode.NON_BUFFERING );
			this.reasoner.precomputeInferences();
			
		} catch ( OWLOntologyCreationException e ) {
			logger.error( "Fail to load ontology from document from input stream", e );
			System.exit( -1 );
		}
		logger.info( "Loaded ontology");
	}
	
	public void LoadFromIri( IRI ontologyIRI ) {
		try {
			logger.info( "Loading ontology from source IRI: " + ontologyIRI );
			this.ontology = this.manager.loadOntology( ontologyIRI );
			this.reasoner = new StructuralReasoner( this.ontology, new SimpleConfiguration(), BufferingMode.NON_BUFFERING );
			this.reasoner.precomputeInferences();
		} catch ( OWLOntologyCreationException e ) {
			logger.error( "Fail to load ontology from source IRI: " + ontologyIRI, e );
			System.exit( -1 );
		}
		logger.info( "Loaded Ontology");
	}
	
	public void computeInference() {
		OWLReasoner hermit = new ReasonerFactory().createReasoner( this.ontology );
		
		InferredOntologyGenerator generator = new InferredOntologyGenerator( hermit );
		generator.addGenerator( new InferredSubClassAxiomGenerator() );
		generator.addGenerator( new InferredClassAssertionAxiomGenerator() );
		OWLOntology inferred;
		try {
			inferred = this.manager.createOntology();
			generator.fillOntology( this.dataFactory, inferred );
			this.ontology = inferred;
		} catch (OWLOntologyCreationException e) {
			logger.warn( "Enable to create inferred ontology", e );
		}
	}
	
	public OntologyModel() {}
	
	public OntologyModel( File ontologyFile ) {
		this.loadFromFile( ontologyFile );
	}
	
	public OntologyModel( IRI ontologyIri ) {
		this.LoadFromIri( ontologyIri );
	}
	
	public String parseAccessionFromIRI( String iri ) {
		Pattern pattern = Pattern.compile( "[\\/#](?!.*[\\/#])([^?=&%]*)" );
		Matcher matcher = pattern.matcher( iri );
		while( matcher.find() ) return matcher.group( 1 );
		return iri;
	}
	
	public CrossReference parseCrossReference( String xRefString ) {
		Pattern pattern;
		if ( new UrlValidator().isValid( xRefString ) )
			pattern = Pattern.compile( "[\\/#](?!.*[\\/#])([a-zA-Z0-9]+)[^:_]*[:_]?([^?=&%]*)" );
		else
			pattern = Pattern.compile( "([a-zA-Z0-9]+)[^:_]*[:_]?[ ]?(.*)" );
		
		Matcher matcher = pattern.matcher( xRefString );
		if ( matcher.find() ) {
			String source = matcher.group( 1 );
			String identifier = matcher.group( 2 );
			return new CrossReference( source, identifier );
		}
		return null;
	}
	
	public Boolean isSubClassOfRelation( String sourceIri, String targetIri ) {
		return this.isSubClassOfRelation( sourceIri, targetIri, 1 );
	}
	
	public Boolean isSubClassOfRelation( String sourceIri, String targetIri, int distance ) {
		if ( sourceIri == null || targetIri == null )
			return false;
		OWLClass source = this.dataFactory.getOWLClass( IRI.create( sourceIri ) );
		OWLClass target = this.dataFactory.getOWLClass( IRI.create( targetIri ) );
		return this.isSubClassOfRelation( source, target, distance );
	}
	
	public Boolean isSubClassOfRelation( OWLClass sourceClass, OWLClass targetClass ) {
		return this.isSubClassOfRelation( sourceClass, targetClass, 1 );
	}
	
	public Boolean isSubClassOfRelation( OWLClass source, OWLClass target, int distance ) {
		if ( source == null || target == null )
			return false;
		while ( distance > 0 ) {
			NodeSet<OWLClass> superClassSet = this.reasoner.getSuperClasses( source, true );
			for ( OWLClass superClass : superClassSet.getFlattened() ) {
				if ( superClass.equals( target ) ) return true;
			}
			distance --;
		}
		return false;
	}
	
	public Boolean hasSameParent( String sourceIri, String targetIri ) {
		return this.hasSameParent( sourceIri, targetIri,null );
	}
	
	public Boolean hasSameParent( String sourceIri, String targetIri, String excludeIri ) {
		if ( sourceIri == null || targetIri == null )
			return false;
		OWLClass source = this.dataFactory.getOWLClass( IRI.create( sourceIri ) );
		OWLClass target = this.dataFactory.getOWLClass( IRI.create( targetIri ) );
		if ( excludeIri == null )
			return this.hasSameParent( source, target );
		else
			return this.hasSameParent( source, target, null );
	}
	
	public Boolean hasSameParent( OWLClass source, OWLClass target ) {
		return this.hasSameParent( source, target, null );
	}
	
	public Boolean hasSameParent( OWLClass source, OWLClass target, OWLClass exclude ) {
		if ( source == null || target == null )
			return false;
		Set<OWLClass> sourceParentSet = this.reasoner.getSuperClasses( source, true ).getFlattened();
		Set<OWLClass> targetParentSet = this.reasoner.getSuperClasses( target, true ).getFlattened();
		for ( OWLClass sourceParent : sourceParentSet ) {
			if ( sourceParent.isOWLNothing() || sourceParent.isOWLThing() ) continue;
			for ( OWLClass targetParent: targetParentSet ) {
				if ( targetParent.isOWLNothing() || targetParent.isOWLThing() ) continue;
				if ( sourceParent.equals( targetParent ) )
					if ( exclude != null ) {
						if ( !exclude.equals( sourceParent ) )
							return true;
					} else
						return true;
			}
		}
		return false;
	}
}

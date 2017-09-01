/**
 * @file SpeciesMatcher.java
 * @author Edison Ong
 * @since Jul 24, 2017
 * @version 1.0
 * @comment 
 */
package cellline.match;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.object.CrossReference;
import cellline.object.Species;

/**
 * 
 */
public class SpeciesMatcher {
	
	public final String NCBI_TAXONOMY_CELLULAR_ORGANISM_ID = "NCBITaxon_131567";
	
	static final Logger logger = LoggerFactory.getLogger( SpeciesMatcher.class );
	
	private HashMap<String, String> taxonomy = null;
	
	private Species source;
	/**
	 * @return the source
	 */
	public Species getSource() {
		return source;
	}
	/**
	 * @param source the source to set
	 */
	public void setSource(Species source) {
		this.source = source;
	}
	
	private Species target;
	/**
	 * @return the target
	 */
	public Species getTarget() {
		return target;
	}
	/**
	 * @param target the target to set
	 */
	public void setTarget(Species target) {
		this.target = target;
	}
	
	public SpeciesMatcher() {
		this.loadTaxonomy();
	}
	
	private void loadTaxonomy() {
		InputStream ncbiFile = getClass().getClassLoader().getResourceAsStream( "species/ncbi-taxonomy.json" );
		JSONParser jsonParser = new JSONParser();
		try {
			this.taxonomy = (JSONObject) jsonParser.parse( new InputStreamReader( ncbiFile ) );
			logger.trace( "Loaded NCBI Taxonomy from resource json file" );
		} catch (FileNotFoundException e) {
			logger.error( e.getMessage() );
			System.exit( -1 );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Boolean match() {
		if ( this.matchContent() ) return true;
			
		//if ( this.isParent( source, target ) || this.isParent( target, source ) ) flag = true;
		return false;
	}
	
	public synchronized Boolean matchContent() {
		if ( this.source == null || this.target == null ) return false;
		return this.source.equals( this.target );
	}
	
	public Boolean isParent( Species children, Species parent ) {
		return this.isParent( children.getAccession(), parent.getAccession() );
	}
	
	public Boolean isParent( String children, String parent ) {
		String current = children;
		while ( this.taxonomy.containsKey( current ) && !this.taxonomy.get( current ).contentEquals( NCBI_TAXONOMY_CELLULAR_ORGANISM_ID ) ) {
			if ( this.taxonomy.get( current ).contentEquals( parent ) ) return true;
			else current = this.taxonomy.get( current );
		}
		return false;
	}
}

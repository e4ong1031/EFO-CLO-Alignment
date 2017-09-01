/**
 * @file EFOValidator.java
 * @author Edison Ong
 * @since Jul 19, 2017
 * @version 1.0
 * @comment 
 */
package cellline.match;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.model.DiseaseOntologyModel;
import cellline.model.EFOOntologyModel;
import cellline.model.OntologyModel;
import cellline.object.CellLine;
import cellline.object.CrossReference;
import cellline.object.Disease;
import cellline.object.OrganismPart;
import cellline.object.Species;

/**
 * 
 */
public class CellLineMatcher {
	
	private CellLine source;
	/**
	 * @return the source
	 */
	public CellLine getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(CellLine source) {
		this.source = source;
	}
	
	private CellLine target;
	/**
	 * @return the target
	 */
	public CellLine getTarget() {
		return target;
	}

	/**
	 * @param target the target to set
	 */
	public void setTarget(CellLine target) {
		this.target = target;
	}
	
	public CellLineMatcher() {
	}
	
	public Boolean match() {
		if ( this.matchSynonym() || this.matchCrossReferences() )
			return true;
		else
			return false;
	}
	
	public Boolean matchIgnoreCase() {
		if ( this.matchSynonymIgnoreCase() || this.matchCrossReferences() )
			return true;
		else
			return false;
	}
	
	public Boolean matchNameExact() {
		if ( this.source.getName() == null || this.target.getName() == null )
			return false;
		else
			return this.source.getName().contentEquals( this.target.getName() );
	}
	
	public Boolean matchNameIgnoreCase() {
		if ( this.source.getName() == null || this.target.getName() == null )
			return false;
		else
			return this.source.getName().equalsIgnoreCase( this.target.getName() );
	}
	
	public int getNameEditDistance() {
		return StringUtils.getLevenshteinDistance( this.source.getName(), this.target.getName() );
	}
	
	public int getNameEditDistanceIgnoreCase() {
		return StringUtils.getLevenshteinDistance( this.source.getName().toUpperCase(), this.target.getName().toUpperCase() );
	}
	
	public int getNameFuzzyDistance() {
		return StringUtils.getFuzzyDistance( this.source.getName(), this.target.getName(), Locale.ENGLISH );
	}
	
	public Boolean matchAccession() {
		if ( this.source.getAccession() == null || this.target.getAccession() == null )
			return false;
		else
			return this.source.getAccession().contentEquals( this.target.getAccession() );
	}
	
	public synchronized Boolean matchSynonymIgnoreCase() {
		Set<String> sourceSynonyms = new HashSet<String>( this.source.getSynonyms() );
		sourceSynonyms.add( this.source.getName() );
		Set<String> targetSynonyms = new HashSet<String>( this.target.getSynonyms() );
		targetSynonyms.add( this.target.getName() );
		for ( String targetSynonym : targetSynonyms ) {
			for ( String sourceSynonym : sourceSynonyms ) {
				if ( sourceSynonym.equalsIgnoreCase( targetSynonym ) )
					return true;
			}
		}
		return false;
	}
	
	public synchronized Boolean matchSynonym() {
		Set<String> sourceSynonyms = new HashSet<String>( this.source.getSynonyms() );
		sourceSynonyms.add( this.source.getName() );
		Set<String> targetSynonyms = new HashSet<String>( this.target.getSynonyms() );
		targetSynonyms.add( this.target.getName() );
		for ( String synonym : targetSynonyms ) {
			if ( sourceSynonyms.contains( synonym ) )
				return true;
		}
		return false;
	}
	
	public synchronized int getShortestSynonymEditDistance() {
		int distance = Integer.MAX_VALUE;
		Set<String> targetSynonyms = new HashSet<String>( this.target.getSynonyms() );
		if ( this.target.getName() != null )
			targetSynonyms.add( this.target.getName() );
		Set<String> sourceSynonyms = new HashSet<String>( this.source.getSynonyms() );
		if ( this.source.getName() != null )
			sourceSynonyms.add( this.source.getName() );
		sourceSynonyms.add( this.source.getName() );
		for ( String targetSynonym : targetSynonyms ) {
			if ( targetSynonym == null )
				continue;
			for ( String sourceSynonym : sourceSynonyms ) {
				if ( sourceSynonym == null )
					continue;
				int current = StringUtils.getLevenshteinDistance( sourceSynonym, targetSynonym );
				if ( current < distance )
					distance = current;
			}
		}
		return distance;
	}
	
	public synchronized int getShortestSynonymEditDistanceIgnoreCase() {
		int distance = Integer.MAX_VALUE;
		Set<String> targetSynonyms = new HashSet<String>( this.target.getSynonyms() );
		if ( this.target.getName() != null )
			targetSynonyms.add( this.target.getName() );
		Set<String> sourceSynonyms = new HashSet<String>( this.source.getSynonyms() );
		if ( this.source.getName() != null )
			sourceSynonyms.add( this.source.getName() );
		sourceSynonyms.add( this.source.getName() );
		for ( String targetSynonym : targetSynonyms ) {
			if ( targetSynonym == null )
				continue;
			for ( String sourceSynonym : sourceSynonyms ) {
				if ( sourceSynonym == null )
					continue;
				int current = StringUtils.getLevenshteinDistance( sourceSynonym.toUpperCase(), targetSynonym.toUpperCase() );
				if ( current < distance )
					distance = current;
			}
		}
		return distance;
	}
	
	public synchronized Boolean matchCrossReferences() {
		Set<CrossReference> targetXRefSet = new HashSet<CrossReference>( this.target.getCrossReferences() );
		if ( this.target.getAccession() != null )
			targetXRefSet.add( new CrossReference( this.target.getAccession() ) );
		Set<CrossReference> sourceXRefSet = new HashSet<CrossReference>( this.source.getCrossReferences() );
		if ( this.source.getAccession() != null )
			sourceXRefSet.add( new CrossReference( this.source.getAccession() ) );
		for ( CrossReference targetXRef : targetXRefSet ) {
			for ( CrossReference sourceXRef : sourceXRefSet ) {
				if ( sourceXRef != null && targetXRef != null )
					if ( sourceXRef.equals( targetXRef ) ) return true;
			}
		}
		return false;
	}
}

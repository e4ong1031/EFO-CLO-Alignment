/**
 * @file DiseaseMatcher.java
 * @author Edison Ong
 * @since Aug 3, 2017
 * @version 1.0
 * @comment 
 */
package cellline.match;

import cellline.model.DiseaseOntologyModel;
import cellline.model.EFOOntologyModel;
import cellline.object.Disease;

/**
 * 
 */
public class DiseaseMatcher {

	private Disease source;
	/**
	 * @return the source
	 */
	public Disease getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(Disease source) {
		this.source = source;
	}
	
	private Disease target;
	/**
	 * @return the target
	 */
	public Disease getTarget() {
		return target;
	}

	/**
	 * @param target the target to set
	 */
	public void setTarget(Disease target) {
		this.target = target;
	}

	private DiseaseOntologyModel doidModel;
	private EFOOntologyModel efoModel;
	
	public DiseaseMatcher() {
		this.doidModel = new DiseaseOntologyModel();
		this.efoModel = new EFOOntologyModel();
	}
	
	public DiseaseMatcher( DiseaseOntologyModel doidModel ) {
		this.doidModel = doidModel;
		this.efoModel = new EFOOntologyModel();
	}
	
	public DiseaseMatcher( EFOOntologyModel efoModel ) {
		this.doidModel = new DiseaseOntologyModel();
		this.efoModel = efoModel;
	}
	
	public DiseaseMatcher( DiseaseOntologyModel doidModel, EFOOntologyModel efoModel ) {
		this.doidModel = doidModel;
		this.efoModel = efoModel;
	}
	
	public Boolean match() {
		if ( this.source == null || this.target == null ) return false;
		
		if ( this.matchContent() ) return true;
		if ( this.matchSemantic() ) return true;
		
		return false;
	}
	
	public synchronized Boolean matchContent() {
		if ( this.source == null || this.target == null ) return false;
		return this.source.equals( this.target );
	}
	
	public synchronized Boolean matchSemantic() {
		if ( this.source == null || this.target == null ) return false;
		if ( this.matchContent() ) return true;
		for ( Disease sourceDOID : this.doidModel.getDiseasesFromCrossReferenceAccession( source.getAccession() ) )
			for ( Disease targetDOID : this.doidModel.getDiseasesFromCrossReferenceAccession( target.getAccession() ) )
				if ( doidModel.isSubClassOfRelation( sourceDOID, targetDOID )
						|| doidModel.isSubClassOfRelation( targetDOID, sourceDOID ) )
					return true;
		for ( Disease sourceEFO : this.efoModel.getDiseasesFromCrossReferenceAccession( source.getAccession() ) )
			for ( Disease targetEFO : this.efoModel.getDiseasesFromCrossReferenceAccession( target.getAccession() ) )
				if ( efoModel.isSubClassOfRelation( sourceEFO, targetEFO )
						|| efoModel.isSubClassOfRelation( targetEFO, sourceEFO ) )
					return true;
		//TODO
		/*
		if ( doidModel.hasSameParent( source, target )
			|| doidModel.hasSameParent( target, source ) 
			|| efoModel.hasSameParent( source, target )
			|| efoModel.hasSameParent( target, source ) ) {
				return true;
		}
		*/
		return false;
	}
}

/**
 * @file OrganismPartMatcher.java
 * @author Edison Ong
 * @since Jul 28, 2017
 * @version 1.0
 * @comment 
 */
package cellline.match;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.object.CrossReference;
import cellline.object.OrganismPart;

/**
 * 
 */
public class OrganismPartMatcher {
	
	private ReentrantLock lock = new ReentrantLock();
	
	static final Logger logger = LoggerFactory.getLogger( OrganismPartMatcher.class );
	
	private OrganismPart source;
	/**
	 * @return the source
	 */
	public OrganismPart getSource() {
		return source;
	}
	/**
	 * @param source the source to set
	 */
	public void setSource(OrganismPart source) {
		this.source = source;
	}
	
	private OrganismPart target;
	/**
	 * @return the target
	 */
	public OrganismPart getTarget() {
		return target;
	}
	/**
	 * @param target the target to set
	 */
	public void setTarget(OrganismPart target) {
		this.target = target;
	}
	
	public OrganismPartMatcher() {}
	
	public Boolean match() {
		return matchContent();
	}
	
	public synchronized Boolean matchContent() {
		if ( this.source == null || this.target == null ) return false;
		return this.source.equals( this.target );
	}
}

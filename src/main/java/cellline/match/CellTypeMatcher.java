/**
 * @file CellTypeMatcher.java
 * @author Edison Ong
 * @since Aug 15, 2017
 * @version 1.0
 * @comment 
 */
package cellline.match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cellline.object.CellType;

/**
 * 
 */
public class CellTypeMatcher {
	
	static final Logger logger = LoggerFactory.getLogger( CellTypeMatcher.class );
	
	private CellType source;
	/**
	 * @return the source
	 */
	public CellType getSource() {
		return source;
	}
	/**
	 * @param source the source to set
	 */
	public void setSource(CellType source) {
		this.source = source;
	}
	
	private CellType target;
	/**
	 * @return the target
	 */
	public CellType getTarget() {
		return target;
	}
	/**
	 * @param target the target to set
	 */
	public void setTarget(CellType target) {
		this.target = target;
	}
	
	public CellTypeMatcher() {}
	
	public Boolean match() {
		return matchContent();
	}
	
	public synchronized Boolean matchContent() {
		if ( this.source == null || this.target == null ) return false;
		return this.source.equals( this.target );
	}
}

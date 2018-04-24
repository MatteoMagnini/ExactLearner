package org.zhaowl.tree;

import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;

public class ObjectPropertyHierarchyT extends AbstractH<OWLObjectProperty> {
	private static final OWLObjectProperty OWL_TOP_OBJECT_PROPERTY = new OWLObjectPropertyImpl(
			OWLRDFVocabulary.OWL_TOP_OBJECT_PROPERTY.getIRI());
	private static final OWLObjectProperty OWL_BOTTOM_OBJECT_PROPERTY = new OWLObjectPropertyImpl(
			OWLRDFVocabulary.OWL_BOTTOM_OBJECT_PROPERTY.getIRI());

	public ObjectPropertyHierarchyT(
			SortedMap<OWLObjectProperty, SortedSet<OWLObjectProperty>> roleHierarchyUp,
			SortedMap<OWLObjectProperty, SortedSet<OWLObjectProperty>> roleHierarchyDown) {
		super(roleHierarchyUp, roleHierarchyDown);
	}
	
 
	
	public boolean isSubpropertyOf(OWLObjectProperty subProperty, OWLObjectProperty superProperty) {
		return isChildOf(subProperty, superProperty);
	}	

	/**
	 * @return The most general roles.
	 */
    private SortedSet<OWLObjectProperty> getMostGeneralRoles() {
		return getMostGeneralEntities();
	}

 

	/* (non-Javadoc)
	 * @see org.dllearner.core.owl.AbstractHierarchy#getTopConcept()
	 */
	public OWLObjectProperty getTopConcept() {
		return OWL_TOP_OBJECT_PROPERTY;
	}

	/* (non-Javadoc)
	 * @see org.dllearner.core.owl.AbstractHierarchy#getBottomConcept()
	 */
	public OWLObjectProperty getBottomConcept() {
		return OWL_BOTTOM_OBJECT_PROPERTY;
	}
	
	/* (non-Javadoc)
	 * @see org.dllearner.core.owl.AbstractHierarchy#toString(java.util.SortedMap, org.semanticweb.owlapi.model.OWLObject, int)
	 */
	String toString(SortedMap<OWLObjectProperty, SortedSet<OWLObjectProperty>> hierarchy,
					OWLObjectProperty prop, int depth) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < depth; i++)
			str.append("  ");
		str.append(prop.toString()).append("\n");
		Set<OWLObjectProperty> tmp;
		if(prop.isTopEntity()) {
			tmp = getMostGeneralRoles();
		} else {
			tmp  = hierarchy.get(prop);
		}
		
		if (tmp != null) {
			for (OWLObjectProperty c : tmp)
				str.append(toString(hierarchy, c, depth + 1));
		}
		return str.toString();
	}
	
 

	public String getWarning() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasWarning() {
		// TODO Auto-generated method stub
		return false;
	}



}

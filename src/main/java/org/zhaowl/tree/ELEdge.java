package org.zhaowl.tree;
 
import org.semanticweb.owlapi.model.OWLProperty;

public class ELEdge {
	public OWLProperty label;
	public String strLabel;
	public ELNode node;

	/**
	 * Constructs an edge given a label and an EL OWLClassExpression tree.
	 * @param label The label of this edge.
	 * @param tree The tree the edge points to (edges are directed).
	 */
	public ELEdge(OWLProperty label, ELNode tree) {
		this.label = label;
		this.node = tree;
		this.strLabel = toMan(label.toString());
	}
	
	/**
	 * @param label the label to set
	 */
	public String toMan(String str)
	{
		String modStr = "";
		modStr = str.substring(str.indexOf("#") + 1);
		modStr = modStr.substring(0, modStr.length() - 1);
		return modStr;
	}
	public void setLabel(OWLProperty label) {
		this.label = label;

		this.strLabel = label.toString();
	}

	/**
	 * @return The label of this edge.
	 */
	public OWLProperty getLabel() {
		return label;
	}

	/**
	 * @return The EL OWLClassExpression tree 
	 */
	public ELNode getNode() {
		return node;
	}
	
	public boolean isObjectProperty(){
		return label.isOWLObjectProperty();
	}
	
	@Override
	public String toString() {
		return "--" + label + "--> " + node.toDescriptionString(); 
	}
}

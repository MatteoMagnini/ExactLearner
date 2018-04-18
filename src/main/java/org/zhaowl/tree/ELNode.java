package org.zhaowl.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLProperty;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class ELNode {
	public ELTree tree;
	
	public TreeSet<OWLClass> label = new TreeSet<OWLClass>();
	
	public List<ELEdge> edges = new LinkedList<ELEdge>();
	public int level; 
	public Set<ELNode> in = new HashSet<ELNode>();
	public Set<ELNode> inSC1 = new HashSet<ELNode>();
	public Set<ELNode> inSC2 = new HashSet<ELNode>();
	public Set<ELNode> out = new HashSet<ELNode>();
	public Set<ELNode> outSC1 = new HashSet<ELNode>();
	public Set<ELNode> outSC2 = new HashSet<ELNode>();
	// parent node in the tree;
	// null indicates that this node is a root node
	public ELNode parent = null;
	public boolean isClassNode;
	public OWLDataRange dataRange;
	public OWLDataFactory df = new OWLDataFactoryImpl();
	
	
	
	public ELNode() {
		
	}
	public ELNode(OWLDataFactory daf) {
		this.df = daf;
	}
	public ELNode(ELTree tree) {
		this(tree, new TreeSet<OWLClass>());
	} 
	
	/**
	 * Constructs an EL OWLClassExpression tree given its root label.
	 * @param label Label of the root node.
	 */
	public ELNode(ELTree tree, TreeSet<OWLClass> label) {
		this.label = label;
		this.edges = new LinkedList<ELEdge>();
		this.tree = tree;
		level = 1;
		parent = null;
		// this is the root node of the overall tree
		tree.rootNode = this;
		tree.addNodeToLevel(this, level);
		tree.size += label.size();
		
		isClassNode = true;
	}
	
	/**
	 * Constructs an EL tree node given a tree and the data range.
	 * @param tree the description tree
	 * @param dataRange the data range
	 */
	public ELNode(ELTree tree, OWLDataRange dataRange) {
		this.dataRange = dataRange;
		this.edges = new LinkedList<ELEdge>();
		this.tree = tree;
		level = 1;
		parent = null;
		// this is the root node of the overall tree
		tree.rootNode = this;
		tree.addNodeToLevel(this, level);
		tree.size += label.size();
		
		isClassNode = false;
	}
	 
	public ELNode(ELNode parentNode, OWLDataProperty parentProperty, OWLDataRange dataRange) {
		this.dataRange = dataRange; 
		
		this.edges = new LinkedList<ELEdge>();
		parent = parentNode;
		// the reference tree is the same as for the parent tree
		tree = parentNode.tree;
		// level increases by 1
		level = parentNode.level + 1;
		// we add an edge from the parent to this node
		ELEdge edge = new ELEdge(parentProperty, this);
		parent.edges.add(edge);
		// we need to update the set of nodes on a particular level
		tree.addNodeToLevel(this, level);		
		 
		Set<ELNode> update = new HashSet<ELNode>();
		
		// loop over all nodes on the same level, which are not in the in set
		Set<ELNode> nodes = tree.getNodesOnLevel(level);
		for(ELNode w : nodes) {
			// to save space, we do not add reflexive relations
			if(w != this) {
				// (w,v') is automatically added
				tree.extendSimulation(w, this);
				
				// check conditions for (v',w)
				boolean sc1 = false, sc2 = false;
				
				if(w.label.size() == 0) {
					tree.extendSimulationSC1(this, w);
					sc1 = true;
				}
				
				if(w.edges.size() == 0) {
					tree.extendSimulationSC2(this, w);
					sc2 = true;
				}
				
				if(sc1 && sc2) {
					tree.extendSimulationSC12(this, w);
				}	
				
				update.add(w.parent);
			}
		}
		update.add(this.parent);  
		tree.updateSimulation(update); 
		
		
		// 1 for the edge (labels are already taken care of by extendLabel)
		tree.size += 1;
		
		isClassNode = false;
	}
	public String getNodeExpression()
	{
		if(this.isRoot())
			return this.tree.toDescriptionString();
		List<ELEdge> edge = new ArrayList<ELEdge>(this.parent.edges);
		for(ELEdge edg : edge)
		{
			if(edg.node.equals(this))
			{
				return edg.strLabel + " some (" + getLabels(this.label) + ")";
			}
		}
		return "FAIL";
	}
	public String getLabels(TreeSet<OWLClass> labs)
	{
		if(labs.size() < 1)
			return "";
		String str = "";
		for(OWLClass cl : labs)
		{
			str += cl.toString().substring(cl.toString().indexOf("#") + 1);
			str = str.substring(0, str.length() - 1) + " and ";
		}
		str = str.substring(0, str.length() - 4);
		return str;
	}
	public ELNode(ELNode parentNode, OWLProperty parentProperty, Set<OWLClass> label) {
//		this.label = label;
		// we first need to add the edge and update the simulation and then add
		// all classes iteratively to the label (each time updating the simulation again)
		this.edges = new LinkedList<ELEdge>();
		parent = parentNode;
		// the reference tree is the same as for the parent tree
		tree = parentNode.tree;
		// level increases by 1
		level = parentNode.level + 1;
		// we add an edge from the parent to this node
		ELEdge edge = new ELEdge(parentProperty, this);
		parent.edges.add(edge);
		// we need to update the set of nodes on a particular level
		tree.addNodeToLevel(this, level);		
		
		// simulation update
		// Monitor mon = MonitorFactory.start("simulation update");
		// the nodes, which need to be updated
		Set<ELNode> update = new HashSet<ELNode>();
		
		// loop over all nodes on the same level, which are not in the in set
		Set<ELNode> nodes = tree.getNodesOnLevel(level);
		for(ELNode w : nodes) {
			// to save space, we do not add reflexive relations
			if(w != this) {
				// (w,v') is automatically added
				tree.extendSimulation(w, this);
				
				// check conditions for (v',w)
				boolean sc1 = false, sc2 = false;
				
				if(w.label.size() == 0) {
					tree.extendSimulationSC1(this, w);
					sc1 = true;
				}
				
				if(w.edges.size() == 0) {
					tree.extendSimulationSC2(this, w);
					sc2 = true;
				}
				
				if(sc1 && sc2) {
					tree.extendSimulationSC12(this, w);
				}	
				
				update.add(w.parent);
			}
		}
		update.add(this.parent); 
		
		// apply updates recursively top-down
		tree.updateSimulation(update);
//		mon.stop();
		
		// add all classes in label
		for(OWLClass nc : label) {
			extendLabel(nc);
		}
		
		// 1 for the edge (labels are already taken care of by extendLabel)
		tree.size += 1;
		
		isClassNode = true;
	}
	
	public ELNode getRoot() {
		ELNode root = this;
		while(root.parent != null) {
			root = parent;
		}
		return root;
	}
	public boolean isRoot() {
		return parent == null;
	}
	public int computeLevel() {
		ELNode root = this;
		int level = 0;
		while(root.parent != null) {
			root = parent;
			level++;
		}
		return level;		
	}
	public int[] getCurrentPosition() {
		int[] position = new int[level-1];
		ELNode root = this;
		while(root.parent != null) {
			position[root.level-2] = root.getChildNumber();
			root = root.parent;	
		}
		return position;
	}
	public int getChildNumber() {
		int count = 0;
		for(ELEdge edge : parent.edges) {
			if(edge.getNode() == this) {
				return count;
			}
			count++;
		}
		throw new RuntimeException("Inconsistent tree. Child tree not reachable from parent.");
	}
	public void replaceInLabel(OWLClass oldClass, OWLClass newClass) {
		label.remove(oldClass);
		label.add(newClass);
		labelSimulationUpdate();
	}
	
	
	
	/**
	 * Adds an entry to the node label.
	 * @param newClass Class to add to label.
	 */
	public void extendLabel(OWLClass newClass) {
		label.add(newClass);
		labelSimulationUpdate();
		tree.size += 1;
//		System.out.println(tree);
//		System.out.println(tree.size);
	}
	public void labelSimulationUpdate() { 
		Set<ELNode> update = new HashSet<ELNode>();
		
		Set<ELNode> tmp = tree.getNodesOnLevel(level);
		for(ELNode w : tmp) {
			if(w != this) {
				// SC1(v,w) can only change from false to true
				if(!inSC1.contains(w) && tree.checkSC1(this, w)) {
					tree.extendSimulationSC1(this, w);
					if(inSC2.contains(w)) {
						tree.extendSimulationSC12(this, w);		
					}
					update.add(w.getParent());
				}
				// SC1(w,v) can only change from true to false
				if(outSC1.contains(w) && !tree.checkSC1(w, this)) {
					tree.shrinkSimulationSC1(w, this);
					if(outSC2.contains(w)) {
						tree.shrinkSimulationSC12(w, this);		
					}
					update.add(w.getParent());
				}
			}
		}
		if(parent != null) {
			update.add(parent);
		} 
		// apply updates recursively top-down
		tree.updateSimulation(update);	
//		mon.stop();
	}
	public void refineEdge(int edgeNumber, OWLProperty op) {
		edges.get(edgeNumber).setLabel(op); 
		Set<ELNode> update = new HashSet<ELNode>();
		update.add(this); 
		tree.updateSimulation(update);	
//		mon.stop();
	}
	public NavigableSet<OWLClass> getLabel() {
		return label;
	}
	public List<ELEdge> getEdges() {
		return edges;
	}
	public int getLevel() {
		return level;
	}public String toString() {
		return toString(0);
	}
	
	private String toString(int indent) {
		String indentString = "";
		for(int i=0; i<indent; i++)
			indentString += "  ";
		
		String str = indentString + label.toString() + "\n";
		for(ELEdge edge : edges) {
			str += indentString + "-- " + edge.getLabel() + " -->\n";
			str += edge.getNode().toString(indent + 2);
		}
		return str;
	}
	public boolean isClassNode() {
		return isClassNode;
	}
	public String toDescriptionString() {
		String str = "";
		if(isClassNode()){
			if(label.isEmpty()) {
				str = "";
			} else {
				Iterator<OWLClass> it = label.iterator();
				while(it.hasNext()) {
					OWLClass nc = it.next();
					if(it.hasNext()) {
						str += toMan(nc.toString()) + " and ";
					} else {
						str += toMan(nc.toString());
					}
				}
			}
		} else {
			str += dataRange;
		}
		
		int current = 1;
		for(ELEdge edge : edges) {
			if(current> 1) {
				str += " and " + toMan(edge.getLabel().toString()) + " some (";
				str += toMan(edge.getNode().toDescriptionString()) + ")";
				continue;
			} 
			if(!label.isEmpty()) {
				str += " and " + toMan(edge.getLabel().toString()) + " some (";
				str += toMan(edge.getNode().toDescriptionString()) + ")";
			}else {
				str += "  " + toMan(edge.getLabel().toString()) + " some (";
				str += toMan(edge.getNode().toDescriptionString()) + ") ";
			}
			
			current++;
		}
		return str;		
	}
	public String toMan(String str)
	{
		String mod = "";
		mod = str.substring(str.indexOf("#")+1); 
		mod = mod.replaceAll(">", "");
		return mod;
	}
	
	private String toDescriptionString(Set<ELNode> nodes) {
		String str = "";
		// comma separated list of descriptions
		for(ELNode node : nodes) {
			str += node.toDescriptionString() + ",";
		}
		// remove last comma
		if(str.length() > 0) {
			str = str.substring(0, str.length()-1);
		}
		return str;
	}
	
	public String toSimulationString() {
		String str = "";
		str += "in: " + toDescriptionString(in) + "\n";
		str += "inSC1: " + toDescriptionString(inSC1) + "\n";
		str += "inSC2: " + toDescriptionString(inSC2) + "\n";
		str += "out: " + toDescriptionString(out) + "\n";
		str += "outSC1: " + toDescriptionString(outSC1) + "\n";
		str += "outSC2: " + toDescriptionString(outSC2) + "\n";		
		return str;
	}
	public static String toString(Set<ELNode> nodes, Map<ELNode,String> nodeNames) {
		String str = "";
		// comma separated list of expressions
		for(ELNode node : nodes) {
			str += nodeNames.get(node) + ",";
		}
		// remove last comma
		if(str.length() > 0) {
			str = str.substring(0, str.length()-1);
		}
		return str;
	}
	
	public String toSimulationString(Map<ELNode,String> nodeNames) {
		String str = "";
		str += "  in: " + toString(in, nodeNames) + "\n";
		str += "  inSC1: " + toString(inSC1, nodeNames) + "\n";
		str += "  inSC2: " + toString(inSC2, nodeNames) + "\n";
		str += "  out: " + toString(out, nodeNames) + "\n";
		str += "  outSC1: " + toString(outSC1, nodeNames) + "\n";
		str += "  outSC2: " + toString(outSC2, nodeNames) + "\n";		
		return str;
	}
	
	public ELNode getParent() {
		return parent;
	}
	
	
	public ELEdge getParentEdge() {
		int childNr = getChildNumber();
		return parent.edges.get(childNr);
	}
 
	public Set<ELNode> getIn() {
		return in;
	}
 
	public Set<ELNode> getInSC1() {
		return inSC1;
	} 
	public Set<ELNode> getInSC2() {
		return inSC2;
	}
 
	public Set<ELNode> getOut() {
		return out;
	}
 
	public Set<ELNode> getOutSC1() {
		return outSC1;
	}
 
	public Set<ELNode> getOutSC2() {
		return outSC2;
	}
	
	public ELTree getTree() {
		return tree;
	}
	
	 public OWLClassExpression transformToDescription() {
	    	OWLOntologyManager man = OWLManager.createOWLOntologyManager();
	        OWLDataFactory dataFactory = man.getOWLDataFactory();
			int nrOfElements = label.size() + edges.size();
			if(nrOfElements == 0) {
				return df.getOWLThing();
			
			} else if(nrOfElements == 1) {
				if(label.size()==1) {
					return label.first();
				} else {
					ELEdge edge = edges.get(0);
					if(edge.isObjectProperty()){
						OWLClassExpression child = edge.getNode().transformToDescription();
						return df.getOWLObjectSomeValuesFrom(edge.getLabel().asOWLObjectProperty(), child);
					}  
					
				}
			// return an intersection of labels and edges
			} else {
				Set<OWLClassExpression> operands = new TreeSet<>();
				for(OWLClass nc : label) {
					operands.add(nc);
				}
				
				for(ELEdge edge : edges) {
					if(edge.isObjectProperty()){
						OWLClassExpression child = edge.getNode().transformToDescription();
						OWLClassExpression osr = dataFactory.getOWLObjectSomeValuesFrom(edge.getLabel().asOWLObjectProperty(), child);
						operands.add(osr);
					}  
				}
				OWLClassExpression is = df.getOWLObjectIntersectionOf(operands);
				return is;
			}
			return null;
		}
}


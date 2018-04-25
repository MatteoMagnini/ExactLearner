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
	
	public TreeSet<OWLClass> label = new TreeSet<>();
	
	public List<ELEdge> edges = new LinkedList<>();
	public int level; 
	//public final Set<ELNode> in = new HashSet<>();
	//public final Set<ELNode> inSC1 = new HashSet<>();
	//public final Set<ELNode> inSC2 = new HashSet<>();
	//public final Set<ELNode> out = new HashSet<>();
//	public final Set<ELNode> outSC1 = new HashSet<>();
//	public final Set<ELNode> outSC2 = new HashSet<>();
	// parent node in the tree;
	// null indicates that this node is a root node
	public ELNode parent = null;
	public boolean isClassNode;
	public OWLDataRange dataRange;
	private OWLDataFactory df = new OWLDataFactoryImpl();

	
	
	public ELNode() {
		
	}
	public ELNode(OWLDataFactory daf) {
		this.df = daf;
	}
	public ELNode(ELTree tree) {
		this(tree, new TreeSet<>());
	} 
	
	/**
	 * Constructs an EL OWLClassExpression tree given its root label.
	 * @param label Label of the root node.
	 */
	public ELNode(ELTree tree, TreeSet<OWLClass> label) {
		this.label = label;
		this.edges = new LinkedList<>();
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
		this.edges = new LinkedList<>();
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
		
		this.edges = new LinkedList<>();
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
		 
		Set<ELNode> update = new HashSet<>();
		
		// loop over all nodes on the same level, which are not in the in set
		Set<ELNode> nodes = tree.getNodesOnLevel(level);
		for(ELNode w : nodes) {
			// to save space, we do not add reflexive relations
			if(w != this) {
//				// (w,v') is automatically added
//				tree.extendSimulation(w, this);
//
//				// check conditions for (v',w)
//				boolean sc1 = false, sc2 = false;
//
//				if(w.label.size() == 0) {
//					tree.extendSimulationSC1(this, w);
//					sc1 = true;
//				}
//
//				if(w.edges.size() == 0) {
//					tree.extendSimulationSC2(this, w);
//					sc2 = true;
//				}
//
//				if(sc1 && sc2) {
//					tree.extendSimulationSC12(this, w);
//				}
				
				update.add(w.parent);
			}
		}
		update.add(this.parent);  
		//tree.updateSimulation(update);
		
		
		// 1 for the edge (labels are already taken care of by extendLabel)
		tree.size += 1;
		
		isClassNode = false;
	}
	public String getNodeExpression()
	{
		if(this.isRoot())
			return this.tree.toDescriptionString();
		List<ELEdge> edge = new ArrayList<>(this.parent.edges);
		for(ELEdge edg : edge)
		{
			if(edg.node.equals(this))
			{
				return edg.strLabel + " some (" + getLabels(this.label) + ")";
			}
		}
		return "FAIL";
	}
	private String getLabels(TreeSet<OWLClass> labs)
	{
		if(labs.size() < 1)
			return "";
		StringBuilder str = new StringBuilder();
		for(OWLClass cl : labs)
		{
			str.append(cl.toString().substring(cl.toString().indexOf("#") + 1));
			str = new StringBuilder(str.substring(0, str.length() - 1) + " and ");
		}
		str = new StringBuilder(str.substring(0, str.length() - 4));
		return str.toString();
	}
	public ELNode(ELNode parentNode, OWLProperty parentProperty, Set<OWLClass> label) {
//		this.label = label;
		// we first need to add the edge and update the simulation and then add
		// all classes iteratively to the label (each time updating the simulation again)
		this.edges = new LinkedList<>();
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
		Set<ELNode> update = new HashSet<>();
		
		// loop over all nodes on the same level, which are not in the in set
		Set<ELNode> nodes = tree.getNodesOnLevel(level);
		for(ELNode w : nodes) {
			// to save space, we do not add reflexive relations
			if(w != this) {
				// (w,v') is automatically added
//				tree.extendSimulation(w, this);
//
//				// check conditions for (v',w)
//				boolean sc1 = false, sc2 = false;
//
//				if(w.label.size() == 0) {
//					tree.extendSimulationSC1(this, w);
//					sc1 = true;
//				}
//
//				if(w.edges.size() == 0) {
//					tree.extendSimulationSC2(this, w);
//					sc2 = true;
//				}
//
//				if(sc1 && sc2) {
//					tree.extendSimulationSC12(this, w);
//				}
				
				update.add(w.parent);
			}
		}
		update.add(this.parent); 
		
		// apply updates recursively top-down
//		tree.updateSimulation(update);
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
			root = root.parent; // BORIS: I hope this is correct (new root is old root's parent)
		}
		return root;
	}
	private boolean isRoot() {
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
	private int getChildNumber() {
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
//		labelSimulationUpdate();
	}
	
	
	public void remove(OWLClass cl) {
		label.remove(cl);
//		labelSimulationUpdate();
	}
	/**
	 * Adds an entry to the node label.
	 * @param newClass Class to add to label.
	 */
	public void extendLabel(OWLClass newClass) {
		label.add(newClass);
//		labelSimulationUpdate();
		tree.size += 1;
//		System.out.println(tree);
//		System.out.println(tree.size);
	}
//	private void labelSimulationUpdate() {
//		Set<ELNode> update = new HashSet<>();
//
//		Set<ELNode> tmp = tree.getNodesOnLevel(level);
//		for(ELNode w : tmp) {
//			if(w != this) {
//				// SC1(v,w) can only change from false to true
//				if(!inSC1.contains(w) && tree.checkSC1(this, w)) {
//					tree.extendSimulationSC1(this, w);
//					if(inSC2.contains(w)) {
//						tree.extendSimulationSC12(this, w);
//					}
//					update.add(w.getParent());
//				}
//				// SC1(w,v) can only change from true to false
//				if(outSC1.contains(w) && !tree.checkSC1(w, this)) {
//					tree.shrinkSimulationSC1(w, this);
//					if(outSC2.contains(w)) {
//						tree.shrinkSimulationSC12(w, this);
//					}
//					update.add(w.getParent());
//				}
//			}
//		}
//		if(parent != null) {
//			update.add(parent);
//		}
//		// apply updates recursively top-down
//		tree.updateSimulation(update);
////		mon.stop();
//	}
	public void refineEdge(int edgeNumber, OWLProperty op) {
		edges.get(edgeNumber).setLabel(op); 
		Set<ELNode> update = new HashSet<>();
		update.add(this); 
//		tree.updateSimulation(update);
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
		StringBuilder indentString = new StringBuilder();
		for(int i=0; i<indent; i++)
			indentString.append("  ");
		
		StringBuilder str = new StringBuilder(indentString + label.toString() + "\n");
		for(ELEdge edge : edges) {
			str.append(indentString).append("-- ").append(edge.getLabel()).append(" -->\n");
			str.append(edge.getNode().toString(indent + 2));
		}
		return str.toString();
	}
	private boolean isClassNode() {
		return isClassNode;
	}
	public String toDescriptionString() {
		StringBuilder str = new StringBuilder();
		if(isClassNode()){
			if(label.isEmpty()) {
				str = new StringBuilder();
			} else {
				Iterator<OWLClass> it = label.iterator();
				while(it.hasNext()) {
					OWLClass nc = it.next();
					if(it.hasNext()) {
						str.append(toMan(nc.toString())).append(" and ");
					} else {
						str.append(toMan(nc.toString()));
					}
				}
			}
		} else {
			str.append(dataRange);
		}
		
		int current = 1;
		for(ELEdge edge : edges) {
			if(current> 1) {
				str.append(" and ").append(toMan(edge.getLabel().toString())).append(" some (");
				str.append(toMan(edge.getNode().toDescriptionString())).append(")");
				continue;
			} 
			if(!label.isEmpty()) {
				str.append(" and ").append(toMan(edge.getLabel().toString())).append(" some (");
				str.append(toMan(edge.getNode().toDescriptionString())).append(")");
			}else {
				str.append("  ").append(toMan(edge.getLabel().toString())).append(" some (");
				str.append(toMan(edge.getNode().toDescriptionString())).append(") ");
			}
			
			current++;
		}
		return str.toString();
	}
	private String toMan(String str)
	{
		String mod = "";
		mod = str.substring(str.indexOf("#")+1); 
		mod = mod.replaceAll(">", "");
		return mod;
	}
	
	private String toDescriptionString(Set<ELNode> nodes) {
		StringBuilder str = new StringBuilder();
		// comma separated list of descriptions
		for(ELNode node : nodes) {
			str.append(node.toDescriptionString()).append(",");
		}
		// remove last comma
		if(str.length() > 0) {
			str = new StringBuilder(str.substring(0, str.length() - 1));
		}
		return str.toString();
	}
	
//	public String toSimulationString() {
//		String str = "";
//		str += "in: " + toDescriptionString(in) + "\n";
//		str += "inSC1: " + toDescriptionString(inSC1) + "\n";
//		str += "inSC2: " + toDescriptionString(inSC2) + "\n";
//		str += "out: " + toDescriptionString(out) + "\n";
//		str += "outSC1: " + toDescriptionString(outSC1) + "\n";
//		str += "outSC2: " + toDescriptionString(outSC2) + "\n";
//		return str;
//	}
	private static String toString(Set<ELNode> nodes, Map<ELNode, String> nodeNames) {
		StringBuilder str = new StringBuilder();
		// comma separated list of expressions
		for(ELNode node : nodes) {
			str.append(nodeNames.get(node)).append(",");
		}
		// remove last comma
		if(str.length() > 0) {
			str = new StringBuilder(str.substring(0, str.length() - 1));
		}
		return str.toString();
	}
	
//	public String toSimulationString(Map<ELNode,String> nodeNames) {
//		String str = "";
//		str += "  in: " + toString(in, nodeNames) + "\n";
//		str += "  inSC1: " + toString(inSC1, nodeNames) + "\n";
//		str += "  inSC2: " + toString(inSC2, nodeNames) + "\n";
//		str += "  out: " + toString(out, nodeNames) + "\n";
//		str += "  outSC1: " + toString(outSC1, nodeNames) + "\n";
//		str += "  outSC2: " + toString(outSC2, nodeNames) + "\n";
//		return str;
//	}
	
	public ELNode getParent() {
		return parent;
	}
	
	
	public ELEdge getParentEdge() {
		int childNr = getChildNumber();
		return parent.edges.get(childNr);
	}
 
//	public Set<ELNode> getIn() {
//		return in;
//	}
//
//	public Set<ELNode> getInSC1() {
//		return inSC1;
//	}
//	public Set<ELNode> getInSC2() {
//		return inSC2;
//	}
 
//	public Set<ELNode> getOut() {
//		return out;
//	}
 
//	public Set<ELNode> getOutSC1() {
//		return outSC1;
//	}
 
//	public Set<ELNode> getOutSC2() {
//		return outSC2;
//	}
	
	public ELTree getTree() {
		return tree;
	}
	//Reference: DL-Learner
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
					throw new RuntimeException("Description   not supported.");
				}
			// return an intersection of labels and edges
			} else {
				Set<OWLClassExpression> operands = new TreeSet<OWLClassExpression>(label);
				
				for(ELEdge edge : edges) {
					if(edge.isObjectProperty()){
						OWLClassExpression child = edge.getNode().transformToDescription();
						OWLClassExpression osr = dataFactory.getOWLObjectSomeValuesFrom(edge.getLabel().asOWLObjectProperty(), child);
						operands.add(osr);
					}  
				}
				return df.getOWLObjectIntersectionOf(operands);
			}
			 
		}
}


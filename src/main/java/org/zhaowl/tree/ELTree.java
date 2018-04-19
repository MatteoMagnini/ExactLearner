package org.zhaowl.tree;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLProperty;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class ELTree implements Cloneable {

	public int maxLevel = 0;

	public int size = 1;

	public ELNode rootNode;

	// the set of all nodes in the tree
	public Collection<ELNode> nodes = new LinkedList<ELNode>();

	// nodes on a given level of the tree
	public Map<Integer, Set<ELNode>> levelNodeMapping = new HashMap<Integer, Set<ELNode>>();

	// the background knowledge (we need to have it explicitly here,
	// since we store simulation information in the tree and simulation
	// updates depend on background knowledge)
	public ClassHierarchyT subsumptionHierarchy;
	//public ObjectPropertyHierarchyT roleHierarchy;
	public OWLDataFactory df = new OWLDataFactoryImpl();
	public Reasoner rs;
	public OWLOntology ontology;

	public void checkTree() {
	}

	public ELTree(Reasoner rs, OWLOntology ontology) {
		this.rs = rs;
		this.ontology = ontology;
		subsumptionHierarchy = rs.getClassHierarchy();
		//roleHierarchy = rs.getObjectPropertyHierarchy();
	}

	public ELTree(Reasoner rs) {
		this.rs = rs;
		subsumptionHierarchy = rs.getClassHierarchy();
		//roleHierarchy = rs.getObjectPropertyHierarchy();
	}

	public ELTree(Reasoner rs, OWLClassExpression description) throws Exception {
		// construct root node and recursively build the tree
		rootNode = new ELNode(this);
		constructTree(description, rootNode);

	}

	public ELTree(OWLClassExpression description) throws Exception {
		// construct root node and recursively build the tree
		rootNode = new ELNode(this);
		constructTree(description, rootNode);

	}

	public void constructTree(OWLClassExpression description, ELNode node) throws Exception {
		if (description.isOWLThing()) {
			// nothing needs to be done as an empty set is owl:Thing
		} else if (!description.isAnonymous()) {
			node.extendLabel(description.asOWLClass());
		} else if (description instanceof OWLObjectSomeValuesFrom) {
			OWLObjectProperty op = ((OWLObjectSomeValuesFrom) description).getProperty().asOWLObjectProperty();
			ELNode newNode = new ELNode(node, op, new TreeSet<OWLClass>());
			constructTree(((OWLObjectSomeValuesFrom) description).getFiller(), newNode);
		} else if (description instanceof OWLDataSomeValuesFrom) {
		} else if (description instanceof OWLObjectIntersectionOf) {
			// loop through all elements of the intersection
			for (OWLClassExpression child : ((OWLObjectIntersectionOf) description).getOperands()) {
				if (!child.isAnonymous()) {
					node.extendLabel(child.asOWLClass());
				} else if (child instanceof OWLObjectSomeValuesFrom) {
					OWLObjectProperty op = ((OWLObjectSomeValuesFrom) child).getProperty().asOWLObjectProperty();
					ELNode newNode = new ELNode(node, op, new TreeSet<OWLClass>());
					constructTree(((OWLObjectSomeValuesFrom) child).getFiller(), newNode);
				} else {
					throw new Exception(description + " specifically " + child);
				}
			}
		} else {
			throw new Exception(description.toString());
		}
	}

	/**
	 * Gets the nodes on a specific level of the tree. This information is cached
	 * here for performance reasons.
	 * 
	 * @param level
	 *            The level (distance from root node).
	 * @return The set of all nodes on the specified level within this tree.
	 */
	public Set<ELNode> getNodesOnLevel(int level) {
		return levelNodeMapping.get(level);
	}
	/*
	 * public OWLClassExpression transformToClassExpression() { return
	 * rootNode.transformToDescription(); }
	 */

	// checks whether this tree is minimal wrt. background knowledge
	public boolean isMinimal() {
		// System.out.println(this);
		// System.out.println(levelNodeMapping);
		// loop through all levels starting from root (level 1)
		for (int i = 1; i <= maxLevel; i++) {
			// get all nodes of this level
			Set<ELNode> nodes = levelNodeMapping.get(i);
			// System.out.println("level " + i + ": " + nodes);
			for (ELNode node : nodes) {
				List<ELEdge> edges = node.getEdges();
				// we need to compare all combination of edges
				// (in both directions because subsumption is obviously
				// not symmetric)
				for (int j = 0; j < edges.size(); j++) {
					for (int k = 0; k < edges.size(); k++) {
						if (j != k) {
							// we first check inclusion property on edges
							OWLProperty<?, ?> op1 = edges.get(j).getLabel();
							OWLProperty<?, ?> op2 = edges.get(k).getLabel();
							if (rs.isSubPropertyOf(op1, op2)) {
								ELNode node1 = edges.get(j).getNode();
								ELNode node2 = edges.get(k).getNode();
								// check simulation condition
								if (node1.in.contains(node2)) { // || node2.in.contains(node1)) {
									// node1 is simulated by node2, i.e. we could remove one
									// of them, so the tree is not minimal
									return false;
								}
							}
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Internal method for updating the node set and the level node mapping. It must
	 * be called when a new node is added to the tree.
	 * 
	 * @param node
	 *            The new node.
	 * @param level
	 *            Level of the new node.
	 */
	public void addNodeToLevel(ELNode node, int level) {
		nodes.add(node);
		if (level <= maxLevel) {
			levelNodeMapping.get(level).add(node);
		} else if (level == maxLevel + 1) {
			Set<ELNode> set = new HashSet<ELNode>();
			set.add(node);
			levelNodeMapping.put(level, set);
			maxLevel++;
		} else {
			throw new RuntimeException("Inconsistent EL OWLClassExpression tree structure.");
		}
	}

	/**
	 * @return the maxLevel
	 */
	public int getMaxLevel() {
		return maxLevel;
	}

	/**
	 * @return the rootNode
	 */
	public ELNode getRootNode() {
		return rootNode;
	}

	/**
	 * Gets the node at the given position. The list is processed as follows:
	 * Starting with the root node, the first element i of list is read and the i-th
	 * child of root node is selected. This node is set as current node and the next
	 * element j of the list is read and the j-th child of the i-th child of the
	 * root node selected etc.
	 * 
	 * @return The node at the specified position.
	 */
	public ELNode getNode(int[] position) {
		// logger.trace(Helper.arrayContent(position));
		// logger.trace(this);
		ELNode currentNode = rootNode;
		for (int aPosition : position) {
			currentNode = currentNode.getEdges().get(aPosition).getNode();
		}
		return currentNode;
	}

	protected void updateSimulation(Set<ELNode> nUpdate) {
		// create a stack and initialize it with the nodes to be updated
		LinkedList<ELNode> list = new LinkedList<ELNode>();
		list.addAll(nUpdate);

		while (list.size() != 0) {
			// take element from bottom of stack (to ensure that all nodes on the
			// same level are tested before any node of a lower level is tested)
			ELNode v = list.pollFirst();
			// loop through all nodes on same level
			Set<ELNode> sameLevel = levelNodeMapping.get(v.getLevel());
			for (ELNode w : sameLevel) {
				if (v != w) {

					// System.out.println(v);
					// System.out.println(w);

					// we update if SC2 did not hold but does now
					if (!v.inSC2.contains(w) && checkSC2(v, w)) {
						// System.out.println("extend sim. after update");

						extendSimulationSC2(v, w);
						if (v.inSC1.contains(w)) {
							extendSimulationSC12(v, w);
						}
						if (!list.contains(v.getParent())) {
							list.add(v.getParent());
						}
						if (!list.contains(w.getParent())) {
							list.add(w.getParent());
						}
					}

					// similar case, but now possibly shrinking the simulation
					if (w.inSC2.contains(v) && !checkSC2(w, v)) {
						// System.out.println("shrink sim. after update");

						shrinkSimulationSC2(w, v);
						if (w.inSC1.contains(v)) {
							shrinkSimulationSC12(w, v);
						}
						if (!list.contains(v.getParent())) {
							list.add(v.getParent());
						}
						if (!list.contains(w.getParent())) {
							list.add(w.getParent());
						}
					}
					/*
					 * if(!v.out.contains(w) ) { System.out.println("test"); if(checkSC2(v,w) &&
					 * v.outSC1.contains(w)) { extendSimulation(v,w); list.add(v.getParent());
					 * list.add(w.getParent()); } else { System.out.println("test in");
					 * shrinkSimulationSC2(v,w); } } if(!w.out.contains(v) ) { if(checkSC2(w,v) &&
					 * w.outSC1.contains(v)) { extendSimulation(w,v); list.add(v.getParent());
					 * list.add(w.getParent()); } else { shrinkSimulationSC2(w,v); } }
					 */
				}
			}
		}
	}

	// SC satisfied if both SC1 and SC2 satisfied
	public boolean checkSC(ELNode node1, ELNode node2) {
		return checkSC1(node1, node2) && checkSC2(node1, node2);
	}

	// tests simulation condition 1 (SC1)
	public boolean checkSC1(ELNode node1, ELNode node2) {
		return isSublabel(node1.getLabel(), node2.getLabel());
	}

	private boolean isSublabel(NavigableSet<OWLClass> subLabel, NavigableSet<OWLClass> superLabel) {
		// implemented according to definition in article
		// (TODO can probably be done more efficiently)
		for (OWLClass nc : superLabel) {
			if (!containsSubclass(nc, subLabel)) {
				return false;
			}
		}
		return true;
	}

	private boolean containsSubclass(OWLClass superClass, NavigableSet<OWLClass> label) {
		for (OWLClass nc : label) {
			if (subsumptionHierarchy != null && subsumptionHierarchy.isSubclassOf(nc, superClass)) {
				return true;
			}
		}
		return false;
	}

	// tests simulation condition 2 (SC2)
	public boolean checkSC2(ELNode node1, ELNode node2) {
		List<ELEdge> edges1 = node1.getEdges();
		List<ELEdge> edges2 = node2.getEdges();

		// System.out.println(node1.transformToDescription());
		// System.out.println(node2.transformToDescription());

		for (ELEdge superEdge : edges2) {
			// try to find an edge satisfying SC2 in the set,
			// i.e. detect whether superEdge is indeed more general
			if (!checkSC2Edge(superEdge, edges1)) {
				// System.out.println("false");
				return false;
			}
		}
		// System.out.println("true");
		return true;
	}

	// check whether edges contains an element satisfying SC2
	private boolean checkSC2Edge(ELEdge superEdge, List<ELEdge> edges) {
		OWLProperty superOP = superEdge.getLabel();
		ELNode superNode = superEdge.getNode();

		for (ELEdge edge : edges) {
			// System.out.println("superEdge: " + superEdge);
			// System.out.println("edge: " + edge);

			OWLProperty op = edge.getLabel();
			// we first check the condition on the properties
			if (rs.isSubPropertyOf(op, superOP)) {
				// check condition on simulations of referred nodes
				ELNode node = edge.getNode();
				// if(superNode.in.contains(node) || node.in.contains(superNode)) {
				if (node.in.contains(superNode)) {
					// we found a node satisfying the condition, so we can return
					return true;
				}
			}
		}

		// none of the edges in the set satisfies the 2nd simulation criterion
		// wrt. the first edge
		return false;
	}

	// adds (node1,node2) to simulation, takes care of all helper sets
	public void extendSimulation(ELNode node1, ELNode node2) {
		node1.in.add(node2);
		node1.inSC1.add(node2);
		node1.inSC2.add(node2);
		node2.out.add(node1);
		node2.outSC1.add(node1);
		node2.outSC2.add(node1);
	}

	public void extendSimulationSC1(ELNode node1, ELNode node2) {
		node1.inSC1.add(node2);
		node2.outSC1.add(node1);
	}

	public void extendSimulationSC2(ELNode node1, ELNode node2) {
		node1.inSC2.add(node2);
		node2.outSC2.add(node1);
	}

	public void extendSimulationSC12(ELNode node1, ELNode node2) {
		node1.in.add(node2);
		node2.out.add(node1);
	}

	// removes (node1,node2) from simulation, takes care of all helper sets
	public void shrinkSimulation(ELNode node1, ELNode node2) {
		node1.in.remove(node2);
		node1.inSC1.remove(node2);
		node1.inSC2.remove(node2);
		node2.out.remove(node1);
		node2.outSC1.remove(node1);
		node2.outSC2.remove(node1);
	}

	public void shrinkSimulationSC1(ELNode node1, ELNode node2) {
		node1.inSC1.remove(node2);
		node2.outSC1.remove(node1);
	}

	public void shrinkSimulationSC2(ELNode node1, ELNode node2) {
		// System.out.println(node2.outSC2);
		node1.inSC2.remove(node2);
		node2.outSC2.remove(node1);
		// System.out.println(node2.outSC2);
	}

	public void shrinkSimulationSC12(ELNode node1, ELNode node2) {
		node1.in.remove(node2);
		node2.out.remove(node1);
	}

	public String toSimulationString() {
		String str = "";
		for (ELNode node : nodes) {
			str += node.toSimulationString() + "\n";
		}
		return str;
	}

	public String toSimulationString(Map<ELNode, String> nodeNames) {
		String str = "";
		for (Entry<ELNode, String> entry : nodeNames.entrySet()) {
			String nodeName = entry.getValue();
			ELNode node = entry.getKey();
			str += nodeName + ":\n";
			str += node.toSimulationString(nodeNames) + "\n";
		}
		return str;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ELTree clone() {
		// Monitor mon = MonitorFactory.start("tree clone");
		// clone "global" tree
		ELTree treeClone = new ELTree(rs, ontology);

		// a mapping between "old" and "new" nodes
		// (hash map should be fast here, but one could also
		// experiment with TreeMap)
		Map<ELNode, ELNode> cloneMap = new HashMap<ELNode, ELNode>();

		// create a new (empty) node for each node in the tree
		// (we loop through the level mapping, because it is cheaper
		// than creating a set of all nodes)
		for (int i = 1; i <= maxLevel; i++) {
			Set<ELNode> tmp = levelNodeMapping.get(i);
			for (ELNode node : tmp) {
				ELNode nodeNew = new ELNode();
				cloneMap.put(node, nodeNew);
			}
		}

		ELNode newRoot = null;

		// loop through all nodes and perform copy operations
		for (Entry<ELNode, ELNode> entry : cloneMap.entrySet()) {
			ELNode oldNode = entry.getKey();
			ELNode newNode = entry.getValue();

			newNode.tree = treeClone;
			newNode.level = oldNode.level;
			newNode.label = (TreeSet<OWLClass>) oldNode.label.clone();
			newNode.dataRange = oldNode.dataRange;
			newNode.isClassNode = oldNode.isClassNode;
			if (oldNode.parent != null) {
				newNode.parent = cloneMap.get(oldNode.parent);
			} else {
				newRoot = newNode;
			}

			// simulation information
			for (ELNode node : oldNode.in) {
				newNode.in.add(cloneMap.get(node));
			}
			for (ELNode node : oldNode.inSC1) {
				newNode.inSC1.add(cloneMap.get(node));
			}
			for (ELNode node : oldNode.inSC2) {
				newNode.inSC2.add(cloneMap.get(node));
			}
			for (ELNode node : oldNode.out) {
				newNode.out.add(cloneMap.get(node));
			}
			for (ELNode node : oldNode.outSC1) {
				newNode.outSC1.add(cloneMap.get(node));
			}
			for (ELNode node : oldNode.outSC2) {
				newNode.outSC2.add(cloneMap.get(node));
			}

			// edges
			for (ELEdge edge : oldNode.edges) {
				// create a new edge with same label and replace the node the edge points to
				newNode.edges.add(new ELEdge(edge.getLabel(), cloneMap.get(edge.getNode())));
			}

		}

		// update global tree
		treeClone.rootNode = newRoot;
		treeClone.maxLevel = maxLevel;
		treeClone.size = size;

		// nodes
		treeClone.nodes = new LinkedList();
		for (ELNode oldNode : nodes) {
			treeClone.nodes.add(cloneMap.get(oldNode));
		}

		// level node mapping
		for (int i = 1; i <= maxLevel; i++) {
			Set<ELNode> oldNodes = levelNodeMapping.get(i);
			Set<ELNode> newNodes = new HashSet<ELNode>();
			for (ELNode oldNode : oldNodes) {
				newNodes.add(cloneMap.get(oldNode));
			}
			treeClone.levelNodeMapping.put(i, newNodes);
		}

		// mon.stop();
		return treeClone;
	}

	public ELTree cloneOld() {
		// create a new reference tree
		ELTree treeClone = new ELTree(rs, ontology);
		// create a root node attached to this reference tree
		ELNode rootNodeClone = new ELNode(treeClone, new TreeSet<OWLClass>(rootNode.getLabel()));
		cloneRecursively(rootNode, rootNodeClone);
		return treeClone;
	}

	// we read from the original structure and write to the new structure
	private void cloneRecursively(ELNode node, ELNode nodeClone) {
		// loop through all edges and clone the subtrees
		for (ELEdge edge : node.getEdges()) {
			ELNode tmp = new ELNode(nodeClone, edge.getLabel(), new TreeSet<OWLClass>(edge.getNode().getLabel()));
			cloneRecursively(edge.getNode(), tmp);
		}
	}

	@Override
	public String toString() {
		return rootNode.toString();
	}

	/**
	 * Returns a string of the tree OWLClassExpression (without the overhead of
	 * converting the tree into a description).
	 * 
	 * @return A string for the OWLClassExpression the tree stands for.
	 */
	public String toDescriptionString() {
		return rootNode.toDescriptionString();
	}

	/**
	 * @return the nodes
	 */
	public Collection<ELNode> getNodes() {
		return nodes;
	}

	public int getDepth() {
		return maxLevel;
	}

	/**
	 * size of tree = number of nodes + sum of cardinality of node labels
	 * 
	 * @return The tree size.
	 */
	public int getSize() {
		// int size = nodes.size();
		// for(ELDescriptionNode node : nodes) {
		// size += node.getLabel().size();
		// }
		return size;
	}
	
	public OWLClassExpression transformToClassExpression() {
		return rootNode.transformToDescription();
	}
}

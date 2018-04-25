package org.zhaowl.oracle;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.zhaowl.engine.ELEngine;
import org.zhaowl.tree.ELNode;
import org.zhaowl.tree.ELTree;
import org.zhaowl.utils.Metrics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ELOracle {

	private final ELEngine myEngineForT;
	private final ELEngine myEngineForH;
	private final Metrics myMetrics;

	public ELOracle(ELEngine elEngineForT, ELEngine elEngineForH, Metrics metrics) {
		myEngineForT = elEngineForT;
		myEngineForH = elEngineForH;
		myMetrics = metrics;
	}

	public OWLClassExpression oracleSiblingMerge(OWLClassExpression left) throws Exception {
		// the oracle must do sibling merging (if possible)
		// on the left hand side
		ELTree tree = new ELTree(left);
		// Set<ELNode> nodes = null;
		// System.out.println(tree.toDescriptionString());

		OWLClassExpression oldTree = tree.transformToClassExpression();
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			Set<ELNode> nodes = tree.getNodesOnLevel(i + 1);
			if (!nodes.isEmpty())
				for (ELNode nod : nodes) {
					// nod.label.addAll(nod.label);
					if (!nod.getEdges().isEmpty() && nod.getEdges().size() > 1) {

						for (int j = 0; j < nod.getEdges().size(); j++) {
							for (int k = 0; k < nod.getEdges().size(); k++) {
								if (j == k) {
									continue;
								}
								if (nod.getEdges().get(j).getStrLabel().equals(nod.getEdges().get(k).getStrLabel())) {

									// System.out.println("they are equal: " +
									// nod.edges.get(j).node.toDescriptionString() + " AND " +
									// nod.edges.get(k).node.toDescriptionString());
									nod.getEdges().get(j).getNode().getLabel().addAll(nod.getEdges().get(k).getNode().getLabel());
									if (!nod.getEdges().get(k).getNode().getEdges().isEmpty())
										nod.getEdges().get(j).getNode().getEdges().addAll(nod.getEdges().get(k).getNode().getEdges());
									nod.getEdges().remove(nod.getEdges().get(k));
									if (myEngineForT.entailed(myEngineForT.getSubClassAxiom(left,
											tree.transformToClassExpression()))) {
										oldTree =  tree.transformToClassExpression();
									} else {
										tree = new ELTree(oldTree);
									}
								}
							}
						}

					}
				}
		}
		// System.out.println(tree.getRootNode());

		System.out.flush();
		return tree.transformToClassExpression();
	}

	private Set<Set<OWLClass>> powerSetBySize(Set<OWLClass> originalSet, int size) {
		Set<Set<OWLClass>> sets = new HashSet<>();
		if (size == 0) {
			sets.add(new HashSet<>());
			return sets;
		}
		List<OWLClass> list = new ArrayList<>(originalSet);

		for (int i = 0; i < list.size(); i++) {
			OWLClass head = list.get(i);
			List<OWLClass> rest = list.subList(i + 1, list.size());
			Set<Set<OWLClass>> powerRest = powerSetBySize(new HashSet<>(rest), size - 1);
			for (Set<OWLClass> p : powerRest) {
				HashSet<OWLClass> appendedSet = new HashSet<>();
				appendedSet.add(head);
				appendedSet.addAll(p);
				sets.add(appendedSet);
			}
		}
		return sets;
	}

	public OWLSubClassOfAxiom saturateWithTreeLeft(OWLSubClassOfAxiom axiom) throws Exception {
		OWLClassExpression sub = axiom.getSubClass();
		OWLClassExpression sup = axiom.getSuperClass();


		Set<OWLClass> cIo = myEngineForT.getClassesInSignature();
		// Set<ELNode> nodes = null;

		ELTree tree = new ELTree(sub);

		for (int i = 0; i < tree.getMaxLevel(); i++) {
			Set<ELNode> nodes = tree.getNodesOnLevel(i + 1);
			if (!nodes.isEmpty())
				for (ELNode nod : nodes) {
					for (OWLClass cl : cIo) {
						// System.out.println("Node before: " + nod);
						if (!nod.getLabel().contains(cl) && !cl.toString().contains(":Thing")) {
							nod.getLabel().add(cl);
						}
						// System.out.println("Node after: " + nod);
						OWLClassExpression newEx = tree.transformToClassExpression();
						// System.out.println("After saturation step: " + tree.toDescriptionString());
						OWLSubClassOfAxiom newAx = myEngineForT.getSubClassAxiom(newEx, sup);
						myMetrics.setMembCount(myMetrics.getMembCount() + 1);
						if (myEngineForT.entailed(newAx)) {
							tree = new ELTree(sub);
						} else {
							sub = tree.transformToClassExpression();
						}

					}

				}
		}
		// System.out.println("Tree: " + tree.getRootNode());
		// System.out.println("Aux Tree: " + auxTree.getRootNode());
		// System.out.println("Final after saturation: " + sub);



		return myEngineForT.getSubClassAxiom(sub, sup);
	}



	public OWLClassExpression unsaturateRight(OWLSubClassOfAxiom ax) throws Exception {
		OWLClassExpression left = ax.getSubClass();
		OWLClassExpression right = ax.getSuperClass();
		ELTree tree = new ELTree(right);
		// Set<ELNode> nodes = null;

		int sizeToCheck = 0;

		// @foundSomething
		// this flag is used to create a new set of elements to iterate over,
		// in order to find if a proper combination of concepts that a node needs
		// in order to make the CI valid

		boolean foundSomething = false;



		for (int i = 0; i < tree.getMaxLevel(); i++) {
			Set<ELNode> nodes = tree.getNodesOnLevel(i + 1);
			for (ELNode nod : nodes) {
				if (nod.getLabel().size() < 2)
					continue;
				while (!foundSomething) {
					// size of power set
					sizeToCheck++;
					// set to be used when building a power set of concepts

					// populate set
					Set<OWLClass> toBuildPS = new HashSet<>(nod.getLabel());

					// set of sets of concepts as power set
					// Set<Set<OWLClass>> conceptSet = new HashSet<>();

					// populate set of sets of concepts
					// @sizeToCheck is the number of concepts in the set
					// @sizeToCheck = 1, returns single concepts in power set (ps) [A,B,C,D]
					// @sizeToCheck = 2, returns ps size 2 of concepts [(A,B), (A,C), (A,D), (B,C),
					// (B,D), (C,D)]
					// and so on ...
					// this is done in order to check which is the minimal concept(s) set required
					// to satisfy the node
					// and at the same time, the CI
					Set<Set<OWLClass>> conceptSet = powerSetBySize(toBuildPS, sizeToCheck);
					System.out.println("stuck here !!!!");
					// loop through concept set
					for (Set<OWLClass> clSet : conceptSet) {

						nod.getLabel().clear();

						nod.getLabel().addAll(clSet);

						myMetrics.setMembCount(myMetrics.getMembCount() + 1);

						// System.out.println(tree.toDescriptionString());
						if (myEngineForT.entailed(
								myEngineForT.getSubClassAxiom(left, tree.transformToClassExpression())
								))

						{

							foundSomething = true;
 

						} else {
							// System.out.println("This one is useless: " + cl);
							// nod.label = new TreeSet<OWLClass>();

						}

					}
				}
				// reset power set size to check
				foundSomething = false;
				sizeToCheck = 0;
			}
		}
		System.out.flush();


		return tree.transformToClassExpression();
	}

	public OWLClassExpression branchRight(OWLClassExpression right) {
//		try {
//
//			ELTree treeR = new ELTree(right);
//			Set<ELNode> nodes = null;
//			List<ELEdge> auxEdges = null;
//			ELTree auxTree = new ELTree(right);
//
//
//			for (int i = 0; i < treeR.maxLevel; i++) {
//				nodes = treeR.getNodesOnLevel(i + 1);
//				for (ELNode nod : nodes) {
//					if (nod.edges.isEmpty())
//						continue;
//					auxEdges = new LinkedList<ELEdge>(nod.edges);
//					for (int j = 0; j < auxEdges.size(); j++) {
//						if (auxEdges.get(j).node.label.size() == 1)
//							continue;
//						// create list of classes in target node
//						List<OWLClass> classAux = new ArrayList<OWLClass>();
//						// fill list with classes
//						for (OWLClass cl : auxEdges.get(j).node.label)
//							classAux.add(cl);
//						// for each class, create a node and add class to target node
//						for (int k = 0; k < classAux.size(); k++) {
//
//							nod.edges.add(new ELEdge(auxEdges.get(j).label, new ELNode(
//									new ELTree( myEngineForT.getOWLObjectIntersectionOf(classAux)));
////									 (new Metrics().fixAxioms(classAux.get(k)))))));
//
//							// add class to new node
//							nod.edges.get(nod.edges.size() - 1).node.label.add(classAux.get(k));
//
//							// remove class from old node
//							auxEdges.get(j).node.label.remove(classAux.get(k));
//
//							// check target for entailment of new tree
//
//							/*
//							 * if (myEngineForT.entailed(myEngineForT.getSubClassAxiom(left,
//							 * myEngineForT.parseClassExpression(treeR.toDescriptionString())))) { continue;
//							 * 
//							 * } else { // tree is invalid, rollback nod.edges.remove(nod.edges.size() - 1);
//							 * nod.edges.get(j).node.label.add(classAux.get(k)); // }
//							 */
//						}
//					}
//				}
//			}
//
//			// System.out.println("branched tree : \n" + treeL.rootNode);
//			// System.out.println(treeL.toDescriptionString());
//
//			System.out.flush();
//			left = null;
//			right = null;
//			auxEdges = null;
//			nodes = null;
//			OWLClassExpression ex =  treeR.transformToClassExpression();
//			treeR = null;
//
//
//			return ex;
//		} catch (Exception e) {
//			System.out.println("Error in branchNode: " + e);
//		}
//		System.out.flush();
//		left = null;
//		right = null;

		return right;
	}
	
//at the moment duplicated
	public Boolean isCounterExample(OWLClassExpression left, OWLClassExpression right){
		return  myEngineForT.entailed(myEngineForT.getSubClassAxiom(left, right))
				&&
				!myEngineForH.entailed(myEngineForH.getSubClassAxiom(left, right));
	}
}

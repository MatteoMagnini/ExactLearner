package org.zhaowl.oracle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhaowl.console.consoleLearner;
import org.zhaowl.engine.ELEngine;
import org.zhaowl.tree.ELEdge;
import org.zhaowl.tree.ELNode;
import org.zhaowl.tree.ELTree;
import org.zhaowl.utils.SimpleClass;

public class ELOracle {

	private final ELEngine myEngineForT;
	private final ELEngine myEngineForH;
	private final consoleLearner myConsole;
	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(ELOracle.class);

	public ELOracle(ELEngine elEngineforT, ELEngine elEngineForH, consoleLearner console) {
		myEngineForT = elEngineforT;
		myEngineForH = elEngineForH;
		myConsole = console;
	}

 

	public OWLClassExpression oracleSiblingMerge(OWLClassExpression left, OWLClassExpression right) throws Exception {
		// the oracle must do sibling merging (if possible)
		// on the left hand side
		ELTree tree = new ELTree(left);
		Set<ELNode> nodes = null;
		// System.out.println(tree.toDescriptionString());

		OWLClassExpression oldTree = myEngineForT.parseClassExpression(tree.toDescriptionString());
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			nodes = tree.getNodesOnLevel(i + 1);
			if (!nodes.isEmpty())
				for (ELNode nod : nodes) {
					// nod.label.addAll(nod.label);
					if (!nod.edges.isEmpty() && nod.edges.size() > 1) {

						for (int j = 0; j < nod.edges.size(); j++) {
							for (int k = 0; k < nod.edges.size(); k++) {
								if (j == k) {
									continue;
								}
								if (nod.edges.get(j).strLabel.equals(nod.edges.get(k).strLabel)) {

									// System.out.println("they are equal: " +
									// nod.edges.get(j).node.toDescriptionString() + " AND " +
									// nod.edges.get(k).node.toDescriptionString());
									nod.edges.get(j).node.label.addAll(nod.edges.get(k).node.label);
									if (!nod.edges.get(k).node.edges.isEmpty())
										nod.edges.get(j).node.edges.addAll(nod.edges.get(k).node.edges);
									nod.edges.remove(nod.edges.get(k));
									if (myEngineForT.entailed(myEngineForT.getSubClassAxiom(left,
											myEngineForT.parseClassExpression(tree.toDescriptionString())))) {
										oldTree = myEngineForT.parseClassExpression(tree.toDescriptionString());
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
		oldTree = null;
		nodes = null;
		left = null;
		right = null;

		System.out.flush();
		return myEngineForT.parseClassExpression(tree.toDescriptionString());
	}

	public Set<Set<OWLClass>> powerSetBySize(Set<OWLClass> originalSet, int size) {
		Set<Set<OWLClass>> sets = new HashSet<Set<OWLClass>>();
		if (size == 0) {
			sets.add(new HashSet<OWLClass>());
			return sets;
		}
		List<OWLClass> list = new ArrayList<OWLClass>(originalSet);

		for (int i = 0; i < list.size(); i++) {
			OWLClass head = list.get(i);
			List<OWLClass> rest = list.subList(i + 1, list.size());
			Set<Set<OWLClass>> powerRest = powerSetBySize(new HashSet<OWLClass>(rest), size - 1);
			for (Set<OWLClass> p : powerRest) {
				HashSet<OWLClass> appendedSet = new HashSet<OWLClass>();
				appendedSet.add(head);
				appendedSet.addAll(p);
				sets.add(appendedSet);
			}
		}
		return sets;
	}

	public OWLAxiom saturateWithTreeLeft(OWLSubClassOfAxiom axiom) throws Exception {
		OWLClassExpression sub = ((OWLSubClassOfAxiom) axiom).getSubClass();
		OWLClassExpression sup = ((OWLSubClassOfAxiom) axiom).getSuperClass();


		Set<OWLClass> cIo = myEngineForT.getClassesInSignature();
		Set<ELNode> nodes = null;

		ELTree tree = new ELTree(sub);

		for (int i = 0; i < tree.getMaxLevel(); i++) {
			nodes = tree.getNodesOnLevel(i + 1);
			if (!nodes.isEmpty())
				for (ELNode nod : nodes) {
					for (OWLClass cl : cIo) {
						// System.out.println("Node before: " + nod);
						if (!nod.label.contains(cl) && !cl.toString().contains(":Thing")) {
							nod.label.add(cl);
						}
						// System.out.println("Node after: " + nod);
						OWLClassExpression newEx = myEngineForT.parseClassExpression(tree.toDescriptionString());
						// System.out.println("After saturation step: " + tree.toDescriptionString());
						OWLAxiom newAx = myEngineForT.getSubClassAxiom(newEx, sup);
						myConsole.membCount++;
						if (myEngineForT.entailed(newAx)) {
							tree = new ELTree(sub);
						} else {
							sub = myEngineForT.parseClassExpression(tree.toDescriptionString());
						}

					}

				}
		}
		// System.out.println("Tree: " + tree.getRootNode());
		// System.out.println("Aux Tree: " + auxTree.getRootNode());
		// System.out.println("Final after saturation: " + sub);

		cIo = null;
		nodes = null;
		System.out.flush();
		tree = null;


		return myEngineForT.getSubClassAxiom(sub, sup);
	}



	public OWLClassExpression unsaturateRight(OWLAxiom ax) throws Exception {
		OWLClassExpression left = ((OWLSubClassOfAxiom) ax).getSubClass();
		OWLClassExpression right = ((OWLSubClassOfAxiom) ax).getSuperClass();
		ELTree tree = null;
		tree = new ELTree(right);
		Set<ELNode> nodes = null;

		int sizeToCheck = 0;

		// @foundSomething
		// this flag is used to create a new set of elements to iterate over,
		// in order to find if a proper combination of concepts that a node needs
		// in order to make the CI valid

		boolean foundSomething = false;



		for (int i = 0; i < tree.getMaxLevel(); i++) {
			nodes = tree.getNodesOnLevel(i + 1);
			for (ELNode nod : nodes) {
				if (nod.label.size() < 2)
					continue;
				while (!foundSomething) {
					// size of power set
					sizeToCheck++;
					// set to be used when building a power set of concepts
					Set<OWLClass> toBuildPS = new HashSet<OWLClass>();

					// populate set
					for (OWLClass cl : nod.label)
						toBuildPS.add(cl);

					// set of sets of concepts as power set
					Set<Set<OWLClass>> conceptSet = new HashSet<Set<OWLClass>>();

					// populate set of sets of concepts
					// @sizeToCheck is the number of concepts in the set
					// @sizeToCheck = 1, returns single concepts in power set (ps) [A,B,C,D]
					// @sizeToCheck = 2, returns ps size 2 of concepts [(A,B), (A,C), (A,D), (B,C),
					// (B,D), (C,D)]
					// and so on ...
					// this is done in order to check which is the minimal concept(s) set required
					// to satisfy the node
					// and at the same time, the CI
					conceptSet = powerSetBySize(toBuildPS, sizeToCheck);
					System.out.println("stuck here !!!!");
					// loop through concept set
					for (Set<OWLClass> clSet : conceptSet) {

						nod.label = new TreeSet<OWLClass>();

						for (OWLClass cl : clSet)
							nod.label.add(cl);

						myConsole.membCount++;

						// System.out.println(tree.toDescriptionString());
						if (myEngineForT.entailed(myEngineForT.parseToOWLSubClassOfAxiom(
								(new ELTree(left)).toDescriptionString(), tree.toDescriptionString())))

						{

							foundSomething = true;
 

						} else {
							// System.out.println("This one is useless: " + cl);
							// nod.label = new TreeSet<OWLClass>();
							continue;

						}

					}
					toBuildPS = null;
					conceptSet = null;

				}
				// reset power set size to check
				foundSomething = false;
				sizeToCheck = 0;
			}
		}
		System.out.flush();
		OWLClassExpression ex = myEngineForT.parseClassExpression(tree.toDescriptionString());

		left = null;
		right = null;
		tree = null;
		nodes = null;
		return ex;
	}

	public OWLClassExpression branchRight(OWLClassExpression left, OWLClassExpression right) {
		try {

			ELTree treeR = new ELTree(right);
			Set<ELNode> nodes = null;
			List<ELEdge> auxEdges = null;
			ELTree auxTree = new ELTree(right);


			for (int i = 0; i < treeR.maxLevel; i++) {
				nodes = treeR.getNodesOnLevel(i + 1);
				for (ELNode nod : nodes) {
					if (nod.edges.isEmpty())
						continue;
					auxEdges = new LinkedList<ELEdge>(nod.edges);
					for (int j = 0; j < auxEdges.size(); j++) {
						if (auxEdges.get(j).node.label.size() == 1)
							continue;
						// create list of classes in target node
						List<OWLClass> classAux = new ArrayList<OWLClass>();
						// fill list with classes
						for (OWLClass cl : auxEdges.get(j).node.label)
							classAux.add(cl);
						// for each class, create a node and add class to target node
						for (int k = 0; k < classAux.size(); k++) {

							nod.edges.add(new ELEdge(auxEdges.get(j).label, new ELNode(new ELTree(
									myEngineForT.parseClassExpression(new SimpleClass().fixAxioms(classAux.get(k)))))));

							// add class to new node
							nod.edges.get(nod.edges.size() - 1).node.label.add(classAux.get(k));

							// remove class from old node
							auxEdges.get(j).node.label.remove(classAux.get(k));

							// check target for entailment of new tree

							/*
							 * if (myEngineForT.entailed(myEngineForT.getSubClassAxiom(left,
							 * myEngineForT.parseClassExpression(treeR.toDescriptionString())))) { continue;
							 * 
							 * } else { // tree is invalid, rollback nod.edges.remove(nod.edges.size() - 1);
							 * nod.edges.get(j).node.label.add(classAux.get(k)); // }
							 */
						}
					}
				}
			}

			// System.out.println("branched tree : \n" + treeL.rootNode);
			// System.out.println(treeL.toDescriptionString());

			System.out.flush();
			left = null;
			right = null;
			auxEdges = null;
			nodes = null;
			OWLClassExpression ex = myEngineForT.parseClassExpression(treeR.toDescriptionString());
			treeR = null;


			return ex;
		} catch (Exception e) {
			System.out.println("Error in branchNode: " + e);
		}
		System.out.flush();
		left = null;
		right = null;

		return null;
	}

}

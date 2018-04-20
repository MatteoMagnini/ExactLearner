package org.zhaowl.learner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zhaowl.console.consoleLearner;
import org.zhaowl.engine.ELEngine;
import org.zhaowl.tree.ELEdge;
import org.zhaowl.tree.ELNode;
import org.zhaowl.tree.ELTree;
import org.zhaowl.utils.Metrics;

public class ELLearner {

	private final ELEngine myEngineForT;
	private final ELEngine myEngineForH;
	private final consoleLearner myConsole;
	private static final Logger LOGGER_ = LoggerFactory
			.getLogger(ELLearner.class);

	public ELLearner(ELEngine elEngineForT, ELEngine elEngineForH, consoleLearner console) {
		myEngineForH = elEngineForH;
		myEngineForT = elEngineForT;
		myConsole = console;
	}

	public OWLClassExpression unsaturateLeft(OWLAxiom ax) throws Exception {
		OWLClassExpression left = ((OWLSubClassOfAxiom) ax).getSubClass();
		OWLClassExpression right = ((OWLSubClassOfAxiom) ax).getSuperClass();
		ELTree tree = new ELTree(left);
		Set<ELNode> nodes = null;

		int sizeToCheck = 0;

		// @foundSomething
		// this flag is used to create a new set of elements to iterate over,
		// in order to find if a proper combination of concepts that a node needs
		// in order to make the CI valid

		boolean foundSomething = false;

		//reasonerForT = createReasoner(ontology, "reasonerForT");
		//myEngineForT = new ELEngine(reasonerForT, shortFormProvider);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			nodes = tree.getNodesOnLevel(i + 1);
			for (ELNode nod : nodes) {

				while (!foundSomething) {
					// size of power set
					sizeToCheck++;
					// set to be used when building a power set of concepts
					Set<OWLClass> toBuildPS = new HashSet<>();

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

					// loop through concept set
					for (Set<OWLClass> clSet : conceptSet) {

						nod.label = new TreeSet<OWLClass>();

						for (OWLClass cl : clSet)
							nod.label.add(cl);

						myConsole.membCount++;

						// System.out.println(tree.toDescriptionString());
						if (myEngineForT.entailed(
								myEngineForT.getSubClassAxiom(tree.transformToClassExpression(), right
										 )))/*
																	 * && !engineForH.entailed(elQueryEngineForT.
																	 * parseToOWLSubClassOfAxiom(
																	 * tree.toDescriptionString(), (new
																	 * ELTree(right)).toDescriptionString())))
																	 */ {
							foundSomething = true;
							try {
								myConsole.addHypothesis(myEngineForT.getSubClassAxiom(
										 tree.transformToClassExpression(), right));
								 
							} catch (Exception e2) {
								e2.printStackTrace();
							}
							// System.out.println("this one is good: " + cl);

						} else {
							// System.out.println("This one is useless: " + cl);
							// nod.label = new TreeSet<OWLClass>();
							continue;

						}

					}

				}
				// reset power set size to check
				foundSomething = false;
				sizeToCheck = 0;
			}
		}
		System.out.flush();
		OWLClassExpression ex =  tree.transformToClassExpression();
		return ex;
	}



	public void decompose(OWLClassExpression left, OWLClassExpression right) {

		// decomposition
		// creates a tree and loops through it to find "hidden" inclusions in it
		// take tree as: A and B and r.(B and C and s.(D and E))
		// tree with 2 nodes = [A,B] -r-> [B,C] -s-> [D,E]
		// decomposition separates tree and recursively checks based on 2 conditions
		// if non root node [B and C s.(D and E)] has some C' in concept names, such
		// that [B and C s.(D and E)] subClassOf C'
		// decompose C'
		// else if non root node without a branch as T-b [A and B and r.(B and C)] has
		// some C' in concept names,
		// such that [A and B and r.(B and C)] subClassOf C'
		// decompose C'
		// if C' turns out to be a single node, i.e. no branches in C', add the last
		// valid subClassOf relation
		// either [B and C s.(D and E)] subClassOf C' or
		// [A and B and r.(B and C)] subClassOf C' to the hypothesis

		try {
			ELTree treeR = null;
			ELTree treeL = null;
			boolean leftSide = false;

			//reasonerForT = createReasoner(ontology, "reasonerForT");
			//myEngineForT = new ELEngine(reasonerForT, shortFormProvider);
			treeL = new ELTree(left);
			if (treeL.nodes.size() == 1) {
				myConsole.addHypothesis(myEngineForT.getSubClassAxiom(left, right));
				 System.out.flush();

				treeR = null;
				treeL = null;
				left = null;
				right = null;
				return;
			} else
				leftSide = true;

			treeR = new ELTree(right);
			if (treeR.nodes.size() == 1) {
				myConsole.addHypothesis(myEngineForT.getSubClassAxiom(left, right));
				 System.out.flush();

				treeR = null;
				treeL = null;
				left = null;
				right = null;
				//disposeOfReasoner(reasonerForT, "reasonerForT");
				return;
			}
			Set<ELNode> nodes = null;

			List<ELEdge> auxEdges = new LinkedList<ELEdge>();
			if (leftSide)
				treeR = new ELTree(left);
			for (int i = 0; i < treeR.maxLevel; i++) {
				nodes = treeR.getNodesOnLevel(i + 1);
				for (ELNode nod : nodes) {
					if (nod.isRoot())
						continue;
					if (nod.label.size() < 1)
						continue;
					while( myEngineForT.getClassesInSignature().iterator().hasNext()) {
						OWLClass cl= myEngineForT.getClassesInSignature().iterator().next();
						if (! cl.isTopEntity()  ) {
							// System.out.println("Class: " + rendering.render(cl));
							OWLAxiom axiom = myEngineForT.getSubClassAxiom(
								 nod.transformToDescription(),
									 cl );
							// System.out.println(axiom);
							for (int j = 0; j < nod.edges.size(); j++)
								auxEdges.add(nod.edges.get(j));
							// auxEx = elQueryEngineForT.parseClassExpression(treeR.toDescriptionString());
							myConsole.membCount++;
							if (myEngineForT.entailed(axiom)) {
								// System.out.println(nod.toDescriptionString());
								// System.out.println(cl);
								System.out.println(" Decompose this: " + axiom);
								decompose(nod.transformToDescription(),
										 cl );

								treeR = null;
								treeL = null;
								left = null;
								right = null;
								nodes = null;
								//disposeOfReasoner(reasonerForT, "reasonerForT");
								System.out.flush();
								return;
							} else {
								nod.edges = new LinkedList<ELEdge>();
								if (!(nod.label.size() < 1)) {
									myConsole.membCount++;
									if (myEngineForT.entailed(myEngineForT.getSubClassAxiom(
											 treeR.transformToClassExpression(), right))
									// && !engineForH.entailed(myEngineForT.getSubClassAxiom(
									// myEngineForT.parseClassExpression(treeR.toDescriptionString()),
									// right))) {
									) {
										// System.out.println(treeR.toDescriptionString());
										decompose( treeR.transformToClassExpression(), right);

										treeR = null;
										treeL = null;
										left = null;
										right = null;
										nodes = null;
										System.out.flush();
										return;
									}
								}
							}
							for (int j = 0; j < auxEdges.size(); j++)
								nod.edges.add(auxEdges.get(j));
						}
					}
				}
				 
			}
		} catch (Exception e) {
			System.out.println("Error in decompose: " + e);

			left = null;
			right = null;
			System.out.flush();
			return;
		}

		left = null;
		right = null;
		System.out.flush();
		return;
	}

	public int count = 0;

	public OWLAxiom saturateWithTreeRight(OWLAxiom axiom) throws Exception {
		OWLClassExpression sub = ((OWLSubClassOfAxiom) axiom).getSubClass();
		OWLClassExpression sup = ((OWLSubClassOfAxiom) axiom).getSuperClass();

		Set<OWLClass> cIo = myEngineForT.getClassesInSignature();
		Set<ELNode> nodes = null;
		count = 0;
		ELTree tree = new ELTree(sup);
		//reasonerForT = createReasoner(ontology, "reasonerForT");
		//myEngineForT = new ELEngine(reasonerForT, shortFormProvider);
		OWLAxiom newAx = null;
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			nodes = tree.getNodesOnLevel(i + 1);
			// if (!nodes.isEmpty())
			for (ELNode nod : nodes) {
				// maxSaturate = 3;
				for (OWLClass cl : cIo) {
					// System.out.println("Node before: " + nod);
					// if(maxSaturate == 0)
					// break;
					if (!nod.label.contains(cl) && !cl.toString().contains(":Thing")) {
						nod.label.add(cl);
					}

					OWLClassExpression newEx = tree.transformToClassExpression();
					if (newEx.equals(null))
						System.out.println("is null");
					newAx = myEngineForT.getSubClassAxiom(sub, newEx);
					
					// check if hypothesis entails new saturated CI
					myConsole.membCount++;

					if (myEngineForT.entailed(newAx)) {
						// CI is entailed by H, roll tree back to "safe" CI

						tree = new ELTree(sup);
					} else {

						sup = tree.transformToClassExpression();
					}

				}
				
				// node post processing
				ELTree treeOnLeft;
				ELTree treeOnRight;
				List<OWLClass> nodeLeft = new ArrayList<OWLClass>();
				List<OWLClass> nodeRight = new ArrayList<OWLClass>();
				for(OWLClass cl : nod.label)
					nodeLeft.add(cl);
				for(OWLClass cl : nod.label)
					nodeRight.add(cl);
				
				
				for(int j = 0; j < nod.label.size(); j ++)
					for(int k = 0; k < nod.label.size(); k++)
					{
						if(j == k)
							continue;
						treeOnLeft = new ELTree(nodeLeft.get(j));
						treeOnRight = new ELTree(nodeRight.get(k));
						if(myEngineForT.entailed(
								myEngineForT.getSubClassAxiom(treeOnLeft.transformToClassExpression(),
										treeOnRight.transformToClassExpression())))
						{
							nod.label.remove(nodeRight.get(k));
						}
						
					}
				treeOnLeft = null;
				treeOnRight = null;
				nodeLeft = null;
				nodeRight = null;
			}
		}
		// System.out.println("Tree: " + tree.getRootNode());
		// System.out.println("Aux Tree: " + auxTree.getRootNode());
		// System.out.println("Final after saturation: " + sub);
		try {
			myConsole.addHypothesis(
					myEngineForT.getSubClassAxiom(sub, tree.transformToClassExpression()));
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		 tree = null;
		newAx = null;
		cIo = null;
		nodes = null;
		//disposeOfReasoner(reasonerForT, "reasonerForT");
		OWLAxiom ax = myEngineForT.getSubClassAxiom(sub, sup);
		System.out.flush();
		return ax;
	}

	public OWLClassExpression learnerSiblingMerge(OWLClassExpression left, OWLClassExpression right) throws Exception {

		/*
		 * the runLearner must do sibling merging (if possible) on the right hand side
		 */
		try {
			ELTree tree = new ELTree(right);
			Set<ELNode> nodes = null;
			// System.out.println(tree.toDescriptionString());
			//reasonerForT = createReasoner(ontology, "reasonerForT");
			//myEngineForT = new ELEngine(reasonerForT, shortFormProvider);
			OWLClassExpression oldTree = tree.transformToClassExpression();
			// System.out.println(tree.getRootNode());
			// System.out.println(tree.toDescriptionString());
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
										// check if new merged tree is entailed by T
										if (myEngineForT.entailed(myEngineForT.getSubClassAxiom(left,
												tree.transformToClassExpression()))) {
											oldTree =tree.transformToClassExpression();
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
			left = null;
			right = null;
			nodes = null;
			oldTree = null;
			System.out.flush();
			//disposeOfReasoner(reasonerForT, "reasonerForT");
			OWLClassExpression ex = tree.transformToClassExpression();
			tree = null;
			return ex;
		} catch (Exception e) {
			System.out.println("error in merge " + e);
		}
		return null;
	}

	public OWLClassExpression branchLeft(OWLClassExpression left, OWLClassExpression right) {
//		try {
//
//			ELTree treeL = new ELTree(left);
//			Set<ELNode> nodes = null;
//			List<ELEdge> auxEdges = null;
//			//reasonerForT = createReasoner(ontology, "reasonerForT");
//			//myEngineForT = new ELEngine(reasonerForT, shortFormProvider);
//			// System.out.println("we branch this one: \n" + treeL.rootNode);
//			// OWLClassExpression auxTree =
//			// myEngineForT.parseClassExpression(treeL.toDescriptionString());
//			// ELNode auxNode = new ELNode();
//			for (int i = 0; i < treeL.maxLevel; i++) {
//				nodes = treeL.getNodesOnLevel(i + 1);
//				for (ELNode nod : nodes) {
//					if (nod.edges.isEmpty())
//						continue;
//					auxEdges = new LinkedList<ELEdge>(nod.edges);
//
//					for (int j = 0; j < auxEdges.size(); j++) {
//						if (auxEdges.get(j).node.label.size() == 1)
//							continue;
//
//						// create list of classes in target node
//						List<OWLClass> classAux = new ArrayList<OWLClass>();
//						// fill list with classes
//						for (OWLClass cl : auxEdges.get(j).node.label)
//							classAux.add(cl);
//						// for each class, create a node and add class to target node
//						for (int k = 0; k < classAux.size(); k++) {
//
//							nod.edges.add(new ELEdge(auxEdges.get(j).label, new ELNode(new ELTree(
//									myEngineForT.parseClassExpression(new Metrics().fixAxioms(classAux.get(k)))))));
//
//							// add class to new node
//							nod.edges.get(nod.edges.size() - 1).node.label.add(classAux.get(k));
//
//							// remove class from old node
//							auxEdges.get(j).node.label.remove(classAux.get(k));
//
//							// check target for entailment of new tree
//
//							if (myEngineForT.entailed(myEngineForT.getSubClassAxiom(
//									treeL.transformToClassExpression(), right))) { // tree
//																												// isvalid,
//																												// update
//																												// auxiliar
//																												// tree
//								continue;
//
//							} else { // tree is invalid, rollback
//								nod.edges.remove(nod.edges.size() - 1);
//								nod.edges.get(j).node.label.add(classAux.get(k)); //
//							}
//						}
//					}
//				}
//			}
//			// System.out.println("branched tree : \n" + treeL.rootNode);
//			// System.out.println(treeL.toDescriptionString());
//			System.out.flush();
//			left = null;
//			right = null;
//			auxEdges = null;
//			nodes = null;
//			OWLClassExpression ex = treeL.transformToClassExpression();
//			treeL = null;
//			return ex;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		System.out.flush();
		return left;
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

}

package org.exactlearner.oracle;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.exactlearner.engine.ELEngine;
import org.exactlearner.tree.ELEdge;
import org.exactlearner.tree.ELNode;
import org.exactlearner.tree.ELTree;

public class ELOracle {
	private int unsaturationCounter = 0;
	private int saturationCounter = 0;
	private int mergeCounter = 0;
	private int branchCounter = 0;
	private int leftCompositionCounter = 0;
	private int rightCompositionCounter = 0;
	private final ELEngine myEngineForT;
	private final ELEngine myEngineForH;
	private OWLClassExpression myExpression;
	private OWLClassExpression myClass;
	private final Random random = new Random();
	private ELTree leftTree;
	private ELTree rightTree;

	public ELOracle(ELEngine elEngineForT, ELEngine elEngineForH) {
		myEngineForT = elEngineForT;
		myEngineForH = elEngineForH;
	}

	/**
	 * @author anaozaki Concept Unsaturation on the right side of the inclusion
	 *
	 *
	 * @param cl
	 *            class name on the left of an inclusion
	 * @param expression
	 *            class expression on the right of an inclusion
	 */
	public OWLSubClassOfAxiom unsaturateRight(OWLClassExpression cl, OWLClassExpression expression, double bound)
			throws Exception {
		this.leftTree = new ELTree(cl);
		this.rightTree = new ELTree(expression);
		for (int i = 0; i < rightTree.getMaxLevel(); i++) {
			for (ELNode nod : rightTree.getNodesOnLevel(i + 1)) {
				OWLClassExpression cls = nod.transformToDescription();
				for (OWLClass cl1 : cls.getClassesInSignature()) {
					if ((random.nextDouble() < bound)
							&& (nod.getLabel().contains(cl1) && !cl1.toString().contains("Thing"))) {
						nod.remove(cl1);

						if (!myEngineForH
								.entailed(myEngineForH.getSubClassAxiom(leftTree.transformToClassExpression(),
										rightTree.transformToClassExpression()))) {

							unsaturationCounter++;
						} else {
							nod.extendLabel(cl1);
						}
					}
				}
			}
		}
		myClass =   leftTree.transformToClassExpression();
		myExpression = rightTree.transformToClassExpression();
		return myEngineForT.getSubClassAxiom(myClass, myExpression);
	}

//	private Boolean unsaturating(double bound) throws Exception {
//		boolean flag = false;
//		 
//		for (int i = 0; i < rightTree.getMaxLevel(); i++) {
//			for (ELNode nod : rightTree.getNodesOnLevel(i + 1)) {
//				OWLClassExpression cls = nod.transformToDescription();
//				for (OWLClass cl1 : cls.getClassesInSignature()) {
//					if ((random.nextDouble() < bound)
//							&& (nod.getLabel().contains(cl1) && !cl1.toString().contains("Thing"))) {
//						nod.remove(cl1);
//
//						if (!myEngineForH
//								.entailed(myEngineForH.getSubClassAxiom(leftTree.transformToClassExpression(), 
//										rightTree.transformToClassExpression()))) {
//							flag = true;
//							unsaturationCounter++;
//						} else {
//							nod.extendLabel(cl1);
//						}
//					}
//				}
//			}
//		}
//		return flag;
//	}

	/**
	 * @author anaozaki Concept Saturation on the left side of the inclusion
	 *
	 *
	 * @param expression
	 *            class expression on the left of an inclusion
	 * @param cl
	 *            class name on the right of an inclusion
	 */
	public OWLSubClassOfAxiom saturateLeft(OWLClassExpression expression, OWLClassExpression cl, double bound)
			throws Exception {
		this.leftTree = new ELTree(expression);
		this.rightTree = new ELTree(cl);
		for (int i = 0; i < leftTree.getMaxLevel(); i++) {
			for (ELNode nod : leftTree.getNodesOnLevel(i + 1)) {
				for (OWLClass cl1 : myEngineForT.getClassesInSignature()) {
					if ((random.nextDouble() < bound) && !nod.getLabel().contains(cl1)) {
						nod.extendLabel(cl1);

						if (!myEngineForH
								.entailed(myEngineForH.getSubClassAxiom(leftTree.transformToClassExpression(),
										rightTree.transformToClassExpression()))) {
							saturationCounter++;
						} else {
							nod.remove(cl1);
						}
					}
				}
			}
		}
		myClass =   rightTree.transformToClassExpression();
		myExpression = leftTree.transformToClassExpression();
		return myEngineForT.getSubClassAxiom(myExpression, myClass);
	}

//	private Boolean saturating(double bound) throws Exception {
//
//		boolean flag = false;
//		 
//		for (int i = 0; i < leftTree.getMaxLevel(); i++) {
//			for (ELNode nod : leftTree.getNodesOnLevel(i + 1)) {
//				for (OWLClass cl1 : myEngineForT.getClassesInSignature()) {
//					if ((random.nextDouble() < bound) && !nod.getLabel().contains(cl1)) {
//						nod.extendLabel(cl1);
//
//						if (!myEngineForH
//								.entailed(myEngineForH.getSubClassAxiom(leftTree.transformToClassExpression(), 
//										rightTree.transformToClassExpression()))) {						
//							flag = true;
//							saturationCounter++;
//						} else {
//							nod.remove(cl1);
//						}
//					}
//				}
//			}
//		}
//		return flag;
//	}

	public OWLSubClassOfAxiom mergeLeft(OWLClassExpression expression, OWLClassExpression cl, double bound)
			throws Exception {
		myClass = cl;
		myExpression = expression;
		while(merging(myExpression, myClass,bound)) {}
		return myEngineForT.getSubClassAxiom(myExpression, myClass);
	}

	private Boolean merging(OWLClassExpression expression, OWLClassExpression cl, double bound) throws Exception {
		boolean flag = false;
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {
				int l1=0;
				if (!nod.getEdges().isEmpty() && nod.getEdges().size() > 1) {

					for (int j = 0; j <= nod.getEdges().size(); j++) {

						for (int k = 0; k <= nod.getEdges().size(); k++) {
							ELTree oldTree = new ELTree(tree.transformToClassExpression());
							if ((random.nextDouble() < bound) && (j != k && nod.getEdges().get(j).getStrLabel()
									.equals(nod.getEdges().get(k).getStrLabel()))) {
								ELTree tmp=new ELTree(tree.transformToClassExpression());
								Set<ELNode> set=tmp.getNodesOnLevel(i+1);
								ELNode n=set.iterator().next();
								for(int i1=0; i1<l1;i1++ ) {
									  n=set.iterator().next();
								}
								n.getEdges().get(j).getNode().getLabel()
										.addAll(n.getEdges().get(k).getNode().getLabel());
								 
								
								if (!n.getEdges().get(k).getNode().getEdges().isEmpty())
									n.getEdges().get(j).getNode().getEdges()
											.addAll(n.getEdges().get(k).getNode().getEdges());
								 
								n.getEdges().remove(n.getEdges().get(k));
								 

								if (!myEngineForT.entailed(
										myEngineForT.getSubClassAxiom(tree.transformToClassExpression(), 
												tmp.transformToClassExpression())) //if the merged tree is in fact a stronger expression
										&&
										!myEngineForH.entailed(
										myEngineForH.getSubClassAxiom(tmp.transformToClassExpression(), cl))) {
									myExpression = tmp.transformToClassExpression();
									myClass = cl;
									
									mergeCounter++;
									return true;
								} else {
									//tree = oldTree;
								}

							}
						}
					}

				}
				l1++;
			}
		}
		return flag;
	}

	public OWLSubClassOfAxiom branchRight(OWLClassExpression cl, OWLClassExpression expression, double bound)
			throws Exception {
		myClass = cl;
		myExpression = expression;
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {
				if (!nod.getEdges().isEmpty()) {

					for (int j = 0; j < nod.getEdges().size(); j++) {
//                        if (random.nextDouble() >= bound) {
//                            continue;
//                        }
//                        Set<OWLClass> concepts = nod.getEdges().get(j).getNode().getLabel();
//                        for (OWLClass c : concepts) {
//                            Set<OWLClass> impliedC = myEngineForT.getSuperClasses(c, false);
//                            for(OWLAxiom ax : myEngineForT.getOntology().getAxioms()) {
//                                System.out.println(ax);
//                            }
//
//                            for (OWLClass d : impliedC) {
//                                System.out.println(d);
//                                if(d.isOWLThing()) {
//                                    continue;
//                                }
//                                nod.getEdges().get(j).getNode().extendLabel(d);
//                            }
//                        }
						if (nod.getEdges().get(j).getNode().getLabel().size() > 1) {


							Iterator<OWLClass> iterator1 = nod.getEdges().get(j).getNode().getLabel().iterator();

							while(iterator1.hasNext()) {
								OWLClass lab =iterator1.next();
								ELTree oldTree = new ELTree(tree.transformToClassExpression());
								ELTree newSubtree = new ELTree(
										nod.getEdges().get(j).getNode().transformToDescription());
								Iterator<OWLClass> iterator = newSubtree.getRootNode().getLabel().iterator();

								while(iterator.hasNext()) {
									iterator.next();
									iterator.remove();

								}
								newSubtree.getRootNode().extendLabel(lab);
								ELEdge newEdge = new ELEdge(nod.getEdges().get(j).getLabel(), newSubtree.getRootNode());
								nod.getEdges().add(newEdge);

								iterator1.remove();
								if ((random.nextDouble() < bound) &&
										!myEngineForT.entailed(
												myEngineForT.getSubClassAxiom(tree.transformToClassExpression(),
														oldTree.transformToClassExpression())) //if the branched tree is in fact a weaker expression
												&&
										!myEngineForH.entailed(
										myEngineForH.getSubClassAxiom(cl, tree.transformToClassExpression()))) {
									myExpression = tree.transformToClassExpression();
									myClass = cl;

									branchCounter++;
								} else {
									tree = oldTree;
								}
							}
						}

					}

				}
			}
		}
		return myEngineForT.getSubClassAxiom(myClass, myExpression);
	}

//	private Boolean branching(OWLClassExpression cl, OWLClassExpression expression, double bound) throws Exception {
//
//		boolean flag = false;
//		ELTree tree = new ELTree(expression);
//		for (int i = 0; i < tree.getMaxLevel(); i++) {
//			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {
//				if (!nod.getEdges().isEmpty()) {
//
//					for (int j = 0; j < nod.getEdges().size(); j++) {
//						if (nod.getEdges().get(j).getNode().getLabel().size() > 1) {
//							Iterator<OWLClass> iterator1 = nod.getEdges().get(j).getNode().getLabel().iterator();
//							
//							while(iterator1.hasNext()) {
//								OWLClass lab =iterator1.next();
//								ELTree oldTree = new ELTree(tree.transformToClassExpression());
//								ELTree newSubtree = new ELTree(
//										nod.getEdges().get(j).getNode().transformToDescription());
//								Iterator<OWLClass> iterator = newSubtree.getRootNode().getLabel().iterator();
//								 
//								while(iterator.hasNext()) {
//									iterator.next();
//									iterator.remove();
//									 
//								}
//								newSubtree.getRootNode().extendLabel(lab);
//								ELEdge newEdge = new ELEdge(nod.getEdges().get(j).getLabel(), newSubtree.getRootNode());
//								nod.getEdges().add(newEdge);
//								 
//								iterator1.remove();
//								if ((random.nextDouble() < bound) && 
//										!myEngineForT.entailed(
//												myEngineForT.getSubClassAxiom(tree.transformToClassExpression(), 
//														oldTree.transformToClassExpression())) //if the branched tree is in fact a weaker expression
//												&&
//										!myEngineForH.entailed(
//										myEngineForH.getSubClassAxiom(cl, tree.transformToClassExpression()))) {
//									myExpression = tree.transformToClassExpression();
//									myClass = cl;
//									flag = true;
//									branchCounter++;
//								} else {
//									tree = oldTree;
//								}
//							}
//						}
//
//					}
//
//				}
//			}
//		}
//		return flag;
//	}

	public OWLSubClassOfAxiom composeLeft(OWLClassExpression expression, OWLClassExpression cl, double bound)
			throws Exception {
		myClass = cl;
		myExpression = expression;

		// we expect that composing with finish sooner because ontologies are normally
		// acyclic
		int k = myEngineForT.getOntology().getAxiomCount();
		while (composingLeft(myExpression, myClass, bound) && (k > 0)) {
			k--;
		}
		return myEngineForT.getSubClassAxiom(myExpression, myClass);
	}

	private Boolean composingLeft(OWLClassExpression expression, OWLClassExpression cl, double bound) throws Exception {
		boolean flag = false;
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {

				TreeSet<OWLClass> myClassSet = new TreeSet<>(nod.getLabel());
				for (OWLClass c : myClassSet) {

					Set<OWLSubClassOfAxiom> myAxiomSet = myEngineForT.getOntology().getSubClassAxiomsForSuperClass(c);

					for (OWLSubClassOfAxiom mySubClassAxiom : myAxiomSet) {
						if (!nod.getLabel().contains(c)) {
							break;
						}
						ELTree oldTree = new ELTree(tree.transformToClassExpression());
						ELTree subtree = new ELTree(mySubClassAxiom.getSubClass());
						nod.getEdges().addAll(subtree.getRootNode().getEdges());
						nod.extendLabel(subtree.getRootNode().getLabel());
						nod.remove(c);
						if ((random.nextDouble() < bound) && !myEngineForH
								.entailed(myEngineForH.getSubClassAxiom(tree.transformToClassExpression(), cl))) {
							myExpression = tree.transformToClassExpression();
							myClass = cl;
							flag = true;

							leftCompositionCounter++;
						} else {
							tree = oldTree;
						}
					}

					// Set<OWLEquivalentClassesAxiom> myEqAxiomSet = myEngineForT.getOntology()
					// .getEquivalentClassesAxioms(c);
					//
					// for (OWLEquivalentClassesAxiom myEqAxiom : myEqAxiomSet) {
					// if (!nod.getLabel().contains(c)) {
					// break;
					// }
					//
					// Set<OWLClassExpression> myExpSet = myEqAxiom.getClassExpressions();
					// for (OWLClassExpression exp : myExpSet) {
					// if (!nod.getLabel().contains(c)) {
					// break;
					// }
					// ELTree oldTree = new ELTree(tree.transformToClassExpression());
					// ELTree newSubtree = new ELTree(exp);
					// nod.getEdges().addAll(newSubtree.getRootNode().getEdges());
					// nod.extendLabel(newSubtree.getRootNode().getLabel());
					// nod.remove(c);
					// if ((random.nextDouble() < bound) && !myEngineForH
					// .entailed(myEngineForH.getSubClassAxiom(tree.transformToClassExpression(),
					// cl))) {
					// myExpression = tree.transformToClassExpression();
					// myClass = cl;
					// flag = true;
					//
					// leftCompositionCounter++;
					// } else {
					// tree = oldTree;
					// }
					// }
					// }
					//
				}

			}
		}
		return flag;
	}

	public OWLSubClassOfAxiom composeRight(OWLClassExpression cl, OWLClassExpression expression, double bound)
			throws Exception {
		myClass = cl;
		myExpression = expression;
		int k = myEngineForT.getOntology().getAxiomCount();
		while (composingRight(myClass, myExpression, bound) && (k > 0)) {
			k--;
		}
		return myEngineForT.getSubClassAxiom(myClass, myExpression);
	}

	private Boolean composingRight(OWLClassExpression cl, OWLClassExpression expression, double bound)
			throws Exception {
		boolean flag = false;
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {

				TreeSet<OWLClass> myClassSet = new TreeSet<>(nod.getLabel());
				for (OWLClass c : myClassSet) {

					Set<OWLSubClassOfAxiom> myAxiomSet = myEngineForT.getOntology().getSubClassAxiomsForSubClass(c);

					for (OWLSubClassOfAxiom mySubClassAxiom : myAxiomSet) {
						if (!nod.getLabel().contains(c)) {
							break;
						}
						ELTree oldTree = new ELTree(tree.transformToClassExpression());
						ELTree subtree = new ELTree(mySubClassAxiom.getSuperClass());
						nod.getEdges().addAll(subtree.getRootNode().getEdges());
						nod.extendLabel(subtree.getRootNode().getLabel());
						nod.remove(c);
						if ((random.nextDouble() < bound) && !myEngineForH
								.entailed(myEngineForH.getSubClassAxiom(cl, tree.transformToClassExpression()))) {
							myExpression = tree.transformToClassExpression();
							myClass = cl;
							flag = true;

							rightCompositionCounter++;
						} else {
							tree = oldTree;
						}
					}

				}

			}
		}
		return flag;
	}

	// at the moment duplicated
	public Boolean isCounterExample(OWLClassExpression left, OWLClassExpression right) {
		return myEngineForT.entailed(myEngineForT.getSubClassAxiom(left, right))
				&& !myEngineForH.entailed(myEngineForH.getSubClassAxiom(left, right));
	}

	public int getNumberUnsaturations() {
		return unsaturationCounter;
	}

	public int getNumberSaturations() {
		return saturationCounter;
	}

	public int getNumberMerging() {
		return mergeCounter;
	}

	public int getNumberBranching() {
		return branchCounter;
	}

	public int getNumberLeftComposition() {
		return leftCompositionCounter;
	}

	public int getNumberRightComposition() {
		return rightCompositionCounter;
	}

}

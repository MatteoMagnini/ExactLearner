package org.zhaowl.learner;

import java.util.TreeSet;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.zhaowl.engine.ELEngine;
import org.zhaowl.tree.ELEdge;
import org.zhaowl.tree.ELNode;
import org.zhaowl.tree.ELTree;
import org.zhaowl.utils.Metrics;

public class ELLearner {
	private int unsaturationCounter = 0;
	private int saturationCounter = 0;
	private int leftDecompositionCounter = 0;
	private int rightDecompositionCounter = 0;
	private int mergeCounter = 0;
	private int branchCounter = 0;
	private final ELEngine myEngineForT;
	private final ELEngine myEngineForH;
	private final Metrics myMetrics;
	private OWLClassExpression myExpression;
	private OWLClass myClass;

	public ELLearner(ELEngine elEngineForT, ELEngine elEngineForH, Metrics metrics) {
		myEngineForH = elEngineForH;
		myEngineForT = elEngineForT;
		myMetrics = metrics;

	}

	/**
	 * @author anaozaki Naive algorithm to return a counterexample where one of the
	 *         sides is a concept name
	 * 
	 * @param left
	 *            class expression on the left of an inclusion
	 * @param right
	 *            class expression on the right of an inclusion
	 */
	public OWLSubClassOfAxiom decompose(OWLClassExpression left, OWLClassExpression right) throws Exception {

		ELTree treeR = new ELTree(right);
		ELTree treeL = new ELTree(left);
         
		for (int i = 0; i < treeL.getMaxLevel(); i++) {

			for (ELNode nod : treeL.getNodesOnLevel(i + 1)) {

				for (OWLClass cl : myEngineForT.getClassesInSignature()) {
					myMetrics.setMembCount(myMetrics.getMembCount() + 1);
					if (isCounterExample(nod.transformToDescription(), cl))
						return myEngineForT.getSubClassAxiom(nod.transformToDescription(), cl);
				}
			}
		}

		for (int i = 0; i < treeR.getMaxLevel(); i++) {

			for (ELNode nod : treeR.getNodesOnLevel(i + 1)) {

				for (OWLClass cl : myEngineForT.getClassesInSignature()) {
					myMetrics.setMembCount(myMetrics.getMembCount() + 1);
					if (isCounterExample(cl, nod.transformToDescription()))
						return myEngineForT.getSubClassAxiom(cl, nod.transformToDescription());
				}
			}
		}

		throw new Exception("Error creating counterexample. Not an EL Terminology");
	}

	public OWLSubClassOfAxiom decomposeLeft(OWLClassExpression expression, OWLClass cl) throws Exception {
		myClass = cl;
		myExpression = expression;
		while (decomposingLeft(myExpression)) {
        }
		return myEngineForT.getSubClassAxiom(myExpression, myClass);
	}

	private Boolean decomposingLeft(OWLClassExpression expression) throws Exception {
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {
				if (!nod.isRoot()) {
					for (OWLClass cls : myEngineForT.getClassesInSignature()) {
						myMetrics.setMembCount(myMetrics.getMembCount() + 1);
						if (isCounterExample(nod.transformToDescription(), cls)) {
							myExpression = nod.transformToDescription();
							myClass = cls;
							leftDecompositionCounter++;
							return true;
						}
					}
				}
				for (int j = 0; j < nod.getEdges().size(); j++) {
                    ELTree oldTree = new ELTree(tree.transformToClassExpression());
					nod.getEdges().remove(j);
					for (OWLClass cls : myEngineForT.getClassesInSignature()) {
						myMetrics.setMembCount(myMetrics.getMembCount() + 1);
						if (isCounterExample(tree.transformToClassExpression(), cls)) {
							myExpression = tree.transformToClassExpression();
							myClass = cls;
							leftDecompositionCounter++;
							return true;
						}
					}
					tree = oldTree;
				}

			}
		}
		return false;
	}

	public OWLSubClassOfAxiom decomposeRight(OWLClass cl, OWLClassExpression expression) throws Exception {
		myClass = cl;
		myExpression = expression;
		while (decomposingRight(myClass, myExpression)) {
        }
		return myEngineForT.getSubClassAxiom(myClass, myExpression);
	}

	private Boolean decomposingRight(OWLClass cl, OWLClassExpression expression) throws Exception {

		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {
				if (!nod.isRoot()) {
					for (OWLClass cls : myEngineForT.getClassesInSignature()) {
						myMetrics.setMembCount(myMetrics.getMembCount() + 1);
						if (isCounterExample(cls, nod.transformToDescription())) {
							myExpression = nod.transformToDescription();
							myClass = cls;
							rightDecompositionCounter++;
							return true;
						}
					}
				}
				for (int j = 0; j < nod.getEdges().size(); j++) {

                    ELTree oldTree = new ELTree(tree.transformToClassExpression());

					// going over all class names because concept saturation is later
					// another option is do concept saturation first and then go over the concepts
					// in the label
					for (OWLClass cls : myEngineForT.getClassesInSignature()) {
						if (!nod.isRoot()) {

							myMetrics.setMembCount(myMetrics.getMembCount() + 1);
							if (myEngineForT.entailed(myEngineForT.getSubClassAxiom(cls,
									nod.getEdges().get(j).getNode().transformToDescription()))) {
								nod.getEdges().remove(j);
								myExpression = tree.transformToClassExpression();
								myClass = cl;
								rightDecompositionCounter++;
								return true;

							}
						}

						if (nod.isRoot()) {
                            ELTree newSubtree = new ELTree(nod.getEdges().get(j).getNode().transformToDescription());
							ELEdge newEdge = new ELEdge(nod.getEdges().get(j).getLabel(), newSubtree.getRootNode());
							for(int k=0;k<nod.getEdges().size();k++)
								nod.getEdges().remove(k);					
							nod.getEdges().add(newEdge);
							rightDecompositionCounter = rightDecompositionCounter + 3;
							if (myEngineForT.entailed(myEngineForT.getSubClassAxiom(cl, cls))
									&& !myEngineForT.entailed(myEngineForT.getSubClassAxiom(cls, cl))
									&& isCounterExample(cls, nod.transformToDescription())) {

								myExpression = nod.transformToDescription();
								myClass = cls;
								rightDecompositionCounter++;
								return true;
							}
						}
						tree = oldTree;
					}
				}
			}
		}
		return false;
	}

	public OWLSubClassOfAxiom unsaturateLeft(OWLClassExpression expression, OWLClass cl) throws Exception {
		myClass = cl;
		myExpression = expression;
		while (unsaturating(myExpression, myClass)) {
        }
		return myEngineForT.getSubClassAxiom(myExpression, myClass);
	}

	private Boolean unsaturating(OWLClassExpression expression, OWLClass cl) throws Exception {
		boolean flag = false;
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {
                OWLClassExpression cls = nod.transformToDescription();
				for (OWLClass cl1 : cls.getClassesInSignature()) {
					if (nod.getLabel().contains(cl1)&& !cl1.toString().contains("Thing")) {
						nod.remove(cl1);
						myMetrics.setMembCount(myMetrics.getMembCount() + 1);
						if (myEngineForT
								.entailed(myEngineForT.getSubClassAxiom(tree.transformToClassExpression(), cl))) {
							myExpression = tree.transformToClassExpression();
							myClass = cl;
							flag = true;
							unsaturationCounter++;
						} else {
							nod.extendLabel(cl1);
						}
					}
				}
			}
		}
		return flag;
	}

	public OWLSubClassOfAxiom saturateRight(OWLClass cl, OWLClassExpression expression) throws Exception {
		myClass = cl;
		myExpression = expression;
		while (saturating(myClass, myExpression)) {
        }
		return myEngineForT.getSubClassAxiom(myClass, myExpression);
	}

	private Boolean saturating(OWLClass cl, OWLClassExpression expression) throws Exception {

		boolean flag = false;
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {
				for (OWLClass cl1 : myEngineForT.getClassesInSignature()) {
					if (!nod.getLabel().contains(cl1)) {
						nod.extendLabel(cl1);
						myMetrics.setMembCount(myMetrics.getMembCount() + 1);
						if (myEngineForT
								.entailed(myEngineForT.getSubClassAxiom(cl, tree.transformToClassExpression()))) {
							myExpression = tree.transformToClassExpression();
							myClass = cl;
							flag = true;
							saturationCounter++;
						} else {
							nod.remove(cl1);
						}
					}
				}
			}
		}
		return flag;
	}

	public OWLSubClassOfAxiom mergeRight(OWLClass cl, OWLClassExpression expression) throws Exception {
		myClass = cl;
		myExpression = expression;
		while (merging(myClass, myExpression)) {
        }
		return myEngineForT.getSubClassAxiom(myClass, myExpression);
	}

	private Boolean merging(OWLClass cl, OWLClassExpression expression) throws Exception {
		boolean flag = false;
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {

				if (!nod.getEdges().isEmpty() && nod.getEdges().size() > 1) {

					for (int j = 0; j < nod.getEdges().size(); j++) {

						for (int k = 0; k < nod.getEdges().size(); k++) {
                            ELTree oldTree = new ELTree(tree.transformToClassExpression());
							if (j != k && nod.getEdges().get(j).getStrLabel()
									.equals(nod.getEdges().get(k).getStrLabel())) {
								nod.getEdges().get(j).getNode().getLabel()
										.addAll(nod.getEdges().get(k).getNode().getLabel());
								if (!nod.getEdges().get(k).getNode().getEdges().isEmpty())
									nod.getEdges().get(j).getNode().getEdges()
											.addAll(nod.getEdges().get(k).getNode().getEdges());
								nod.getEdges().remove(nod.getEdges().get(k));

								myMetrics.setMembCount(myMetrics.getMembCount() + 1);
								if (myEngineForT.entailed(
										myEngineForT.getSubClassAxiom(cl, tree.transformToClassExpression()))) {
									myExpression = tree.transformToClassExpression();
									myClass = cl;
									flag = true;
									mergeCounter++;
								} else {
									tree = oldTree;
								}

							}
						}
					}

				}

			}
		}
		return flag;
	}

	public OWLSubClassOfAxiom branchLeft(OWLClassExpression expression, OWLClass cl) throws Exception {
		myClass = cl;
		myExpression = expression;
		while (branching(myExpression, myClass)) {
        }
		return myEngineForT.getSubClassAxiom(myExpression, myClass);
	}

	private Boolean branching(OWLClassExpression expression, OWLClass cl) throws Exception {

		boolean flag = false;
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {
				if (!nod.getEdges().isEmpty()) {

					for (int j = 0; j < nod.getEdges().size(); j++) {
						if (nod.getEdges().get(j).getNode().getLabel().size() > 1) {
							TreeSet<OWLClass> s=new  TreeSet<OWLClass>();
                        	s.addAll(nod.getEdges().get(j).getNode().getLabel());
							for (OWLClass lab : s) {
                                ELTree oldTree = new ELTree(tree.transformToClassExpression());
                                ELTree newSubtree = new ELTree(nod.getEdges().get(j).getNode().transformToDescription());
                                TreeSet<OWLClass> ts=new  TreeSet<OWLClass>();
                                ts.addAll(newSubtree.getRootNode().getLabel());
                                for (OWLClass l : ts) {
									newSubtree.getRootNode().getLabel().remove(l);
								}	
								newSubtree.getRootNode().extendLabel(lab);
                                ELEdge newEdge = new ELEdge(nod.getEdges().get(j).getLabel(), newSubtree.getRootNode());
								nod.getEdges().add(newEdge);
								nod.getEdges().get(j).getNode().remove(lab);
								myMetrics.setMembCount(myMetrics.getMembCount() + 1);
								if (myEngineForT.entailed(
										myEngineForT.getSubClassAxiom(tree.transformToClassExpression(), cl))) {
									myExpression = tree.transformToClassExpression();
									myClass = cl;
									flag = true;
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
		return flag;
	}

	// at the moment duplicated
	private Boolean isCounterExample(OWLClassExpression left, OWLClassExpression right) {
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

	public int getNumberLeftDecomposition() {
		return leftDecompositionCounter;
	}

	public int getNumberRightDecomposition() {
		return rightDecompositionCounter;
	}
}

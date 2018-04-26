package org.zhaowl.oracle;

 
import java.util.Random;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.zhaowl.engine.ELEngine;
import org.zhaowl.tree.ELEdge;
import org.zhaowl.tree.ELNode;
import org.zhaowl.tree.ELTree;


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


    public ELOracle(ELEngine elEngineForT, ELEngine elEngineForH) {
		myEngineForT = elEngineForT;
		myEngineForH = elEngineForH;
	}

	public OWLSubClassOfAxiom unsaturateRight(OWLClassExpression cl, OWLClassExpression expression, double bound) throws Exception {
		myClass = cl;
		myExpression = expression;
		while (unsaturating(myClass,myExpression, bound)) {
        }
		return myEngineForT.getSubClassAxiom(myClass,myExpression);
	}

	private Boolean unsaturating(OWLClassExpression cl, OWLClassExpression expression, double bound) throws Exception {
		boolean flag = false;
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {
                OWLClassExpression cls = nod.transformToDescription();
				for (OWLClass cl1 : cls.getClassesInSignature()) {
					if ((random.nextDouble() < bound)&&(nod.getLabel().contains(cl1)&& !cl1.toString().contains("Thing"))) {
						nod.remove(cl1);
						 
						if (!myEngineForH
								.entailed(myEngineForH.getSubClassAxiom(cl,tree.transformToClassExpression()))) {
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
	
	public OWLSubClassOfAxiom saturateLeft(OWLClassExpression expression, OWLClassExpression cl, double bound) throws Exception {
		myClass = cl;
		myExpression = expression;
		while (saturating(myExpression,myClass, bound)) {
        }
		return myEngineForT.getSubClassAxiom(myExpression,myClass);
	}

	private Boolean saturating(OWLClassExpression expression, OWLClassExpression cl, double bound) throws Exception {

		boolean flag = false;
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {
				for (OWLClass cl1 : myEngineForT.getClassesInSignature()) {
					if ((random.nextDouble() < bound) && !nod.getLabel().contains(cl1)) {
						nod.extendLabel(cl1);
						 
						if (!myEngineForH
								.entailed(myEngineForH.getSubClassAxiom( tree.transformToClassExpression(),cl))) {
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
 
	public OWLSubClassOfAxiom mergeLeft(OWLClassExpression expression, OWLClassExpression cl, double bound) throws Exception {
		myClass = cl;
		myExpression = expression;
		while (merging(myExpression,myClass, bound)) {
        }
		return myEngineForT.getSubClassAxiom(myExpression,myClass);
	}

	private Boolean merging(OWLClassExpression expression, OWLClassExpression cl, double bound) throws Exception {
		boolean flag = false;
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {

				if (!nod.getEdges().isEmpty() && nod.getEdges().size() > 1) {

					for (int j = 0; j < nod.getEdges().size(); j++) {

						for (int k = 0; k < nod.getEdges().size(); k++) {
                            ELTree oldTree = new ELTree(tree.transformToClassExpression());
							if ((random.nextDouble() < bound)&& (j != k && nod.getEdges().get(j).getStrLabel()
									.equals(nod.getEdges().get(k).getStrLabel()))) {
								nod.getEdges().get(j).getNode().getLabel()
										.addAll(nod.getEdges().get(k).getNode().getLabel());
								if (!nod.getEdges().get(k).getNode().getEdges().isEmpty())
									nod.getEdges().get(j).getNode().getEdges()
											.addAll(nod.getEdges().get(k).getNode().getEdges());
								nod.getEdges().remove(nod.getEdges().get(k));

								 
								if (!myEngineForH.entailed(
										myEngineForH.getSubClassAxiom(tree.transformToClassExpression(),cl))) {
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
	
	public OWLSubClassOfAxiom branchRight(OWLClassExpression cl, OWLClassExpression expression, double bound) throws Exception {
		myClass = cl;
		myExpression = expression;
		while (branching(myClass,myExpression, bound)) {
        }
		return myEngineForT.getSubClassAxiom(myClass,myExpression);
	}

	private Boolean branching(OWLClassExpression cl, OWLClassExpression expression, double bound) throws Exception {

		boolean flag = false;
		ELTree tree = new ELTree(expression);
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {
				if (!nod.getEdges().isEmpty()) {

					for (int j = 0; j < nod.getEdges().size(); j++) {
						if (nod.getEdges().get(j).getNode().getLabel().size() > 1) {
							 
							for (OWLClass lab : nod.getEdges().get(j).getNode().getLabel()) {
                                ELTree oldTree = new ELTree(tree.transformToClassExpression());
                                ELTree newSubtree = new ELTree(nod.getEdges().get(j).getNode().transformToDescription());
								for (OWLClass l : newSubtree.getRootNode().getLabel())
									newSubtree.getRootNode().remove(l);
								newSubtree.getRootNode().extendLabel(lab);
                                ELEdge newEdge = new ELEdge(nod.getEdges().get(j).getLabel(), newSubtree.getRootNode());
								nod.getEdges().add(newEdge);
								nod.getEdges().get(j).getNode().remove(lab);
								
								if ((random.nextDouble() < bound) && !myEngineForH.entailed(
										myEngineForH.getSubClassAxiom(cl,tree.transformToClassExpression()))) {
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

	
	public OWLSubClassOfAxiom composeLeft(OWLClassExpression expression,OWLClassExpression cl, double bound) throws Exception {
		myClass = cl;
		myExpression = expression;
		while (composingLeft(myExpression,myClass)) {
        }
		return myEngineForT.getSubClassAxiom(myExpression,myClass);
	}

	private Boolean composingLeft(OWLClassExpression expression, OWLClassExpression cl) throws Exception {
		Boolean flag=false; 
		
		return flag;
	} 
	
	public OWLSubClassOfAxiom composeRight(OWLClassExpression cl, OWLClassExpression expression, double bound) throws Exception {
		myClass = cl;
		myExpression = expression;
		while (composingRight(myClass,myExpression)) {
        }
		return myEngineForT.getSubClassAxiom(myClass,myExpression);
	}

	private Boolean composingRight(OWLClassExpression cl, OWLClassExpression expression) throws Exception {
		Boolean flag=false; 
		
		return flag;
	} 
	
//at the moment duplicated
	public Boolean isCounterExample(OWLClassExpression left, OWLClassExpression right){
		return  myEngineForT.entailed(myEngineForT.getSubClassAxiom(left, right))
				&&
				!myEngineForH.entailed(myEngineForH.getSubClassAxiom(left, right));
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

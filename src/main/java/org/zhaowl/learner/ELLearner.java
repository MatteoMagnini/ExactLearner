package org.zhaowl.learner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.zhaowl.console.consoleLearner;
import org.zhaowl.engine.ELEngine;
import org.zhaowl.tree.ELNode;
import org.zhaowl.tree.ELTree;

public class ELLearner {
    private int unsaturationCounter=0;
    private int saturationCounter=0;
    private int mergeCounter=0;
    private int branchCounter=0;
	private final ELEngine myEngineForT;
	private final ELEngine myEngineForH;
	private final consoleLearner myConsole;
	private OWLClassExpression myExpression; 
	private OWLClass myClass;
 

	public ELLearner(ELEngine elEngineForT, ELEngine elEngineForH, consoleLearner console) {
		myEngineForH = elEngineForH;
		myEngineForT = elEngineForT;
		myConsole = console;
 
	}
 
	
	 /** 
	 * @author anaozaki 
	 * Naive algorithm to return a counterexample where one of the sides is a concept name 
     * 
     * @param left
     *            class expression on the left of an inclusion
     * @param right
     *            class expression on the right of an inclusion*/
	public OWLAxiom decompose(OWLClassExpression left, OWLClassExpression right) throws Exception {

			ELTree treeR = new ELTree(right);
			ELTree treeL = new ELTree(left);
			 
			for (int i = 0; i < treeL.getMaxLevel(); i++) {
				 
				for (ELNode nod : treeL.getNodesOnLevel(i + 1)) {

					for (OWLClass cl  : myEngineForT.getClassesInSignature()) {
						myConsole.membCount++;
						if( isCounterExample(nod.transformToDescription(),cl))
						 return myEngineForT.getSubClassAxiom( nod.transformToDescription(),cl);
					}
				}
			}

			for (int i = 0; i < treeR.getMaxLevel(); i++) {
				 
				for (ELNode nod : treeR.getNodesOnLevel(i + 1)) {

					for (OWLClass cl  : myEngineForT.getClassesInSignature()) {
						myConsole.membCount++;
						if( isCounterExample(cl,nod.transformToDescription()))
							return myEngineForT.getSubClassAxiom(cl, nod.transformToDescription());
					}
				}
			}
			
			throw new Exception("Error creating counterexample. Not an EL Terminology");				 
	}

	public OWLAxiom decomposeLeft(OWLClassExpression left, OWLClass  right) throws Exception {

		 //TODO!!
		OWLAxiom axiom=null;
		 
			ELTree treeR = new ELTree(right);
			ELTree treeL = new ELTree(left);
			 

			for (int i = 0; i < treeL.getMaxLevel(); i++) {
				 
				for (ELNode nod : treeL.getNodesOnLevel(i + 1)) {

					for (OWLClass cl  : myEngineForT.getClassesInSignature()) {
						if( isCounterExample(  nod.transformToDescription(),cl))
						 myConsole.addHypothesis(myEngineForT.getSubClassAxiom( nod.transformToDescription(),cl));
					}
				}
			}

			for (int i = 0; i < treeR.getMaxLevel(); i++) {
				 
				for (ELNode nod : treeR.getNodesOnLevel(i + 1)) {

					for (OWLClass cl  : myEngineForT.getClassesInSignature()) {
						if( isCounterExample( cl,nod.transformToDescription()))
						 myConsole.addHypothesis(myEngineForT.getSubClassAxiom(cl, nod.transformToDescription()));
					}
				}
			}
			 
			return axiom;
	}
	
	public OWLAxiom decomposeRight(OWLClass left, OWLClassExpression right) throws Exception {

		 //TODO!!
		OWLAxiom axiom=null;
		 
			ELTree treeR = new ELTree(right);
			ELTree treeL = new ELTree(left);
			 

			for (int i = 0; i < treeL.getMaxLevel(); i++) {
				 
				for (ELNode nod : treeL.getNodesOnLevel(i + 1)) {

					for (OWLClass cl  : myEngineForT.getClassesInSignature()) {
						if( isCounterExample(  nod.transformToDescription(),cl))
						 myConsole.addHypothesis(myEngineForT.getSubClassAxiom( nod.transformToDescription(),cl));
					}
				}
			}

			for (int i = 0; i < treeR.getMaxLevel(); i++) {
				 
				for (ELNode nod : treeR.getNodesOnLevel(i + 1)) {

					for (OWLClass cl  : myEngineForT.getClassesInSignature()) {
						if( isCounterExample( cl,nod.transformToDescription()))
						 myConsole.addHypothesis(myEngineForT.getSubClassAxiom(cl, nod.transformToDescription()));
					}
				}
			}
			 
			return axiom;
	}
	
	public OWLAxiom unsaturateLeft(OWLClassExpression expression, OWLClass cl) throws Exception {
		myClass=cl;
		myExpression=expression;
		while(unsaturating(myExpression,myClass)) {
			continue;
		}
        return myEngineForT.getSubClassAxiom( myExpression,myClass);	 
	}
    
	private Boolean unsaturating(OWLClassExpression expression, OWLClass cl) throws Exception {
		OWLClassExpression cls=null;		 
		boolean flag=false;
		ELTree tree = new ELTree(expression);	 
		for (int i = 0; i < tree.getMaxLevel(); i++) {	 
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {           
				cls = nod.transformToDescription();
                for(OWLClass cl1 : cls.getClassesInSignature()) {
                	if(nod.getLabel().contains(cl1)) {
                		nod.remove(cl1);
                		myConsole.membCount++;
                		if(myEngineForT.entailed(myEngineForT.getSubClassAxiom(tree.transformToClassExpression(),cl))) {
                			myExpression=tree.transformToClassExpression();
                			myClass=cl;
                			flag=true;
                			unsaturationCounter++;
                		}	else {
                			nod.extendLabel(cl1);
                		}
                	}
                }		 
			}
		} 
        return flag;	 
	}
	
	public OWLAxiom saturateRight(OWLClass cl, OWLClassExpression expression) throws Exception {
		myClass=cl;
		myExpression=expression;
		while(saturating(myClass,myExpression)) {
			continue;
		}
        return myEngineForT.getSubClassAxiom(myClass, myExpression);	 
	}
    
	private Boolean saturating(OWLClass cl, OWLClassExpression expression) throws Exception {
		 		 
		boolean flag=false;
		ELTree tree = new ELTree(expression);	 
		for (int i = 0; i < tree.getMaxLevel(); i++) {
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {           		 
                for(OWLClass cl1 : myEngineForT.getClassesInSignature()) {
                	if(!nod.getLabel().contains(cl1)) {
                		nod.extendLabel(cl1);
                		myConsole.membCount++;
                		if(myEngineForT.entailed(myEngineForT.getSubClassAxiom(cl,tree.transformToClassExpression()))) {
                			myExpression=tree.transformToClassExpression();
                			myClass=cl;
                			flag=true;
                			saturationCounter++;
                		}	else {
                			nod.remove(cl1);
                		}
                	}
                }		 
			}
		} 
        return flag;	 
	}
	
 
	public OWLAxiom mergeRight(OWLClass cl, OWLClassExpression expression) throws Exception {
		myClass=cl;
		myExpression=expression;
		while(merging(myClass,myExpression)) {
			continue;
		}
        return myEngineForT.getSubClassAxiom(myClass, myExpression);	 
	}
    
	private Boolean merging(OWLClass cl, OWLClassExpression expression) throws Exception {
		ELTree oldTree=null; 		 
		boolean flag=false;
		ELTree tree = new ELTree(expression);	 
		for (int i = 0; i < tree.getMaxLevel(); i++) {	 
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {  
				
				if (!nod.getEdges().isEmpty() && nod.getEdges().size() > 1) {

					for (int j = 0; j < nod.getEdges().size(); j++) {
						
						for (int k = 0; k < nod.getEdges().size(); k++) {
							oldTree=new ELTree(tree.transformToClassExpression()); 
							if (j!=k && nod.getEdges().get(j).getStrLabel().equals(nod.getEdges().get(k).getStrLabel())) {
								nod.getEdges().get(j).getNode().getLabel().addAll(nod.getEdges().get(k).getNode().getLabel());
								if (!nod.getEdges().get(k).getNode().getEdges().isEmpty())
									nod.getEdges().get(j).getNode().getEdges().addAll(nod.getEdges().get(k).getNode().getEdges());
								nod.getEdges().remove(nod.getEdges().get(k));
								// check if new merged tree is entailed by T
								myConsole.membCount++;
		                		if(myEngineForT.entailed(myEngineForT.getSubClassAxiom(cl,tree.transformToClassExpression()))) {
		                			myExpression=tree.transformToClassExpression();
		                			myClass=cl;
		                			flag=true;
		                			mergeCounter++;
		                		}	else {
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
 
	public OWLAxiom branchLeft(OWLClassExpression left, OWLClass right) {
		OWLAxiom axiom =null;
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
		return axiom;
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
	//at the moment duplicated
	private Boolean isCounterExample(OWLClassExpression left, OWLClassExpression right){
			return  myEngineForT.entailed(myEngineForT.getSubClassAxiom(left, right))
					&&
					!myEngineForH.entailed(myEngineForH.getSubClassAxiom(left, right));
		}
}

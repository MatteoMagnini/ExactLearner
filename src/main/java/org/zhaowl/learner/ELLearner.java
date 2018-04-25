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
import org.zhaowl.tree.ELEdge;
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
 
	public OWLAxiom branchLeft(OWLClassExpression expression, OWLClass cl) throws Exception {
		myClass=cl;
		myExpression=expression;
		while(branching(myExpression,myClass)) {
			continue;
		}
        return myEngineForT.getSubClassAxiom( myExpression,myClass);	 
	}
    
	private Boolean branching(OWLClassExpression expression, OWLClass cl) throws Exception {
		 	 
		boolean flag=false;
		ELTree oldTree =null;
		ELTree newSubtree =null;
		ELTree tree = new ELTree(expression);
		ELEdge newEdge=null;
		for (int i = 0; i < tree.getMaxLevel(); i++) {	 
			for (ELNode nod : tree.getNodesOnLevel(i + 1)) {           
				if (!nod.getEdges().isEmpty()) {
					
					for (int j = 0; j < nod.getEdges().size(); j++) {
						if(nod.getEdges().get(j).getNode().getLabel().size()>1)				
							for (OWLClass lab : nod.getEdges().get(j).getNode().getLabel()) {
								oldTree=new ELTree(tree.transformToClassExpression()); 
								newSubtree=new ELTree(nod.getEdges().get(j).getNode().transformToDescription());
								for(OWLClass l: newSubtree.getRootNode().getLabel())
									newSubtree.getRootNode().remove(l);
								newSubtree.getRootNode().extendLabel(lab);
								newEdge=new ELEdge(nod.getEdges().get(j).getLabel(),newSubtree.getRootNode());
								nod.getEdges().add(newEdge); 
								nod.getEdges().get(j).getNode().remove(lab);
								myConsole.membCount++;
		                		if(myEngineForT.entailed(myEngineForT.getSubClassAxiom(tree.transformToClassExpression(),cl))) {
		                			myExpression=tree.transformToClassExpression();
		                			myClass=cl;
		                			flag=true;
		                			branchCounter++;
		                		}	else {
		                			tree = oldTree;
		                		}
							}
						 
					}

				}
			}
		} 
        return flag;	 
	}
	
	 

	//at the moment duplicated
	private Boolean isCounterExample(OWLClassExpression left, OWLClassExpression right){
			return  myEngineForT.entailed(myEngineForT.getSubClassAxiom(left, right))
					&&
					!myEngineForH.entailed(myEngineForH.getSubClassAxiom(left, right));
		}
}

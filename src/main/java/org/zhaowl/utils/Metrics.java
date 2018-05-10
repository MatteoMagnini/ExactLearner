package org.zhaowl.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;

import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public class Metrics {
	private final OWLObjectRenderer myRenderer;
	private int membCount = 0;
	private int equivCount = 0;
	private int sizeOfLargestConcept = 0;
	 
	private int depthOfLargestConcept = 0;
	 
	private int sizeOfHypothesis = 0;
	private int sizeOfTarget = 0;

	public Metrics(OWLObjectRenderer renderer) {
		this.myRenderer = renderer;
	}

	public int sizeOfCIT(Set<OWLLogicalAxiom> axSet) {

		int ontSize = 0;

		for (OWLAxiom axe : axSet) {

			String inclusion = myRenderer.render(axe);

			if (inclusion.contains("SubClassOf") || inclusion.contains("EquivalentTo")) {
				inclusion = inclusion.replaceAll(" and ", " ");
				inclusion = inclusion.replaceAll(" some ", " ");
				inclusion = inclusion.replaceAll("SubClassOf", " ");
				inclusion = inclusion.replaceAll("EquivalentTo", " ");
				ontSize += inclusion.split(" ").length;
			}

		}
		return ontSize;
	}
	
	public int sizeOfConcept(Set<OWLLogicalAxiom> axSet) {
		int largestConceptSize = 0;
		 
		for (OWLAxiom axe : axSet) {
			if (axe.isOfType(AxiomType.SUBCLASS_OF)) {
				OWLSubClassOfAxiom axiom = (OWLSubClassOfAxiom) axe;
				axiom.getSubClass();

				String left = myRenderer.render(axiom.getSubClass());
				String right = myRenderer.render(axiom.getSuperClass());
				 
				left = left.replaceAll(" and ", " ");
				left = left.replaceAll(" some ", " ");

				if (left.split(" ").length > largestConceptSize)			 
					largestConceptSize = left.split(" ").length;

				right = right.replaceAll(" and ", " ");
				right = right.replaceAll(" some ", " ");
				if (right.split(" ").length > largestConceptSize)	 
					largestConceptSize = right.split(" ").length;

			}
			if (axe.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
				OWLEquivalentClassesAxiom axiom = (OWLEquivalentClassesAxiom) axe;
				String concept; 
				for(OWLClassExpression exp: axiom.getClassExpressions()) {
					concept = myRenderer.render(exp);
					concept = concept.replaceAll(" and ", " ");
					concept = concept.replaceAll(" some ", " ");
				if (concept.split(" ").length > largestConceptSize)			 
					largestConceptSize = concept.split(" ").length;
				}
			}
		}
		return  largestConceptSize;
				
	}

	public ArrayList<String> getSuggestionNames(String s, File newFile) throws IOException {

		ArrayList<String> names = new ArrayList<>();

		FileInputStream in = new FileInputStream(newFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		String line = reader.readLine();
		if (s.equals("concept")) {
			while (line != null) {
				if (line.startsWith("Class:")) {
					String conceptName = line.substring(7);
					if (!conceptName.equals("owl:Thing")) {
						names.add(conceptName);
					}
				}
				line = reader.readLine();
			}
		} else if (s.equals("role")) {
			while (line != null) {
				if (line.startsWith("ObjectProperty:")) {
					String roleName = line.substring(16);
					names.add(roleName);

				}

				line = reader.readLine();
			}
		}
		reader.close();
		return names;
	}

	public int getMembCount() {
		return membCount;
	}

	public void setMembCount(int membCount) {
		this.membCount = membCount;
	}

	public int getEquivCount() {
		return equivCount;
	}

	public void setEquivCount(int equivCount) {
		this.equivCount = equivCount;
	}

	public int getSizeOfLargestConcept() {
		return sizeOfLargestConcept;
	}

	public void setSizeOfLargestConcept(int sizeOfLargestConcept) {
		this.sizeOfLargestConcept = sizeOfLargestConcept;
	}

	 

	public int getDepthOfLargestConcept() {
		return depthOfLargestConcept;
	}

	public void setDepthOfLargestConcept(int depthOfLargestConcept) {
		this.depthOfLargestConcept = depthOfLargestConcept;
	}

	 

	public int getSizeOfHypothesis() {
		return sizeOfHypothesis;
	}

	public void setSizeOfHypothesis(int sizeOfHypothesis) {
		this.sizeOfHypothesis = sizeOfHypothesis;
	}

	public int getSizeOfTarget() {
		return sizeOfTarget;
	}

	public void setSizeOfTarget(int sizeOfTarget) {
		this.sizeOfTarget = sizeOfTarget;
	}

	public void computeTargetSizes(Set<OWLLogicalAxiom> logicalAxioms) {
		this.setSizeOfTarget(sizeOfCIT(logicalAxioms));
		this.setSizeOfLargestConcept(sizeOfConcept(logicalAxioms));
		 
	}

	public void computeHypothesisSizes(Set<OWLLogicalAxiom> logicalAxioms) {
		this.sizeOfHypothesis = sizeOfCIT(logicalAxioms);
 

	}
}

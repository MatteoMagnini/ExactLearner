package org.zhaowl.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;

import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.OWLAxiom;

public class Metrics {
	private final OWLObjectRenderer myRenderer;
    private int membCount = 0;
    private int equivCount = 0;
 
	public Metrics(OWLObjectRenderer renderer)
	{
		this.myRenderer = renderer;
	}
 

	public int sizeOfCIT(Set<OWLAxiom> axSet, boolean x) {

		return showCIs(axSet, x);
	}

	private int showCIs(Set<OWLAxiom> axSet, boolean x) {
	 
		int smallestSize = 0;
		OWLAxiom smallestOne = null;
		int ontSize = 0;
 
		for (OWLAxiom axe : axSet) {
			 
			String inclusion = myRenderer.render(axe);
			inclusion = inclusion.replaceAll(" and ", " ");
			inclusion = inclusion.replaceAll(" some ", " ");
			if (axe.toString().contains("SubClassOf"))
				inclusion = inclusion.replaceAll("SubClassOf", "");
			else
				inclusion = inclusion.replaceAll("EquivalentTo", "");
			inclusion = inclusion.replaceAll(" and ", "");
			 
			String[] arrIncl = inclusion.split(" ");
			int totalSize = 0;
			for (String anArrIncl : arrIncl)
				if (anArrIncl.length() > 1)
					totalSize++;

			if (smallestOne == null) {
				smallestOne = axe;
				smallestSize = totalSize;
			} else {
				if (smallestSize >= totalSize) {
					smallestOne = axe;
					smallestSize = totalSize;
				}
			}
			ontSize += totalSize;
			 
		}
		return ontSize;
//
//		if(x)
//			System.out.println("Size of T: " + ontSize);
//		else
//			System.out.println("Size of H: " + ontSize);
	}

// --Commented out by Inspection START (30/04/2018, 15:29):
//	public void showCIH(Set<OWLAxiom> axSet) {
//		showCIs(axSet, false);
//	}
// --Commented out by Inspection STOP (30/04/2018, 15:29)

 

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
	
// --Commented out by Inspection START (30/04/2018, 15:29):
//	public int[] showCISizes(Set<OWLAxiom> axSet)
//	{
//		int[] returns = new int[2];
//
//		int sumSize = 0;
//
//		OWLAxiom smallestOne = null;
//		int smallestSize = 0;
//		for (OWLAxiom axe : axSet) {
//			String inclusion = myRenderer.render(axe);
//			inclusion = inclusion.replaceAll(" and ", " ");
//			inclusion = inclusion.replaceAll(" some ", " ");
//
//			if(axe.toString().contains("SubClassOf"))
//				inclusion = inclusion.replaceAll("SubClassOf", "");
//			else
//				inclusion = inclusion.replaceAll("EquivalentTo", "");
//
//			String[] arrIncl = inclusion.split(" ");
//            int totalSize = 0;
//
//			for (String anArrIncl : arrIncl)
//				if (anArrIncl.length() > 0 && !anArrIncl.equals("some"))
//					totalSize++;
//
//
//			if(smallestOne == null) {
//				smallestOne = axe;
//				smallestSize = totalSize;
//			}
//			else
//			{
//				if(smallestSize > totalSize)
//				{
//					smallestOne = axe;
//					smallestSize = totalSize;
//				}
//			}
//
//			sumSize += totalSize;
//
//		}
//		System.out.println("Smallest logical axiom: " + myRenderer.render(smallestOne));
//		System.out.println("Size is: " + smallestSize);
//		returns[0] = smallestSize;
//		returns[1] = sumSize / axSet.size();
//		return returns;
//	}
// --Commented out by Inspection STOP (30/04/2018, 15:29)

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
}

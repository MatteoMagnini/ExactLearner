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
// --Commented out by Inspection START (25/04/2018, 16:31):
//	public Metrics()
//	{
//
//	}
// --Commented out by Inspection STOP (25/04/2018, 16:31)

	public void showCIT(Set<OWLAxiom> axSet, boolean x) {

		showCIs(axSet, x);
	}

	private void showCIs(Set<OWLAxiom> axSet, boolean x) {
		int avgSize = 0;
		int sumSize = 0;
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
			sumSize += totalSize;
		}
		if(x)
			System.out.println("Size of T: " + ontSize);
		else
			System.out.println("Size of H: " + ontSize);
	}

	public void showCIH(Set<OWLAxiom> axSet) {
		showCIs(axSet, false);
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
	
	public int[] showCISizes(Set<OWLAxiom> axSet)
	{
		int[] returns = new int[2];
		
		int sumSize = 0;
		
		OWLAxiom smallestOne = null;
		int smallestSize = 0;
		for (OWLAxiom axe : axSet) {
			String inclusion = myRenderer.render(axe);
			inclusion = inclusion.replaceAll(" and ", " ");
			inclusion = inclusion.replaceAll(" some ", " ");
			
			if(axe.toString().contains("SubClassOf"))
				inclusion = inclusion.replaceAll("SubClassOf", "");
			else
				inclusion = inclusion.replaceAll("EquivalentTo", ""); 
			//System.out.println(inclusion);
			String[] arrIncl = inclusion.split(" ");
            int totalSize = 0;

			for (String anArrIncl : arrIncl)
				if (anArrIncl.length() > 0 && !anArrIncl.equals("some"))
					totalSize++;
			
			//for(int i = 0; i < arrIncl.length; i++)
			//	System.out.println(arrIncl[i] + "=====" +arrIncl[i].length());
			
			//System.out.println(totalSize);
			if(smallestOne == null) {
				smallestOne = axe;
				smallestSize = totalSize;
			}
			else
			{
				if(smallestSize > totalSize)
				{
					smallestOne = axe;
					smallestSize = totalSize;
				}
			}
				
			sumSize += totalSize;
			//System.out.println("Size of : " + rendering.render(axe) + "." + totalSize);
		}
		System.out.println("Smallest logical axiom: " + myRenderer.render(smallestOne));
		System.out.println("Size is: " + smallestSize);
		returns[0] = smallestSize;
		returns[1] = sumSize / axSet.size();
		return returns;
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
}

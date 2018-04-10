package org.zhaowl.userInterface;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.zhaowl.learner.ELLearner;
import org.zhaowl.oracle.ELOracle;
import org.zhaowl.utils.SimpleClass;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;

public class ELInterface extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */

	// ************ START FRAME SPECIFIC VARIABLES ********************* //
	public JList list = new JList();
	public JTextArea hypoField = new JTextArea();
	public JLabel memberCount = new JLabel("Total membership queries: 0");
	public JLabel equivalenceCount = new JLabel("Total equivalence queries: 0");

	public JLabel entailed = new JLabel("Entailed: ----");
	public JLabel loadedOnto = new JLabel("No ontology loaded");
	public int membCount = 0;
	public int equivCount = 0;
	private JTextField conc2;
	private JTextField conc1;
	public Boolean win = false;
	public Boolean wePlayin = false;

	public JList list_1 = new JList();
	public JCheckBox oracleSaturate = new JCheckBox("Saturate");
	public JCheckBox oracleMerge;
	public JCheckBox oracleBranch;
	public JCheckBox oracleUnsaturate;

	public JCheckBox learnerSat;
	public JCheckBox learnerMerge;
	public JCheckBox learnerDecompL;
	public JCheckBox learnerUnsat;
	public JCheckBox learnerBranch;
	public JCheckBox learnerDecompR;
	public JCheckBox ezBox;
	public JCheckBox autoBox = new JCheckBox("Auto learn [might take some moments]");
	public JCheckBox fileLoad;
	private final JScrollPane scrollPane_1 = new JScrollPane();
	public JTextField filePath;

	// ************ END FRAME SPECIFIC VARIABLES ********************* //

	// ************ START OWL SPECIFIC VARIABLES ********************* //.
	public OWLOntologyManager manager;
	public ManchesterOWLSyntaxOWLObjectRendererImpl rendering;

	public OWLReasoner reasonerForT;
	public Set<OWLAxiom> axiomsT;
	public ELEngine ELQueryEngineForT;
	public String ontologyFolder;
	public String ontologyName;
	public File hypoFile;
	public File newFile;

	public ArrayList<String> concepts = new ArrayList<String>();
	public ArrayList<String> roles = new ArrayList<String>();

	public Set<OWLClass> cIo = null;

	public OWLReasoner reasonerForH;
	public ShortFormProvider shortFormProvider;
	public Set<OWLAxiom> axiomsH;
	public String ontologyFolderH;
	public OWLOntology ontology;
	public OWLOntology ontologyH;
	public OWLAxiom lastCE = null;
	private JLabel averageCI;
	private JLabel smallestCI;

	public OWLAxiom smallestOne = null;
	public int smallestSize = 0;
	// ************ END OWL SPECIFIC VARIABLES ********************* //
	
	

	// ************ START CONSOLE SPECIFIC VARIABLES ********************* //

	public boolean consoleLoad = false;
	public String consoleOntologyPath = "";
	
	// ************* END CONSOLE SPECIFIC VARIABLES ********************* //
	public long timeStart = 0;
	public long timeEnd = 0;
	private JLabel lblNewLabel_3;
	public static void main(String[] args) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
				try {
					ELInterface frame = new ELInterface();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			});
		
	}

	/**
	 * Create the frame.
	 */

	public void consoleLaunch(String[] args)
	{
		fileLoad.setSelected(true);
		filePath.setText(args[0]);
		setLearnerSkills(args);
		try {
			loadOntology();
			try {
				learner();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Total membership queries: " + membCount); 
		System.out.println("Total equivalence queries: " + equivCount);
		System.out.println("Target TBox size: " + axiomsT.size());
		System.out.println("Hypothesis TBox size: " + ontologyH.getAxioms().size());
	}
	
	 
	public void setLearnerSkills(String[] args)
	{
		if (args[1].equals("t"))
			learnerDecompL.setSelected(true);
		else
			learnerDecompL.setSelected(false);

		if (args[2].equals("t"))
			learnerBranch.setSelected(true);
		else
			learnerBranch.setSelected(false);

		if (args[3].equals("t"))
			learnerUnsat.setSelected(true);
		else
			learnerUnsat.setSelected(false);

		if (args[4].equals("t"))
			learnerDecompR.setSelected(true);
		else
			learnerDecompR.setSelected(false);

		if (args[5].equals("t"))
			learnerMerge.setSelected(true);
		else
			learnerMerge.setSelected(false);

		if (args[6].equals("t"))
			learnerSat.setSelected(true);
		else
			learnerSat.setSelected(false);
	}
	
	public ELInterface() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 877, 518);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JButton btnNewButton = new JButton("Exit");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.runFinalization();
				System.exit(0);
			}
		});
		btnNewButton.setBounds(672, 450, 89, 23);
		contentPane.add(btnNewButton);

		JButton btnNewButton_1 = new JButton("Load");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					loadOntology();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		btnNewButton_1.setBounds(10, 450, 131, 23);
		contentPane.add(btnNewButton_1);

		JButton btnNewButton_2 = new JButton("Membership query");
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!conc1.getText().isEmpty() && !conc2.getText().isEmpty()) {
					Boolean memb = false;
					try {
						memb = membershipQuery(conc1.getText(), conc2.getText());
					} catch (Exception e1) {
						System.out.println("Error in membership query");
					}
					if (memb) {
						entailed.setText("Entailed: Yes");
						membCount++;
						memberCount.setText("Total member queries: " + membCount);
						try {
							hypoField.setText(showHypothesis());
						} catch (Exception e2) {
							e2.printStackTrace();
						}
					} else {
						entailed.setText("Entailed: No");
						membCount++;
						memberCount.setText("Total member queries: " + membCount);
					}
				} else {
					JOptionPane.showMessageDialog(null, "Fields can't be empty!", "Alert",
							JOptionPane.INFORMATION_MESSAGE);
				}
			}
		});
		btnNewButton_2.setBounds(10, 45, 171, 23);
		contentPane.add(btnNewButton_2);

		JButton btnNewButton_3 = new JButton("Equivalence query");
		btnNewButton_3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				try {
					timeStart = System.currentTimeMillis();
					learner();
				} catch (Throwable e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		});
		btnNewButton_3.setBounds(10, 258, 171, 23);
		contentPane.add(btnNewButton_3);

		JLabel lblNewLabel = new JLabel("SubClassOf");
		lblNewLabel.setBounds(202, 14, 121, 14);
		contentPane.add(lblNewLabel);

		loadedOnto.setBounds(151, 454, 107, 14);
		contentPane.add(loadedOnto);

		entailed.setBounds(191, 49, 121, 14);
		contentPane.add(entailed);

		memberCount.setBounds(261, 262, 225, 14);
		contentPane.add(memberCount);

		equivalenceCount.setBounds(261, 287, 225, 14);
		contentPane.add(equivalenceCount);

		conc2 = new JTextField();
		conc2.setBounds(303, 11, 183, 20);
		contentPane.add(conc2);
		conc2.setColumns(10);

		conc1 = new JTextField();
		conc1.setBounds(10, 11, 171, 20);
		contentPane.add(conc1);
		conc1.setColumns(10);

		JButton btnNewButton_4 = new JButton("[DEBUG] Show CIs in T");
		btnNewButton_4.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("TBox size = " + axiomsT.size());
				int i = 0;
				for (OWLAxiom axe : ontology.getAxioms()) {

					if (axe.toString().contains("Thing"))
						continue;
					if (axe.toString().contains("SubClassOf") || axe.toString().contains("Equivalent")) {
						System.out.println("TBox CI element #" + (i + 1) + " = " + rendering.render(axe));
						// tCount++;
						i++;
					}
				}
				// get sizes of inclusions
				showCIT(ontology.getAxioms());
				// showCISizes(ontologyH.getAxioms());

			}
		});
		btnNewButton_4.setBounds(638, 10, 213, 23);
		contentPane.add(btnNewButton_4);
		autoBox.setSelected(true);

		autoBox.setBounds(485, 375, 366, 23);
		contentPane.add(autoBox);
		oracleSaturate.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
			}
		});

		oracleSaturate.setBounds(485, 113, 107, 23);
		contentPane.add(oracleSaturate);
		scrollPane_1.setBounds(10, 335, 105, 104);

		contentPane.add(scrollPane_1);
		scrollPane_1.setViewportView(list);
		list.setModel(new AbstractListModel() {
			String[] values = new String[] { "animals", "football", "generations", "university", "EX" };

			public int getSize() {
				return values.length;
			}

			public Object getElementAt(int index) {
				return values[index];
			}
		});
		list.setSelectedIndex(0);

		filePath = new JTextField();
		filePath.setEditable(false);
		filePath.setBounds(123, 419, 189, 20);
		contentPane.add(filePath);
		filePath.setColumns(10);

		JButton btnNewButton_5 = new JButton("Select OWL file");
		btnNewButton_5.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = new JFileChooser(new File("/src/main/resources/ontologies"));

				fc.setDialogTitle("Open class File");
				fc.setApproveButtonText("Open");
				fc.setAcceptAllFileFilterUsed(false);
				fc.addChoosableFileFilter(new FileNameExtensionFilter("Ontology File (*.owl)", "owl"));
				int returnVal = fc.showOpenDialog(fc);

				File file = null;
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					file = fc.getSelectedFile();
					// System.out.println(file.getPath());
					filePath.setText(file.getPath());

				}

			}
		});
		btnNewButton_5.setBounds(322, 421, 164, 23);
		contentPane.add(btnNewButton_5);

		JButton btnNewButton_7 = new JButton("[DEBUG] Show CIs in H");
		btnNewButton_7.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("H-TBox size = " + ontologyH.getAxiomCount());
				int i = 0;
				for (OWLAxiom axe : ontologyH.getAxioms()) {

					if (axe.toString().contains("SubClassOf") || axe.toString().contains("Equivalent")) {

						System.out.println("HBox CI element #" + (i + 1) + " = " + rendering.render(axe));

						i++;
					}
				}
				showCIH(ontologyH.getAxioms());
			}
		});
		btnNewButton_7.setBounds(638, 45, 213, 23);
		contentPane.add(btnNewButton_7);

		fileLoad = new JCheckBox("Load From File");
		fileLoad.setBounds(121, 389, 97, 23);
		contentPane.add(fileLoad);

		ezBox = new JCheckBox("Easy mode [returns 1 direct inclusion from target T]");
		ezBox.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
			}
		});
		ezBox.setBounds(485, 349, 366, 23);
		contentPane.add(ezBox);

		oracleMerge = new JCheckBox("Merge");
		oracleMerge.setBounds(485, 139, 124, 23);
		contentPane.add(oracleMerge);

		learnerSat = new JCheckBox("Saturate");
		learnerSat.setBounds(625, 212, 108, 23);
		contentPane.add(learnerSat);

		learnerMerge = new JCheckBox("Sibling Merge");
		learnerMerge.setBounds(625, 238, 136, 23);
		contentPane.add(learnerMerge);

		learnerDecompL = new JCheckBox("Decompose Left");
		learnerDecompL.setBounds(485, 262, 136, 23);
		contentPane.add(learnerDecompL);

		learnerUnsat = new JCheckBox("Unsaturate");
		learnerUnsat.setBounds(485, 212, 114, 23);
		contentPane.add(learnerUnsat);

		learnerBranch = new JCheckBox("Branch");
		learnerBranch.setBounds(485, 238, 103, 23);
		contentPane.add(learnerBranch);

		JLabel lblNewLabel_1 = new JLabel("Learner skills");
		lblNewLabel_1.setBounds(492, 191, 154, 14);
		contentPane.add(lblNewLabel_1);

		oracleBranch = new JCheckBox("Branch");
		oracleBranch.setBounds(625, 139, 127, 23);
		contentPane.add(oracleBranch);

		averageCI = new JLabel("Target average CI size: 0");
		averageCI.setBounds(261, 337, 264, 14);
		contentPane.add(averageCI);

		smallestCI = new JLabel("Target smallest CI size: 0");
		smallestCI.setBounds(261, 316, 264, 14);
		contentPane.add(smallestCI);

		oracleUnsaturate = new JCheckBox("Unsaturate");
		oracleUnsaturate.setBounds(626, 113, 97, 23);
		contentPane.add(oracleUnsaturate);

		learnerDecompR = new JCheckBox("Decompose Right");
		learnerDecompR.setBounds(626, 264, 131, 23);
		contentPane.add(learnerDecompR);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 79, 409, 164);
		contentPane.add(scrollPane);
		scrollPane.setViewportView(hypoField);

		JLabel lblNewLabel_2 = new JLabel("Oracle Skills");
		lblNewLabel_2.setBounds(492, 92, 145, 14);
		contentPane.add(lblNewLabel_2);

		lblNewLabel_3 = new JLabel("[Small ontologies]");
		lblNewLabel_3.setBounds(10, 316, 105, 14);
		contentPane.add(lblNewLabel_3);

	}

	public void showCIT(Set<OWLAxiom> axSet) {
		int avgSize = 0;
		int sumSize = 0;
		smallestSize = 0;
		smallestOne = null;
		int ontSize = 0;
		int totalSize = 0;
		for (OWLAxiom axe : axiomsT) {

			if (axe.toString().contains("Thing"))
				continue;
			String inclusion = rendering.render(axe);
			inclusion = inclusion.replaceAll(" and ", " ");
			inclusion = inclusion.replaceAll(" some ", " ");
			if (axe.toString().contains("SubClassOf"))
				inclusion = inclusion.replaceAll("SubClassOf", "");
			else
				inclusion = inclusion.replaceAll("EquivalentTo", "");
			inclusion = inclusion.replaceAll(" and ", "");
			// ==System.out.println(inclusion);
			String[] arrIncl = inclusion.split(" ");
			totalSize = 0;
			for (int i = 0; i < arrIncl.length; i++)
				if (arrIncl[i].length() > 1)
					totalSize++;
			// for(int i = 0; i < arrIncl.length; i++)
			// System.out.println(arrIncl[i] + "=====" +arrIncl[i].length());

			// System.out.println(totalSize);
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
			// System.out.println("Size of : " + rendering.render(axe) + "." + totalSize);
			// System.out.println("Size of : " + inclusion + "." + totalSize);
		}
		//System.out.println("Smallest logical axiom: " + rendering.render(smallestOne));
		//System.out.println("Size is: " + smallestSize);
		System.out.println("Size of T: " + ontSize);
		//System.out.println("Avg: " + sumSize / axiomsT.size());

	}

	public void showCIH(Set<OWLAxiom> axSet) {
		int avgSize = 0;
		int sumSize = 0;
		smallestSize = 0;
		smallestOne = null;
		int ontSize = 0;
		int totalSize = 0;
		for (OWLAxiom axe : ontologyH.getAxioms()) {

			if (axe.toString().contains("Thing"))
				continue;
			String inclusion = rendering.render(axe);
			inclusion = inclusion.replaceAll(" and ", " ");
			inclusion = inclusion.replaceAll(" some ", " ");
			if (axe.toString().contains("SubClassOf"))
				inclusion = inclusion.replaceAll("SubClassOf", "");
			else
				inclusion = inclusion.replaceAll("EquivalentTo", "");
			inclusion = inclusion.replaceAll(" and ", "");
			// ==System.out.println(inclusion);
			String[] arrIncl = inclusion.split(" ");
			totalSize = 0;
			for (int i = 0; i < arrIncl.length; i++)
				if (arrIncl[i].length() > 1)
					totalSize++;
			// for(int i = 0; i < arrIncl.length; i++)
			// System.out.println(arrIncl[i] + "=====" +arrIncl[i].length());

			// System.out.println(totalSize);
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
			// System.out.println("Size of : " + rendering.render(axe) + "." + totalSize);
			// System.out.println("Size of : " + inclusion + "." + totalSize);
		}
		//System.out.println("Smallest logical axiom: " + rendering.render(smallestOne));
		//System.out.println("Size is: " + smallestSize);
		System.out.println("Size of H: " + ontSize);
		//System.out.println("Avg: " + sumSize / ontologyH.getAxioms().size());

	}

	public void getOntologyName() {

		int con = 0;
		for (int i = 0; i < ontology.getOntologyID().toString().length(); i++)
			if (ontology.getOntologyID().toString().charAt(i) == '/')
				con = i;
		ontologyName = ontology.getOntologyID().toString().substring(con + 1,
				ontology.getOntologyID().toString().length());
		ontologyName = ontologyName.substring(0, ontologyName.length() - 3);
		if (!ontologyName.contains(".owl"))
			ontologyName = ontologyName + ".owl";
		ontologyFolder += ontologyName;
		ontologyFolderH += "hypo_" + ontologyName;
	}

	// public int globalDecompose = 5;

	public void learner() throws Throwable {
		ELLearner learner = new ELLearner(reasonerForH, shortFormProvider, ontology, ontologyH, ELQueryEngineForT,
				this,rendering);

		//ELOracle oracle = new ELOracle(reasonerForH, shortFormProvider, ontology, ontologyH, ELQueryEngineForT, this);
		// we get a counter example from oracle
		// while () {
		if (autoBox.isSelected()) {
			showQueryCount();
			hypoField.setText(showHypothesis());

			if (equivalenceQuery()) {
				victory();
				timeEnd = System.currentTimeMillis();
				System.out.println("Total time (ms): " + (timeEnd - timeStart));
				lastCE = null;
				learner = null;
				return;
			} else if (ezBox.isSelected()) {
				equivCount++;
				ezEq();
			} else {
				equivCount++;
				doCE();
			}
			//System.out.println(rendering.render(lastCE));

			OWLClassExpression left = null;
			OWLClassExpression right = null;
			// lastCE is last counter example provided by oracle, unsaturate and saturate
			if (lastCE.isOfType(AxiomType.SUBCLASS_OF)) {
				left = ((OWLSubClassOfAxiom) lastCE).getSubClass();
				right = ((OWLSubClassOfAxiom) lastCE).getSuperClass();
			} else {

				learner = null;
				learner();

				return;

			}
			lastCE = ELQueryEngineForT.getSubClassAxiom(left, right);
			// check if complex side is left
			if (checkLeft(lastCE)) {

				// decompose tries to find underlying inclusions inside the left hand side
				// by recursively breaking the left expression and adding new inclusions to the
				// hypothesis
				/*if(oracleMerge.isSelected())
					oracle.oracleSiblingMerge(left, right);
				if(oracleSaturate.isSelected())
					oracle.saturateWithTreeLeft(lastCE);*/
				if (learnerDecompL.isSelected()) {
					// System.out.println("lhs decomp");
					learner.decompose(left, right);
				}
				// branch edges on left side of the inclusion (if possible) to make it logically
				// stronger (more general)
				if (learnerBranch.isSelected()) {
					// System.out.println("lhs branch");
					left = learner.branchLeft(left, right);
				}
				lastCE = ELQueryEngineForT.getSubClassAxiom(left, right);

				// unsaturate removes useless concepts from nodes in the inclusion
				if (learnerUnsat.isSelected()) {
					// System.out.println("lhs unsaturate");

					left = learner.unsaturateLeft(lastCE);
				}
				lastCE = ELQueryEngineForT.getSubClassAxiom(left, right);
				try {
					addHypothesis(lastCE);
					hypoField.setText(showHypothesis());
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			} else {
				// decompose tries to find underlying inclusions inside the right hand side
				// by recursively breaking the left expression and adding new inclusions to the
				// hypothesis
				if (learnerDecompR.isSelected()) {
					// System.out.println("rhs decomp");
					learner.decompose(left, right);
				}
				// merge edges on right side of the inclusion (if possible) to make it logically
				// stronger (more general)
				if (learnerMerge.isSelected()) {
					// System.out.println("rhs merge");
					right = learner.learnerSiblingMerge(left, right);
				}
				// rebuild inclusion for final step
				lastCE = ELQueryEngineForT.getSubClassAxiom(left, right);
				if (learnerSat.isSelected()) {
					// System.out.println("rhs saturate");
					lastCE = learner.saturateWithTreeRight(lastCE);
				}
				left = ((OWLSubClassOfAxiom) lastCE).getSubClass();
				right = ((OWLSubClassOfAxiom) lastCE).getSuperClass();
				try {
					addHypothesis(lastCE);
					hypoField.setText(showHypothesis());

					learner = null;
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
			learner();
		} else {
			if (equivalenceQuery()) {
				victory();
				timeEnd = System.currentTimeMillis();
				System.out.println("Total time (ms): " + (timeEnd - timeStart));
				return;
			} else if (ezBox.isSelected()) {
				equivCount++;
				ezEq();
			} else {
				equivCount++;
				doCE();
			}

			OWLClassExpression left = null;
			OWLClassExpression right = null;
			// lastCE is last counter example provided by oracle, unsaturate and saturate
			if (lastCE.isOfType(AxiomType.SUBCLASS_OF)) {
				left = ((OWLSubClassOfAxiom) lastCE).getSubClass();
				right = ((OWLSubClassOfAxiom) lastCE).getSuperClass();
			} else {
				return;

			}
			lastCE = ELQueryEngineForT.getSubClassAxiom(left, right);
			// check if complex side is left
			if (checkLeft(lastCE)) {

				// decompose tries to find underlying inclusions inside the left hand side
				// by recursively breaking the left expression and adding new inclusions to the
				// hypothesis
				if (learnerDecompL.isSelected())
					learner.decompose(left, right);

				// branch edges on left side of the inclusion (if possible) to make it logically
				// stronger (more general)
				if (learnerMerge.isSelected())
					left = learner.learnerSiblingMerge(left, right);

				lastCE = ELQueryEngineForT.getSubClassAxiom(left, right);
				// unsaturate removes useless concepts from nodes in the inclusion
				if (learnerUnsat.isSelected())
					left = learner.unsaturateLeft(lastCE);

				lastCE = ELQueryEngineForT.getSubClassAxiom(left, right);
				try {
					addHypothesis(lastCE);
					hypoField.setText(showHypothesis());
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			} else {
				// decompose tries to find underlying inclusions inside the right hand side
				// by recursively breaking the left expression and adding new inclusions to the
				// hypothesis
				if (learnerDecompR.isSelected())
					learner.decompose(left, right);

				// merge edges on right side of the inclusion (if possible) to make it logically
				// stronger (more general)
				if (learnerMerge.isSelected())
					right = learner.learnerSiblingMerge(left, right);

				// rebuild inclusion for final step
				lastCE = ELQueryEngineForT.getSubClassAxiom(left, right);
				if (learnerSat.isSelected())
					lastCE = learner.saturateWithTreeRight(lastCE);

				left = ((OWLSubClassOfAxiom) lastCE).getSubClass();
				right = ((OWLSubClassOfAxiom) lastCE).getSuperClass();
				try {
					addHypothesis(lastCE);
					hypoField.setText(showHypothesis());
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}
		// }

		learner = null;
		showQueryCount();

	}

	public ArrayList<String> getPS(ArrayList<String> strings) {
		Set<String> inputSet = new HashSet<String>(strings);

		List<Set<String>> subSets = new ArrayList<Set<String>>();
		for (String addToSets : inputSet) {
			List<Set<String>> newSets = new ArrayList<Set<String>>();
			for (Set<String> curSet : subSets) {
				Set<String> copyPlusNew = new HashSet<String>();
				copyPlusNew.addAll(curSet);
				copyPlusNew.add(addToSets);
				newSets.add(copyPlusNew);
			}
			Set<String> newValSet = new HashSet<String>();
			newValSet.add(addToSets);
			newSets.add(newValSet);
			subSets.addAll(newSets);
		}
		ArrayList<String> allSet = new ArrayList<String>();
		for (Set<String> setT : subSets) {

			String str = "";
			for (String setEntry : setT) {
				// System.out.print(setEntry + " ");
				str += setEntry + " and ";
			}
			allSet.add(str.substring(0, str.length() - 5));
		}
		System.out.println("Total combinations for concepts [no empty set]: " + subSets.size());
		return allSet;
	}

	public boolean checkLeft(OWLAxiom axiom) {

		String left = rendering.render(((OWLSubClassOfAxiom) axiom).getSubClass());
		String right = rendering.render(((OWLSubClassOfAxiom) axiom).getSuperClass());
		for (String rol : roles) {
			if (left.contains(rol)) {
				// System.out.println("complex is left");
				return true;
			} else if (right.contains(rol)) {
				// System.out.println("complex is right");
				return false;
			} else
				continue;
		}
		// System.out.println("default, saturating left");
		return true;
	}

	public void equivalenceCheck() {

		int x = 0;
		if (!wePlayin)
			JOptionPane.showMessageDialog(null, "No Ontology loaded yet, please load an Ontology to start playing!",
					"Alert", JOptionPane.INFORMATION_MESSAGE);
		else {
			if (autoBox.isSelected()) {
				System.gc();
				boolean check = equivalenceQuery();
				do {
					equivCount++;
					x++;
					if (check) {
						// victory
						victory();
						System.out.println("It took: " + x);
						System.out.flush();
					} else {
						// generate counter example
						System.out.flush();
						doCE();
					}
				} while (!equivalenceQuery());
			} else {
				boolean check = equivalenceQuery();
				equivCount++;
				if (check) {
					// victory
					victory();
				} else {
					// generate counter example
					doCE();
				}
			}
		}

	}

	public void doCE() {
		String counterEx = "";
		System.out.println("Generating counterexample... ");
		try {
			counterEx = getCounterExample();

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			hypoField.setText(showHypothesis());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(counterEx);
	}

	public void victory() {
		win = true;
		System.out.println("You dun did it!!!");
		showQueryCount();
		axiomsT = new HashSet<OWLAxiom>();
		for (OWLAxiom axe : ontology.getAxioms())
			if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
					|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
				axiomsT.add(axe);
	}

	public void showQueryCount() {
		memberCount.setText("Total membership queries: " + membCount);

		equivalenceCount.setText("Total equivalence queries: " + equivCount);
	}

	public void loadOntology() throws InterruptedException {
		win = false; 
		if(consoleLoad)
		{
			try {
				equivCount = 0;
				membCount = 0;
				hypoField.setText("");
				
				manager = OWLManager.createOWLOntologyManager();
				
					ontology = manager
							.loadOntologyFromOntologyDocument(new File(consoleOntologyPath));
				
				rendering = new ManchesterOWLSyntaxOWLObjectRendererImpl();

				reasonerForT = createReasoner(ontology);
				shortFormProvider = new SimpleShortFormProvider();
				axiomsT = new HashSet<OWLAxiom>();
				for (OWLAxiom axe : ontology.getAxioms())
					if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
							|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
						axiomsT.add(axe);

				lastCE = null;

				ELQueryEngineForT = new ELEngine(reasonerForT, shortFormProvider);
				// transfer Origin ontology to ManchesterOWLSyntaxOntologyFormat
				OWLOntologyFormat format = manager.getOntologyFormat(ontology);
				ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
				if (format.isPrefixOWLOntologyFormat()) {
					manSyntaxFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
				}
				format = null;
				// create personalized names for ontology
				ontologyFolderH = "src/main/resources/tmp/";
				ontologyFolder = "src/main/resources/tmp/";
				ontologyName = "";
				getOntologyName();

				{ // save ontologies
					newFile = new File(ontologyFolder);
					hypoFile = new File(ontologyFolderH);
					// save owl file as a new file in different location
					if (newFile.exists()) {
						newFile.delete();
					}
					newFile.createNewFile();
					manager.saveOntology(ontology, manSyntaxFormat, IRI.create(newFile.toURI()));

					// Create OWL Ontology Manager for hypothesis and load hypothesis file
					if (hypoFile.exists()) {
						hypoFile.delete();
					}
					hypoFile.createNewFile();

					ontologyH = manager.loadOntologyFromOntologyDocument(hypoFile);
				}

				shortFormProvider = new SimpleShortFormProvider();
				axiomsH = ontologyH.getAxioms();
				loadedOnto.setText("Ontology loaded.");
				wePlayin = true;

				System.out.println(ontology);
				System.out.println("Loaded successfully.");
				System.out.println();

				concepts = new SimpleClass(rendering).getSuggestionNames("concept", newFile);
				roles = new SimpleClass(rendering).getSuggestionNames("role", newFile);

				System.out.println("Total number of concepts is: " + concepts.size());

				SimpleClass simpleObject = new SimpleClass(rendering);
				int[] mins = simpleObject.showCISizes(axiomsT);

				smallestSize = mins[0];
				// System.out.println(mins[0]);
				// System.out.println(smallestSize);
				// showCISizes(axiomsT);
				smallestCI.setText("Target smallest CI size: " + smallestSize);
				averageCI.setText("Target average CI size: " + mins[1]);

				mins = null;

				System.out.flush();
			} catch (OWLOntologyCreationException e) {
				System.out.println("Could not load ontology: " + e.getMessage());
			} catch (OWLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		if (!fileLoad.isSelected()) {
			try {
				equivCount = 0;
				membCount = 0;
				hypoField.setText("");
				memberCount.setText("Total membership queries: 0");
				equivalenceCount.setText("Total equivalence queries: 0");
				manager = OWLManager.createOWLOntologyManager();
				System.out.println("Trying to load");
				if (list.getSelectedIndex() == 0)
					ontology = manager
							.loadOntologyFromOntologyDocument(new File("src/main/resources/ontologies/animals.owl"));
				else if (list.getSelectedIndex() == 1)
					ontology = manager
							.loadOntologyFromOntologyDocument(new File("src/main/resources/ontologies/football.owl"));
				else if (list.getSelectedIndex() == 2)
					ontology = manager.loadOntologyFromOntologyDocument(
							new File("src/main/resources/ontologies/generations.owl"));
				else if (list.getSelectedIndex() == 3)
					ontology = manager
							.loadOntologyFromOntologyDocument(new File("src/main/resources/ontologies/university.owl"));
				else if (list.getSelectedIndex() == 4)
					ontology = manager.loadOntologyFromOntologyDocument(
							new File("src/main/resources/ontologies/football_reverse.owl"));

				rendering = new ManchesterOWLSyntaxOWLObjectRendererImpl();

				reasonerForT = createReasoner(ontology);
				shortFormProvider = new SimpleShortFormProvider();
				axiomsT = new HashSet<OWLAxiom>();
				for (OWLAxiom axe : ontology.getAxioms())
					if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
							|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
						axiomsT.add(axe);

				lastCE = null;

				ELQueryEngineForT = new ELEngine(reasonerForT, shortFormProvider);
				// transfer Origin ontology to ManchesterOWLSyntaxOntologyFormat
				OWLOntologyFormat format = manager.getOntologyFormat(ontology);
				ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
				if (format.isPrefixOWLOntologyFormat()) {
					manSyntaxFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
				}
				format = null;
				// create personalized names for ontology
				ontologyFolderH = "src/main/resources/tmp/";
				ontologyFolder = "src/main/resources/tmp/";
				ontologyName = "";
				getOntologyName();

				{ // save ontologies
					newFile = new File(ontologyFolder);
					hypoFile = new File(ontologyFolderH);
					// save owl file as a new file in different location
					if (newFile.exists()) {
						newFile.delete();
					}
					newFile.createNewFile();
					manager.saveOntology(ontology, manSyntaxFormat, IRI.create(newFile.toURI()));

					// Create OWL Ontology Manager for hypothesis and load hypothesis file
					if (hypoFile.exists()) {
						hypoFile.delete();
					}
					hypoFile.createNewFile();

					ontologyH = manager.loadOntologyFromOntologyDocument(hypoFile);
				}

				shortFormProvider = new SimpleShortFormProvider();
				axiomsH = ontologyH.getAxioms();
				loadedOnto.setText("Ontology loaded.");
				wePlayin = true;

				System.out.println(ontology);
				System.out.println("Loaded successfully.");
				System.out.println();

				concepts = new SimpleClass(rendering).getSuggestionNames("concept", newFile);
				roles = new SimpleClass(rendering).getSuggestionNames("role", newFile);

				System.out.println("Total number of concepts is: " + concepts.size());

				SimpleClass simpleObject = new SimpleClass(rendering);
				int[] mins = simpleObject.showCISizes(axiomsT);

				smallestSize = mins[0];
				// System.out.println(mins[0]);
				// System.out.println(smallestSize);
				// showCISizes(axiomsT);
				smallestCI.setText("Target smallest CI size: " + smallestSize);
				averageCI.setText("Target average CI size: " + mins[1]);

				mins = null;

				System.out.flush();
			} catch (OWLOntologyCreationException e) {
				System.out.println("Could not load ontology: " + e.getMessage());
			} catch (OWLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				equivCount = 0;
				membCount = 0;
				hypoField.setText(""); 
				manager = OWLManager.createOWLOntologyManager();

				ontology = manager.loadOntologyFromOntologyDocument(new File(filePath.getText()));

				rendering = new ManchesterOWLSyntaxOWLObjectRendererImpl();

				reasonerForT = createReasoner(ontology);
				shortFormProvider = new SimpleShortFormProvider();
				axiomsT = new HashSet<OWLAxiom>();
				for (OWLAxiom axe : ontology.getAxioms())
					if (!axe.toString().contains("Thing") && axe.isOfType(AxiomType.SUBCLASS_OF)
							|| axe.isOfType(AxiomType.EQUIVALENT_CLASSES))
						axiomsT.add(axe);

				lastCE = null;

				ELQueryEngineForT = new ELEngine(reasonerForT, shortFormProvider);
				// transfer Origin ontology to ManchesterOWLSyntaxOntologyFormat
				OWLOntologyFormat format = manager.getOntologyFormat(ontology);
				ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
				if (format.isPrefixOWLOntologyFormat()) {
					manSyntaxFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
				}
				format = null;
				// create personalized names for ontology
				ontologyFolderH = "src/main/resources/tmp/";
				ontologyFolder = "src/main/resources/tmp/";
				ontologyName = "";
				getOntologyName();
				System.out.println(ontologyName);
				System.out.println(ontologyFolderH);
				System.out.println(ontologyFolder);
				{ // save ontologies
					newFile = new File(ontologyFolder);
					hypoFile = new File(ontologyFolderH);
					// save owl file as a new file in different location
					if (newFile.exists()) {
						newFile.delete();
					}
					newFile.createNewFile();
					manager.saveOntology(ontology, manSyntaxFormat, IRI.create(newFile.toURI()));

					// Create OWL Ontology Manager for hypothesis and load hypothesis file
					if (hypoFile.exists()) {
						hypoFile.delete();
					}
					hypoFile.createNewFile();

					ontologyH = manager.loadOntologyFromOntologyDocument(hypoFile);
				}

				shortFormProvider = new SimpleShortFormProvider();
				axiomsH = ontologyH.getAxioms();
				loadedOnto.setText("Ontology loaded.");
				wePlayin = true;

				System.out.println(ontology);
				System.out.println("Loaded successfully.");
				System.out.println();

				concepts = new SimpleClass(rendering).getSuggestionNames("concept", newFile);
				roles = new SimpleClass(rendering).getSuggestionNames("role", newFile);

				System.out.println("Total number of concepts is: " + concepts.size());

				int[] mins = new SimpleClass(rendering).showCISizes(axiomsT);
				smallestSize = mins[0];
				smallestCI.setText("Target smallest CI size: " + mins[0]);
				averageCI.setText("Target average CI size: " + mins[1]);
				mins = null;

				System.out.flush();
			} catch (OWLOntologyCreationException e) {
				System.out.println("Could not load ontology: " + e.getMessage());
			} catch (OWLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public OWLReasoner createReasoner(final OWLOntology rootOntology) {
		return new Reasoner.ReasonerFactory().createReasoner(rootOntology);
	}

	public Boolean membershipQuery(String concept1String, String concept2String) throws Exception {
		Boolean queryAns = false;
		if (!wePlayin)
			JOptionPane.showMessageDialog(null, "No Ontology loaded yet, please load an Ontology to start playing!",
					"Alert", JOptionPane.INFORMATION_MESSAGE);
		else {
			OWLAxiom subclassAxiom = ELQueryEngineForT.parseToOWLSubClassOfAxiom(concept1String, concept2String);
			queryAns = ELQueryEngineForT.entailed(subclassAxiom);
			if (queryAns) {
				Boolean queryAddedHypo = ontologyH.containsAxiom(subclassAxiom);
				if (!queryAddedHypo) {
					addHypothesis(subclassAxiom);
				} else {
					String message = "The inclusion [" + rendering.render(subclassAxiom)
							+ "] has been queried before.\n" + "Therefore, it will not be added into the hypothesis.";
					membCount--;
					System.out.println(message);
					// JOptionPane.showMessageDialog(null, message, "Alert",
					// JOptionPane.INFORMATION_MESSAGE);
				}
			}
		}
		return queryAns;
	}

	public Boolean equivalenceQuery() {

		reasonerForH = createReasoner(ontologyH);
		ELEngine ELQueryEngineForH = new ELEngine(reasonerForH, shortFormProvider);
		Boolean queryAns = ELQueryEngineForH.entailed(axiomsT);
		reasonerForH.dispose();
		return queryAns;
	}

	public String getCounterExample() throws Exception {
		reasonerForH = createReasoner(ontologyH);
		ELEngine ELQueryEngineForH = new ELEngine(reasonerForH, shortFormProvider);

		ELOracle oracle = new ELOracle(reasonerForH, shortFormProvider, ontology, ontologyH, ELQueryEngineForT, this);
		// reasonerForH.dispose();

		Iterator<OWLAxiom> iteratorT = axiomsT.iterator();
		while (iteratorT.hasNext()) {
			OWLAxiom selectedAxiom = iteratorT.next();
			selectedAxiom.getAxiomType();

			// first get CounterExample from an axiom with the type SUBCLASS_OF
			if (selectedAxiom.isOfType(AxiomType.SUBCLASS_OF)) {
				Boolean queryAns = ELQueryEngineForH.entailed(selectedAxiom);
				// if hypothesis does NOT entail the CI
				if (!queryAns) {
					//System.out.println("Chosen CE:" + rendering.render(selectedAxiom));
					OWLSubClassOfAxiom counterexample = (OWLSubClassOfAxiom) selectedAxiom;
					OWLClassExpression subclass = counterexample.getSubClass();
					OWLClassExpression superclass = counterexample.getSuperClass();

					// create new counter example from the subclass and superclass
					// of axiom NOT entailed by H

					OWLAxiom newCounterexampleAxiom = getCounterExamplefromSubClassAxiom(subclass, superclass);
					if (newCounterexampleAxiom != null) {
						// if we actually got something, we use it as new counter example

						
						//System.out.println("subclass 1");
						// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
						// ADD SATURATION FOR newCounterexampleAxiom HERE
						OWLClassExpression ex = null;
						if (checkLeft(newCounterexampleAxiom)) {
							if (oracleMerge.isSelected()) {
								ex = null;
								// System.out.println(newCounterexampleAxiom);
								// if (checkLeft(newCounterexampleAxiom)) {
								ex = oracle.oracleSiblingMerge(
										((OWLSubClassOfAxiom) newCounterexampleAxiom).getSubClass(),
										((OWLSubClassOfAxiom) newCounterexampleAxiom).getSuperClass());
								newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(ex, superclass);
								ex = null;
							}
							if (oracleSaturate.isSelected())
								newCounterexampleAxiom = oracle
										.saturateWithTreeLeft((OWLSubClassOfAxiom) newCounterexampleAxiom);
						} else {

							if (oracleBranch.isSelected()) {
								ex = null;
								OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) newCounterexampleAxiom;
								ex = oracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
								newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(auxAx.getSubClass(), ex);
								auxAx = null;
								ex = null;
							}
							if (oracleUnsaturate.isSelected()) {
								ex = null;
								 ex = oracle.unsaturateRight(newCounterexampleAxiom);
								 newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(subclass, ex);
								ex = null;
							}
						}
						/*
						 * } else { ex = siblingMerge(((OWLSubClassOfAxiom)
						 * newCounterexampleAxiom).getSuperClass()); newCounterexampleAxiom =
						 * saturateWithTreeRight( ELQueryEngineForT.getSubClassAxiom(subclass, ex)); }
						 */

						// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
						lastCE = newCounterexampleAxiom;
						subclass = null;
						superclass = null;
						oracle = null;
						counterexample = null;
						selectedAxiom = null;
						iteratorT = null;
						return addHypothesis(newCounterexampleAxiom);
					}
				}
			}

			// get CounterExample from an axiom with the type EQUIVALENT_CLASSES
			if (selectedAxiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {

				OWLEquivalentClassesAxiom counterexample = (OWLEquivalentClassesAxiom) selectedAxiom;
				Set<OWLSubClassOfAxiom> eqsubclassaxioms = counterexample.asOWLSubClassOfAxioms();
				Iterator<OWLSubClassOfAxiom> iterator = eqsubclassaxioms.iterator();

				while (iterator.hasNext()) {
					OWLSubClassOfAxiom subClassAxiom = iterator.next();

					OWLClassExpression subclass = subClassAxiom.getSubClass();

					Set<OWLClass> superclasses = ELQueryEngineForT.getSuperClasses(subclass, true);
					if (!superclasses.isEmpty()) {
						Iterator<OWLClass> iteratorSuperClass = superclasses.iterator();
						while (iteratorSuperClass.hasNext()) {
							OWLClassExpression SuperclassInSet = iteratorSuperClass.next();
							OWLAxiom newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(subclass,
									SuperclassInSet);
							Boolean querySubClass = ELQueryEngineForH.entailed(newCounterexampleAxiom);
							Boolean querySubClassforT = ELQueryEngineForT.entailed(newCounterexampleAxiom);
							if (!querySubClass && querySubClassforT) {

								//System.out.println("eq 1");
								// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
								// ADD SATURATION FOR newCounterexampleAxiom HERE
								OWLClassExpression ex = null;
								if (checkLeft(newCounterexampleAxiom)) {
									if (oracleMerge.isSelected()) {

										ex = null;
										// System.out.println(newCounterexampleAxiom);
										// if (checkLeft(newCounterexampleAxiom)) {
										ex = oracle.oracleSiblingMerge(
												((OWLSubClassOfAxiom) newCounterexampleAxiom).getSubClass(),
												((OWLSubClassOfAxiom) newCounterexampleAxiom).getSuperClass());
										newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(ex,
												((OWLSubClassOfAxiom) newCounterexampleAxiom).getSuperClass());
										ex = null;
									}
									if (oracleSaturate.isSelected())
										newCounterexampleAxiom = oracle
												.saturateWithTreeLeft((OWLSubClassOfAxiom) newCounterexampleAxiom);
								} else {
									if (oracleBranch.isSelected()) {
										ex = null;
										OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) newCounterexampleAxiom;
										ex = oracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
										newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(auxAx.getSubClass(),
												ex);
										auxAx = null;
										ex = null;
									}
									if (oracleUnsaturate.isSelected()) {
										ex = null;
										 ex = oracle.unsaturateRight(newCounterexampleAxiom);
										 newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(((OWLSubClassOfAxiom)
										 newCounterexampleAxiom).getSubClass(),ex);
										ex = null;
									}
								}
								// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
								lastCE = newCounterexampleAxiom;
								oracle = null;
								subclass = null;
								SuperclassInSet = null;
								superclasses = null;
								counterexample = null;
								subClassAxiom = null;
								selectedAxiom = null;
								iteratorT = null;
								System.out.flush();
								return addHypothesis(newCounterexampleAxiom);
							}
						}
					}

				}
			}
		}

		Iterator<OWLAxiom> iterator = axiomsT.iterator();

		while (iterator.hasNext()) {
			OWLAxiom Axiom = iterator.next();

			Axiom.getAxiomType();
			if ((Axiom.isOfType(AxiomType.SUBCLASS_OF)) || (Axiom.isOfType(AxiomType.EQUIVALENT_CLASSES))) {

				Axiom.getAxiomType();
				if (Axiom.isOfType(AxiomType.SUBCLASS_OF)) {
					OWLSubClassOfAxiom selectedAxiom = (OWLSubClassOfAxiom) Axiom;
					Boolean queryAns = ELQueryEngineForH.entailed(selectedAxiom);

					if (!queryAns) {
						lastCE = selectedAxiom;
						//System.out.println("subclass 2");
						// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
						// ADD SATURATION FOR Axiom HERE
						OWLClassExpression ex = null;
						if (checkLeft(selectedAxiom)) {
							if (oracleMerge.isSelected()) {
								ex = null;
								// System.out.println(newCounterexampleAxiom);
								// if (checkLeft(newCounterexampleAxiom)) {
								ex = oracle.oracleSiblingMerge(((OWLSubClassOfAxiom) selectedAxiom).getSubClass(),
										((OWLSubClassOfAxiom) selectedAxiom).getSuperClass());
								selectedAxiom = (OWLSubClassOfAxiom) ELQueryEngineForT.getSubClassAxiom(ex,
										((OWLSubClassOfAxiom) selectedAxiom).getSuperClass());
								ex = null;
							}
							if (oracleSaturate.isSelected())
								selectedAxiom = (OWLSubClassOfAxiom) oracle
										.saturateWithTreeLeft((OWLSubClassOfAxiom) selectedAxiom);
						} else {

							if (oracleBranch.isSelected()) {
								ex = null;
								OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) selectedAxiom;
								ex = oracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
								selectedAxiom = (OWLSubClassOfAxiom) ELQueryEngineForT
										.getSubClassAxiom(auxAx.getSubClass(), ex);
								auxAx = null;
								ex = null;
							}
							if (oracleUnsaturate.isSelected()) {
								ex = null;
								 ex = oracle.unsaturateRight((OWLSubClassOfAxiom) selectedAxiom);
								 selectedAxiom = (OWLSubClassOfAxiom)
								 ELQueryEngineForT.getSubClassAxiom(((OWLSubClassOfAxiom)
								 selectedAxiom).getSubClass(), ex);
								ex = null;
							}

						}
						// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
						lastCE = selectedAxiom;

						oracle = null;
						Axiom = null;
						iterator = null;
						iteratorT = null;
						System.out.flush();
						return addHypothesis((OWLSubClassOfAxiom) selectedAxiom);
					}
				}

				if (Axiom.isOfType(AxiomType.EQUIVALENT_CLASSES)) {
					OWLEquivalentClassesAxiom counterexample = (OWLEquivalentClassesAxiom) Axiom;

					Set<OWLSubClassOfAxiom> eqsubclassaxioms = counterexample.asOWLSubClassOfAxioms();
					Iterator<OWLSubClassOfAxiom> iteratorAsSub = eqsubclassaxioms.iterator();
					while (iteratorAsSub.hasNext()) {
						OWLSubClassOfAxiom subClassAxiom = iteratorAsSub.next();
						Boolean queryAns = ELQueryEngineForH.entailed(subClassAxiom);
						if (!queryAns) {
							lastCE = subClassAxiom;
							//System.out.println("eqcl 2");
							// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*-*-*-*-*-*-*-*-*
							// ADD SATURATION FOR subClassAxiom HERE
							OWLClassExpression ex = null;

							if (checkLeft(subClassAxiom)) {
								if (oracleMerge.isSelected()) {
									ex = null;
									// System.out.println(newCounterexampleAxiom);
									// if (checkLeft(newCounterexampleAxiom)) {
									ex = oracle.oracleSiblingMerge(((OWLSubClassOfAxiom) subClassAxiom).getSubClass(),
											((OWLSubClassOfAxiom) subClassAxiom).getSuperClass());
									subClassAxiom = (OWLSubClassOfAxiom) ELQueryEngineForT.getSubClassAxiom(ex,
											((OWLSubClassOfAxiom) subClassAxiom).getSuperClass());
									ex = null;
								}
								if (oracleSaturate.isSelected())
									subClassAxiom = (OWLSubClassOfAxiom) oracle
											.saturateWithTreeLeft((OWLSubClassOfAxiom) subClassAxiom);
							} else {
								if (oracleBranch.isSelected()) {
									ex = null;
									OWLSubClassOfAxiom auxAx = (OWLSubClassOfAxiom) subClassAxiom;
									ex = oracle.branchRight(auxAx.getSubClass(), auxAx.getSuperClass());
									subClassAxiom = (OWLSubClassOfAxiom) ELQueryEngineForT
											.getSubClassAxiom(auxAx.getSubClass(), ex);
									auxAx = null;
									ex = null;
								}
								if (oracleUnsaturate.isSelected()) {
									ex = null;
									 ex = oracle.unsaturateRight((OWLSubClassOfAxiom) subClassAxiom);
									 subClassAxiom = (OWLSubClassOfAxiom)
									 ELQueryEngineForT.getSubClassAxiom(((OWLSubClassOfAxiom)
									 subClassAxiom).getSubClass(),ex);
									ex = null;
								}
							}
							// *-*-*-*-*-*-*-*-*-*-*-*-*-**-*-*-*-*-*?*-*-*-*-*-*-*-*
							lastCE = subClassAxiom;
							oracle = null;
							Axiom = null;
							iterator = null;
							iteratorAsSub = null;
							iteratorT = null;
							System.out.flush();
							return addHypothesis(subClassAxiom);
						}
					}
				}
			}
		}
		System.out.println("no more CIs");
		oracle = null;
		iterator = null;
		iteratorT = null;
		System.out.flush();
		return null;
	}

	private OWLAxiom getCounterExamplefromSubClassAxiom(OWLClassExpression subclass, OWLClassExpression superclass) {
		reasonerForH = createReasoner(ontologyH);
		ELEngine ELQueryEngineForH = new ELEngine(reasonerForH, shortFormProvider);
		Set<OWLClass> superclasses = ELQueryEngineForT.getSuperClasses(superclass, false);
		Set<OWLClass> subclasses = ELQueryEngineForT.getSubClasses(subclass, false);

		if (!subclasses.isEmpty()) {

			Iterator<OWLClass> iteratorSubClass = subclasses.iterator();
			while (iteratorSubClass.hasNext()) {
				OWLClassExpression SubclassInSet = iteratorSubClass.next();
				OWLAxiom newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(SubclassInSet, superclass);
				Boolean querySubClass = ELQueryEngineForH.entailed(newCounterexampleAxiom);
				Boolean querySubClassforT = ELQueryEngineForT.entailed(newCounterexampleAxiom);
				if (!querySubClass && querySubClassforT) {
					SubclassInSet = null;
					superclass = null;
					iteratorSubClass = null;
					ELQueryEngineForH = null;

					reasonerForH.dispose();
					return newCounterexampleAxiom;
				}
			}
		}
		if (!superclasses.isEmpty()) {

			Iterator<OWLClass> iteratorSuperClass = superclasses.iterator();
			while (iteratorSuperClass.hasNext()) {
				OWLClassExpression SuperclassInSet = iteratorSuperClass.next();
				OWLAxiom newCounterexampleAxiom = ELQueryEngineForT.getSubClassAxiom(subclass, SuperclassInSet);
				Boolean querySubClass = ELQueryEngineForH.entailed(newCounterexampleAxiom);
				Boolean querySubClassforT = ELQueryEngineForT.entailed(newCounterexampleAxiom);
				if (!querySubClass && querySubClassforT) {

					SuperclassInSet = null;
					superclass = null;
					subclass = null;
					iteratorSuperClass = null;
					ELQueryEngineForH = null;

					reasonerForH.dispose();
					return newCounterexampleAxiom;
				}
			}
		}

		ELQueryEngineForH = null;
		superclass = null;
		subclass = null;
		reasonerForH.dispose();
		return null;
	}

	public String addHypothesis(OWLAxiom addedAxiom) throws Exception {
		String StringAxiom = rendering.render(addedAxiom);

		AddAxiom newAxiomInH = new AddAxiom(ontologyH, addedAxiom);
		manager.applyChange(newAxiomInH);
		saveOWLFile(ontologyH, hypoFile);

		// minimize hypothesis
		ontologyH = MinHypothesis(ontologyH, addedAxiom);
		saveOWLFile(ontologyH, hypoFile);
		newAxiomInH = null;
		addedAxiom = null;
		return StringAxiom;
	}

	public void ezEq() {
		if (equivalenceQuery()) {
			victory();
			return;
		}

		for (OWLAxiom ax : axiomsT) {
			if (ax.toString().contains("Thing"))
				continue;
			if (!axiomsH.contains(ax)) {
				try {
					addHypothesis(ax);
					lastCE = ax;
					hypoField.setText(showHypothesis());
					axiomsT.remove(ax);
					axiomsH.add(ax);
					break;
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		showQueryCount();
	}

	private OWLOntology MinHypothesis(OWLOntology hypoOntology, OWLAxiom addedAxiom) {
		Set<OWLAxiom> tmpaxiomsH = hypoOntology.getAxioms();
		Iterator<OWLAxiom> ineratorMinH = tmpaxiomsH.iterator();
		Set<OWLAxiom> checkedAxiomsSet = new HashSet<OWLAxiom>();
		String removedstring = "";
		Boolean flag = false;
		if (tmpaxiomsH.size() > 1) {
			while (ineratorMinH.hasNext()) {
				OWLAxiom checkedAxiom = ineratorMinH.next();
				if (!checkedAxiomsSet.contains(checkedAxiom)) {
					checkedAxiomsSet.add(checkedAxiom);

					OWLOntology tmpOntologyH = hypoOntology;
					RemoveAxiom removedAxiom = new RemoveAxiom(tmpOntologyH, checkedAxiom);
					manager.applyChange(removedAxiom);

					OWLReasoner tmpreasoner = createReasoner(tmpOntologyH);
					ELEngine tmpELQueryEngine = new ELEngine(tmpreasoner, shortFormProvider);
					Boolean queryAns = tmpELQueryEngine.entailed(checkedAxiom);
					tmpreasoner.dispose();

					if (queryAns) {
						RemoveAxiom removedAxiomFromH = new RemoveAxiom(hypoOntology, checkedAxiom);
						manager.applyChange(removedAxiomFromH);
						removedstring = "\t[" + rendering.render(checkedAxiom) + "]\n";
						if (checkedAxiom.equals(addedAxiom)) {
							flag = true;
						}
					} else {
						AddAxiom addAxiomtoH = new AddAxiom(hypoOntology, checkedAxiom);
						manager.applyChange(addAxiomtoH);
						addAxiomtoH = null;
					}
				}
			}
			if (!removedstring.equals("")) {
				String message;
				if (flag) {
					message = "The axiom [" + rendering.render(addedAxiom) + "] will not be added to the hypothesis\n"
							+ "since it can be replaced by some axiom(s) that already exist in the hypothesis.";
				} else {
					message = "The axiom [" + removedstring + "]" + "will be removed after adding: \n["
							+ rendering.render(addedAxiom) + "]";
				}
				//System.out.println(message);
				// JOptionPane.showMessageDialog(null, message, "Alert",
				// JOptionPane.INFORMATION_MESSAGE);
			}
		}
		tmpaxiomsH = null;
		ineratorMinH = null;
		checkedAxiomsSet = null;

		return hypoOntology;
	}

	private void saveOWLFile(OWLOntology ontology, File file) throws Exception {

		OWLOntologyFormat format = manager.getOntologyFormat(ontology);
		ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
		if (format.isPrefixOWLOntologyFormat()) {
			// need to remove prefixes
			manSyntaxFormat.clearPrefixes();
		}
		format = null;
		manager.saveOntology(ontology, manSyntaxFormat, IRI.create(file.toURI()));
	}

	public String showHypothesis() throws Exception {

		Set<OWLAxiom> axiomsInH = ontologyH.getAxioms();
		String hypoInManchester = "";
		for (OWLAxiom axiom : axiomsInH) {
			hypoInManchester = hypoInManchester + rendering.render(axiom) + "\n";
		}
		axiomsInH = null;
		return hypoInManchester;
	}
}

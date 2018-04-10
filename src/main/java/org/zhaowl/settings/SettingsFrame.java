package org.zhaowl.settings;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder; 
public class SettingsFrame extends JFrame {

	private JPanel learnerSat;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SettingsFrame frame = new SettingsFrame();
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
	
	public JCheckBox oracleSat;
	public JCheckBox oracleMerge;
	public JCheckBox oracleBranch;
	public JCheckBox learnerSaturate;
	public JCheckBox learnerUnsat;
	public JCheckBox learnerDecomp;
	public JCheckBox learnerMerge;
	public JCheckBox learnerBranch;
	public JCheckBox oracleUnsat;
	public SettingsFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		learnerSat = new JPanel();
		learnerSat.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(learnerSat);
		learnerSat.setLayout(null);
		
		JButton btnNewButton = new JButton("Back");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);
			}
		});
		btnNewButton.setBounds(335, 227, 89, 23);
		learnerSat.add(btnNewButton);
		
		JLabel lblNewLabel = new JLabel("Oracle");
		lblNewLabel.setBounds(10, 11, 184, 14);
		learnerSat.add(lblNewLabel);
		
		JLabel lblNewLabel_1 = new JLabel("Learner");
		lblNewLabel_1.setBounds(204, 11, 220, 14);
		learnerSat.add(lblNewLabel_1);
		
		  oracleSat = new JCheckBox("Saturation");
		oracleSat.setBounds(6, 32, 188, 23);
		learnerSat.add(oracleSat);

		  oracleMerge = new JCheckBox("Merging");
		oracleMerge.setBounds(6, 101, 188, 14);
		learnerSat.add(oracleMerge); 
		
		  oracleBranch = new JCheckBox("Branching");
		oracleBranch.setBounds(6, 75, 188, 23);
		learnerSat.add(oracleBranch);
		
		  learnerSaturate = new JCheckBox("Saturation");
		learnerSaturate.setBounds(204, 32, 97, 23);
		learnerSat.add(learnerSaturate);
		
		  learnerUnsat = new JCheckBox("Unsaturation");
		learnerUnsat.setBounds(204, 54, 97, 23);
		learnerSat.add(learnerUnsat);
		
		  learnerDecomp = new JCheckBox("Decompose");
		learnerDecomp.setBounds(204, 123, 97, 23);
		learnerSat.add(learnerDecomp);
		
		  learnerMerge = new JCheckBox("Merging");
		learnerMerge.setBounds(204, 75, 97, 23);
		learnerSat.add(learnerMerge);
		
		  learnerBranch = new JCheckBox("Branching");
		learnerBranch.setBounds(204, 97, 97, 23);
		learnerSat.add(learnerBranch);
		
		 
		
		JButton btnNewButton_1 = new JButton("Save settings");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				File settingsFile = new File("src/main/resources/settings/settings.txt");
				boolean[] arrSet = new boolean[9];
				String strSet = ""; 
				strSet += oracleSat.isSelected() + ";";
				strSet += oracleUnsat.isSelected() + ";";
				strSet += oracleMerge.isSelected() + ";";
				strSet += oracleBranch.isSelected() + ";";
				strSet += learnerSaturate.isSelected() + ";";
				strSet += learnerUnsat.isSelected() + ";";
				strSet += learnerMerge.isSelected() + ";";
				strSet += learnerBranch.isSelected() + ";";
				strSet += learnerDecomp.isSelected();
				//System.out.println(strSet);
				BufferedWriter wr =null;
				try {
					wr = new BufferedWriter(new FileWriter(settingsFile));
					wr.write(strSet);

					wr.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
		btnNewButton_1.setBounds(10, 227, 173, 23);
		learnerSat.add(btnNewButton_1);
		
		  oracleUnsat = new JCheckBox("Unsaturate");
		oracleUnsat.setBounds(6, 54, 97, 23);
		learnerSat.add(oracleUnsat);
	}
}

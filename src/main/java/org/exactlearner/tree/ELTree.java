package org.exactlearner.tree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

 
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

public class ELTree {

    private int maxLevel = 0;

    private int size = 1;

    private ELNode rootNode;

    // nodes on a given level of the tree
    private final Map<Integer, Set<ELNode>> levelNodeMapping;


    public ELTree(OWLClassExpression description) throws Exception {
        // construct root node and recursively build the tree
        levelNodeMapping = new HashMap<>();
        setRootNode(new ELNode(this));
        constructTree(description, getRootNode());

    }


    // Copying constructor
    // WARNING: does not work yet (something stays uncopied), its use breaks sibling merging
    // @todo FIX IT!!!
    public ELTree(ELTree tree) {
        this.maxLevel = tree.maxLevel;
        this.size = tree.size;
        this.rootNode = new ELNode(tree.rootNode);
        levelNodeMapping = new HashMap<>(tree.levelNodeMapping);
    }

    
    //Reference: DL Learner
    private void constructTree(OWLClassExpression description, ELNode node) throws Exception {
//		if (description.isOWLThing()) {
//			// nothing needs to be done as an empty set is owl:Thing
//		} else
        if (!description.isAnonymous()) {
            node.extendLabel(description.asOWLClass());
        } else if (description instanceof OWLObjectSomeValuesFrom) {
            OWLObjectProperty op = ((OWLObjectSomeValuesFrom) description).getProperty().asOWLObjectProperty();
            ELNode newNode = new ELNode(node, op, new TreeSet<>());
            constructTree(((OWLObjectSomeValuesFrom) description).getFiller(), newNode);
        } else
 
            if (description instanceof OWLObjectIntersectionOf) {
                // loop through all elements of the intersection
                for (OWLClassExpression child : ((OWLObjectIntersectionOf) description).getOperands()) {
                    if (!child.isAnonymous()) {
                        node.extendLabel(child.asOWLClass());
                    } else if (child instanceof OWLObjectSomeValuesFrom) {
                        OWLObjectProperty op = ((OWLObjectSomeValuesFrom) child).getProperty().asOWLObjectProperty();
                        ELNode newNode = new ELNode(node, op, new TreeSet<>());
                        constructTree(((OWLObjectSomeValuesFrom) child).getFiller(), newNode);
                    } else {
                        throw new Exception(description + " specifically " + child);
                    }
                }
            } else {
                throw new Exception(description.toString());
            }
    }

    /**
     * Gets the nodes on a specific level of the tree. This information is cached
     * here for performance reasons.
     *
     * @param level The level (distance from root node).
     * @return The set of all nodes on the specified level within this tree.
     */
    public Set<ELNode> getNodesOnLevel(int level) {
        return levelNodeMapping.get(level);
    }


    /**
     * Internal method for updating the node set and the level node mapping. It must
     * be called when a new node is added to the tree.
     *
     * @param node  The new node.
     * @param level Level of the new node.
     */
    public void addNodeToLevel(ELNode node, int level) {
        //nodes.add(node);
        if (level <= maxLevel) {
            levelNodeMapping.get(level).add(node);
        } else if (level == maxLevel + 1) {
            Set<ELNode> set = new HashSet<>();
            set.add(node);
            levelNodeMapping.put(level, set);
            maxLevel++;
        } else {
            throw new RuntimeException("Inconsistent EL OWLClassExpression tree structure.");
        }
    }

    /**
     * @return the maxLevel
     */
    public int getMaxLevel() {
        return maxLevel;
    }


    @Override
    public String toString() {
        return getRootNode().toString();
    }

    public OWLClassExpression transformToClassExpression() {
        return getRootNode().transformToDescription();
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public ELNode getRootNode() {
        return rootNode;
    }

    public void setRootNode(ELNode rootNode) {
        this.rootNode = rootNode;
    }
}

/**
 * Edited from https://coderanch.com/t/346509/java/JTree-drag-drop-tree-Java
 * by Craig Wood and mentioned on
 * https://stackoverflow.com/questions/4588109/drag-and-drop-nodes-in-jtree
 */

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class TreeDragAndDrop {
    private JScrollPane getContent() {
        JTree tree = new JTree();
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new TreeTransferHandler());
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        expandTree(tree);
        return new JScrollPane(tree);
    }

    private void expandTree(JTree tree) {
        DefaultMutableTreeNode root =
                (DefaultMutableTreeNode)tree.getModel().getRoot();
        Enumeration e = root.breadthFirstEnumeration();
        while(e.hasMoreElements()) {
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode)e.nextElement();
            if(node.isLeaf()) continue;
            int row = tree.getRowForPath(new TreePath(node.getPath()));
            tree.expandRow(row);
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new TreeDragAndDrop().getContent());
        f.setSize(400,400);
        f.setLocation(200,200);
        f.setVisible(true);
    }
}

class TreeTransferHandler extends TransferHandler {
    DataFlavor nodesFlavor;
    DataFlavor[] flavors = new DataFlavor[1];
    DefaultMutableTreeNode[] nodesToRemove;

    public TreeTransferHandler() {
        try {
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
                    ";class=\"" +
                    javax.swing.tree.DefaultMutableTreeNode[].class.getName() +
                    "\"";
            nodesFlavor = new DataFlavor(mimeType);
            flavors[0] = nodesFlavor;
        } catch(ClassNotFoundException e) {
            System.out.println("ClassNotFound: " + e.getMessage());
        }
    }

    public boolean canImport(TransferHandler.TransferSupport support) {
        if(!support.isDrop()) {
            return false;
        }
        support.setShowDropLocation(true);
        if(!support.isDataFlavorSupported(nodesFlavor)) {
            return false;
        }
        // Do not allow a drop on the drag source selections.
        JTree.DropLocation dl =
                (JTree.DropLocation)support.getDropLocation();
        JTree tree = (JTree)support.getComponent();
        int dropRow = tree.getRowForPath(dl.getPath());
        int[] selRows = tree.getSelectionRows();
        for(int i = 0; i < selRows.length; i++) {
            if(selRows[i] == dropRow) {
                return false;
            }
            DefaultMutableTreeNode treeNode =
                    (DefaultMutableTreeNode)tree.getPathForRow(selRows[i]).getLastPathComponent();
            for (TreeNode offspring: Collections.list(treeNode.depthFirstEnumeration())) {
                if (tree.getRowForPath(new TreePath(((DefaultMutableTreeNode)offspring).getPath())) == dropRow) {
                    return false;
                }
            }
        }
        return true;
    }

    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            return null;
        }
        // Make up a node array of copies for transfer and
        // another for/of the nodes that will be removed in
        // exportDone after a successful drop.
        List<DefaultMutableTreeNode> copies =
                new ArrayList<DefaultMutableTreeNode>();
        List<DefaultMutableTreeNode> toRemove =
                new ArrayList<DefaultMutableTreeNode>();
        DefaultMutableTreeNode firstNode =
                (DefaultMutableTreeNode) paths[0].getLastPathComponent();
        HashSet<TreeNode> doneItems = new LinkedHashSet<>(paths.length);
        DefaultMutableTreeNode copy = copy(firstNode, doneItems, tree);
        copies.add(copy);
        toRemove.add(firstNode);
        for (int i = 1; i < paths.length; i++) {
            DefaultMutableTreeNode next =
                    (DefaultMutableTreeNode) paths[i].getLastPathComponent();
            if (doneItems.contains(next)) {
                continue;
            }
            // Do not allow higher level nodes to be added to list.
            if (next.getLevel() < firstNode.getLevel()) {
                break;
            } else if (next.getLevel() > firstNode.getLevel()) {  // child node
                copy.add(copy(next, doneItems, tree));
                // node already contains child
            } else {                                        // sibling
                copies.add(copy(next, doneItems, tree));
                toRemove.add(next);
            }
            doneItems.add(next);
        }
        DefaultMutableTreeNode[] nodes =
                copies.toArray(new DefaultMutableTreeNode[copies.size()]);
        nodesToRemove =
                toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
        return new NodesTransferable(nodes);
    }

    private DefaultMutableTreeNode copy(DefaultMutableTreeNode node, HashSet<TreeNode> doneItems, JTree tree) {
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(node);
        doneItems.add(node);
        for (int i=0; i<node.getChildCount(); i++) {
            copy.add(copy((DefaultMutableTreeNode)((TreeNode)node).getChildAt(i), doneItems, tree));
        }
        int row = tree.getRowForPath(new TreePath(copy.getPath()));
        tree.expandRow(row);
        return copy;
    }

    protected void exportDone(JComponent source, Transferable data, int action) {
        if((action & MOVE) == MOVE) {
            JTree tree = (JTree)source;
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            // Remove nodes saved in nodesToRemove in createTransferable.
            for(int i = 0; i < nodesToRemove.length; i++) {
                model.removeNodeFromParent(nodesToRemove[i]);
            }
        }
    }

    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    public boolean importData(TransferHandler.TransferSupport support) {
        if(!canImport(support)) {
            return false;
        }
        // Extract transfer data.
        DefaultMutableTreeNode[] nodes = null;
        try {
            Transferable t = support.getTransferable();
            nodes = (DefaultMutableTreeNode[])t.getTransferData(nodesFlavor);
        } catch(UnsupportedFlavorException ufe) {
            System.out.println("UnsupportedFlavor: " + ufe.getMessage());
        } catch(java.io.IOException ioe) {
            System.out.println("I/O error: " + ioe.getMessage());
        }
        // Get drop location info.
        JTree.DropLocation dl =
                (JTree.DropLocation)support.getDropLocation();
        int childIndex = dl.getChildIndex();
        TreePath dest = dl.getPath();
        DefaultMutableTreeNode parent =
                (DefaultMutableTreeNode)dest.getLastPathComponent();
        JTree tree = (JTree)support.getComponent();
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        // Configure for drop mode.
        int index = childIndex;    // DropMode.INSERT
        if(childIndex == -1) {     // DropMode.ON
            index = parent.getChildCount();
        }
        // Add data to model.
        for(int i = 0; i < nodes.length; i++) {
            model.insertNodeInto(nodes[i], parent, index++);
        }
        return true;
    }

    public String toString() {
        return getClass().getName();
    }

    public class NodesTransferable implements Transferable {
        DefaultMutableTreeNode[] nodes;

        public NodesTransferable(DefaultMutableTreeNode[] nodes) {
            this.nodes = nodes;
        }

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
            if(!isDataFlavorSupported(flavor))
                throw new UnsupportedFlavorException(flavor);
            return nodes;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return nodesFlavor.equals(flavor);
        }
    }
}
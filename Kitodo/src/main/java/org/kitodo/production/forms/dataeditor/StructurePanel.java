/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.production.forms.dataeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.kitodo.api.dataeditor.rulesetmanagement.StructuralElementViewInterface;
import org.kitodo.api.dataformat.MediaUnit;
import org.kitodo.api.dataformat.Structure;
import org.kitodo.api.dataformat.View;
import org.kitodo.production.helper.Helper;
import org.kitodo.production.metadata.MetadataEditor;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

public class StructurePanel implements Serializable {
    private static final long serialVersionUID = 1L;

    private DataEditorForm dataEditor;

    /**
     * If changing the tree node fails, we need this value to undo the user’s
     * select action.
     */
    private TreeNode previouslySelectedNode;

    private TreeNode selectedNode;

    /**
     * Whether the media shall be shown separately in a second tree. If false,
     * the media—if linked—will be merged shown within the structure tree.
     */
    private Boolean separateMedia = Boolean.FALSE;

    private Structure structure;

    /**
     * The logical structure tree of the edited document.
     */
    private DefaultTreeNode structureTree;

    /**
     * List of structure trees to be displayed. This list contains a tree for
     * each linked logical parent, then the logical tree, and last, in mixed
     * mode, a tree with all the unlinked media, or in separate mode, the
     * physical tree.
     */
    private final List<DefaultTreeNode> trees = new ArrayList<>();

    /**
     * Creates a new structure panel.
     *
     * @param dataEditor
     *            the master meta-data editor
     */
    StructurePanel(DataEditorForm dataEditor) {
        this.dataEditor = dataEditor;
    }

    /**
     * Clear content.
     */
    public void clear() {
        trees.clear();
        structureTree = null;
        selectedNode = null;
        previouslySelectedNode = null;
        structure = null;
    }

    void deleteSelectedStructure() {
        Optional<Structure> selectedStructure = getSelectedStructure();
        if (!selectedStructure.isPresent()) {
            /*
             * No element is selected or the selected element is not a structure
             * but, for example, a media unit.
             */
            return;
        }
        LinkedList<Structure> ancestors = MetadataEditor.getAncestorsOfStructure(selectedStructure.get(), structure);
        if (ancestors.isEmpty()) {
            // The selected element is the root node of the tree.
            return;
        }
        Structure parent = ancestors.getLast();
        parent.getChildren().remove(selectedStructure.get());
        show();
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    Optional<Structure> getSelectedStructure() {
        StructureTreeNode structureTreeNode = (StructureTreeNode) selectedNode.getData();
        Object dataObject = structureTreeNode.getDataObject();
        return Optional.ofNullable(dataObject instanceof Structure ? (Structure) dataObject : null);
    }

    public List<DefaultTreeNode> getTrees() {
        return trees;
    }

    /**
     * Updates the live structure of the workpiece with the current members of
     * the structure tree in their given order. The live structure of the
     * workpiece which is stored in the root element of the structure tree.
     */
    void preserve() {
        if (!structureTree.getChildren().isEmpty()) {
            preserveRecursive(structureTree.getChildren().get(0));
        }
    }

    /**
     * Updates the live structure of a structure tree node and returns it, to
     * provide for updating the parent. If the tree node contains children which
     * aren’t structures, {@code null} is returned to skip them on the level
     * above.
     */
    private static Structure preserveRecursive(TreeNode treeNode) {
        StructureTreeNode structureTreeNode = (StructureTreeNode) treeNode.getData();
        if (Objects.isNull(structureTreeNode) || !(structureTreeNode.getDataObject() instanceof Structure)) {
            return null;
        }
        Structure structure = (Structure) structureTreeNode.getDataObject();

        List<Structure> childrenLive = structure.getChildren();
        childrenLive.clear();
        for (TreeNode child : treeNode.getChildren()) {
            Structure maybeChildStructure = preserveRecursive(child);
            if (maybeChildStructure != null) {
                childrenLive.add(maybeChildStructure);
            }
        }
        return structure;
    }

    /**
     * Set selected TreeNode.
     *
     * @param selected
     *          TreeNode that will be selected
     */
    public void setSelectedNode(TreeNode selected) {
        if (Objects.nonNull(selected)) {
            this.selectedNode = selected;
        }
    }

    /**
     * Loads the tree(s) into the panel and sets the selected element to the
     * root element of the structure tree.
     */
    void show() {
        trees.clear();
        this.structure = dataEditor.getWorkpiece().getStructure();
        Pair<List<DefaultTreeNode>, Collection<View>> result = buildStructureTree();
        trees.addAll(result.getLeft());
        if (separateMedia != null) {
            Set<MediaUnit> mediaUnitsShowingOnTheStructureTree = result.getRight().parallelStream()
                    .map(View::getMediaUnit).collect(Collectors.toSet());
            DefaultTreeNode mediaTree = buildMediaTree(dataEditor.getWorkpiece().getMediaUnit().getChildren(),
                mediaUnitsShowingOnTheStructureTree);
            if (mediaTree != null) {
                trees.add(mediaTree);
            }
        }
        this.structureTree = trees.get(result.getLeft().size() - 1);
        this.selectedNode = structureTree.getChildren().get(0);
        this.previouslySelectedNode = selectedNode;
    }

    /**
     * Creates the structure tree. If hierarchical links exist upwards, they are
     * displayed above the tree as separate trees.
     *
     * @return the structure tree(s) and the collection of views displayed in
     *         the tree
     */
    private Pair<List<DefaultTreeNode>, Collection<View>> buildStructureTree() {

        DefaultTreeNode result = new DefaultTreeNode();
        result.setExpanded(true);
        Collection<View> viewsShowingOnAChild = buildStructureTreeRecursively(structure, result);
        return Pair.of(Collections.singletonList(result), viewsShowingOnAChild);
    }

    private Collection<View> buildStructureTreeRecursively(Structure structure, TreeNode result) {

        StructuralElementViewInterface divisionView = dataEditor.getRuleset().getStructuralElementView(
            structure.getType(), dataEditor.getAcquisitionStage(), dataEditor.getPriorityList());
        /*
         * Creating the tree node by handing over the parent node automatically
         * appends it to the parent as a child. That’s the logic of the JSF
         * framework. So you do not have to add the result anywhere.
         */
        DefaultTreeNode parent = new DefaultTreeNode(
                new StructureTreeNode(this, divisionView.getLabel(), divisionView.isUndefined(), false, structure),
                result);
        parent.setExpanded(true);

        Set<View> viewsShowingOnAChild = new HashSet<>();
        for (Structure child : structure.getChildren()) {
            viewsShowingOnAChild.addAll(buildStructureTreeRecursively(child, parent));
        }

        if (Boolean.FALSE.equals(separateMedia)) {
            String page = Helper.getTranslation("page").concat(" ");
            for (View view : structure.getViews()) {
                if (!viewsShowingOnAChild.contains(view)) {
                    new DefaultTreeNode(new StructureTreeNode(this, page.concat(view.getMediaUnit().getOrderlabel()),
                            false, false, view), parent).setExpanded(true);
                    viewsShowingOnAChild.add(view);
                }
            }
        }
        return viewsShowingOnAChild;
    }

    /**
     * Creates the media tree.
     *
     * @param mediaUnits
     *            media units to show on the tree
     * @param mediaUnitsShowingOnTheStructureTree
     *            media units already showing on the structure tree. In mixed
     *            mode, only media units not yet linked anywhere will show in
     *            the second tree as unlinked media.
     *
     * @return the media tree
     */
    private DefaultTreeNode buildMediaTree(List<MediaUnit> mediaUnits,
            Collection<MediaUnit> mediaUnitsShowingOnTheStructureTree) {
        DefaultTreeNode result = new DefaultTreeNode();
        result.setExpanded(true);

        /*
         * Creating the tree node by handing over the parent node automatically
         * appends it to the parent as a child. That's the logic of the JSF
         * framework. So you do not have to add the result anywhere.
         */
        DefaultTreeNode mediaTreeRoot = new DefaultTreeNode(new StructureTreeNode(this,
                Helper.getTranslation(separateMedia ? "dataEditor.mediaTree" : "dataEditor.unlinkedMediaTree"), false,
                false, mediaUnits), result);
        mediaTreeRoot.setExpanded(true);

        String page = Helper.getTranslation("page").concat(" ");
        boolean isEmpty = true;
        for (MediaUnit mediaUnit : mediaUnits) {
            if (Boolean.TRUE.equals(separateMedia) || !mediaUnitsShowingOnTheStructureTree.contains(mediaUnit)) {
                new DefaultTreeNode(
                        new StructureTreeNode(this, page.concat(mediaUnit.getOrderlabel()), false, false, mediaUnit),
                        mediaTreeRoot).setExpanded(true);
                isEmpty = false;
            }
        }
        return !isEmpty ? result : null;
    }

    void treeElementSelect() {
        /*
         * The newly selected element has already been set in 'selectedNode' by
         * JSF at this point.
         */
        try {
            dataEditor.switchStructure();
            previouslySelectedNode = selectedNode;
        } catch (Exception e) {
            Helper.setErrorMessage(e.getLocalizedMessage());
            selectedNode = previouslySelectedNode;
        }
    }
}

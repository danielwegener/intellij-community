package com.intellij.ide.structureView.newStructureView;

import com.intellij.ide.structureView.ModelListener;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.util.treeView.*;
import com.intellij.ide.util.treeView.smartTree.SmartTreeStructure;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.Alarm;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

final class StructureTreeBuilder extends AbstractTreeBuilder {
  private final Project myProject;
  private final StructureViewModel myStructureModel;

  private final MyCopyPasteListener myCopyPasteListener;
  private final PsiTreeChangeListener myPsiTreeChangeListener;
  private final ModelListener myModelListener;

  private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

  public StructureTreeBuilder(Project project,
                              JTree tree,
                              DefaultTreeModel treeModel,
                              AbstractTreeStructure treeStructure,
                              StructureViewModel structureModel) {
    super(
      tree,
      treeModel,
      treeStructure, null
    );

    myProject = project;
    myStructureModel = structureModel;

    myPsiTreeChangeListener = new MyPsiTreeChangeListener();
    myModelListener = new ModelListener() {
      public void onModelChanged() {
        myUpdater.addSubtreeToUpdate(myRootNode);
      }
    };
    PsiManager.getInstance(myProject).addPsiTreeChangeListener(myPsiTreeChangeListener);

    myCopyPasteListener = new MyCopyPasteListener();
    CopyPasteManager.getInstance().addContentChangedListener(myCopyPasteListener);
    initRootNode();
    myStructureModel.addModelListener(myModelListener);
  }

  public void dispose() {
    myUpdateAlarm.cancelAllRequests();
    PsiManager.getInstance(myProject).removePsiTreeChangeListener(myPsiTreeChangeListener);
    CopyPasteManager.getInstance().removeContentChangedListener(myCopyPasteListener);
    myStructureModel.removeModelListener(myModelListener);
    super.dispose();
  }

  protected boolean isAlwaysShowPlus(NodeDescriptor nodeDescriptor) {
    return ((AbstractTreeNode)nodeDescriptor).isAlwaysShowPlus();
  }

  protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
    return false;
  }

  protected boolean isSmartExpand() {
    return false;
  }

  protected final AbstractTreeUpdater createUpdater(){
    return new AbstractTreeUpdater(this) {
      protected void updateSubtree(DefaultMutableTreeNode node) {
        if(!myProject.isDisposed()) {
          super.updateSubtree(node);
        }
      }
    };
  }
  
  
  private final class MyPsiTreeChangeListener extends PsiTreeChangeAdapter {

    public MyPsiTreeChangeListener() {
    }

    public void childRemoved(PsiTreeChangeEvent event) {
      PsiElement child = event.getOldChild();
      if (child instanceof PsiWhiteSpace) return; //optimization

      childrenChanged();
    }

    public void childAdded(PsiTreeChangeEvent event) {
      PsiElement child = event.getNewChild();
      if (child instanceof PsiWhiteSpace) return; //optimization
      childrenChanged();
    }

    public void childReplaced(PsiTreeChangeEvent event) {
      PsiElement oldChild = event.getOldChild();
      PsiElement newChild = event.getNewChild();
      if (oldChild instanceof PsiWhiteSpace && newChild instanceof PsiWhiteSpace) return; //optimization
      if (oldChild instanceof PsiCodeBlock && newChild instanceof PsiCodeBlock) return; //optimization
      childrenChanged();
    }

    public void childMoved(PsiTreeChangeEvent event) {
      childrenChanged();
      childrenChanged();
    }

    public void childrenChanged(PsiTreeChangeEvent event) {
      childrenChanged();
    }

    private void childrenChanged() {
      myUpdateAlarm.cancelAllRequests();
      myUpdateAlarm.addRequest(new Runnable() {
        public void run() {
          PsiDocumentManager.getInstance(myProject).commitAllDocuments();
          ((SmartTreeStructure)getTreeStructure()).rebuildTree();
          myUpdater.addSubtreeToUpdate(myRootNode);
        }
      }, 300);
    }

    public void propertyChanged(PsiTreeChangeEvent event) {
      myUpdater.addSubtreeToUpdate(myRootNode);
    }
  }

  private final class MyCopyPasteListener implements CopyPasteManager.ContentChangedListener {
    public void contentChanged() {
      myUpdater.addSubtreeToUpdate(myRootNode);
    }
  }
}
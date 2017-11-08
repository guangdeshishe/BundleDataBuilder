package com.agilezhu.bundledatabuilder;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;

/**
 * Created by AgileZhu on 17/10/25.
 */
public class GenarateCodeAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        //显示选择生成Bundle数据字段弹窗
        SelectFieldsDialog dialog = new SelectFieldsDialog();
        dialog.bindFieldsData(e);
        dialog.setVisible(true);

    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        PsiClass psiClass = CodeUtil.getPsiClassFromContext(e);
        //判断是否在类里面
        if (editor == null || psiClass == null){
            //没有在某个类里则隐藏
            e.getPresentation().setVisible(false);
            return;
        }
        e.getPresentation().setVisible(true);
    }

    //显示一个弹窗消息
    public void showMessage(AnActionEvent e, String message) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        Messages.showMessageDialog(project, message, "温馨提示", Messages.getInformationIcon());
    }
}

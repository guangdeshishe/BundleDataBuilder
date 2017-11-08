package com.agilezhu.bundledatabuilder;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.ui.components.JBList;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;

public class SelectFieldsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonCancel;
    private JButton buttonOk;
    private JScrollPane fieldListPannel;
    private JCheckBox selectAllBtn;
    private JList fieldList;
    private AnActionEvent anActionEvent;
    private PsiClass psiClass;
    DefaultListModel<String> listModel;

    public SelectFieldsDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOk);

//        setLocationRelativeTo(null);
        setSize(500, 300);
        int x = (Toolkit.getDefaultToolkit().getScreenSize().width - this.getSize().width) / 2;
        int y = (Toolkit.getDefaultToolkit().getScreenSize().height - this.getSize().height) / 2;
        this.setLocation(x, y);

        fieldList = new JList();
        fieldList.setBorder(null);
        fieldListPannel.setViewportView(fieldList);

        setTitle("Select Fields for Builder Generation");

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        selectAllBtn.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (selectAllBtn.isSelected()) {
                    selectAllItems();
                } else {
                    unSelectAllItems();
                }
            }
        });
    }

    private void onOK() {
        int[] selectedIndices = fieldList.getSelectedIndices();
        PsiField[] fields = getFields(psiClass);
        PsiField[] selectedFields = new PsiField[selectedIndices.length];
        int i = 0;
        for (int selectIndex : selectedIndices) {
            selectedFields[i] = fields[selectIndex];
            i++;
        }
        WriteCommandAction.runWriteCommandAction(psiClass.getProject(), new Runnable() {
            @Override
            public void run() {
                CodeUtil.generateBundleDataBuilderCode(anActionEvent, psiClass, selectedFields);
            }
        });

        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        SelectFieldsDialog dialog = new SelectFieldsDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    public void bindFieldsData(AnActionEvent e) {
        psiClass = CodeUtil.getPsiClassFromContext(e);
        PsiField[] fields = getFields(psiClass);
        anActionEvent = e;
        this.psiClass = psiClass;
        listModel = new DefaultListModel<>();
        for (PsiField field : fields) {
            if (!field.getName().startsWith(CodeUtil.STATIC_FINAL_KEY_START)) {
                listModel.addElement(field.getName());
            }
        }
        fieldList.setModel(listModel);
        selectAllItems();
    }

    private void selectAllItems() {
        if (listModel == null || listModel.size() == 0) {
            return;
        }
        int[] indexs = new int[listModel.size()];
        for (int i = 0; i < indexs.length; i++) {
            indexs[i] = i;
        }
        fieldList.setSelectedIndices(indexs);
    }

    private void unSelectAllItems(){
        fieldList.setSelectedIndices(new int[0]);
    }

    private PsiField[] getFields(PsiClass psiClass) {
        return psiClass.getFields();
    }
}

package com.agilezhu.bundledatabuilder;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.HashMap;

public class CodeUtil {

    //数据类型对应的Bundle put get方法名
    public static final HashMap<String, String> mBaseFieldType = new HashMap<>();
    public static final String STATIC_FINAL_KEY_START = "BUNDLE_DATA_KEY_";//静态变量名开头

    static {
        mBaseFieldType.put("byte", "Byte");
        mBaseFieldType.put("byte[]", "ByteArray");
        mBaseFieldType.put("short", "Short");
        mBaseFieldType.put("short[]", "ShortArray");
        mBaseFieldType.put("int", "Int");
        mBaseFieldType.put("int[]", "IntArray");
        mBaseFieldType.put("ArrayList<Integer>", "IntegerArrayList");
        mBaseFieldType.put("float", "Float");
        mBaseFieldType.put("float[]", "FloatArray");
        mBaseFieldType.put("long", "Long");
        mBaseFieldType.put("long[]", "LongArray");
        mBaseFieldType.put("double", "Double");
        mBaseFieldType.put("double[]", "DoubleArray");
        mBaseFieldType.put("char", "Char");
        mBaseFieldType.put("char[]", "CharArray");
        mBaseFieldType.put("CharSequence", "CharSequence");
        mBaseFieldType.put("CharSequence[]", "CharSequenceArray");
        mBaseFieldType.put("ArrayList<CharSequence>", "CharSequenceArrayList");
        mBaseFieldType.put("String", "String");
        mBaseFieldType.put("String[]", "StringArray");
        mBaseFieldType.put("ArrayList<String>", "StringArrayList");
        mBaseFieldType.put("boolean", "Boolean");
        mBaseFieldType.put("boolean[]", "BooleanArray");
        mBaseFieldType.put("PersistableBundle", "All");
        mBaseFieldType.put("Bundle", "Bundle");
        mBaseFieldType.put("IBinder", "Binder");
        mBaseFieldType.put("Size", "Size");
        mBaseFieldType.put("SizeF", "SizeF");
    }

    //返回当前打开的class类
    public static PsiClass getPsiClassFromContext(AnActionEvent e) {
        PsiFile psiFile = e.getData(PlatformDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (psiFile == null || editor == null) {
            return null;
        }
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        //通过光标位置，查找到所属的类
        return PsiTreeUtil.getParentOfType(element, PsiClass.class);
    }

    //生成BundleDataBuilder代码
    public static void generateBundleDataBuilderCode(AnActionEvent e, PsiClass psiClass, PsiField[] selectedFields) {
        //删除已经存在的parse或者build方法
        deleteRelativeMethod(psiClass);

        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());

        PsiField[] keyFields = new PsiField[selectedFields.length];

        //删除自动生成的key静态变量
        deleteGeneratedFiled(psiClass);

        //自动生成静态变量key
        generateStaticFinalField(elementFactory, psiClass, selectedFields, keyFields);

        //生成Parse方法
        generateParseMethod(elementFactory, psiClass, selectedFields, keyFields);

        //生成Build方法
        generateBuildMethod(elementFactory, psiClass, selectedFields, keyFields);

        //生成get方法
        generateGetMethod(elementFactory, psiClass, selectedFields);

        //生成set方法
        generateSetMethod(elementFactory, psiClass, selectedFields);
    }

    //删除自动生成的key静态变量
    private static void deleteGeneratedFiled(PsiClass psiClass) {
        PsiField[] allFiedls = psiClass.getFields();
        for (PsiField field : allFiedls) {
            if (field.getName().startsWith(STATIC_FINAL_KEY_START)) {
                field.delete();
            }
        }
    }

    //删除已经存在的parse或者build方法
    private static void deleteRelativeMethod(PsiClass psiClass) {
        PsiMethod[] methods = psiClass.getMethods();
        for (PsiMethod method : methods) {
            String methodName = method.getName();
            if (methodName.contains("parse") || methodName.contains("build")) {
                method.delete();
            }
        }
    }

    //生成get方法
    private static void generateGetMethod(PsiElementFactory elementFactory, PsiClass psiClass, PsiField[] selectedFields) {
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        for (int i = 0; i < selectedFields.length; i++) {
            PsiField field = selectedFields[i];
            String fieldType = field.getTypeElement().getType().getPresentableText();
            String methodString = "public " + fieldType + " get" + getFieldRealName(field.getName()) + "(){\nreturn " + field.getName() + ";\n}\n";

            PsiMethod parseMethod = elementFactory.createMethodFromText(methodString, psiClass);
            styleManager.shortenClassReferences(psiClass.addBefore(parseMethod, psiClass.getLastChild()));
        }

    }

    //生成set方法
    private static void generateSetMethod(PsiElementFactory elementFactory, PsiClass psiClass, PsiField[] selectedFields) {
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        for (int i = 0; i < selectedFields.length; i++) {
            PsiField field = selectedFields[i];
            String fieldType = field.getTypeElement().getType().getPresentableText();
            String readFieldName = getFieldRealName(field.getName());
            String methodString = "public "+ psiClass.getName()+ " set" + readFieldName + "("+fieldType+" "+readFieldName+"){\n"+field.getName()+"=" + readFieldName + ";\nreturn this;\n}\n";

            PsiMethod parseMethod = elementFactory.createMethodFromText(methodString, psiClass);
            styleManager.shortenClassReferences(psiClass.addBefore(parseMethod, psiClass.getLastChild()));
        }

    }

    //生成Parse方法
    private static void generateParseMethod(PsiElementFactory elementFactory, PsiClass psiClass, PsiField[] selectedFields, PsiField[] keyFields) {
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        StringBuilder builder = new StringBuilder("public void parse(Bundle bundle) {\n");
        for (int i = 0; i < selectedFields.length; i++) {
            PsiField field = selectedFields[i];
            PsiField keyField = keyFields[i];
            builder.append(field.getName() + " = bundle.get" + getBundleDataType(field) + "(" + keyField.getName() + ");\n");
        }
        builder.append("}");
        PsiMethod parseMethod = elementFactory.createMethodFromText(builder.toString(), psiClass);
        styleManager.shortenClassReferences(psiClass.addBefore(parseMethod, psiClass.getLastChild()));
    }

    //生成Build方法
    private static void generateBuildMethod(PsiElementFactory elementFactory, PsiClass psiClass, PsiField[] selectedFields, PsiField[] keyFields) {
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        StringBuilder builder = new StringBuilder("public Bundle build() {\n" +
                "Bundle bundle = new Bundle();\n");
        for (int i = 0; i < selectedFields.length; i++) {
            PsiField field = selectedFields[i];
            PsiField keyField = keyFields[i];
            builder.append("bundle.put" + getBundleDataType(field) + "(" + keyField.getName() + ", " + field.getName() + ");\n");
        }
        builder.append("return bundle;\n}");
        PsiMethod parseMethod = elementFactory.createMethodFromText(builder.toString(), psiClass);
        styleManager.shortenClassReferences(psiClass.addBefore(parseMethod, psiClass.getLastChild()));
    }

    //返回Bundle数据类型
    private static String getBundleDataType(PsiField field) {
        String fieldType = field.getTypeElement().getType().getPresentableText();
        PsiClass fieldTypeClass = PsiTreeUtil.getParentOfType(field.getTypeElement(), PsiClass.class);

        if (mBaseFieldType.containsKey(fieldType)) {
            return mBaseFieldType.get(fieldType);
        }
        if (fieldType.contains("[]")) {
            return "ParcelableArray";
        } else if (fieldType.startsWith("ArrayList")) {
            return "ParcelableArrayList";
        } else if (fieldType.startsWith("SparseArray")) {
            return "SparseParcelableArray";
        }
        return "Parcelable";
    }

    //返回去除m开头的变量名
    private static String getFieldRealName(String fieldName) {
//        fieldName = fieldName.replaceAll("\\[]", "");
//        fieldName = fieldName.replaceAll("<.*>","");
        if (fieldName.startsWith("m")) {
            return upperFirstChar(fieldName.substring(1));
        }
        return upperFirstChar(fieldName);
    }

    //将第一个字母大小
    private static String upperFirstChar(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    //生成静态变量
    private static void generateStaticFinalField(PsiElementFactory elementFactory, PsiClass psiClass, PsiField[] selectedFields, PsiField[] keyFields) {
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(psiClass.getProject());
        int i = 0;
        for (PsiField field : selectedFields) {
            String fileName = field.getName();
            String fieldString = "private static final String " + STATIC_FINAL_KEY_START + fileName.toUpperCase() + " = \"bundle_data_key_" + fileName + "\";";
            PsiField keyField = elementFactory.createFieldFromText(fieldString, psiClass);
            keyFields[i] = keyField;
            i++;
            styleManager.shortenClassReferences(psiClass.addBefore(keyField, psiClass.getLastChild()));
        }
    }
}

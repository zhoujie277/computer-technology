package com.future.javac.processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementScanner8;
import javax.tools.Diagnostic;
import java.util.EnumSet;

public class NameCheckScanner extends ElementScanner8<Void, Void> {

    private final Messager messager;

    public NameCheckScanner(Messager messager) {
        this.messager = messager;
    }

    /**
     * 检查 Java 类
     */
    @Override
    public Void visitType(TypeElement e, Void p) {
        scan(e.getTypeParameters(), p);
        checkCamelCase(e, true);
        super.visitType(e, p);
        return null;
    }

    /**
     * 检查方法命名是否合法
     */
    @Override
    public Void visitExecutable(ExecutableElement e, Void unused) {
        if (e.getKind() == ElementKind.METHOD) {
            Name name = e.getSimpleName();
            if (name.contentEquals(e.getEnclosingElement().getSimpleName())) {
                messager.printMessage(Diagnostic.Kind.WARNING, "一个普通方法 \"" + name + "\" 不应当与类名重复，避免与构造函数混淆", e);
            }
            checkCamelCase(e, false);
        }
        super.visitExecutable(e, unused);
        return null;
    }

    /**
     * 检查变量命名是否合法
     */
    @Override
    public Void visitVariable(VariableElement e, Void unused) {
        // 如果这个 variable 是枚举或常量，则按大写命名检查，否则按照驼峰命名法则检查。
        if (e.getKind() == ElementKind.ENUM_CONSTANT || e.getConstantValue() != null
                || heuristicallyConstant(e)) {
            checkAllCaps(e);
        } else {
            checkCamelCase(e, false);
        }
        super.visitVariable(e, unused);
        return null;
    }

    /**
     * 判断一个变量是否是常量
     */
    private boolean heuristicallyConstant(VariableElement e) {
        if (e.getEnclosingElement().getKind() == ElementKind.INTERFACE) {
            return true;
        } else if (e.getKind() == ElementKind.FIELD
                && e.getModifiers().containsAll(EnumSet.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL))) {
            return true;
        }
        return false;
    }

    private void checkCamelCase(Element e, boolean initialCaps) {
        String name = e.getSimpleName().toString();
        boolean previousUpper = false;
        boolean conventional = true;
        int firstCodePoint = name.codePointAt(0);

        if (Character.isUpperCase(firstCodePoint)) {
            previousUpper = true;
            if (!initialCaps) {
                messager.printMessage(Diagnostic.Kind.WARNING, "名称 \"" + name + " \" 应当以小写字母开头", e);
                return;
            }
        } else if (Character.isLowerCase(firstCodePoint)) {
            if (initialCaps) {
                messager.printMessage(Diagnostic.Kind.WARNING, "名称 \"" + name + "\" 应当以大写字母开头", e);
                return;
            }
        } else {
            conventional = false;
        }
        if (conventional) {
            int cp = firstCodePoint;
            for (int i = Character.charCount(cp); i < name.length(); i += Character.charCount(cp)) {
                cp = name.codePointAt(i);
                if (Character.isUpperCase(cp)) {
                    if (previousUpper) {
                        conventional = false;
                        break;
                    }
                    previousUpper = true;
                } else {
                    previousUpper = false;
                }
            }
        }
        if (!conventional) {
            messager.printMessage(Diagnostic.Kind.WARNING, "名称 \"" + name + "\" 应当符合驼峰命名法。", e);

        }
    }

    private void checkAllCaps(Element e) {
        String name = e.getSimpleName().toString();

        boolean conventional = true;
        int firstCodePoint = name.codePointAt(0);
        if (!Character.isUpperCase(firstCodePoint)) {
            conventional = false;
        } else {
            boolean previousUnderscore = false;
            int cp = firstCodePoint;
            for (int i = Character.charCount(cp); i < name.length(); i += Character.charCount(cp)) {
                cp = name.codePointAt(i);
                if (cp == (int) '_') {
                    if (previousUnderscore) {
                        conventional = false;
                        break;
                    }
                    previousUnderscore = true;
                } else {
                    previousUnderscore = false;
                    if (!Character.isUpperCase(cp) && !Character.isDigit(cp)) {
                        conventional = false;
                        break;
                    }
                }
            }
        }
        if (!conventional) {
            messager.printMessage(Diagnostic.Kind.WARNING, "常量 \"" + name + "\" 应当全部以大写字母或下划线命名，并且以字母开头", e);
        }
    }
}

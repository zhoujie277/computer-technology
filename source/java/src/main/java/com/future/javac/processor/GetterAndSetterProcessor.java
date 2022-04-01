package com.future.javac.processor;

import com.future.annotation.JGetterAndSetter;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * 根据注解生成 GetterAndSetter 方法
 *
 * @author future
 */
@SupportedAnnotationTypes("com.future.annotation.JGetterAndSetter")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SuppressWarnings("unused")
public class GetterAndSetterProcessor extends AbstractProcessor {

    private JavacTrees javacTrees;

    // 封装了创建语法树结点的 API。
    private TreeMaker treeMaker;
    // Names 提供了访问标识符 Name 的方法
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        JavacProcessingEnvironment env = (JavacProcessingEnvironment) processingEnv;
        Context context = env.getContext();
        javacTrees = JavacTrees.instance(processingEnv);
        treeMaker = TreeMaker.instance(context);
        names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(JGetterAndSetter.class);
        for (Element element : annotated) {
            // 抽象语法树元素的基类
            JCTree tree = javacTrees.getTree(element);
            tree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    jcClassDecl.defs.stream()
                            .filter(it -> it.getKind().equals(Tree.Kind.VARIABLE))
                            .map(it -> (JCTree.JCVariableDecl) it)
                            .forEach(it -> {
                                jcClassDecl.defs = jcClassDecl.defs.prepend(genGetterMethod(it));
                                jcClassDecl.defs = jcClassDecl.defs.prepend(genSetterMethod(it));
                            });
                    super.visitClassDef(jcClassDecl);
                }
            });
        }
        return true;
    }

    private JCTree.JCMethodDecl genGetterMethod(JCTree.JCVariableDecl jcVariableDec1) {
        JCTree.JCReturn returnStatement = treeMaker.Return(treeMaker.Select(
                treeMaker.Ident(names.fromString("this")), jcVariableDec1.getName()));
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<JCTree.JCStatement>().append(returnStatement);
        // public 方法访问级别修饰符
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC);
        // 方法名 (getXxx), 根据字段名生成首字母大写的 get 方法
        Name getMethodName = getMethodName(jcVariableDec1.getName());
        // 返回值类型，get 方法的返回值类型与字段类型一样
        JCTree.JCExpression returnMethodType = jcVariableDec1.vartype;
        // 生成方法体
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        // 泛型参数列表
        List<JCTree.JCTypeParameter> methodGenericParamList = List.nil();
        // 参数值列表
        List<JCTree.JCVariableDecl> parameterList = List.nil();
        // 异常抛出列表
        List<JCTree.JCExpression> thrownCauseList = List.nil();
        return treeMaker.MethodDef(modifiers, getMethodName, returnMethodType, methodGenericParamList, parameterList, thrownCauseList, body, null);
    }

    private JCTree.JCMethodDecl genSetterMethod(JCTree.JCVariableDecl jcVariableDecl) {
        // this.value = value;
        JCTree.JCExpressionStatement statement = treeMaker.Exec(treeMaker.Assign(
                treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName()),
                treeMaker.Ident(jcVariableDecl.getName())));
        // 语句
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<JCTree.JCStatement>().append(statement);
        // set 方法参数
        JCTree.JCVariableDecl param = treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER, List.nil())
                , jcVariableDecl.name, jcVariableDecl.vartype, null);
        // 方法访问修饰符 public
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC);
        // 方法名
        Name setMethodName = setMethodName(jcVariableDecl.getName());
        // 返回值类型
        JCTree.JCExpression returnMethodType = treeMaker.Type(new Type.JCVoidType());
        // 生成方法体
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        // 泛型参数列表
        List<JCTree.JCTypeParameter> methodGenericParamList = List.nil();
        // 参数值列表
        List<JCTree.JCVariableDecl> parameterList = List.of(param);
        // 异常抛出列表
        List<JCTree.JCExpression> thrownCauseList = List.nil();
        return treeMaker.MethodDef(modifiers, setMethodName, returnMethodType, methodGenericParamList
                , parameterList, thrownCauseList, body, null);
    }

    private Name getMethodName(Name name) {
        String fieldName = name.toString();
        return names.fromString("get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
    }

    private Name setMethodName(Name name) {
        String fieldName = name.toString();
        return names.fromString("set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
    }
}


















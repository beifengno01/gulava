/*
 *  Copyright (c) 2015 The Gulava Authors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package gulava.processor;

import gulava.annotation.MakeLogicValue;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.tools.Diagnostic;

/**
 * A {@link TypeVisitor} which generates a fresh instantiation of the given type mirror. This
 * means that {@code ?} and {@link Object} generic type parameters are instantiated to new
 * variables. {@link Void} generic type parameters are instantiated as {@code null} references.
 *
 * <p>For instance, if the type mirror is {@code Cons<?, Cons<?, Void>>} (which represents a
 * sequence of exactly two elements), this visitor will generate
 * {@code "new Cons<>(new Var(), new Cons<>(new Var(), null))"} (the classes will be namespace-
 * qualified in the actual generated code).
 */
public final class FreshInstantiation extends SimpleTypeVisitor6<String, Void> {
  private final Element element;
  private final Messager messager;

  /**
   * @param element the element from which the {@link TypeMirror}s this will visit originates.
   *     This is used for error reporting.
   * @param messager what to report errors to
   */
  public FreshInstantiation(Element element, Messager messager) {
    this.element = element;
    this.messager = messager;
  }

  @Override
  public String visitWildcard(WildcardType type, Void v) {
    if ((type.getExtendsBound() != null) || (type.getSuperBound() != null)) {
      messager.printMessage(Diagnostic.Kind.ERROR,
          "wildcard bounds not allowed here: " + type,
          element);
      return "null";
    }
    return "new " + ClassNames.VAR + "()";
  }

  @Override
  public String visitDeclared(DeclaredType type, Void v) {
    Name typeName = Processors.qualifiedName(type);
    TypeElement typeElement = (TypeElement) type.asElement();

    if (typeName.contentEquals("java.lang.Object")) {
      return "new " + ClassNames.VAR + "()";
    }
    if (typeName.contentEquals("java.lang.Void")) {
      return "null";
    }

    List<String> subInstantiations = new ArrayList<>();
    for (TypeMirror typeArgument : type.getTypeArguments()) {
      subInstantiations.add(visit(typeArgument));
    }

    if (typeElement.getModifiers().contains(Modifier.ABSTRACT)
        || (typeElement.getSuperclass() instanceof NoType)) {
      // typeElement is abstract or an interface
      return typeName + ".of(" + Processors.join(", ", subInstantiations) + ")";
    }
    if (subInstantiations.isEmpty()) {
      return "new " + typeName + "()";
    }
    return "new " + typeName + "<>(" + Processors.join(", ", subInstantiations) + ")";
  }

  @Override
  public String visitUnknown(TypeMirror type, Void v) {
    messager.printMessage(Diagnostic.Kind.ERROR,
        "Unsupported type. Expect Object, ?, Void, or an explicitly parameterized LogicValue: "
        + type,
        element);
    return "null";
  }

  @Override
  public String defaultAction(TypeMirror type, Void v) {
    return visitUnknown(type, v);
  }
}

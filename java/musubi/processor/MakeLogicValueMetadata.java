/*
 *  Copyright (c) 2015 Dmitry Neverov and Google
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
package musubi.processor;

import musubi.annotation.MakeLogicValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 * Contains information derived from a {@code @MakeLogicValue} annotation and the type that it
 * annotates.
 */
public final class MakeLogicValueMetadata {
  private final String name;
  private final List<String> fields;
  private final TypeElement interfaze;
  private final boolean autoDefineToString;

  private MakeLogicValueMetadata(
      String name, List<String> fields, TypeElement interfaze, boolean autoDefineToString) {
    this.name = name;
    this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
    this.interfaze = interfaze;
    this.autoDefineToString = autoDefineToString;
  }

  public String getName() {
    return name;
  }

  public List<String> getFields() {
    return fields;
  }

  public TypeElement getInterface() {
    return interfaze;
  }

  public boolean autoDefineToString() {
    return autoDefineToString;
  }

  /**
   * Returns the metadata stored in a single annotation of type {@code MakeLogicValue}.
   */
  public static MakeLogicValueMetadata forInterface(TypeElement interfaze, Messager messager) {
    MakeLogicValue annotation = interfaze.getAnnotation(MakeLogicValue.class);
    List<String> fields = new ArrayList<>();
    boolean autoDefineToString = true;

    for (ExecutableElement method : ElementFilter.methodsIn(interfaze.getEnclosedElements())) {
      if (method.getSimpleName().contentEquals("toString")) {
        autoDefineToString = false;
      } else if (!method.getModifiers().contains(Modifier.STATIC)) {
        fields.add(method.getSimpleName().toString());
      }
    }

    String name = annotation.name();
    if ((name == null) || name.isEmpty()) {
      messager.printMessage(Diagnostic.Kind.ERROR,
          "Require non-empty name in @MakeLogicValue annotation.", interfaze);
      name = "";
    }

    return new MakeLogicValueMetadata(name, fields, interfaze, autoDefineToString);
  }
}
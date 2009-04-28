/*
 * Copyright (c) 2004 JetBrains s.r.o. All  Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * -Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduct the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of JetBrains or IntelliJ IDEA
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. JETBRAINS AND ITS LICENSORS SHALL NOT
 * BE LIABLE FOR ANY DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT
 * OF OR RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL JETBRAINS OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN
 * IF JETBRAINS HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */
package com.intellij.psi.impl.smartPointers;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.ReflectionCache;
import org.jetbrains.annotations.NotNull;

public class LazyPointerImpl<E extends PsiElement> implements SmartPointerEx<E> {
  private E myElement = null;
  private PsiAnchor myAnchor = null;
  private SmartPsiElementPointer myPointer = null;
  private final Class<? extends PsiElement> myElementClass;
  private final Project myProject;

  public LazyPointerImpl(E element) {
    myElementClass = element.getClass();
    if (element instanceof PsiCompiledElement) {
      myElement = element;
    }
    else {
      myAnchor = PsiAnchor.create(element);
    }
    myProject = element.getProject();
  }

  private static SmartPsiElementPointer setupPointer(PsiElement element) {
    return SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);
  }

  public void fastenBelt() {
    if (myAnchor != null) {
      final PsiElement element = myAnchor.retrieve();
      if (element != null) {
        myPointer = setupPointer(element);
        ((SmartPointerEx)myPointer).fastenBelt();
        myAnchor = null;
      }
      else myAnchor = null;
    }
  }

  public void documentAndPsiInSync() {
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof LazyPointerImpl)) return false;

    final LazyPointerImpl that = (LazyPointerImpl)o;

    if (myAnchor != null ? !myAnchor.equals(that.myAnchor) : that.myAnchor != null) return false;
    if (myElement != null ? !myElement.equals(that.myElement) : that.myElement != null) return false;
    if (myElementClass != null ? !myElementClass.equals(that.myElementClass) : that.myElementClass != null) return false;
    if (myPointer != null ? !myPointer.equals(that.myPointer) : that.myPointer != null) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myElement != null ? myElement.hashCode() : 0);
    result = 31 * result + (myAnchor != null ? myAnchor.hashCode() : 0);
    result = 31 * result + (myPointer != null ? myPointer.hashCode() : 0);
    result = 31 * result + (myElementClass != null ? myElementClass.hashCode() : 0);
    return result;
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  public E getElement() {
    if (myElement != null) return myElement.isValid() ? myElement : null;
    if (myPointer != null) return (E) myPointer.getElement();
    if (myAnchor != null) {
      final PsiElement psiElement = myAnchor.retrieve();
      if (psiElement != null) {
        return ReflectionCache.isAssignable(myElementClass, psiElement.getClass()) ? (E) psiElement : null;
      }
    }

    return null;
  }

  public PsiFile getContainingFile() {
    if (myElement != null) {
      return myElement.getContainingFile();
    }

    if (myAnchor != null) {
      return myAnchor.getFile();
    }

    return null;
  }

}

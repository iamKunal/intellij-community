// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.file.exclude;

import com.google.common.collect.Sets;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.openapi.vfs.newvfs.impl.CachedFileType;
import com.intellij.util.FileContentUtilCore;
import gnu.trove.THashSet;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A persistent {@code Set<VirtualFile>} or persistent {@code Map<VirtualFile, String>}
 */
abstract class PersistentFileSetManager implements PersistentStateComponent<Element> {
  private static final String FILE_ELEMENT = "file";
  private static final String URL_ATTR = "url";
  private static final String VALUE_ATTR = "value";

  private final Map<VirtualFile, String> myMap = new HashMap<>();

  boolean addFile(@NotNull VirtualFile file, @NotNull FileType type) {
    if (!(file instanceof VirtualFileWithId)) {
      throw new IllegalArgumentException("file must be instanceof VirtualFileWithId but got: "+file);
    }
    if (file.isDirectory()) {
      throw new IllegalArgumentException("file must not be directory but got: "+file);
    }
    String value = type.getName();
    String prevValue = myMap.put(file, value);
    if (!value.equals(prevValue)) {
      onFileSettingsChanged(Collections.singleton(file));
    }
    return true;
  }

  boolean removeFile(@NotNull VirtualFile file) {
    boolean isRemoved = myMap.remove(file) != null;
    if (isRemoved) {
      onFileSettingsChanged(Collections.singleton(file));
    }
    return isRemoved;
  }

  String getFileValue(@NotNull VirtualFile file) {
    return myMap.get(file);
  }

  protected void onFileSettingsChanged(@NotNull Collection<? extends VirtualFile> files) {
    WriteAction.run(() -> CachedFileType.clearCache());
    FileContentUtilCore.reparseFiles(files);
  }

  @NotNull
  Collection<VirtualFile> getFiles() {
    return myMap.keySet();
  }

  @Override
  public Element getState() {
    Element root = new Element("root");
    List<Map.Entry<VirtualFile, String>> sorted = new ArrayList<>(myMap.entrySet());
    sorted.sort(Comparator.comparing(e -> e.getKey().getPath()));
    for (Map.Entry<VirtualFile, String> e : sorted) {
      Element element = new Element(FILE_ELEMENT);
      element.setAttribute(URL_ATTR, VfsUtilCore.pathToUrl(e.getKey().getPath()));
      String fileTypeName = e.getValue();
      if (fileTypeName != null && !PlainTextFileType.INSTANCE.getName().equals(fileTypeName)) {
        element.setAttribute(VALUE_ATTR, fileTypeName);
      }
      root.addContent(element);
    }
    return root;
  }

  @Override
  public void loadState(@NotNull Element state) {
    Set<VirtualFile> oldFiles = new THashSet<>(getFiles());
    myMap.clear();
    for (Element fileElement : state.getChildren(FILE_ELEMENT)) {
      Attribute urlAttr = fileElement.getAttribute(URL_ATTR);
      if (urlAttr != null) {
        String url = urlAttr.getValue();
        VirtualFile vf = VirtualFileManager.getInstance().findFileByUrl(url);
        if (vf != null) {
          String value = fileElement.getAttributeValue(VALUE_ATTR);
          myMap.put(vf, Objects.requireNonNullElse(value, PlainTextFileType.INSTANCE.getName()));
        }
      }
    }

    Collection<VirtualFile> toReparse = Sets.symmetricDifference(myMap.keySet(), oldFiles);
    onFileSettingsChanged(toReparse);
  }
}

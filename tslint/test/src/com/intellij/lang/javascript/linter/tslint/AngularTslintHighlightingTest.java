// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.lang.javascript.linter.tslint;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.lang.javascript.linter.LinterHighlightingTest;
import com.intellij.lang.javascript.linter.tslint.highlight.TsLintInspection;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AngularTslintHighlightingTest extends LinterHighlightingTest {

  private static final String RULES_DIRECTORY_TO_PATCH_MARKER = "<RULES_DIRECTORY_TO_PATCH>";

  @Override
  protected String getBasePath() {
    return TsLintTestUtil.getTestDataRelativePath() + "/highlighting/angular";
  }

  @NotNull
  @Override
  protected InspectionProfileEntry getInspection() {
    return new TsLintInspection();
  }

  @Override
  protected String getPackageName() {
    return TslintUtil.PACKAGE_NAME;
  }

  @Override
  protected Map<String, String> getPackageVersionsToInstall() {
    HashMap<String, String> map = new HashMap<>();
    map.put("tslint", null);
    map.put("typescript", null);
    map.put("codelyzer", null);
    map.put("@angular/core", null);
    map.put("@angular/compiler", null);
    map.put("zone.js", null);
    map.put("rxjs", null);
    return map;
  }

  public void testErrorsFromTemplateFileFilteredInTs() {
    doEditorHighlightingTest("app.component.ts", () -> patchAdditionalRulesDir());
  }

  private void patchAdditionalRulesDir() {
    VirtualFile tslintJsonVFile = myFixture.findFileInTempDir("tslint.json");
    try {
      String text = VfsUtilCore.loadText(tslintJsonVFile);
      WriteAction.run(() -> {
        VirtualFile tslintPackageVFile = LocalFileSystem.getInstance().findFileByPath(getNodePackage().getSystemDependentPath());
        String textAfter = text.replace(RULES_DIRECTORY_TO_PATCH_MARKER,
                                        tslintPackageVFile.getParent().findChild("codelyzer").getPath());
        VfsUtil.saveText(tslintJsonVFile, textAfter);
      });
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

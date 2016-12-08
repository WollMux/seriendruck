package de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui;

import java.util.List;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.DatasetSelectionType;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams;

/**
 * Zählt alle Schlüsselwörter auf, die Übergabeargumente für
 * {@link MailMergeController#doMailMerge(List, boolean, DatasetSelectionType, Map)}
 * sein können. Jedes UI-Element steuert in {@link UIElement#addSubmitArgs(Map)},
 * ob und welche Argumente es setzt.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public enum SubmitArgument {
  targetDirectory,
  filenameTemplate,
  emailFrom,
  emailToFieldName,
  emailText,
  emailSubject,
  indexSelection,
}
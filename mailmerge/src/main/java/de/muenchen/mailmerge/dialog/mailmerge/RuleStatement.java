package de.muenchen.mailmerge.dialog.mailmerge;

import de.muenchen.mailmerge.dialog.mailmerge.gui.Section;
import de.muenchen.mailmerge.dialog.mailmerge.gui.UIElement;

/**
 * Folgende Statements können innerhalb des Regeln-Abschnitts der
 * Seriendruckdialog-Beschreibung angewendet werden. Über sie werden z.B. die
 * Sichtbarkeiten der {@link Section}s und {@link UIElement}e gesteuert und die pro
 * Option zu verwendenden Druckfunktionen spezifiziert.
 *
 * Die Methode {@link #getByname(String)} ermöglicht eine Zuordnung von Strings der
 * Konfigurationsdatei auf den entsprechenden enum-Typen.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
enum RuleStatement {
  ON_ACTION_TYPE,
  ON_OUTPUT,
  SHOW_GROUPS,
  USE_PRINTFUNCTIONS,
  SET_DESCRIPTION,
  IGNORE_DOC_PRINTFUNCTIONS,
  unknown;

  static RuleStatement getByname(String s)
  {
    for (RuleStatement k : RuleStatement.values())
    {
      if (k.toString().equals(s))
        return k;
    }
    return unknown;
  }
}
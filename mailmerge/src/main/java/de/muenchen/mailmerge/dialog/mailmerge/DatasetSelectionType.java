package de.muenchen.mailmerge.dialog.mailmerge;

/**
 * Auf welche Art hat der Benutzer die zu druckenden Datensätze ausgewählt.
 * 
 * @author Matthias Benkmann (D-III-ITD D.10)
 */
public enum DatasetSelectionType {
  /**
   * Alle Datensätze.
   */
  ALL,

  /**
   * Der durch {@link MailMergeNew#rangeStart} und {@link MailMergeNew#rangeEnd}
   * gegebene Wert.
   */
  RANGE,

  /**
   * Die durch {@link MailMergeNew#selectedIndexes} bestimmten Datensätze.
   */
  INDIVIDUAL;
}
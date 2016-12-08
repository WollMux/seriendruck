package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.util.List;
import java.util.Map;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui.SubmitArgument;

/**
 * Übernimmt die Aufgabe des Controllers bezüglich dieser Klasse (MailMergeParams),
 * die die View darstellt.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public interface MailMergeController
{
  /**
   * Gibt Auskunft darüber, ob die Druckfunktion name existiert.
   */
  public boolean hasPrintfunction(String name);

  /**
   * Liefert die Spaltennamen der aktuellen Datenquelle
   */
  public List<String> getColumnNames();

  /**
   * Liefert einen Vorschlag für einen Dateinamen zum Speichern der Einzeldokumente
   * (im Fall von Einzeldokumentdruck und E-Mail versandt), so wie er aus
   * Benutzersicht wahrscheinlich erwünscht ist OHNE Suffix.
   */
  public String getDefaultFilename();

  /**
   * Liefert das Textdokument für das der Seriendruck gestartet werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public XTextDocument getTextDocument();

  /**
   * Startet den MailMerge
   */
  public void doMailMerge(List<String> usePrintFunctions,
      boolean ignoreDocPrintFuncs, DatasetSelectionType datasetSelectionType,
      Map<SubmitArgument, Object> args);
}
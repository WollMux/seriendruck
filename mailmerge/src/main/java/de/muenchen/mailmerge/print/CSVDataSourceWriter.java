package de.muenchen.mailmerge.print;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.mailmerge.ModalDialogs;

/**
 * Implementiert einen DataSourceWriter, der Daten in eine CSV-Datei data.csv in
 * einem frei wählbaren Zielverzeichnis schreibt.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class CSVDataSourceWriter implements DataSourceWriter
{
  /**
   * Enthält die zu erzeugende bzw. erzeugte csv-Datei.
   */
  File csvFile = null;

  /**
   * Sammelt alle über {@link #addDataset(HashMap)} gesetzten Datensätze
   */
  ArrayList<HashMap<String, String>> datasets;

  /**
   * Sammelt die Namen aller über {@link #addDataset(HashMap)} gesetzten Spalten.
   */
  HashSet<String> columns;

  /**
   * Enthält nach einem Aufruf von {@link #getHeaders()} die sortierten Headers.
   */
  ArrayList<String> headers = null;

  /**
   * Wenn {@link #validateColumntHeaders()} Leerzeichen in den Headern findet,
   * müssen die PersistentData des Originaldokuments angepasst werden.
   */
  private boolean adjustPersistentData = false;

  /**
   * Erzeugt einen CSVDataSourceWriter, der die zu erzeugende csv-Datei in
   * parentDir ablegt.
   */
  public CSVDataSourceWriter(File parentDir)
  {
    csvFile = new File(parentDir, OOoBasedMailMerge.TABLE_NAME + ".csv");
    datasets = new ArrayList<HashMap<String, String>>();
    columns = new HashSet<String>();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.DataSourceWriter#getSize
   * ()
   */
  @Override
  public int getSize()
  {
    return datasets.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.DataSourceWriter#addDataset
   * (java.util.HashMap)
   */
  @Override
  public void addDataset(HashMap<String, String> ds) throws Exception
  {
    datasets.add(ds);
    columns.addAll(ds.keySet());
  }

  /*
   * (non-Javadoc)
   * 
   * @seede.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.DataSourceWriter#
   * flushAndClose()
   */
  @Override
  public void flushAndClose() throws Exception
  {
    validateColumnHeaders();

    FileOutputStream fos = new FileOutputStream(csvFile);
    PrintStream p = new PrintStream(fos, true, "UTF-8");
    p.print(line(getHeaders()));
    for (HashMap<String, String> ds : datasets)
    {
      ArrayList<String> entries = new ArrayList<String>();
      for (String key : getHeaders())
      {
        String val = ds.get(key);
        if (val == null) val = "";
        entries.add(val);
      }
      p.print(line(entries));
    }
    p.close();
  }

  /**
   * Überprüft ob die Headerzeilen der Datenquelle gültig sind, dh. keine
   * Zeilenumbrüche enthalten. Wenn Zeilenumbrüche gefunden werden, wird eine
   * entsprechende Meldung angezeigt.
   * 
   * @throws ColumnNotFoundException
   *           Falls die Datenquelle in der Headerzeile mindestens 1 Spalte mit
   *           Zeilenumbruch enthält.
   */
  private void validateColumnHeaders() throws ColumnNotFoundException
  {
    Logger.debug(L.m("validateColumnHeaders()"));
    String invalidHeaders = "";
    for (String key : getHeaders())
    {
      if (key.contains("\n"))
      {
        invalidHeaders += "• " + key + "\n";
      }
    }
    if (!invalidHeaders.isEmpty())
    {
      boolean anpassen =
        ModalDialogs.showQuestionModal(
          L.m("WollMux-Seriendruck"),
          L.m("Zeilenumbrüche in Spaltenüberschriften sind für den Seriendruck nicht erlaubt.\n")
            + L.m("\nBitte entfernen Sie die Zeilenumbrüche aus den folgenden Überschriften der Datenquelle:\n\n")
            + invalidHeaders
            + L.m("\nSoll das Hauptdokument entsprechend angepasst werden?"));

      if (anpassen)
      {
        adjustPersistentData = true;
      }
      throw new ColumnNotFoundException(
        L.m("Spaltenüberschriften enthalten newlines"));
    }
  }

  /**
   * Erzeugt die zu dem durch list repräsentierten Datensatz zugehörige
   * vollständige Textzeile für die csv-Datei.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private String line(List<String> list)
  {
    StringBuffer buf = new StringBuffer();
    for (String el : list)
    {
      if (buf.length() != 0) buf.append(",");
      buf.append(literal(el));
    }
    buf.append("\n");
    return buf.toString();
  }

  /**
   * Erzeugt ein für die csv-Datei gültiges literal aus dem Wert value und
   * übernimmt insbesondere das Escaping der Anführungszeichen.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private String literal(String value)
  {
    String esc = value.replaceAll("\"", "\"\"");
    return "\"" + esc + "\"";
  }

  /**
   * Liefert eine alphabetisch sortierte Liste alle Spaltennamen zurück, die jemals
   * über {@link #addDataset(HashMap)} benutzt bzw. gesetzt wurden.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private ArrayList<String> getHeaders()
  {
    if (headers != null) return headers;
    headers = new ArrayList<String>(columns);
    Collections.sort(headers);
    return headers;
  }

  /**
   * Liefert das File-Objekt der csv-Datei zurück, in die geschrieben wird/wurde.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public File getCSVFile()
  {
    return csvFile;
  }

  /**
   * Liefert den Wert von {@link #adjustPersistentData} zurück.
   * 
   * @author Ulrich Kitzinger (GBI I21)
   */
  @Override
  public boolean isAdjustMainDoc()
  {
    return adjustPersistentData;
  }
}
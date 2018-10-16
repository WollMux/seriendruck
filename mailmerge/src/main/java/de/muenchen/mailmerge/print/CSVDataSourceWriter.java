package de.muenchen.mailmerge.print;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
  ArrayList<Map<String, String>> datasets;

  /**
   * Sammelt die Namen aller über {@link #addDataset(HashMap)} gesetzten Spalten.
   */
  HashSet<String> columns;

  /**
   * Enthält nach einem Aufruf von {@link #getHeaders()} die sortierten Headers.
   */
  ArrayList<String> headers = null;

  /**
   * Erzeugt einen CSVDataSourceWriter, der die zu erzeugende csv-Datei in
   * parentDir ablegt.
   */
  public CSVDataSourceWriter(File parentDir)
  {
    csvFile = new File(parentDir, OOoBasedMailMerge.TABLE_NAME + ".csv");
    datasets = new ArrayList<>();
    columns = new HashSet<>();
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
  public void addDataset(Map<String, String> ds) throws Exception
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
    FileOutputStream fos = new FileOutputStream(csvFile);
    try (PrintStream p = new PrintStream(fos, true, "UTF-8"))
    {
      p.print(line(getHeaders()));
      for (Map<String, String> ds : datasets)
      {
        ArrayList<String> entries = new ArrayList<>();
        for (String key : getHeaders())
        {
          String val = ds.get(key);
          if (val == null)
            val = "";
          entries.add(val);
        }
        p.print(line(entries));
      }
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
    StringBuilder buf = new StringBuilder();
    for (String el : list)
    {
      if (buf.length() != 0)
        buf.append(",");
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
    if (headers != null)
      return headers;
    headers = new ArrayList<>(columns);
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
}
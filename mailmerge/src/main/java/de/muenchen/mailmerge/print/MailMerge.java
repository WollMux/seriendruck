/*
 * Dateiname: MailMerge.java
 * Projekt  : WollMux
 * Funktion : Druckfunktionen für den Seriendruck.
 *
 * Copyright (c) 2010-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 05.01.2007 | BNK | Erstellung
 * 15.01.2007 | BNK | Fortschrittsindikator
 * 29.01.2007 | BNK | "Keine Beschreibung vorhanden" durch Datensatznummer ersetzt.
 * 09.03.2007 | BNK | [P1257]Auch Datenquellen unterstützen, die keine Schlüssel haben.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.mailmerge.print;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sdb.CommandType;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XSheetCellRanges;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.XCellRange;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.Datasource;
import de.muenchen.allg.itd51.wollmux.core.db.OOoDatasource;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.db.TimeoutException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.mailmerge.dialog.mailmerge.MailMergeProgressWindow;

public class MailMerge
{
  /**
   * Anzahl Millisekunden, die maximal gewartet wird, bis alle Datensätze für den
   * Serienbrief aus der Datenbank gelesen wurden.
   */
  static final int DATABASE_TIMEOUT = 20000;

  /**
   * Druckt das zu pmod gehörende Dokument für alle Datensätze (offerSelection==true)
   * oder die Datensätze, die der Benutzer in einem Dialog auswählt (offerSelection
   * == false) aus der aktuell über Bearbeiten/Datenbank austauschen eingestellten
   * Tabelle. Für die Anzeige der Datensätze im Dialog wird die Spalte
   * "WollMuxDescription" verwendet. Falls die Spalte "WollMuxSelected" vorhanden ist
   * und "1", "ja" oder "true" enthält, so ist der entsprechende Datensatz in der
   * Auswahlliste bereits vorselektiert.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static void mailMerge(XPrintModel pmod, boolean offerSelection)
  { // TESTED
    XTextDocument doc = pmod.getTextDocument();
    XPropertySet settings = null;
    try
    {
      settings =
        UNO.XPropertySet(UNO.XMultiServiceFactory(doc).createInstance(
          "com.sun.star.document.Settings"));
    }
    catch (Exception x)
    {
      Logger.error(L.m("Kann DocumentSettings nicht auslesen"), x);
      return;
    }

    String datasource =
      (String) UNO.getProperty(settings, "CurrentDatabaseDataSource");
    String table = (String) UNO.getProperty(settings, "CurrentDatabaseCommand");
    Integer type = (Integer) UNO.getProperty(settings, "CurrentDatabaseCommandType");

    Logger.debug("Ausgewählte Datenquelle: \"" + datasource
      + "\"  Tabelle/Kommando: \"" + table + "\"  Typ: \"" + type + "\"");

    mailMerge(pmod, datasource, table, type, offerSelection);
  }

  /**
   * Falls offerSelection == false wird das zu pmod gehörende Dokument für jeden
   * Datensatz aus Tabelle table in Datenquelle datasource einmal ausgedruckt. Falls
   * offerSelection == true, wird dem Benutzer ein Dialog präsentiert, in dem er die
   * "WollMuxDescription"-Spalten aller Datensätze angezeigt bekommt und die
   * auszudruckenden Datensätze auswählen kann. Dabei sind alle Datensätze, die eine
   * Spalte "WollMuxSelected" haben, die den Wert "true", "ja" oder "1" enthält
   * bereits vorselektiert.
   *
   * @param type
   *          muss {@link CommandType#TABLE} sein.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  static void mailMerge(XPrintModel pmod, String datasource, String table,
      Integer type, boolean offerSelection)
  {
    /*
     * Kann nur mit Tabellennamen umgehen, nicht mit beliebigen Statements. Falls
     * eine andere Art von Kommando eingestellt ist, wird der SuperMailMerge
     * gestartet, damit der Benutzer eine Tabelle auswählt.
     */
    if (datasource == null || datasource.length() == 0 || table == null
      || table.length() == 0 || type == null || type.intValue() != CommandType.TABLE)
    {
      superMailMerge(pmod);
      return;
    }

    ConfigThingy conf = new ConfigThingy("Datenquelle");
    conf.add("NAME").add("Knuddel");
    conf.add("TABLE").add(table);
    conf.add("SOURCE").add(datasource);
    Datasource ds;
    try
    {
      ds =
        new OOoDatasource(new HashMap<String, Datasource>(), conf, new URL(
          "file:///"), true);
    }
    catch (Exception x)
    {
      Logger.error(x);
      return;
    }

    Set<String> schema = ds.getSchema();
    QueryResults data;
    try
    {
      data = ds.getContents(DATABASE_TIMEOUT);
    }
    catch (TimeoutException e)
    {
      Logger.error(
        L.m("Konnte Daten für Serienbrief nicht aus der Datenquelle auslesen"), e);
      return;
    }

    mailMerge(pmod, offerSelection, schema, data);
  }

  /**
   * Falls offerSelection == false wird das zu pmod gehörende Dokument für jeden
   * Datensatz aus data einmal ausgedruckt. Falls offerSelection == true, wird dem
   * Benutzer ein Dialog präsentiert, in dem er die "WollMuxDescription"-Spalten
   * aller Datensätze angezeigt bekommt und die auszudruckenden Datensätze auswählen
   * kann. Dabei sind alle Datensätze, die eine Spalte "WollMuxSelected" haben, die
   * den Wert "true", "ja" oder "1" enthält bereits vorselektiert.
   *
   * @param schema
   *          muss die Namen aller Spalten für den MailMerge enthalten.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  static void mailMerge(XPrintModel pmod, boolean offerSelection,
      Set<String> schema, QueryResults data)
  {
    Vector<ListElement> list = new Vector<>();
    int index = 1;
    for (Dataset dataset : data)
    {
      list.add(new ListElement(dataset, L.m("Datensatz ") + index));
      ++index;
    }

    if (offerSelection)
    {
      if (!selectFromListDialog(list))
        return;
    }

    boolean modified = pmod.getDocumentModified(); // Modified-Zustand merken, um
    // ihn nachher wiederherzustellen
    pmod.collectNonWollMuxFormFields(); // falls der Benutzer manuell welche
    // hinzugefuegt hat

    MailMergeProgressWindow progress = new MailMergeProgressWindow(list.size());

    Iterator<ListElement> iter = list.iterator();
    while (iter.hasNext())
    {
      progress.makeProgress();
      ListElement ele = iter.next();
      if (offerSelection && !ele.isSelected())
        continue;
      Iterator<String> colIter = schema.iterator();
      while (colIter.hasNext())
      {
        String column = colIter.next();
        String value = null;
        try
        {
          value = ele.getDataset().get(column);
        }
        catch (Exception e)
        {
          Logger.error(
            L.m("Spalte \"%1\" fehlt unerklärlicherweise => Abbruch des Drucks",
              column), e);
          return;
        }

        if (value != null)
          pmod.setFormValue(column, value);
      }
      pmod.printWithProps();
    }

    progress.close();

    pmod.setDocumentModified(modified);
  }

  /**
   * Präsentiert einen Dialog, der den Benutzer aus list (enthält {@link ListElement}
   * s) auswählen lässt. ACHTUNG! Diese Methode kehrt erst zurück nachdem der
   * Benutzer den Dialog geschlossen hat.
   *
   * @return true, gdw der Benutzer mit Okay bestätigt hat.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static boolean selectFromListDialog(final Vector<ListElement> list)
  {
    final boolean[] result = new boolean[] {
      false, false };
    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            createSelectFromListDialog(list, result);
          }
          catch (Exception x)
          {
            Logger.error(x);
            synchronized (result)
            {
              result[0] = true;
              result.notifyAll();
            }
          }
          ;
        }
      });

      synchronized (result)
      {
        while (!result[0])
          result.wait();
      }
      return result[1];

    }
    catch (Exception x)
    {
      Logger.error(x);
      return false;
    }
  }

  /**
   * Präsentiert einen Dialog, der den Benutzer aus list (enthält {@link ListElement}
   * s) auswählen lässt. ACHTUNG! Diese Methode darf nur im Event Dispatching Thread
   * aufgerufen werden.
   *
   * @param result
   *          ein 2-elementiges Array auf das nur synchronisiert zugegriffen wird.
   *          Das erste Element wird auf false gesetzt, sobald der Dialog geschlossen
   *          wird. Das zweite Element wird in diesem Fall auf true gesetzt, wenn der
   *          Benutzer mir Okay bestätigt hat. Bei sonstigen Arten, den Dialog zu
   *          beenden bleibt das zweite Element unangetastet, sollte also mit false
   *          vorbelegt werden.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static void createSelectFromListDialog(final Vector<ListElement> list,
      final boolean[] result)
  {
    final JFrame myFrame = new JFrame(L.m("Gewünschte Ausdrucke wählen"));
    myFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    myFrame.addWindowListener(new WindowAdapter()
    {
      @Override
      public void windowClosed(WindowEvent e)
      {
        synchronized (result)
        {
          result[0] = true;
          result.notifyAll();
        }
      }
    });
    myFrame.setAlwaysOnTop(true);
    JPanel myPanel = new JPanel(new BorderLayout());
    myPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    myFrame.setContentPane(myPanel);

    final JList<ListElement> myList = new JList<>(list);
    myList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    for (int i = 0; i < list.size(); ++i)
    {
      ListElement ele = list.get(i);
      if (ele.isSelected())
        myList.addSelectionInterval(i, i);
    }

    JScrollPane scrollPane = new JScrollPane(myList);
    myPanel.add(scrollPane, BorderLayout.CENTER);

    Box top = Box.createVerticalBox();
    top.add(new JLabel(
      L.m("Bitte wählen Sie, welche Ausdrucke Sie bekommen möchten")));
    top.add(Box.createVerticalStrut(5));
    myPanel.add(top, BorderLayout.NORTH);

    Box bottomV = Box.createVerticalBox();
    bottomV.add(Box.createVerticalStrut(5));
    Box bottom = Box.createHorizontalBox();
    bottomV.add(bottom);
    myPanel.add(bottomV, BorderLayout.SOUTH);

    JButton button = new JButton(L.m("Abbrechen"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        myFrame.dispose();
      }
    });
    bottom.add(button);

    bottom.add(Box.createHorizontalGlue());

    button = new JButton(L.m("Alle"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        myList.setSelectionInterval(0, list.size() - 1);
      }
    });
    bottom.add(button);

    bottom.add(Box.createHorizontalStrut(5));

    button = new JButton(L.m("Keinen"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        myList.clearSelection();
      }
    });
    bottom.add(button);

    bottom.add(Box.createHorizontalGlue());

    button = new JButton(L.m("Start"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        for (int i = 0; i < list.size(); ++i)
        {
          (list.get(i)).setSelected(false);
        }
        int[] sel = myList.getSelectedIndices();
        for (int element : sel)
        {
          (list.get(element)).setSelected(true);
        }
        synchronized (result)
        {
          result[1] = true;
        }
        myFrame.dispose();
      }
    });
    bottom.add(button);

    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    myFrame.setLocation(x, y);
    myFrame.setVisible(true);
    myFrame.requestFocus();
  }

  /**
   * Liefert die sichtbaren Zellen des Arbeitsblattes mit Namen sheetName aus dem
   * Calc Dokument, dessen Fenstertitel windowTitle ist. Die erste Zeile der
   * Calc-Tabelle wird herangezogen als Spaltennamen. Diese Spaltennamen werden zu
   * schema hinzugefügt.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  static QueryResults getVisibleCalcData(String windowTitle,
      String sheetName, Set<String> schema)
  {
    CalcCellQueryResults results = new CalcCellQueryResults();

    try
    {
      XSpreadsheetDocument foundDoc  = null;

      for (XSpreadsheetDocument doc : UnoCollection.getCollection(UNO.desktop.getComponents(), XSpreadsheetDocument.class))
      {
        if (doc != null)
        {
          String title =
            (String) UNO.getProperty(
              UNO.XModel(doc).getCurrentController().getFrame(), "Title");
          if (windowTitle.equals(title))
          {
            foundDoc = doc;
            break;
          }
        }
      }

      if (foundDoc != null)
      {
        XCellRangesQuery sheet =
          UNO.XCellRangesQuery(foundDoc.getSheets().getByName(sheetName));
        if (sheet != null)
        {
          SortedSet<Integer> columnIndexes = new TreeSet<Integer>();
          SortedSet<Integer> rowIndexes = new TreeSet<Integer>();
          XSheetCellRanges visibleCellRanges = sheet.queryVisibleCells();
          XSheetCellRanges nonEmptyCellRanges =
            sheet.queryContentCells((short) (com.sun.star.sheet.CellFlags.VALUE
              | com.sun.star.sheet.CellFlags.DATETIME
              | com.sun.star.sheet.CellFlags.STRING | com.sun.star.sheet.CellFlags.FORMULA));
          CellRangeAddress[] nonEmptyCellRangeAddresses =
            nonEmptyCellRanges.getRangeAddresses();
          for (int i = 0; i < nonEmptyCellRangeAddresses.length; ++i)
          {
            XSheetCellRanges ranges =
              UNO.XCellRangesQuery(visibleCellRanges).queryIntersection(
                nonEmptyCellRangeAddresses[i]);
            CellRangeAddress[] rangeAddresses = ranges.getRangeAddresses();
            for (int k = 0; k < rangeAddresses.length; ++k)
            {
              CellRangeAddress addr = rangeAddresses[k];
              for (int x = addr.StartColumn; x <= addr.EndColumn; ++x)
                columnIndexes.add(Integer.valueOf(x));

              for (int y = addr.StartRow; y <= addr.EndRow; ++y)
                rowIndexes.add(Integer.valueOf(y));
            }
          }

          if (!columnIndexes.isEmpty() && !rowIndexes.isEmpty())
          {
            XCellRange sheetCellRange = UNO.XCellRange(sheet);

            /*
             * Erste sichtbare Zeile durchscannen und alle nicht-leeren Zelleninhalte
             * als Tabellenspaltennamen interpretieren. Ein Mapping in
             * mapColumnNameToIndex wird erzeugt, wobei NICHT auf den Index in der
             * Calc-Tabelle gemappt wird, sondern auf den Index im später für jeden
             * Datensatz existierenden String[]-Array.
             */
            int ymin = rowIndexes.first().intValue();
            Map<String, Integer> mapColumnNameToIndex =
              new HashMap<>();
            int idx = 0;
            Iterator<Integer> iter = columnIndexes.iterator();
            while (iter.hasNext())
            {
              int x = iter.next().intValue();
              String columnName =
                UNO.XTextRange(sheetCellRange.getCellByPosition(x, ymin)).getString();
              if (columnName.length() > 0)
              {
                mapColumnNameToIndex.put(columnName, Integer.valueOf(idx));
                schema.add(columnName);
                ++idx;
              }
              else
                iter.remove(); // Spalten mit leerem Spaltennamen werden nicht
              // benötigt.
            }

            results.setColumnNameToIndexMap(mapColumnNameToIndex);

            /*
             * Datensätze erzeugen
             */
            Iterator<Integer> rowIndexIter = rowIndexes.iterator();
            rowIndexIter.next(); // erste Zeile enthält die Tabellennamen, keinen
            // Datensatz
            while (rowIndexIter.hasNext())
            {
              int y = rowIndexIter.next().intValue();
              String[] data = new String[columnIndexes.size()];
              Iterator<Integer> columnIndexIter = columnIndexes.iterator();
              idx = 0;
              while (columnIndexIter.hasNext())
              {
                int x = columnIndexIter.next().intValue();
                String value =
                  UNO.XTextRange(sheetCellRange.getCellByPosition(x, y)).getString();
                data[idx++] = value;
              }

              results.addDataset(data);
            }
          }
        }
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
    }

    return results;
  }

  /**
   * Startet den ultimativen Seriendruck für pmod.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void superMailMerge(XPrintModel pmod)
  {
    SuperMailMerge.superMailMerge(pmod);
  }
}

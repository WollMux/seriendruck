package de.muenchen.mailmerge.print;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameAccess;
import com.sun.star.sdb.CommandType;
import com.sun.star.sdbc.XConnection;
import com.sun.star.sdbc.XDataSource;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Klasse, die den ultimativen Seriendruck realisiert.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
class SuperMailMerge
{
  /**
   * Liste von {@link Runnable}-Objekten, die sequentiell abgearbeitet werden im
   * Nicht-Event-Dispatching-Thread.
   */
  private List<Runnable> todo = new LinkedList<>();

  /**
   * Wird dies auf false gesetzt, so beendet sich {@link #run()}.
   */
  private boolean running = true;

  /**
   * Die Menge der Namen aller OOo-Datenquellen.
   */
  private Set<String> datasourceNames = new TreeSet<>();

  /**
   * Die Menge aller Titel von offenen Calc-Dokument-Fenstern.
   */
  private Set<String> calcDocumentTitles = new TreeSet<>();

  /**
   * Die ComboBox in der der Benutzer die OOo-Datenquelle bzw, das Calc-Dokument
   * für den MailMerge auswählen kann.
   */
  private JComboBox<String> datasourceSelector;

  /**
   * Das XPrintModel für diesen MailMerge.
   */
  private XPrintModel pmod;

  /**
   * Die ComboBox in der der Benutzer die Tabelle für den MailMerge auswählen kann.
   */
  private JComboBox<String> tableSelector;

  /**
   * Der Name der aktuell ausgewählten Datenquelle (bzw, der Titel des ausgewählten
   * Calc-Dokuments). ACHTUNG! Diese Variable wird initial vom Nicht-EDT befüllt,
   * dann aber nur noch im Event Dispatching Thread verwendet bis zu dem Zeitpunkt
   * wo die Datenquellenauswahl beendet ist und der Druck durch den nicht-EDT
   * Thread angeleiert wird.
   */
  private String selectedDatasource = "";

  /**
   * Der Name der aktuell ausgewählten Tabelle. ACHTUNG! Diese Variable wird
   * initial vom Nicht-EDT befüllt, dann aber nur noch im Event Dispatching Thread
   * verwendet bis zu dem Zeitpunkt wo die Datenquellenauswahl beendet ist und der
   * Druck durch den nicht-EDT Thread angeleiert wird.
   */
  private String selectedTable = "";

  private SuperMailMerge(XPrintModel pmod)
  { // TESTED
    this.pmod = pmod;

    /*
     * Namen aller OOo-Datenquellen bestimmen.
     */
    String[] datasourceNamesA = UNO.XNameAccess(UNO.dbContext).getElementNames();
    for (int i = 0; i < datasourceNamesA.length; ++i)
      datasourceNames.add(datasourceNamesA[i]);

    /*
     * Titel aller offenen Calc-Fenster bestimmen.
     */
    for (XSpreadsheetDocument doc : UnoCollection.getCollection(UNO.desktop.getComponents(), XSpreadsheetDocument.class))
    {
      try
      {
        if (doc != null)
        {
          String title =
            (String) UNO.getProperty(
              UNO.XModel(doc).getCurrentController().getFrame(), "Title");
          if (title != null)
            calcDocumentTitles.add(title);
        }
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }

    /*
     * Aktuell über Bearbeiten/Datenbank austauschen gewählte Datenquelle/Tabelle
     * bestimmen, falls gesetzt.
     */
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
    Integer type =
      (Integer) UNO.getProperty(settings, "CurrentDatabaseCommandType");
    if (datasource != null && datasourceNames.contains(datasource)
      && table != null && table.length() > 0 && type != null
      && type.intValue() == CommandType.TABLE)
    {
      selectedDatasource = datasource;
      selectedTable = table;
    }

    /*
     * Erzeugen der GUI auf die todo-Liste setzen.
     */
    todo.add(new Runnable()
    {
      @Override
      public void run()
      {
        inEDT("createGUI");
      }
    });
  }

  /**
   * Startet den ultimativen MailMerge. ACHTUNG! Diese Methode kehrt erst zurück,
   * wenn der Ausdruck abgeschlossen oder abgebrochen wurde.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void superMailMerge(XPrintModel pmod)
  {
    SuperMailMerge merge = new SuperMailMerge(pmod);
    merge.run();
  }

  /**
   * Arbeitet die {@link #todo}-Liste ab, solange {@link #running}==true.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void run()
  {
    try
    {
      while (running)
      {
        Runnable r;
        synchronized (todo)
        {
          while (todo.isEmpty())
            todo.wait();
          r = todo.remove(0);
        }
        r.run();
      }
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Erstellt die GUI für die Auswahl der Datenquelle/Tabelle für den
   * SuperMailMerge. Darf nur im EDT aufgerufen werden.
   *
   * Diese Methode wird indirekt per Reflection aufgerufen (daher keine
   * "unused"-Warnung)
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  @SuppressWarnings("unused")
  public void createGUI()
  {
    final JFrame myFrame = new JFrame(L.m("Seriendruck"));
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new WindowListener()
    {
      @Override
      public void windowOpened(WindowEvent e)
      {}

      @Override
      public void windowClosing(WindowEvent e)
      {
        stopRunning();
        myFrame.dispose();
      }

      @Override
      public void windowClosed(WindowEvent e)
      {}

      @Override
      public void windowIconified(WindowEvent e)
      {}

      @Override
      public void windowDeiconified(WindowEvent e)
      {}

      @Override
      public void windowActivated(WindowEvent e)
      {}

      @Override
      public void windowDeactivated(WindowEvent e)
      {}
    });

    myFrame.setAlwaysOnTop(true);
    Box vbox = Box.createVerticalBox();
    JPanel myPanel = new JPanel();
    // myPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    myFrame.add(myPanel);
    myPanel.add(vbox);

    /*
     * Datenquellen-Auswahl-ComboBox bauen
     */
    Box hbox = Box.createHorizontalBox();
    vbox.add(hbox);
    hbox.add(new JLabel(L.m("Datenquelle")));
    datasourceSelector = new JComboBox<>();
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(datasourceSelector);
    int selected = 0;
    int idx = 0;
    Iterator<String> iter = calcDocumentTitles.iterator();
    while (iter.hasNext())
    {
      datasourceSelector.addItem(iter.next());
      ++idx;
    }
    iter = datasourceNames.iterator();
    while (iter.hasNext())
    {
      String dsName = iter.next();
      if (dsName.equals(selectedDatasource))
        selected = idx;
      datasourceSelector.addItem(dsName);
      ++idx;
    }

    if (idx > 0)
    {
      datasourceSelector.setSelectedIndex(selected);
      String newDatasource = (String) datasourceSelector.getSelectedItem();
      if (newDatasource != null)
        selectedDatasource = newDatasource;
    }

    /*
     * Auf Änderungen der Datenquellen-Auswahl-Combobox reagieren.
     */
    datasourceSelector.addItemListener(new ItemListener()
    {
      @Override
      public void itemStateChanged(ItemEvent e)
      {
        String newDatasource = (String) datasourceSelector.getSelectedItem();
        String newTable = (String) tableSelector.getSelectedItem();
        if (newDatasource != null && !newDatasource.equals(selectedDatasource))
        {
          selectedDatasource = newDatasource;
          selectedTable = newTable;
          addTodo("updateTableSelector", new String[] {
            selectedDatasource, selectedTable });
        }
      }
    });

    /*
     * Tabellenauswahl-ComboBox bauen.
     */
    hbox = Box.createHorizontalBox();
    vbox.add(Box.createVerticalStrut(5));
    vbox.add(hbox);
    hbox.add(new JLabel(L.m("Tabelle")));
    hbox.add(Box.createHorizontalStrut(5));
    tableSelector = new JComboBox<>();
    hbox.add(tableSelector);

    /*
     * Buttons hinzufügen.
     */

    hbox = Box.createHorizontalBox();
    vbox.add(Box.createVerticalStrut(5));
    vbox.add(hbox);
    JButton button = new JButton(L.m("Abbrechen"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        stopRunning();
        myFrame.dispose();
      }
    });
    hbox.add(button);

    button = new JButton(L.m("Start"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        selectedTable = (String) tableSelector.getSelectedItem();
        selectedDatasource = (String) datasourceSelector.getSelectedItem();
        if (selectedTable != null && selectedDatasource != null)
        {
          clearTodo();
          addTodo("print", Boolean.FALSE);
          myFrame.dispose();
        }
      }
    });
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(button);

    button = new JButton(L.m("Einzelauswahl"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        selectedTable = (String) tableSelector.getSelectedItem();
        selectedDatasource = (String) datasourceSelector.getSelectedItem();
        if (selectedTable != null && selectedDatasource != null)
        {
          clearTodo();
          addTodo("print", Boolean.TRUE);
          myFrame.dispose();
        }
      }
    });
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(button);

    addTodo("updateTableSelector", new String[] {
      selectedDatasource, selectedTable });

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
   * Wird im Nicht-EDT aufgerufen und bestimmt die Tabellen der neu ausgewählten
   * Datenquelle und lässt dann im EDT die {@link #tableSelector}-ComboBox updaten.
   *
   * Diese Methode wird indirekt über Reflection aufgerufen (daher keine
   * "unused"-Warnung)
   *
   * @param datasourceAndTableName
   *          das erste Element ist der Name der neu ausgewählten Datenquelle bzw.
   *          des Calc-Dokuments. Das zweite Element ist der Name der vorher
   *          ausgewählten Tabelle (oder null). Letzterer wird benötigt, da falls
   *          die neue Datenquelle eine Tabelle gleichen Namens besitzt, diese als
   *          aktuelle Auswahl der ComboBox eingestellt werden soll.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  @SuppressWarnings("unused")
  public void updateTableSelector(String[] datasourceAndTableName)
  {
    String datasourceName = datasourceAndTableName[0];
    final String tableName = datasourceAndTableName[1]; // ACHTUNG!! Darf null
    // sein!
    String[] tableNames = null;
    if (calcDocumentTitles.contains(datasourceName))
    {
      for (XSpreadsheetDocument doc : UnoCollection.getCollection(UNO.desktop.getComponents(), XSpreadsheetDocument.class))
      {
        try
        {
          if (doc != null)
          {
            String title =
              (String) UNO.getProperty(
                UNO.XModel(doc).getCurrentController().getFrame(), "Title");
            if (datasourceName.equals(title))
            {
              tableNames = UNO.XNameAccess(doc.getSheets()).getElementNames();
              break;
            }
          }
        }
        catch (Exception x)
        {
          Logger.error(x);
          return;
        }
      }
    }
    else if (datasourceNames.contains(datasourceName))
    {
      try
      {
        XDataSource ds =
          UNO.XDataSource(UNO.dbContext.getRegisteredObject(datasourceName));
        long lgto = MailMerge.DATABASE_TIMEOUT / 1000;
        if (lgto < 1)
          lgto = 1;
        ds.setLoginTimeout((int) lgto);
        XConnection conn = ds.getConnection("", "");
        XNameAccess tables = UNO.XTablesSupplier(conn).getTables();
        tableNames = tables.getElementNames();
      }
      catch (Exception x)
      {
        Logger.error(x);
        return;
      }
    }
    else
      return; // kann passieren, falls weder Datenquellen noch Calc-Dokumente
    // vorhanden.

    if (tableNames == null || tableNames.length == 0)
      tableNames = new String[] { "n/a" };

    final String[] tNames = tableNames;
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          tableSelector.removeAllItems();
          int selected = 0;
          for (int i = 0; i < tNames.length; ++i)
          {
            if (tNames[i].equals(tableName))
              selected = i;
            tableSelector.addItem(tNames[i]);
          }
          tableSelector.setSelectedIndex(selected);
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  @SuppressWarnings("unused") // wird per reflection aufgerufen
  public void print(Boolean offerselection)
  {
    if (calcDocumentTitles.contains(selectedDatasource))
    {
      Set<String> schema = new HashSet<>();
      QueryResults data =
        MailMerge.getVisibleCalcData(selectedDatasource, selectedTable, schema);
      MailMerge.mailMerge(pmod, offerselection.booleanValue(), schema, data);
    }
    else
      MailMerge.mailMerge(pmod, selectedDatasource, selectedTable,
        Integer.valueOf(CommandType.TABLE), offerselection.booleanValue());
  }

  /**
   * Fügt den Aufruf der public-Methode method zur {@link #todo}-Liste hinzu.
   *
   * @param method
   *          der Name einer public-Methode.
   * @param param
   *          Parameter, der der Methode übergeben werden soll, oder null falls die
   *          Methode keine Parameter erwartet.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void addTodo(String method, Object param)
  {
    try
    {
      Class<?>[] paramTypes = null;
      Object[] params = null;
      if (param != null)
      {
        paramTypes = new Class[] { param.getClass() };
        params = new Object[] { param };
      }
      final Object[] finalParams = params;
      final Method m = this.getClass().getMethod(method, paramTypes);
      final SuperMailMerge self = this;
      synchronized (todo)
      {
        todo.add(new Runnable()
        {
          @Override
          public void run()
          {
            try
            {
              m.invoke(self, finalParams);
            }
            catch (Exception x)
            {
              Logger.error(x);
            }
          }
        });
        todo.notifyAll();
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Leert die {@link #todo}-Liste.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void clearTodo()
  {
    synchronized (todo)
    {
      todo.clear();
    }
  }

  /**
   * Löscht die {@link #todo}-Liste und fügt ihr dann einen Befehl zum Setzen von
   * {@link #running} auf false hinzu.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void stopRunning()
  {
    synchronized (todo)
    {
      todo.clear();
      todo.add(new Runnable()
      {
        @Override
        public void run()
        {
          running = false;
        }
      });
      todo.notifyAll();
    }
  }

  /**
   * Führt die public-Methode "method" im EDT aus (ansynchron).
   *
   * @param method
   *          der Name einer public-Methode
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void inEDT(String method)
  {
    try
    {
      final Method m = this.getClass().getMethod(method, (Class[]) null);
      final SuperMailMerge self = this;
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            m.invoke(self, (Object[]) null);
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

}
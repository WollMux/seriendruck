/*
 * Dateiname: MailMergeParams.java
 * Projekt  : WollMux
 * Funktion : Dialoge zur Bestimmung der Parameter für den wirklichen Merge (z.B. ob in Gesamtdokument oder auf Drucker geschrieben werden soll.)
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
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
 * 11.10.2007 | BNK | Erstellung
 * 25.05.2010 | ERT | GUI für PDF-Gesamtdruck
 * 20.12.2010 | ERT | Defaultwerte für Druckdialog von ... bis
 * 08.05.2012 | jub | vorgeschlagener name für den anhang eines serienbrief/emailversands
 *                    kommt ohne endung, da für den nutzer auswahl zwischen pdf/odt 
 *                    möglich ist
 * 23.01.2014 | loi | Für den Seriendruck einen Wollmux Druckerauswahl Dialog eingefugt,
 *                    da der LO Dialog Druckeroptionen zur Auswahl bietet, die im Druck 
 *                    nicht umgesetz werden.                  
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.dialog.TextComponentTags;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui.Section;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui.SubmitArgument;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui.UIElementAction;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui.UIElementType;

/**
 * Dialoge zur Bestimmung der Parameter für den wirklichen Merge (z.B. ob in
 * Gesamtdokument oder auf Drucker geschrieben werden soll.)
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class MailMergeParams
{
  /**
   * URL der Konfiguration der Fallback-Konfiguration für den Abschnitt
   * Dialoge/Seriendruckdialog, falls dieser Abschnitt nicht in der
   * WollMux-Konfiguration definiert wurde.
   * 
   * Dieser Fallback wurde eingebaut, um mit alten WollMux-Standard-Configs
   * kompatibel zu bleiben, sollte nach ausreichend Zeit aber wieder entfernt werden!
   */
  private final URL DEFAULT_MAILMERGEDIALOG_URL =
    this.getClass().getClassLoader().getResource("data/seriendruckdialog.conf");

  /**
   * Tag für {@link TextComponentTags}, das als Platzhalter für die Serienbriefnummer
   * steht.
   */
 public  static final String TAG_SERIENBRIEFNUMMER = "#SB";

  /**
   * Tag für {@link TextComponentTags}, das als Platzhalter für die Datensatznummer
   * steht.
   */
  public static final String TAG_DATENSATZNUMMER = "#DS";

  /**
   * Enthält die Hintergrundfarbe des Beschreibungsfeldes im Druckdialog
   */
  public static final Color DESC_COLOR = new Color(0xffffc8);

  /**
   * Enthält den {@link MailMergeController}, der Infos zum aktuellen
   * Seriendruckkontext liefern kann und den eigentlichen Seriendruck ausführen kann.
   */
  public MailMergeController mmc;
  
  //
  
  public MailMergeController getMMC() {
    return mmc;
  }

  public HashSet<String> getVisibleGroups()
  {
    return visibleGroups;
  }

  public void setVisibleGroups(HashSet<String> visibleGroups)
  {
    this.visibleGroups = visibleGroups;
  }

  public String getDefaultEmailFrom()
  {
    return defaultEmailFrom;
  }

  public void setDefaultEmailFrom(String defaultEmailFrom)
  {
    this.defaultEmailFrom = defaultEmailFrom;
  }

  public String getCurrentActionType()
  {
    return currentActionType;
  }

  public void setCurrentActionType(String currentActionType)
  {
    this.currentActionType = currentActionType;
  }

  public String getCurrentOutput()
  {
    return currentOutput;
  }

  public void setCurrentOutput(String currentOutput)
  {
    this.currentOutput = currentOutput;
  }

  public DatasetSelectionType getDatasetSelectionType()
  {
    return datasetSelectionType;
  }

  public void setDatasetSelectionType(DatasetSelectionType datasetSelectionType)
  {
    this.datasetSelectionType = datasetSelectionType;
  }

  public ArrayList<Section> getSections()
  {
    return sections;
  }

  public void setSections(ArrayList<Section> sections)
  {
    this.sections = sections;
  }

  public JDialog getDialog()
  {
    return dialog;
  }

  public void setDialog(JDialog dialog)
  {
    this.dialog = dialog;
  }

  public Boolean getIgnoreDocPrintFuncs()
  {
    return ignoreDocPrintFuncs;
  }

  public void setIgnoreDocPrintFuncs(Boolean ignoreDocPrintFuncs)
  {
    this.ignoreDocPrintFuncs = ignoreDocPrintFuncs;
  }

  public List<String> getUsePrintFunctions()
  {
    return usePrintFunctions;
  }

  public void setUsePrintFunctions(List<String> usePrintFunctions)
  {
    this.usePrintFunctions = usePrintFunctions;
  }

  public DruckerController getDruckerController()
  {
    return druckerController;
  }

  public void setDruckerController(DruckerController druckerController)
  {
    this.druckerController = druckerController;
  }

  /**
   * Der Dialog, der durch {@link #showDoMailmergeDialog(JFrame, MailMergeNew, List)}
   * angezeigt wird. Bei jedem Aufruf mit dem gleichen parent Frame wird der selbe
   * Dialog verwendet, damit die Vorbelegungen erhalten bleiben.
   */
  private JDialog dialog = null;
  
  
  /**
   * Das Model für den Druckerauswahldialog beim Seriendruck
   */
  private DruckerModel druckerModel;
  
  /**
   * Der Controller für den Druckerauswahldialog beim Seriendruck
   */
  private DruckerController druckerController;

  /**
   * Enthält den Regeln-Abschnitt aus der Seriendruckdialog-Beschreibung.
   */
  private ConfigThingy rules;

  /**
   * Enthält alle zum aktuellen Zeitpunkt sichtbaren Gruppen, die über das
   * {@link RuleStatement#SHOW_GROUPS} sichtbar geschaltet wurden.
   */
  private HashSet<String> visibleGroups = new HashSet<String>();

  /**
   * Enthält eine Liste aller erzeugter {@link Section}-Objekte in der Reihenfolge
   * der Seriendruckdialog-Beschreibung.
   */
  private ArrayList<Section> sections = new ArrayList<Section>();

  /**
   * Enthält den String der im Attribut VALUE zur zuletzt ausgeführten
   * {@link UIElementAction#setActionType}-Action angegeben war. Beispiel:
   * 
   * Wird in der GUI das Formularelement '(LABEL "Gesamtdokument erstellen" TYPE
   * "radio" ACTION "setActionType" VALUE "gesamtdok")' ausgewählt, dann enthält
   * diese Variable den Wert "gesamtdok".
   */
  private String currentActionType = "";

  /**
   * Enthält den String der im Attribut VALUE zur zuletzt ausgeführten
   * {@link UIElementAction#setOutput}-Action angegeben war. Beispiel:
   * 
   * Wird in der GUI das Formularelement '(LABEL "ODT-Datei" TYPE "radio" GROUP "odt"
   * ACTION "setOutput" VALUE "odt")' ausgewählt, dann enthält diese Variable den
   * Wert "odt".
   */
  private String currentOutput = "";

  /**
   * Sammelt die JTextComponent-Objekte alle in der Seriendruckdialog-Beschreibung
   * enthaltenen Formularfelder vom Typ {@link UIElementType#description} auf (das
   * ist normalerweise immer nur eins, aber es ist niemand daran gehindert, das
   * Element öfters in den Dialog einzubinden - wenn auch ohne größeren Sinn)
   */
  public ArrayList<JTextComponent> descriptionFields =
    new ArrayList<JTextComponent>();

  /**
   * Enthält die Namen der über das zuletzt ausgeführte
   * {@link RuleStatement#USE_PRINTFUNCTIONS} -Statement gesetzten PrintFunctions.
   */
  private List<String> usePrintFunctions = new ArrayList<String>();

  /**
   * Enthält den Wert des zuletzt ausgeführten
   * {@link RuleStatement#IGNORE_DOC_PRINTFUNCTIONS}-Statements
   */
  private Boolean ignoreDocPrintFuncs;

  /**
   * Enthält den String, der als Vorbelegung im Formularfeld für das
   * Absender-Email-Feld gesetzt wird.
   */
  private String defaultEmailFrom = "";

  /**
   * Auf welche Art hat der Benutzer die zu druckenden Datensätze ausgewählt.
   */
  private DatasetSelectionType datasetSelectionType = DatasetSelectionType.ALL;

  /**
   * Zeigt den Dialog an, der die Serienbriefverarbeitung (Direktdruck oder in neues
   * Dokument) anwirft. Bei jedem Aufruf mit dem gleichen parent Frame wird der selbe
   * Dialog verwendet, damit die Vorbelegungen erhalten bleiben.
   * 
   * @param parent
   *          Elternfenster für den anzuzeigenden Dialog.
   * 
   * @param mmc
   *          Die Methode
   *          {@link MailMergeNew#doMailMerge(de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.MailMergeType, de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.DatasetSelectionType)}
   *          wird ausgelöst, wenn der Benutzer den Seriendruck startet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void showDoMailmergeDialog(final JFrame parent,
      final MailMergeController mmc)
  {
    ConfigThingy sdConf = null;
    try
    {
      sdConf =
        WollMuxFiles.getWollmuxConf().query("Dialoge").query("Seriendruckdialog").getLastChild();
    }
    catch (NodeNotFoundException e)
    {}
    if (sdConf == null)
    {
      Logger.log(L.m("Kein Abschnitt Dialoge/Seriendruckdialog in der WollMux-Konfiguration "
        + "angegeben! Verwende Default-Konfiguration für den Seriendruckdialog."));
      try
      {
        sdConf =
          new ConfigThingy("Default", DEFAULT_MAILMERGEDIALOG_URL).query("Dialoge").query(
            "Seriendruckdialog").getLastChild();
      }
      catch (Exception e)
      {
        Logger.error(
          L.m(
            "Kann Default-Konfiguration des Seriendruckdialogs nicht aus internem file %1 bestimmen. Dies darf nicht vorkommenen!",
            DEFAULT_MAILMERGEDIALOG_URL), e);
        return;
      }
    }

    String defaultFrom = "";
    try
    {
      ConfigThingy emailConf =
        WollMuxFiles.getWollmuxConf().query("EMailEinstellungen").getLastChild();
      String defaultEmailFromColumnName =
        emailConf.getString("DEFAULT_SENDER_DB_SPALTE", "");
      defaultFrom =
        DatasourceJoinerFactory.getDatasourceJoiner().getSelectedDataset().get(
          defaultEmailFromColumnName);
    }
    catch (Exception e)
    {
      Logger.debug(L.m("Kann Voreinstellung der Absender E-Mailadresse für den Seriendruckdialog nicht bestimmen"));
    }

    showDoMailmergeDialog(parent, mmc, sdConf, defaultFrom);
  }

  /**
   * Zeigt den Dialog an, der die Serienbriefverarbeitung (Direktdruck oder in neues
   * Dokument) anwirft. Bei jedem Aufruf mit dem gleichen parent Frame wird der selbe
   * Dialog verwendet, damit die Vorbelegungen erhalten bleiben.
   * 
   * @param parent
   *          Elternfenster für den anzuzeigenden Dialog.
   * 
   * @param mmc
   * @param defaultEmailFrom
   *          Die Methode
   *          {@link MailMergeNew#doMailMerge(de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.MailMergeType, de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.DatasetSelectionType)}
   *          wird ausgelöst, wenn der Benutzer den Seriendruck startet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD-D101)
   */
  private void showDoMailmergeDialog(final JFrame parent,
      final MailMergeController mmc, ConfigThingy dialogConf, String defaultEmailFrom)
  {
    this.mmc = mmc;
    this.setDefaultEmailFrom(defaultEmailFrom);

    // erzeugt ein model für den Druckerauswahldialog falls noch keine existiert
    if (druckerModel == null) {
      druckerModel = new DruckerModel();
    }
       
    if(getDialog() != null && getDialog().getParent() == parent) {
      // erzeugt einen controller für den Druckerauswahldialog falls noch keiner existiert
      if (getDruckerController() == null) {  
        setDruckerController(new DruckerController(druckerModel, (JFrame)getDialog().getOwner(), this));
      }      
    } else {
      String title = L.m("Seriendruck");
      try
      {
        title = dialogConf.get("TITLE").toString();
      }
      catch (NodeNotFoundException e1)
      {}
      //set JDialog to Modeless type so that it remains visible when changing focus between opened 
      //calc and writer document. Drawback: when this Dialog is open, the "Seriendruck" bar is 
      //active too.
      setDialog(new JDialog(parent, title, false));
      getDialog().setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
      // erzeugt einen controller für den Druckerauswahldialog falls noch keiner existiert
      if (getDruckerController() == null) {  
        setDruckerController(new DruckerController(druckerModel, (JFrame)getDialog().getOwner(), this));
      }      
      try
      {
        rules = dialogConf.get("Regeln");
      }
      catch (NodeNotFoundException e2)
      {
        Logger.error(L.m("Dialogbeschreibung für den Seriendruckdialog enthält keinen Abschnitt 'Regeln'"));
        return;
      }
      ConfigThingy fensterConf = new ConfigThingy("");
      try
      {
        fensterConf = dialogConf.get("Fenster");
      }
      catch (NodeNotFoundException e1)
      {}

      Box vbox = Box.createVerticalBox();
      vbox.setBorder(new EmptyBorder(8, 5, 10, 5));
      getDialog().add(vbox);

      for (ConfigThingy sectionConf : fensterConf) 
        getSections().add(new Section(sectionConf, vbox, this)); 
    }
    
    updateView();
    setDialogLocation();
    getDialog().setResizable(false);
    getDialog().setVisible(true);
  } 
  
  private void setDialogLocation() {
    
    int frameWidth = getDialog().getWidth();
    int frameHeight = getDialog().getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    getDialog().setLocation(x, y);
  }

  /**
   * Führt dialog.pack() aus wenn die preferredSize des dialogs die aktuelle Größe
   * überschreitet und platziert den Dialog in die Mitte des Bildschirms.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static void repack(JDialog dialog)
  {
    Dimension pref = dialog.getPreferredSize();
    Dimension actual = dialog.getSize();
    if (actual.height != pref.height || actual.width < pref.width)
    {
      dialog.pack();
    }
  }

  /**
   * Führt die im Regeln-Abschnitt angegebenen Regeln aus, passt alle Sichtbarkeiten
   * und Vorbelegungen für Radio-Buttons korrekt an und zeichnet den Dialog bei
   * Bedarf neu.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void updateView()
  {
    processRules();
    for (Section s : getSections())
      s.updateView();
    repack(getDialog());
  }

  /**
   * Führt die im Regeln-Abschnitt definierten Regeln aus.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private void processRules()
  {
    for (ConfigThingy rule : rules)
    {
      // trifft rule zu?
      boolean matches = true;
      for (ConfigThingy key : rule)
      {
        RuleStatement statement = RuleStatement.getByname(key.getName());
        if (statement == RuleStatement.ON_ACTION_TYPE)
          if (!getCurrentActionType().equals(key.toString())) matches = false;
        if (statement == RuleStatement.ON_OUTPUT)
          if (!getCurrentOutput().equals(key.toString())) matches = false;
      }
      if (!matches) continue;

      // Regel trifft zu. Jetzt die Befehle bearbeiten
      boolean hadUsePrintFunctions = false;
      boolean hadIgnoreDocPrintFunctions = false;

      for (ConfigThingy key : rule)
      {
        RuleStatement k = RuleStatement.getByname(key.getName());
        switch (k)
        {
          case SHOW_GROUPS:
            getVisibleGroups().clear();
            for (ConfigThingy group : key)
              getVisibleGroups().add(group.toString());
            break;

          case SET_DESCRIPTION:
            String str = key.toString();
            String[] lines = str.split("\\n");
            for (JTextComponent tf : descriptionFields)
            {
              tf.setText(str);
              if (tf instanceof JTextArea)
              {
                JTextArea ta = (JTextArea) tf;
                if (ta.getRows() < lines.length) ta.setRows(lines.length);
              }
            }
            break;

          case USE_PRINTFUNCTIONS:
            hadUsePrintFunctions = true;
            getUsePrintFunctions().clear();
            for (ConfigThingy func : key)
              getUsePrintFunctions().add(func.toString());
            break;

          case IGNORE_DOC_PRINTFUNCTIONS:
            hadIgnoreDocPrintFunctions = true;
            setIgnoreDocPrintFuncs(Boolean.parseBoolean(key.toString()));
            break;

          case ON_ACTION_TYPE:
            break;
          case ON_OUTPUT:
            break;
          case unknown:
            break;
          default:
            break;
        }
      }

      // implizite Voreinstellung für IGNORE_DOC_PRINTFUNCTIONS, falls nicht in Regel
      // gesetzt:
      if (hadUsePrintFunctions && !hadIgnoreDocPrintFunctions)
        setIgnoreDocPrintFuncs(null);
    }
  }

  /**
   * Prüft ob die UIElementAction action mit dem Action-Wert value im aktuellen
   * Kontext ausgeführt werden könnte und liefert true zurück, wenn nichts gegen eine
   * Ausführung der Aktion spricht oder false, wenn es Gründe gibt, die eine
   * Ausführung behindern könnten (diese Gründe werden dann als String in die
   * übergebene reasons-Liste aufgenommen). Die Actions setActionType und setOutput
   * sind z.B. dann nicht ausführbar, wenn in einem zugehörigen Regeln-Abschnitt eine
   * USE_PRINTFUNCTIONS-Anweisung steht, deren Druckfunktionen nicht verfügbar sind.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public boolean isActionAvailableInCurrentContext(UIElementAction action,
      String actionValue, List<String> reasons)
  {
    switch (action)
    {
      case selectAll:
        return true;

      case unknown:
        return true;

      case setActionType:
        ConfigThingy myrules =
          rules.queryByChild(RuleStatement.ON_ACTION_TYPE.toString());
        for (ConfigThingy rule : myrules)
        {
          String value =
            rule.getString(RuleStatement.ON_ACTION_TYPE.toString(), null);
          if (!value.equals(actionValue)) continue;
          if (requiredPrintfunctionsAvailable(rule, reasons)) return true;
        }
        return false;

      case setOutput:
        myrules = rules.queryByChild(RuleStatement.ON_OUTPUT.toString());
        for (ConfigThingy rule : myrules)
        {
          String value = rule.getString(RuleStatement.ON_OUTPUT.toString(), null);
          if (!value.equals(actionValue)) continue;
          String actionType =
            rule.getString(RuleStatement.ON_ACTION_TYPE.toString(), null);
          if (actionType == null || !actionType.equals(getCurrentActionType())) continue;
          if (requiredPrintfunctionsAvailable(rule, reasons)) return true;
        }
        return false;
        
      case abort:
        break;
      case selectRange:
        break;
      case submit:
        break;
      default:
        break;
    }
    return false;
  }

  /**
   * Prüft, ob die in einer Regel rule unter dem Schlüssel
   * {@link RuleStatement#USE_PRINTFUNCTIONS} beschriebenen Druckfunktionen
   * ausführbar sind und liefert hängt im Fehlerfall eine textuelle Beschreibung an
   * die übergebene Liste reasons an.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private boolean requiredPrintfunctionsAvailable(ConfigThingy rule,
      List<String> reasons)
  {
    boolean allAvailable = true;
    try
    {
      ConfigThingy usePrintFuncs =
        rule.get(RuleStatement.USE_PRINTFUNCTIONS.toString());
      for (ConfigThingy funcName : usePrintFuncs)
      {
        if (!mmc.hasPrintfunction(funcName.toString()))
        {
          allAvailable = false;
          reasons.add(L.m("Druckfunktion %1 ist nicht verfügbar", funcName));
        }
      }
    }
    catch (NodeNotFoundException e)
    {
      return false;
    }
    return allAvailable;
  }

  /**
   * Testmethode
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void main(String[] args)
  {
    final List<String> fieldNames = new ArrayList<String>();
    fieldNames.add("Anrede");
    fieldNames.add("Name");
    fieldNames.add("EMail");
    final MailMergeParams mmp = new MailMergeParams();
    ConfigThingy sdConf = new ConfigThingy("sdConf");
    try
    {
      sdConf =
        new ConfigThingy(
          "dialogConf",
          new URL(
            "file:///home/christoph.lutz/workspace/WollMux/src/data/seriendruckdialog.conf"));
      sdConf = sdConf.get("Dialoge").get("Seriendruckdialog");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    final ConfigThingy seriendruckConf = sdConf;
    MailMergeController mmc = new MailMergeController()
    {
      public boolean hasPrintfunction(String name)
      {
        String[] funcs =
          new String[] {
          "OOoMailMergeToPrinter", "OOoMailMergeToOdtFile", "MailMergeNewToODTEMail",
          "MailMergeNewToPDFEMail", "MailMergeNewSetFormValue", /* "PDFGesamtdokument", */
          "MailMergeNewToSingleODT", "MailMergeNewToSinglePDF", "PDFGesamtdokumentOutput" };
        for (String func : funcs)
          if (func.equals(name)) return true;
        return false;
      }

      public void doMailMerge(List<String> usePrintFunctions,
          boolean ignoreDocPrintFuncs, DatasetSelectionType datasetSelectionType,
          Map<SubmitArgument, Object> pmodArgs)
      {
        Logger.init(System.out, Logger.ALL);
        System.out.print("PrintFunctions: ");
        for (String func : usePrintFunctions)
          System.out.print("'" + func + "' ");
        System.out.println("");
        System.out.println("IgnoreDocPrintFuncs: " + ignoreDocPrintFuncs);
        System.out.println("datasetSelectionType: " + datasetSelectionType);
        System.out.println("pmodArgs: ");
        for (Entry<SubmitArgument, Object> en : pmodArgs.entrySet())
          System.out.println("  " + en.getKey() + ": " + en.getValue());
      }

      public List<String> getColumnNames()
      {
        return fieldNames;
      }

      public String getDefaultFilename()
      {
        return "MeinDokument";
      }

      private boolean initialized = false;

      public XTextDocument getTextDocument()
      {
        if (!initialized) try
        {
          initialized = true;
          UNO.init();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
        return UNO.XTextDocument(UNO.desktop.getCurrentComponent());
      }
    };

    mmp.showDoMailmergeDialog(new JFrame("irgendwas"), mmc, seriendruckConf,
      "christoph.lutz@muenchen.de");

    try
    {
      Thread.sleep(5000);
    }
    catch (InterruptedException e)
    {}
    System.exit(0);
  }
}

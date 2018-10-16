package de.muenchen.mailmerge.dialog.mailmerge;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResultsWithSchema;
import de.muenchen.allg.itd51.wollmux.core.dialog.TextComponentTags;
import de.muenchen.allg.itd51.wollmux.core.document.SimulationResults.SimulationResultsProcessor;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.mailmerge.ModalDialogs;
import de.muenchen.mailmerge.dialog.mailmerge.gui.IndexSelection;
import de.muenchen.mailmerge.dialog.mailmerge.gui.SubmitArgument;
import de.muenchen.mailmerge.document.DocumentManager;
import de.muenchen.mailmerge.document.TextDocumentController;
import de.muenchen.mailmerge.email.EMailSender;
import de.muenchen.mailmerge.print.model.PrintModels;

public class MailMergeControllerImpl implements MailMergeController
{

  private static final Logger LOGGER = LoggerFactory.getLogger(MailMergeControllerImpl.class);

  /**
   * ID der Property in der die Serienbriefdaten gespeichert werden.
   */
  private static final String PROP_QUERYRESULTS = "MailMergeNew_QueryResults";

  /**
   * ID der Property in der das Zielverzeichnis für den Druck in Einzeldokumente
   * gespeichert wird.
   */
  private static final String PROP_TARGETDIR = "MailMergeNew_TargetDir";

  /**
   * ID der Property in der das Dateinamenmuster für den Einzeldokumentdruck
   * gespeichert wird.
   */
  private static final String PROP_FILEPATTERN = "MailMergeNew_FilePattern";

  /**
   * ID der Property in der der Name des Feldes gespeichert wird, in dem die
   * E-Mail-Adressen der Empfänger enthalten ist.
   */
  private static final String PROP_EMAIL_TO_FIELD_NAME =
    "MailMergeNew_EMailToFieldName";

  /**
   * ID der Property in der der Name des Feldes gespeichert wird, in dem die
   * E-Mail-Adressen der Empfänger enthalten sind.
   */
  private static final String PROP_EMAIL_FROM = "MailMergeNew_EMailFrom";

  /**
   * ID der Property in der die Betreffzeile vom Typ String der zu verschickenden
   * E-Mail enthalten ist.
   */
  private static final String PROP_EMAIL_SUBJECT = "MailMergeNew_EMailSubject";

  /**
   * ID der Property in der die Betreffzeile vom Typ String der zu verschickenden
   * E-Mail enthalten ist.
   */
  private static final String PROP_EMAIL_MESSAGE_TEXTTAGS =
    "MailMergeNew_EMailMessageTextTags";

  /**
   * ID der Property in der das Dateinamenmuster für den Einzeldokumentdruck
   * gespeichert wird.
   */
  private static final String PROP_DATASET_EXPORT = "MailMergeNew_DatasetExport";

  /**
   * ID der Property, die einen List der Indizes der zu druckenden Datensätze
   * speichert.
   */
  private static final String PROP_MAILMERGENEW_SELECTION = "MailMergeNew_Selection";

  private static final String TEMP_MAIL_DIR_PREFIX = "wollmuxmail";

  private static final String MAIL_ERROR_MESSAGE_TITLE =
    L.m("Fehler beim E-Mail-Versand");

  private TextDocumentController documentController;

  /**
   * Stellt die Felder und Datensätze für die Serienbriefverarbeitung bereit.
   */
  private MailMergeDatasource ds;

  public MailMergeControllerImpl(TextDocumentController documentController)
  {
    this.documentController = documentController;
    this.ds = new MailMergeDatasource(documentController);
  }

  @Override
  public boolean hasPrintfunction(String name)
  {
    final XPrintModel pmod = PrintModels.createPrintModel(documentController, true);
    try
    {
      pmod.usePrintFunction(name);
      return true;
    }
    catch (NoSuchMethodException ex)
    {
      return false;
    }
  }

  @Override
  public List<String> getColumnNames()
  {
    return ds.getColumnNames();
  }

  @Override
  public String getDefaultFilename()
  {
    String title = documentController.getFrameController().getTitle();
    // Suffix entfernen:
    if (title.toLowerCase().matches(".+\\.(odt|doc|ott|dot)$"))
      title = title.substring(0, title.length() - 4);
    return simplifyFilename(title);
  }

  @Override
  public XTextDocument getTextDocument()
  {
    return documentController.getModel().doc;
  }

  @Override
  public void doMailMerge(List<String> usePrintFunctions, boolean ignoreDocPrintFuncs,
      DatasetSelectionType datasetSelectionType, Map<SubmitArgument, Object> args)
  {
    documentController.collectNonWollMuxFormFields();
    QueryResultsWithSchema data = ds.getData();

    List<Integer> selected = new ArrayList<>();
    switch (datasetSelectionType)
    {
      case ALL:
        for (int i = 0; i < data.size(); ++i)
        {
          selected.add(i);
        }
        break;
      case INDIVIDUAL:
        IndexSelection indexSelection =
          (IndexSelection) args.get(SubmitArgument.indexSelection);
        selected.addAll(indexSelection.getSelectedIndexes());
        break;
      case RANGE:
        indexSelection = (IndexSelection) args.get(SubmitArgument.indexSelection);
        if (indexSelection.getRangeStart() < 1)
          indexSelection.setRangeStart(1);
        if (indexSelection.getRangeEnd() < 1)
          indexSelection.setRangeEnd(data.size());
        if (indexSelection.getRangeEnd() > data.size())
          indexSelection.setRangeEnd(data.size());
        if (indexSelection.getRangeStart() > data.size())
          indexSelection.setRangeStart(data.size());
        if (indexSelection.getRangeStart() > indexSelection.getRangeEnd())
        {
          int t = indexSelection.getRangeStart();
          indexSelection.setRangeStart(indexSelection.getRangeEnd());
          indexSelection.setRangeEnd(t);
        }
        for (int i = indexSelection.getRangeStart(); i <= indexSelection.getRangeEnd(); ++i)
         {
          selected.add(i - 1); // wir zählen ab 0, anders als rangeStart/End
        }
        break;
    }

    // PrintModel erzeugen und Parameter setzen:
    final XPrintModel pmod = PrintModels.createPrintModel(documentController, !ignoreDocPrintFuncs);
    try
    {
      pmod.setPropertyValue("MailMergeNew_Schema", data.getSchema());
      pmod.setPropertyValue(PROP_QUERYRESULTS, data);
      pmod.setPropertyValue(PROP_MAILMERGENEW_SELECTION, selected);

      Object o = args.get(SubmitArgument.targetDirectory);
      if (o != null)
        pmod.setPropertyValue(PROP_TARGETDIR, o);

      o = args.get(SubmitArgument.filenameTemplate);
      if (o != null)
        pmod.setPropertyValue(PROP_FILEPATTERN, o);

      o = args.get(SubmitArgument.emailToFieldName);
      if (o != null)
        pmod.setPropertyValue(PROP_EMAIL_TO_FIELD_NAME, o);

      o = args.get(SubmitArgument.emailFrom);
      if (o != null)
        pmod.setPropertyValue(PROP_EMAIL_FROM, o);

      o = args.get(SubmitArgument.emailSubject);
      if (o != null)
        pmod.setPropertyValue(PROP_EMAIL_SUBJECT, o);

      o = args.get(SubmitArgument.emailText);
      if (o != null)
        pmod.setPropertyValue(PROP_EMAIL_MESSAGE_TEXTTAGS, o);
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
      return;
    }

    // Benötigte Druckfunktionen zu pmod hinzufügen:
    try
    {
      for (String printFunctionName : usePrintFunctions)
      {
        pmod.usePrintFunction(printFunctionName);
      }
    }
    catch (NoSuchMethodException e)
    {
      LOGGER.error("", e);
      ModalDialogs.showInfoModal(
        L.m("Fehler beim Drucken"),
        L.m(
          "Eine notwendige Druckfunktion ist nicht definiert. Bitte wenden Sie sich an Ihre Systemadministration damit Ihre Konfiguration entsprechend erweitert bzw. aktualisiert werden kann.\n\n%1",
          e));
      pmod.cancel();
      return;
    }

    doPrint(pmod);
  }

  private void doPrint(final XPrintModel pmod)
  {
    // Drucken im Hintergrund, damit der EDT nicht blockiert.
    new Thread()
    {
      @Override
      public void run()
      {
        long startTime = System.currentTimeMillis();

        documentController.clearFormFields();
        documentController.setFormFieldsPreviewMode(true);
        pmod.printWithProps();
        documentController.setFormFieldsPreviewMode(false);

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        LOGGER.debug(L.m("MailMerge finished after %1 seconds", duration));
      }
    }.start();
  }

  /**
   * Nimmt filePattern, ersetzt darin befindliche Tags durch entsprechende
   * Spaltenwerte aus ds und setzt daraus einen Dateipfad mit Elternverzeichnis
   * targetDir zusammen. Die Spezialtags {@link MailMergeParams#TAG_DATENSATZNUMMER}
   * und {@link MailMergeParams#TAG_SERIENBRIEFNUMMER} werden durch die Strings
   * datensatzNummer und serienbriefNummer ersetzt.
   *
   * @param totalDatasets
   *          die Gesamtzahl aller Datensätze (auch der für den aktuellen
   *          Druckauftrag nicht gewählten). Wird verwendet um datensatzNummer und
   *          serienbriefNummer mit 0ern zu padden.
   *
   * @throws MissingMapEntryException
   *           wenn ein Tag verwendet wird, zu dem es keine Spalte im aktuellen
   *           Datensatz existiert.
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  static String createOutputPathFromPattern(TextComponentTags filePattern,
      XPrintModel pmod)
  {
    int digits = 4;
    try
    {
      QueryResults r = (QueryResults) pmod.getPropertyValue(PROP_QUERYRESULTS);
      digits = ("" + r.size()).length();
    }
    catch (Exception e)
    {}

    @SuppressWarnings("unchecked")
    HashMap<String, String> dataset =
      new HashMap<>((HashMap<String, String>) pmod.getProp(PROP_DATASET_EXPORT,
        new HashMap<String, String>()));

    // Zähler für #DS und #SB mit gleicher Länge erzeugen (ggf. mit 0en auffüllen)
    MailMergeNew.fillWithLeading0(dataset, MailMergeParams.TAG_DATENSATZNUMMER, digits);
    MailMergeNew.fillWithLeading0(dataset, MailMergeParams.TAG_SERIENBRIEFNUMMER, digits);

    String fileName = filePattern.getContent(dataset);
    return MailMergeNew.simplifyFilename(fileName);
  }

  /**
   * Speichert das übergebene Dokument in eine ODF-Datei. Die WollMux-Daten bleiben
   * dabei erhalten.
   *
   * @author Ignaz Forster (D-III-ITD-D102)
   */
  public static void sendAsEmail(XPrintModel pmod, boolean isODT)
  {
    String targetDir = (String) pmod.getProp(PROP_TARGETDIR, null);
    File tmpOutDir = null;
    if (targetDir != null)
      tmpOutDir = new File(targetDir);
    else
      try
      {
        tmpOutDir = File.createTempFile(TEMP_MAIL_DIR_PREFIX, null);
        tmpOutDir.delete();
        tmpOutDir.mkdir();
        try
        {
          pmod.setPropertyValue(PROP_TARGETDIR, tmpOutDir.toString());
        }
        catch (Exception e)
        {
          LOGGER.error(L.m("darf nicht vorkommen"), e);
        }
      }
      catch (java.io.IOException e)
      {
        LOGGER.error("", e);
      }
    if (tmpOutDir == null)
    {
      ModalDialogs.showInfoModal(MAIL_ERROR_MESSAGE_TITLE, L.m(
        "Das temporäre Verzeichnis %1 konnte nicht angelegt werden.",
        TEMP_MAIL_DIR_PREFIX));
      pmod.cancel();
      return;
    }

    String from = pmod.getProp(PROP_EMAIL_FROM, "").toString();
    if (!MailMergeNew.isMailAddress(from))
    {
      ModalDialogs.showInfoModal(MAIL_ERROR_MESSAGE_TITLE, L.m(
        "Die Absenderadresse '%1' ist ungültig.", from));
      pmod.cancel();
      return;
    }

    String fieldName = pmod.getProp(PROP_EMAIL_TO_FIELD_NAME, "").toString();
    @SuppressWarnings("unchecked")
    HashMap<String, String> ds =
      new HashMap<>((HashMap<String, String>) pmod.getProp(
        PROP_DATASET_EXPORT, new HashMap<String, String>()));
    String to = ds.get(fieldName);
    PrintModels.setStage(pmod, L.m("Sende an %1", to));
    if (!MailMergeNew.isMailAddress(to))
    {
      int res =
        JOptionPane.showConfirmDialog(
          null,
          L.m(
            "Die Empfängeradresse '%1' ist ungültig!\n\nDiesen Datensatz überspringen und fortsetzen?",
            to), MAIL_ERROR_MESSAGE_TITLE, JOptionPane.OK_CANCEL_OPTION);
      if (res == JOptionPane.CANCEL_OPTION)
        pmod.cancel();
      return;
    }

    String subject =
      pmod.getProp(PROP_EMAIL_SUBJECT, L.m("<kein Betreff>")).toString();

    String message = "";
    TextComponentTags messageTags =
      (TextComponentTags) pmod.getProp(PROP_EMAIL_MESSAGE_TEXTTAGS, null);
    if (messageTags != null)
      message = messageTags.getContent(ds);

    File attachment = null;
    try
    {
      attachment = saveToFile(pmod, isODT);
      EMailSender mail = new EMailSender();
      mail.createNewMultipartMail(from, to, subject, message);
      mail.addAttachment(attachment);
      mail.sendMessage();
    }
    catch (ConfigurationErrorException e)
    {
      LOGGER.error("", e);
      ModalDialogs.showInfoModal(
        MAIL_ERROR_MESSAGE_TITLE,
        L.m("Es konnten keine Angaben zum Mailserver gefunden werden - eventuell ist die WollMux-Konfiguration nicht vollständig."));
      pmod.cancel();
      return;
    }
    catch (MessagingException e)
    {
      LOGGER.error("", e);
      ModalDialogs.showInfoModal(MAIL_ERROR_MESSAGE_TITLE,
        L.m("Der Versand der E-Mail ist fehlgeschlagen."));
      pmod.cancel();
      return;
    }
    catch (Exception e)
    {
      LOGGER.error("", e);
      pmod.cancel();
      return;
    }
    finally
    {
      if (attachment != null)
        attachment.delete();
    }
  }

  /**
   * Speichert das übergebene Dokument in eine ODF-Datei. Die WollMux-Daten bleiben
   * dabei erhalten.
   *
   * @author Ignaz Forster (D-III-ITD-D102)
   * @throws java.io.IOException
   */
  public static File saveToFile(XPrintModel pmod, boolean isODT)
  {
    XTextDocument textDocument = pmod.getTextDocument();

    File outputDir =
      new File(pmod.getProp(PROP_TARGETDIR,
        System.getProperty("user.home") + "/Seriendruck").toString());

    String filename;
    TextComponentTags filePattern =
      (TextComponentTags) pmod.getProp(PROP_FILEPATTERN, null);
    if (filePattern != null)
      filename = MailMergeControllerImpl.createOutputPathFromPattern(filePattern, pmod);
    else
      filename = L.m("Dokument.odt");

    // jub .odt/.pdf ergänzen, falls nicht angegeben.
    if (!filename.toLowerCase().endsWith(".odt")
      && !filename.toLowerCase().endsWith(".pdf"))
    {
      if (isODT)
        filename = filename + ".odt";
      else
        filename = filename + ".pdf";
    }

    File file = new File(outputDir, filename);

    MailMergeNew.saveOutputFile(file, textDocument);

    return file;
  }

  /**
   * Liefert die Größe der von MailMergeNew im XPrintModel gesetzten Selection.
   */
  @SuppressWarnings("unchecked")
  public static int mailMergeNewGetSelectionSize(XPrintModel pmod)
  {
    List<Integer> selection;
    try
    {
      selection = (List<Integer>) pmod.getPropertyValue(PROP_MAILMERGENEW_SELECTION);
    }
    catch (Exception e)
    {
      LOGGER.error("", e);
      return 0;
    }
    return selection.size();
  }

  /**
   * Implementierung einer Druckfunktion, die das jeweils nächste Element der
   * Seriendruckdaten nimmt und die Seriendruckfelder im Dokument entsprechend setzt;
   * wird der SimulationsResultsProcessor simProc übergeben, so werden die
   * Dokumentänderungen nur simuliert und nicht tatsächlich im Dokument ausgeführt.
   * Im Fall, dass simProc != null ist, wird auch die nächste Druckfunktion in der
   * Aufrufkette nicht aufgerufen, sondern statt dessen der in simProc enthaltene
   * handler. Die Druckfunktion zieht folgende Properties heran:
   *
   * <ul>
   * <li>{@link MailMergeNew#PROP_QUERYRESULTS} (ein Objekt vom Typ {@link QueryResults})</li>
   *
   * <li>"MailMergeNew_Schema", was ein Set mit den Spaltennamen enthält</li>
   *
   * <li>{@link MailMergeNew#PROP_MAILMERGENEW_SELECTION}, was eine Liste der Indizes der
   * ausgewählten Datensätze ist (0 ist der erste Datensatz).</li> *
   * <ul>
   *
   * @param pmod
   *          PrintModel welches das Hauptdokument des Seriendrucks beschreibt.
   * @param simProc
   *          Ist simProc != null, so werden die Wertänderungen nur simuliert und
   *          nach jedem Datensatz einmal der in simProc enthaltene handler
   *          aufgerufen.
   * @throws Exception
   *           Falls irgend etwas schief geht
   *
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD-D101)
   *         TESTED
   */
  @SuppressWarnings("unchecked")
  public static void mailMergeNewSetFormValue(XPrintModel pmod,
      SimulationResultsProcessor simProc) throws Exception
  {
    TextDocumentController documentController =
      DocumentManager.getTextDocumentController(pmod.getTextDocument());

    QueryResults data = (QueryResults) pmod.getPropertyValue(PROP_QUERYRESULTS);
    Collection<String> schema = (Collection<String>) pmod.getPropertyValue("MailMergeNew_Schema");
    List<Integer> selection =
      (List<Integer>) pmod.getPropertyValue(PROP_MAILMERGENEW_SELECTION);
    if (selection.isEmpty())
      return;

    Iterator<Dataset> iter = data.iterator();
    Iterator<Integer> selIter = selection.iterator();
    int selectedIdx = selIter.next();

    pmod.setPrintProgressMaxValue((short) selection.size());

    int index = -1;
    int serienbriefNummer = 1;
    while (iter.hasNext() && selectedIdx >= 0)
    {
      if (pmod.isCanceled())
        return;

      Dataset ds = iter.next();
      if (++index < selectedIdx)
        continue;

      int datensatzNummer = index + 1; // same as datensatzNummer = selectedIdx+1;

      if (selIter.hasNext())
        selectedIdx = selIter.next();
      else
        selectedIdx = -1;

      if (simProc != null)
        documentController.startSimulation();

      HashMap<String, String> dataSetExport = new HashMap<>();
      try
      {
        pmod.setPropertyValue(PROP_DATASET_EXPORT, dataSetExport);
      }
      catch (Exception x)
      {}

      for(String spalte : schema)
      {
        String value = ds.get(spalte);
        // Wert zuerst entsetzen um sicher eine Änderung hervorzurufen.
        // Denn ansonsten werden die Sichtbarkeiten nicht richtig aktualisiert.
        pmod.setFormValue(spalte, "");
        pmod.setFormValue(spalte, value);
        dataSetExport.put(spalte, value);
      }
      pmod.setFormValue(MailMergeParams.TAG_DATENSATZNUMMER, "" + datensatzNummer);
      dataSetExport.put(MailMergeParams.TAG_DATENSATZNUMMER, "" + datensatzNummer);
      pmod.setFormValue(MailMergeParams.TAG_SERIENBRIEFNUMMER, ""
        + serienbriefNummer);
      dataSetExport.put(MailMergeParams.TAG_SERIENBRIEFNUMMER, ""
        + serienbriefNummer);

      // Weiterreichen des Drucks an die nächste Druckfunktion. Dies findet nicht
      // statt, wenn simProc != null ist, da die Verarbeitung in diesem Fall über
      // simProc durchgeführt wird.
      if (simProc == null)
        pmod.printWithProps();
      else
        simProc.processSimulationResults(documentController.stopSimulation());

      pmod.setPrintProgressValue((short) serienbriefNummer);
      ++serienbriefNummer;
    }
  }

  /**
   * PrintFunction, die das jeweils nächste Element der Seriendruckdaten nimmt und
   * die Seriendruckfelder im Dokument entsprechend setzt. Herangezogen werden die
   * Properties {@link MailMergeNew#PROP_QUERYRESULTS} (ein Objekt vom Typ {@link QueryResults})
   * und "MailMergeNew_Schema", was ein Set mit den Spaltennamen enthält, sowie
   * {@link MailMergeNew#PROP_MAILMERGENEW_SELECTION}, was eine Liste der Indizes der
   * ausgewählten Datensätze ist (0 ist der erste Datensatz). Dies funktioniert
   * natürlich nur dann, wenn pmod kein Proxy ist.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static void mailMergeNewSetFormValue(XPrintModel pmod) throws Exception
  {
    MailMergeControllerImpl.mailMergeNewSetFormValue(pmod, null);
  }

  /**
   * Ersetzt alle möglicherweise bösen Zeichen im Dateinamen name durch eine
   * Unterstrich.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static String simplifyFilename(String name)
  {
    return name.replaceAll("[^\\p{javaLetterOrDigit},.()=+_-]", "_");
  }

  @Override
  public void showDatasourceSelectionDialog(JFrame parent, Runnable callback)
  {
    ds.showDatasourceSelectionDialog(parent, callback);
  }

  @Override
  public boolean hasDatasource()
  {
    return ds.hasDatasource();
  }

  @Override
  public void close()
  {
    ds.dispose();
  }

  @Override
  public void bringDatasourceToFront()
  {
    ds.toFront();
  }

  @Override
  public void showAddMissingColumnsDialog(JFrame parent)
  {
    AdjustFields.showAddMissingColumnsDialog(parent, documentController, ds);
  }

  @Override
  public void showAdjustFieldsDialog(JFrame parent)
  {
    AdjustFields.showAdjustFieldsDialog(parent, documentController, ds);
  }

  @Override
  public int getNumberOfDatasets()
  {
    return ds.getNumberOfDatasets();
  }

  @Override
  public List<String> getValuesForDataset(int rowIndex)
  {
    return ds.getValuesForDataset(rowIndex);
  }

  @Override
  public boolean isDatasourceSupportingAddColumns()
  {
    return ds.supportsAddColumns();
  }
}

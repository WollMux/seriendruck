package de.muenchen.mailmerge.func;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.mailmerge.dialog.pdf.InfoDialog;
import de.muenchen.mailmerge.dialog.pdf.InputDialog;
import de.muenchen.mailmerge.dialog.pdf.ParametersDialog;

/**
 * Diese Klasse implementiert eine Druckfunktion für den WollMux, die als Plugin
 * über den WollMux aufgerufen werden kann und die Verkettung mehrerer Ausdrucke
 * in eine PDF-Datei ermöglicht. Dabei wird aus OpenOffice.org heraus jeder
 * Ausdruck als einzelne pdf-Datei exportiert und diese über die Bibliothek
 * pdfBox zusammgefügt.
 *
 * Die Einbindung der Druckfunktionen kann über die Datei conf/funktionen.conf
 * der WollMux-Konfiguration vorgenommen werden. Im Paket
 * WollMux-Standard-Config ist die Einbindung des Plugins bereits konfiguriert
 * und kann daher als Beispiel für die Einbindung herangezogen werden.
 *
 * Die Haupt-Druckfuntion heißt mailMerge() und sollte in der WollMux-Konfig
 * unter dem Namen "PDFGesamtdokument" und mit einem geringen ORDER-Wert (z.B.
 * 40) eingebunden. Diese Methode benötigt darüber hinaus eine zweite
 * Druckfunktion mailMergeOutput(), die in der WollMux-Konfiguration unter dem
 * Namen "PDFGesamtdokumentOutput" und mit einem hohen ORDER-Wert (z.B. 200)
 * eingebunden werden muss.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class PDFMailMerge
{
  /**
   * Name der Output-Druckfunktion, die von mailMerge() benötigt wird.
   */
  private static final String OUTPUT_METHOD_CONFIG_NAME = "PDFGesamtdokumentOutput";

  /**
   * Temporäre Dateien bekommen diesen Präfix im Dateinamen
   */
  private static final String TEMPFILE_PREFIX = "pdfMailMerge";

  private static final String PDFMM_TMP_OUTPUT_DIR = "PDFMailMerge_PDFBox_TMP_OUTPUT_DIR";

  private static final String PDFMM_TMP_FILE_COUNT = "PDFMailMerge_PDFBox_TMP_FILE_COUNT";

  /**
   * Die Aufgabe dieser Druckfunktion ist es, den Dialog zur Auswahl der
   * Optionen anzuzeigen, den Druck an Druckfunktionen mit höherem ORDER-Wert
   * weiterzuleiten, das PDF-Dokument abzuschließen und den externen
   * PDF-Betrachter zu starten. Der Export der Einzeldokumente (also die finale
   * Druck-Aktion) findet in der Methode mailMergeOutput statt, die in der
   * WollMux-Konfiguration unter dem Namen "PDFGesamtdokumentOutput" definiert
   * sein muss.
   *
   * @param pmod
   *          Das Print-Model, das der WollMux dieser Druckfunktion übergibt.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void mailMerge(final XPrintModel pmod)
  {
    // Benötigte Output-Druckfunktion nachladen
    try
    {
      pmod.usePrintFunction(OUTPUT_METHOD_CONFIG_NAME);
    } catch (NoSuchMethodException e)
    {
      InfoDialog.showInfo(
          "Die benötigte Druckfunktion '" + OUTPUT_METHOD_CONFIG_NAME
              + "' konnte nicht gefunden werden. Bitte verständigen Sie Ihre Systemadministration.",
          null, 80);
      return;
    }

    // Dialog für Optionen des PDF-Gesamtdrucks starten und Werte PDFMM_DUPLEX,
    // PDFMM_OUTPUT_FILE, pdfViewer auswerten
    ParametersDialog pd = new ParametersDialog();
    pd.showDialog(true, null, null, false);
    if (pd.isCanceled())
    {
      pmod.cancel();
      return;
    }
    String pdfViewer = pd.getPDFViewerCmd();
    File outputFile = pd.getOutputFile();
    if (outputFile == null)
      try
      {
        outputFile = File.createTempFile(TEMPFILE_PREFIX, ".pdf");
      } catch (Exception e)
      {
        InfoDialog.showInfo(
            "Beim Drucken in das pdf-Gesamtdokument ist ein Fehler aufgetreten.",
            e, 80);
        pmod.cancel();
        return;
      }

    // Temporäres Ausgabeverzeichnis anlegen für Output-Druckfunktion
    File tmpOutDir;
    try
    {
      tmpOutDir = File.createTempFile(TEMPFILE_PREFIX, null);
      if (!tmpOutDir.delete() || !tmpOutDir.mkdir())
        throw new Exception("Directory " + tmpOutDir + " nicht angelegt.");
    } catch (Exception e)
    {
      InfoDialog.showInfo("Kann kein temporäres Ausgabeverzeichnis anlegen.",
          e, 80);
      pmod.cancel();
      return;
    }
    try
    {
      pmod.setPropertyValue(PDFMM_TMP_OUTPUT_DIR, tmpOutDir.getAbsolutePath());
    } catch (Exception e)
    {
    }

    // Druck weiterleiten an Druckfunktionen mit höherem ORDER-Wert
    pmod.printWithProps();

    // Ausgabedokument abschließen
    PDFMergerUtility merger = new PDFMergerUtility();
    merger.setDestinationFileName(outputFile.getAbsolutePath());
    try
    {
      boolean duplex = pd.isDuplexPrintRequired();
      PDDocument dest = new PDDocument();
      pmod.setPrintProgressMaxValue((short) tmpOutDir.list().length);
      short n = 1;
      pmod.setPrintProgressValue(n++);

      // Files alphabetisch sortieren:
      SortedMap<String, File> sorter = new TreeMap<>();
      for (File file : tmpOutDir.listFiles())
        sorter.put(file.getName(), file);

      // mergen
      for (Entry<String, File> entry : sorter.entrySet())
      {
        if (pmod.isCanceled())
        {
          break;
        }
        File file = entry.getValue();
        PDDocument source = PDDocument.load(file);
        if (duplex && source.getNumberOfPages() % 2 != 0)
          source.addPage(new PDPage(PDRectangle.A4));
        merger.appendDocument(dest, source);
        source.close();
        file.delete();
        pmod.setPrintProgressValue(n++);
        Thread.sleep(1);
      }
      boolean tryagain = true;
      while (tryagain)
      {
        if (pmod.isCanceled())
        {
          break;
        }
        try
        {
          dest.save(outputFile.getAbsolutePath());
          // Erfolg also nicht nochmal versuchen
          tryagain = false;
        } catch (Exception ex)
        {
          InputDialog id = new InputDialog("Fehler beim PDF-Druck",
              "Beim Schreiben in die Datei " + outputFile.getAbsolutePath()
                  + " ist ein Fehler aufgetreten (" + ex.getMessage()
                  + "), wollen Sie es erneut versuchen?", 80);
          if (id.askForInput())
          {
            // Erneuter Versuch
            tryagain = true;
            ParametersDialog fileNameDialog = new ParametersDialog(
                outputFile.getParent());
            fileNameDialog.showDialog(false, outputFile.getAbsolutePath(),
                pdfViewer, duplex);
            if (fileNameDialog.isCanceled())
            {
              tryagain = false;
              outputFile = null;
            } else
            {
              outputFile = fileNameDialog.getOutputFile();
              if (outputFile == null)
              {
                InfoDialog.showInfo(
                    "Ausgabedatei konnte nicht ermittelt werden.", null, 80);
                tryagain = false;
              }
            }
          } else
          {
            // Der Benutzer ignoriert den Fehler
            tryagain = false;
            outputFile = null;
          }
        }
        Thread.sleep(1);
      }

      // aufräumen
      dest.close();
      // Wenn abgebrochen wird, sind immer noch Dateien da, die gelöscht
      // werden müssen.
      if (tmpOutDir.listFiles().length > 0)
      {
        for (File f : tmpOutDir.listFiles())
        {
          f.delete();
        }
      }
      tmpOutDir.delete();
    } catch (Exception e)
    {
      InfoDialog.showInfo("Fehler beim Verbinden der PDF-Dokumente:", e, 80);
      pmod.cancel();
      return;
    }

    // Ausgabedokument anzeigen oder speichern
    String outputFileName = null;
    if (outputFile != null && outputFile.exists())
      outputFileName = outputFile.getAbsolutePath();

    if (pdfViewer != null && outputFileName != null && !pmod.isCanceled())
      try
      {
        Runtime.getRuntime().exec(new String[] { pdfViewer, outputFileName });
      } catch (IOException e)
      {
        InfoDialog.showInfo(
            "Fehler beim Starten des PDF-Betrachters.\n\nFolgendes Kommando wurde aufgerufen:\n"
                + pdfViewer + " " + outputFileName,
            e, 80);
        return;
      }
  }

  /**
   * Diese Methode exportiert pmod.getTextDocument() als pdf-Datei im durch
   * PDFMM_TMP_OUTPUT_DIR definierten temporären Verzeichnis. Die erzeugten
   * pdf-Dateien bekommen eindeutige, alphabetisch sortierbare Namen, über die
   * die korrekte Reihenfolge der Ausdrucke festgelegt wird. Kommt es während
   * dem Seriendruck zu Fehlern, so erscheint ein modaler Info-Dialog mit der
   * entsprechenden Fehlermeldung.
   *
   * @param pmod
   *          Das Print-Model, das der WollMux dieser Druckfunktion übergibt.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void mailMergeOutput(XPrintModel pmod)
  {
    if (pmod.isCanceled())
      return;

    // tmporäres Output-Verzeichnis holen (muss von Vorgänger-Druckfunktion
    // kommen)
    String tmpPath = (String) getProp(pmod, PDFMM_TMP_OUTPUT_DIR);
    if (tmpPath == null || tmpPath.length() == 0)
      return;

    // count initialisieren bzw. erhöhen.
    Integer count = (Integer) getProp(pmod, PDFMM_TMP_FILE_COUNT);
    if (count == null)
      count = -1;
    try
    {
      count++;
      pmod.setPropertyValue(PDFMM_TMP_FILE_COUNT, count);
    } catch (Exception e)
    {
    }
    String countStr = "000000000000000" + count;
    countStr = countStr.substring(countStr.length() - 12);

    try
    {
      // export current OOo-Document as pdf to tempfile
      File tmpFile = new File(tmpPath, countStr + ".pdf");
      XStorable xStorable = UnoRuntime.queryInterface(XStorable.class,
          pmod.getTextDocument());
      xStorable.storeToURL(tmpFile.toURI().toString(),
          new PropertyValue[] {
              new PropertyValue("FilterName", -1, "writer_pdf_Export",
                  PropertyState.DIRECT_VALUE),
              new PropertyValue("Overwrite", -1, "true",
                  PropertyState.DIRECT_VALUE) });
    } catch (Exception e)
    {
      InfoDialog.showInfo(
          "Beim Drucken in das pdf-Gesamtdokument ist ein Fehler aufgetreten.",
          e, 80);
      pmod.cancel();
      return;
    }
  }

  /**
   * Helpermethode: Macht das selbe wie pmod.getPropertyValue(id), Exceptions
   * werden aber ignoriert und statt dessen null zurück geliefert. Insbesondere
   * soll natürlich eine mögliche UnknownPropertyException ignoriert werden,
   * wenn nur getestet wird, ob der Wert schon gesetzt ist.
   */
  private static Object getProp(XPrintModel pmod, String id)
  {
    try
    {
      return pmod.getPropertyValue(id);
    } catch (UnknownPropertyException e)
    {
    } catch (WrappedTargetException e)
    {
    }
    return null;
  }
}

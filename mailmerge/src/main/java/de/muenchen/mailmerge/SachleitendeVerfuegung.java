/*
 * Dateiname: SachleitendeVerfuegung.java
 * Projekt  : WollMux
 * Funktion : Hilfen für Sachleitende Verfügungen.
 *
 * Copyright (c) 2009-2015 Landeshauptstadt München
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
 * 26.09.2006 | LUT | Erstellung als SachleitendeVerfuegung
 * 31.07.2009 | BED | +"copyOnly"
 * 04.05.2011 | LUT | Ziffernanzeige und String "Abdruck" konfigurierbar
 *                    Patch von Jan Gerrit Möltgen (JanGerrit@burg-borgholz.de)
 * 09.09.2014 | JGM | Update der Dokumentenstruktur vorm Drucken eingefuegt.
 *                    Behebt Bug#12079 "SLV Druckbloecke in Bereichen"
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 *
 */
package de.muenchen.mailmerge;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.container.XNamed;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.style.XStyle;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.SyncActionListener;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.mailmerge.dialog.SachleitendeVerfuegungenDruckdialog;
import de.muenchen.mailmerge.dialog.SachleitendeVerfuegungenDruckdialog.VerfuegungspunktInfo;
import de.muenchen.mailmerge.document.DocumentManager;
import de.muenchen.mailmerge.document.TextDocumentController;

public class SachleitendeVerfuegung
{
  public static final String BLOCKNAME_SLV_ALL_VERSIONS = "AllVersions";

  public static final String BLOCKNAME_SLV_ORIGINAL_ONLY = "OriginalOnly";

  public static final String BLOCKNAME_SLV_NOT_IN_ORIGINAL = "NotInOriginal";

  public static final String BLOCKNAME_SLV_DRAFT_ONLY = "DraftOnly";

  public static final String BLOCKNAME_SLV_COPY_ONLY = "CopyOnly";

  public static final String GROUP_ID_SLV_ALL_VERSIONS =
    "SLV_" + BLOCKNAME_SLV_ALL_VERSIONS;

  public static final String GROUP_ID_SLV_ORIGINAL_ONLY =
    "SLV_" + BLOCKNAME_SLV_ORIGINAL_ONLY;

  public static final String GROUP_ID_SLV_NOT_IN_ORIGINAL =
    "SLV_" + BLOCKNAME_SLV_NOT_IN_ORIGINAL;

  public static final String GROUP_ID_SLV_DRAFT_ONLY =
    "SLV_" + BLOCKNAME_SLV_DRAFT_ONLY;

  public static final String GROUP_ID_SLV_COPY_ONLY =
    "SLV_" + BLOCKNAME_SLV_COPY_ONLY;

  public static final String PRINT_FUNCTION_NAME = "SachleitendeVerfuegung";

  private static final String PARAGRAPH_STYLES = "ParagraphStyles";

  private static final String ParaStyleNameVerfuegungspunkt =
    "WollMuxVerfuegungspunkt";

  private static final String ParaStyleNameVerfuegungspunkt1 =
    "WollMuxVerfuegungspunkt1";

  private static final String ParaStyleNameVerfuegungspunktMitZuleitung =
    "WollMuxVerfuegungspunktMitZuleitung";

  private static final String ParaStyleNameZuleitungszeile =
    "WollMuxZuleitungszeile";

  private static final String CharStyleNameRoemischeZiffer =
    "WollMuxRoemischeZiffer";

  private static final String FrameNameVerfuegungspunkt1 =
    "WollMuxVerfuegungspunkt1";

  /**
   * Erkennt mindestens eine römische oder eine arabische Ziffer gefolgt von einem
   * "." (auch im römischen Modus werden die Ziffern über 15 arabisch dargestellt).
   */
  private static final String zifferPattern = "^([XIV]+|\\d+)\\.\t";

  /**
   * Enthält einen Vector mit den ersten 15 Ziffern (gemäß der Konfig-Einstellung
   * SachleitendeVerfuegungen/NUMBERS). Mehr wird in Sachleitenden Verfügungen
   * sicherlich nicht benötigt :-). Höhere Ziffern sind automatisch arabische
   * Ziffern.
   */
  private static final String[] romanNumbers = getNumbers();

  /**
   * Enthält den String "Abdruck" oder die per SachleitendeVerfuegungen/ABDRUCK_NAME
   * konfigurierte Alternative.
   */
  private static final String copyName = getCopyName();

  /**
   * Holt sich aus dem übergebenen Absatz paragraph nur den Breich der römischen
   * Ziffer (+Tab) und formatiert diesen im Zeichenformat WollMuxRoemischeZiffer.
   *
   * @param paragraph
   */
  private static void formatRoemischeZifferOnly(XTextRange paragraph)
  {
    XTextCursor zifferOnly = getZifferOnly(paragraph, false);
    if (zifferOnly != null)
    {
      UNO.setProperty(zifferOnly, "CharStyleName", CharStyleNameRoemischeZiffer);

      // Zeichen danach auf Standardformatierung setzen, damit der Text, der
      // danach geschrieben wird nicht auch obiges Zeichenformat besitzt:
      // ("Standard" gilt laut DevGuide auch in englischen Versionen)
      UNO.setProperty(zifferOnly.getEnd(), "CharStyleName", "Standard");
    }
  }

  /**
   * Liefert true, wenn es sich bei dem übergebenen Absatz paragraph um einen als
   * Verfuegungspunkt markierten Absatz handelt.
   *
   * @param paragraph
   *          Das Objekt mit der Property ParaStyleName, die für den Vergleich
   *          herangezogen wird.
   * @return true, wenn der Name des Absatzformates mit "WollMuxVerfuegungspunkt"
   *         beginnt.
   */
  private static boolean isVerfuegungspunkt(XTextRange paragraph)
  {
    String paraStyleName = "";
    try
    {
      paraStyleName =
        AnyConverter.toString(UNO.getProperty(paragraph, "ParaStyleName"));
    }
    catch (IllegalArgumentException e)
    {}
    return paraStyleName.startsWith(ParaStyleNameVerfuegungspunkt);
  }

  /**
   * Liefert true, wenn es sich bei dem übergebenen Absatz paragraph um einen als
   * VerfuegungspunktMitZuleitung markierten Absatz handelt.
   *
   * @param paragraph
   *          Das Objekt mit der Property ParaStyleName, die für den Vergleich
   *          herangezogen wird.
   * @return true, wenn der Name des Absatzformates mit
   *         "WollMuxVerfuegungspunktMitZuleitung" beginnt.
   */
  private static boolean isVerfuegungspunktMitZuleitung(XTextRange paragraph)
  {
    String paraStyleName = "";
    try
    {
      paraStyleName =
        AnyConverter.toString(UNO.getProperty(paragraph, "ParaStyleName"));
    }
    catch (IllegalArgumentException e)
    {}
    return paraStyleName.startsWith(ParaStyleNameVerfuegungspunktMitZuleitung);
  }

  /**
   * Liefert true, wenn es sich bei dem übergebenen Absatz paragraph um einen als
   * Zuleitungszeile markierten Absatz handelt.
   *
   * @param paragraph
   *          Das Objekt mit der Property ParaStyleName, die für den Vergleich
   *          herangezogen wird.
   * @return true, wenn der Name des Absatzformates mit "WollMuxZuleitungszeile"
   *         beginnt.
   */
  private static boolean isZuleitungszeile(XTextRange paragraph)
  {
    String paraStyleName = "";
    try
    {
      paraStyleName =
        AnyConverter.toString(UNO.getProperty(paragraph, "ParaStyleName"));
    }
    catch (IllegalArgumentException e)
    {}
    return paraStyleName.startsWith(ParaStyleNameZuleitungszeile);
  }

  /**
   * Liefert true, wenn der übergebene Paragraph paragraph den für Abdrucke typischen
   * String in der Form "Abdruck von I[, II, ...][ und n]" enthält, andernfalls
   * false.
   *
   * @param paragraph
   *          der zu prüfende Paragraph
   * @return
   */
  private static boolean isAbdruck(XTextRange paragraph)
  {
    String str = paragraph.getString();
    return str.contains(copyName + " von " + romanNumbers[0])
      || str.contains(copyName + " von <Vorgänger>.");
  }

  /**
   * Liefert den letzten Teil suffix, der am Ende eines Abdruck-Strings der Form
   * "Abdruck von I[, II, ...][ und n]<suffix>" gefunden wird oder "", wenn der kein
   * Teil gefunden wurde. Das entspricht dem Text, den der Benutzer manuell
   * hinzugefügt hat.
   *
   * @param paragraph
   *          der Paragraph, der den Abdruck-String enthält.
   * @return den suffix des Abdruck-Strings, der überlicherweise vom Benutzer manuell
   *         hinzugefügt wurde.
   */
  private static String getAbdruckSuffix(XTextRange paragraph)
  {
    String str = paragraph.getString();
    Matcher m =
      Pattern.compile(
        "[XIV0-9]+\\.\\s*" + copyName + " von " + romanNumbers[0]
          + "(, [XIV0-9]+\\.)*( und [XIV0-9]+\\.)?(.*)").matcher(str);
    if (m.matches())
      return m.group(3);
    return "";
  }

  /**
   * Sucht nach allen Absätzen im Haupttextbereich des Dokuments doc (also nicht in
   * Frames), deren Absatzformatname mit "WollMuxVerfuegungspunkt" beginnt und
   * numeriert die bereits vorhandenen römischen Ziffern neu durch oder erzeugt eine
   * neue Ziffer, wenn in einem entsprechenden Verfügungspunkt noch keine Ziffer
   * gesetzt wurde. Ist ein Rahmen mit dem Namen WollMuxVerfuegungspunkt1 vorhanden,
   * der einen als Verfügungpunkt markierten Paragraphen enthält, so wird dieser
   * Paragraph immer (gemäß Konzept) als Verfügungspunkt "I" behandelt.
   *
   * @param doc
   *          Das Dokument, in dem alle Verfügungspunkte angepasst werden sollen.
   */
  public static void ziffernAnpassen(TextDocumentController documentController)
  {
    XTextRange punkt1 = getVerfuegungspunkt1(documentController.getModel().doc);

    // Zähler für Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    int count = 0;
    if (punkt1 != null)
      count++;

    // Paragraphen des Texts enumerieren und dabei alle Verfuegungspunkte neu
    // nummerieren. Die Enumeration erfolgt über einen ParagraphCursor, da sich
    // dieser stabiler verhält als das Durchgehen der XEnumerationAccess, bei
    // der es zu OOo-Abstürzen kam. Leider konnte ich das Problem nicht exakt
    // genug isolieren um ein entsprechende Ticket bei OOo dazu aufmachen zu
    // können, da der Absturz nur sporadisch auftrat.
    XParagraphCursor cursor =
      UNO.XParagraphCursor(documentController.getModel().doc.getText().createTextCursorByRange(
        documentController.getModel().doc.getText().getStart()));
    if (cursor != null)
    {
      do
      {
        // ganzen Paragraphen markieren
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor))
        {
          count++;

          if (isAbdruck(cursor))
          {
            // Behandlung von Paragraphen mit einem "Abdruck"-String
            String abdruckStr = abdruckString(count) + getAbdruckSuffix(cursor);
            if (!cursor.getString().equals(abdruckStr))
            {
              cursor.setString(abdruckStr);
              formatRoemischeZifferOnly(cursor);
            }
          }
          else
          {
            // Behandlung von normalen Verfügungspunkten:
            String numberStr = romanNumber(count) + "\t";
            XTextRange zifferOnly = getZifferOnly(cursor, false);
            if (zifferOnly != null)
            {
              // Nummer aktualisieren wenn sie nicht mehr stimmt.
              if (!zifferOnly.getString().equals(numberStr))
                zifferOnly.setString(numberStr);
            }
            else
            {
              // Nummer neu anlegen, wenn wie noch gar nicht existierte
              zifferOnly = cursor.getText().createTextCursorByRange(cursor.getStart());
              zifferOnly.setString(numberStr);
              formatRoemischeZifferOnly(zifferOnly);
            }
          }
        }
      } while (cursor.gotoNextParagraph(false));
    }

    // Verfuegungspunt1 setzen
    if (punkt1 != null)
    {
      XTextRange zifferOnly = getZifferOnly(punkt1, false);
      if (zifferOnly != null)
      {
        if (count == 1)
          zifferOnly.setString("");
      }
      else
      {
        if (count > 1) punkt1.getStart().setString(romanNumbers[0]);
      }
    }

    // Setzte die Druckfunktion SachleitendeVerfuegung wenn mindestens manuell
    // eingefügter Verfügungspunkt vorhanden ist. Ansonsten setze die
    // Druckfunktion zurück.
    int effectiveCount = (punkt1 != null) ? count - 1 : count;
    if (effectiveCount > 0)
      documentController.addPrintFunction(PRINT_FUNCTION_NAME);
    else
      documentController.removePrintFunction(PRINT_FUNCTION_NAME);
  }

  /**
   * Liefert eine XTextRange, die genau die römische Ziffer (falls vorhanden mit
   * darauf folgendem \t-Zeichen) am Beginn eines Absatzes umschließt oder null,
   * falls keine Ziffer gefunden wurde. Bei der Suche nach der Ziffer werden nur die
   * ersten 7 Zeichen des Absatzes geprüft.
   *
   * @param par
   *          die TextRange, die den Paragraphen umschließt, in dessen Anfang nach
   *          der römischen Ziffer gesucht werden soll.
   * @param includeNoTab
   *          ist includeNoTab == true, so enthält der cursor immer nur die Ziffer
   *          ohne das darauf folgende Tab-Zeichen.
   * @return die TextRange, die genau die römische Ziffer umschließt falls eine
   *         gefunden wurde oder null, falls keine Ziffer gefunden wurde.
   */
  private static XTextCursor getZifferOnly(XTextRange par, boolean includeNoTab)
  {
    XParagraphCursor cursor =
      UNO.XParagraphCursor(par.getText().createTextCursorByRange(par.getStart()));

    for (int i = 0; i < 7; i++)
    {
      String text = "";
      if (!cursor.isEndOfParagraph())
      {
        cursor.goRight((short) 1, true);
        text = cursor.getString();
        if (includeNoTab) text += "\t";
      }
      else
      {
        // auch eine Ziffer erkennen, die nicht mit \t endet.
        text = cursor.getString() + "\t";
      }
      if (text.matches(zifferPattern + "$"))
        return cursor;
    }

    return null;
  }

  /**
   * Liefert das Textobjekt des TextRahmens WollMuxVerfuegungspunkt1 oder null, falls
   * der Textrahmen nicht existiert. Der gesamte Text innerhalb des Textrahmens wird
   * dabei automatisch mit dem Absatzformat WollMuxVerfuegungspunkt1 vordefiniert.
   *
   * @param doc
   *          das Dokument, in dem sich der TextRahmen WollMuxVerfuegungspunkt1
   *          befindet (oder nicht).
   * @return Das Textobjekts des TextRahmens WollMuxVerfuegungspunkt1 oder null,
   *         falls der Textrahmen nicht existiert.
   */
  private static XTextRange getVerfuegungspunkt1(XTextDocument doc)
  {
    XTextFrame frame = null;
    try
    {
      frame =
        UNO.XTextFrame(UNO.XTextFramesSupplier(doc).getTextFrames().getByName(
          FrameNameVerfuegungspunkt1));
    }
    catch (java.lang.Exception e)
    {}

    if (frame != null)
    {
      XTextCursor cursor = frame.getText().createTextCursorByRange(frame.getText());
      if (isVerfuegungspunkt(cursor)) return cursor;

      // Absatzformat WollMuxVerfuegungspunkt1 setzen wenn noch nicht gesetzt.
      UNO.setProperty(cursor, "ParaStyleName", ParaStyleNameVerfuegungspunkt1);
      return cursor;
    }
    else
      return null;
  }

  /**
   * Erzeugt einen String in der Form "i.<tab>Abdruck von I.[, II., ...][ und
   * <i-1>]", der passend zu einem Abdruck mit der Verfügungsnummer number angezeigt
   * werden soll.
   *
   * @param number
   *          Die Nummer des Verfügungspunktes des Abdrucks
   * @return String in der Form "Abdruck von I.[, II., ...][ und <i-1>]" oder
   *         AbdruckDefaultStr, wenn der Verfügungspunkt bei i==0 und i==1 keinen
   *         Vorgänger besitzen kann.
   */
  private static String abdruckString(int number)
  {
    String str = romanNumber(number) + "\t" + copyName + " von " + romanNumber(1);
    for (int j = 2; j < (number - 1); ++j)
      str += ", " + romanNumber(j);
    if (number >= 3)
      str += " und " + romanNumber(number - 1);
    return str;
  }

  /**
   * Liefert die römische Zahl zum übgebenen integer Wert i. Die römischen Zahlen
   * werden dabei aus dem begrenzten Array romanNumbers ausgelesen. Ist i-1 kein
   * gültiger Index des Arrays, so sieht der Rückgabewert wie folgt aus
   * "<dezimalzahl(i)>.". Hier kann bei Notwendigkeit natürlich auch ein
   * Berechnungsschema für römische Zahlen implementiert werden, was für die
   * Sachleitenden Verfügungen vermutlich aber nicht erforderlich sein wird.
   *
   * @param i
   *          Die Zahl, zu der eine römische Zahl geliefert werden soll.
   * @return Die römische Zahl, oder "<dezimalzahl(i)>, wenn i-1 nicht in den
   *         Arraygrenzen von romanNumbers.
   */
  private static String romanNumber(int i)
  {
    String number = "" + i + ".";
    if (i > 0 && i <= romanNumbers.length)
      number = romanNumbers[i - 1];
    return number;
  }

  /**
   * Erzeugt einen Vector mit Elementen vom Typ Verfuegungspunkt, der dem Druckdialog
   * übergeben werden kann und alle für den Druckdialog notwendigen Informationen
   * enthält.
   *
   * @param doc
   *          Das zu scannende Dokument
   * @return Vector of Verfuegungspunkt, der für jeden Verfuegungspunkt im Dokument
   *         doc einen Eintrag enthält.
   */
  private static List<Verfuegungspunkt> scanVerfuegungspunkte(XTextDocument doc)
  {
    List<Verfuegungspunkt> verfuegungspunkte = new ArrayList<Verfuegungspunkt>();

    // Verfügungspunkt1 hinzufügen wenn verfügbar.
    XTextRange punkt1 = getVerfuegungspunkt1(doc);
    if (punkt1 != null)
    {
      Verfuegungspunkt original =
        new Verfuegungspunkt(L.m(romanNumbers[0] + " Original"));
      original.addZuleitungszeile(L.m("Empfänger siehe Empfängerfeld"));
      verfuegungspunkte.add(original);
    }

    Verfuegungspunkt currentVerfpunkt = null;

    // Paragraphen des Texts enumerieren und Verfügungspunkte erstellen. Die
    // Enumeration erfolgt über einen ParagraphCursor, da sich
    // dieser stabiler verhält als das Durchgehen der XEnumerationAccess, bei
    // der es zu OOo-Abstürzen kam. Leider konnte ich das Problem nicht exakt
    // genug isolieren um ein entsprechende Ticket bei OOo dazu aufmachen zu
    // können, da der Absturz nur sporadisch auftrat.
    XParagraphCursor cursor =
      UNO.XParagraphCursor(doc.getText().createTextCursorByRange(
        doc.getText().getStart()));

    if (cursor != null)
    {
      do
      {
        // ganzen Paragraphen markieren
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor))
        {
          String heading = cursor.getString();
          currentVerfpunkt = new Verfuegungspunkt(heading);
          currentVerfpunkt.setMinNumberOfCopies(1);
          verfuegungspunkte.add(currentVerfpunkt);
        }

        // Zuleitungszeilen hinzufügen (auch wenn der Paragraph Verfügungspunkt
        // und Zuleitungszeile zugleich ist)
        if ((isZuleitungszeile(cursor) || isVerfuegungspunktMitZuleitung(cursor))
          && currentVerfpunkt != null
          // ausgeblendete Zeilen ignorieren
          && Boolean.FALSE.equals(UNO.getProperty(cursor, "CharHidden")))
        {
          String zuleit = cursor.getString();
          // nicht leere Zuleitungszeilen zum Verfügungspunkt hinzufügen.
          if (!(zuleit.length() == 0))
            currentVerfpunkt.addZuleitungszeile(zuleit);
        }
      } while (cursor.gotoNextParagraph(false));
    }

    return verfuegungspunkte;
  }

  /**
   * Zeigt den Druckdialog für Sachleitende Verfügungen an und liefert die dort
   * getroffenen Einstellungen als Liste von VerfuegungspunktInfo-Objekten zurück,
   * oder null, wenn Fehler auftraten oder der Druckvorgang abgebrochen wurde.
   *
   * @param doc
   *          Das Dokument, aus dem die anzuzeigenden Verfügungspunkte ausgelesen
   *          werden.
   */
  public static List<VerfuegungspunktInfo> callPrintDialog(XTextDocument doc)
  {
    //JGM: Update der Dokumentenstruktur (Kommandos und TextSections)
    DocumentManager.getTextDocumentController(doc).updateDocumentCommands();
    for (Verfuegungspunkt vp : scanVerfuegungspunkte(doc))
    {
      String text = L.m("Verfügungspunkt '%1'", vp.getHeading());
      for (String zuleit : vp.getZuleitungszeilen())
      {
        text += "\n  --> '" + zuleit + "'";
      }
      Logger.debug2(text);
    }

    // Beschreibung des Druckdialogs auslesen.
    ConfigThingy conf = WollMuxFiles.getWollmuxConf();
    ConfigThingy svdds =
      conf.query("Dialoge").query("SachleitendeVerfuegungenDruckdialog");
    ConfigThingy printDialogConf = null;
    try
    {
      printDialogConf = svdds.getLastChild();
    }
    catch (NodeNotFoundException e)
    {
      Logger.error(
        L.m("Fehlende Dialogbeschreibung für den Dialog 'SachleitendeVerfuegungenDruckdialog'."),
        e);
      return null;
    }

    // Dialog ausführen und Rückgabewert zurückliefern.
    try
    {
      SyncActionListener s = new SyncActionListener();
      new SachleitendeVerfuegungenDruckdialog(printDialogConf, scanVerfuegungspunkte(doc), s);
      ActionEvent result = s.synchronize();
      String cmd = result.getActionCommand();
      SachleitendeVerfuegungenDruckdialog slvd =
        (SachleitendeVerfuegungenDruckdialog) result.getSource();
      if (SachleitendeVerfuegungenDruckdialog.CMD_SUBMIT.equals(cmd) && slvd != null)
      {
        if (slvd.getPrintOrderAsc())
        {
          return slvd.getCurrentSettings();
        }
        else
        {
          // sonst in umgekehrter Reihenfolge
          List<VerfuegungspunktInfo> lList = new ArrayList<VerfuegungspunktInfo>();
          ListIterator<VerfuegungspunktInfo> lIt = slvd.getCurrentSettings().listIterator(slvd.getCurrentSettings().size());
          while (lIt.hasPrevious())
          {
            lList.add(lIt.previous());
          }
          return lList;
        }
      }
      return null;
    }
    catch (ConfigurationErrorException e)
    {
      Logger.error(e);
      return null;
    }
  }

  /**
   * Druckt den Verfügungpunkt Nummer verfPunkt aus dem Dokument aus, das im
   * XPrintModel pmod hinterlegt ist.
   *
   * @param pmod
   *          Das PrintModel zu diesem Druckvorgang.
   * @param verfPunkt
   *          Die Nummer des auszuduruckenden Verfügungspunktes, wobei alle folgenden
   *          Verfügungspunkte ausgeblendet werden.
   * @param isDraft
   *          wenn isDraft==true, werden alle draftOnly-Blöcke eingeblendet,
   *          ansonsten werden sie ausgeblendet.
   * @param isOriginal
   *          wenn isOriginal, wird die Ziffer des Verfügungspunktes I ausgeblendet
   *          und alle notInOriginal-Blöcke ebenso. Andernfalls sind Ziffer und
   *          notInOriginal-Blöcke eingeblendet.
   * @param copyCount
   *          enthält die Anzahl der Kopien, die von diesem Verfügungspunkt erstellt
   *          werden sollen.
   * @throws PrintFailedException
   */
  public static void printVerfuegungspunkt(XPrintModel pmod, int verfPunkt,
      boolean isDraft, boolean isOriginal, short copyCount)
  {
    XTextDocument doc = pmod.getTextDocument();

    // Steht der viewCursor in einem Bereich, der im folgenden ausgeblendet
    // wird, dann wird der ViewCursor in einen sichtbaren Bereich verschoben. Um
    // den viewCursor wieder herstellen zu können, wird er hier gesichert und
    // später wieder hergestellt.
    XTextCursor vc = null;
    XTextCursor oldViewCursor = null;
    XTextViewCursorSupplier suppl =
      UNO.XTextViewCursorSupplier(UNO.XModel(pmod.getTextDocument()).getCurrentController());
    if (suppl != null)
      vc = suppl.getViewCursor();
    if (vc != null)
      oldViewCursor = vc.getText().createTextCursorByRange(vc);

    // Zähler für Verfuegungspunktnummer auf 1 initialisieren, wenn ein
    // Verfuegungspunkt1 vorhanden ist.
    XTextRange punkt1 = getVerfuegungspunkt1(doc);
    int count = 0;
    if (punkt1 != null)
      count++;

    // Auszublendenden Bereich festlegen:
    XTextRange setInvisibleRange = null;
    XParagraphCursor cursor =
      UNO.XParagraphCursor(doc.getText().createTextCursorByRange(
        doc.getText().getStart()));
    if (cursor != null)
    {
      do
      {
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor))
        {
          // Punkt1 merken
          if (punkt1 == null) punkt1 = cursor.getText().createTextCursorByRange(cursor);

          count++;

          if (count == (verfPunkt + 1))
          {
            cursor.collapseToStart();
            cursor.gotoRange(cursor.getText().getEnd(), true);
            setInvisibleRange = cursor;
          }
        }
      } while (setInvisibleRange == null && cursor.gotoNextParagraph(false));
    }

    // Prüfen, welche Textsections im ausgeblendeten Bereich liegen und diese
    // ebenfalls ausblenden (und den alten Stand merken):
    List<XTextSection> hidingSections =
      getSectionsFromPosition(pmod.getTextDocument(), setInvisibleRange);
    HashMap<XTextSection, Boolean> sectionOldState =
      new HashMap<XTextSection, Boolean>();
    for (XTextSection section : hidingSections)
      try
      {
        boolean oldState =
          AnyConverter.toBoolean(UNO.getProperty(section, "IsVisible"));
        sectionOldState.put(section, oldState);
        UNO.setProperty(section, "IsVisible", Boolean.FALSE);
      }
      catch (IllegalArgumentException x)
      {}

    // ensprechende Verfügungspunkte ausblenden
    if (setInvisibleRange != null)
      UNO.hideTextRange(setInvisibleRange, true);

    // Ein/Ausblenden Druckblöcke (z.B. draftOnly):
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_DRAFT_ONLY, isDraft, false);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_NOT_IN_ORIGINAL, !isOriginal, false);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_ORIGINAL_ONLY, isOriginal, false);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_ALL_VERSIONS, true, false);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_COPY_ONLY, !isDraft && !isOriginal, false);

    // Ein/Ausblenden der Sichtbarkeitsgruppen:
    pmod.setGroupVisible(GROUP_ID_SLV_DRAFT_ONLY, isDraft);
    pmod.setGroupVisible(GROUP_ID_SLV_NOT_IN_ORIGINAL, !isOriginal);
    pmod.setGroupVisible(GROUP_ID_SLV_ORIGINAL_ONLY, isOriginal);
    pmod.setGroupVisible(GROUP_ID_SLV_ALL_VERSIONS, true);
    pmod.setGroupVisible(GROUP_ID_SLV_COPY_ONLY, !isDraft && !isOriginal);

    // Ziffer von Punkt 1 ausblenden falls isOriginal
    XTextRange punkt1ZifferOnly = null;
    if (isOriginal && punkt1 != null)
    {
      punkt1ZifferOnly = getZifferOnly(punkt1, true);
      UNO.hideTextRange(punkt1ZifferOnly, true);
    }

    // -----------------------------------------------------------------------
    // Druck des Dokuments
    // -----------------------------------------------------------------------
    for (int j = 0; j < copyCount; ++j)
      pmod.printWithProps();

    // Ausblendung von Ziffer von Punkt 1 wieder aufheben
    if (punkt1ZifferOnly != null)
      UNO.hideTextRange(punkt1ZifferOnly, false);

    // Sichtbarkeitsgruppen wieder einblenden
    pmod.setGroupVisible(GROUP_ID_SLV_DRAFT_ONLY, true);
    pmod.setGroupVisible(GROUP_ID_SLV_NOT_IN_ORIGINAL, true);
    pmod.setGroupVisible(GROUP_ID_SLV_ORIGINAL_ONLY, true);
    pmod.setGroupVisible(GROUP_ID_SLV_ALL_VERSIONS, true);
    pmod.setGroupVisible(GROUP_ID_SLV_COPY_ONLY, true);

    // Alte Eigenschaften der Druckblöcke wieder herstellen:
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_DRAFT_ONLY, true, true);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_NOT_IN_ORIGINAL, true, true);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_ORIGINAL_ONLY, true, true);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_ALL_VERSIONS, true, true);
    pmod.setPrintBlocksProps(BLOCKNAME_SLV_COPY_ONLY, true, true);

    // ausgeblendete TextSections wieder einblenden
    for (XTextSection section : hidingSections)
    {
      Boolean oldState = sectionOldState.get(section);
      if (oldState != null)
        UNO.setProperty(section, "IsVisible", oldState);
    }

    // Verfügungspunkte wieder einblenden:
    if (setInvisibleRange != null)
      UNO.hideTextRange(setInvisibleRange, false);

    // ViewCursor wieder herstellen:
    if (vc != null && oldViewCursor != null)
      vc.gotoRange(oldViewCursor, false);
  }

  /**
   * Diese Methode liefert in eine Liste aller Textsections aus doc, deren Anker an
   * der selben Position oder hinter der Position der TextRange pos liegt.
   *
   * @param doc
   *          Textdokument in dem alle enthaltenen Textsections geprüft werden.
   * @param pos
   *          Position, ab der die TextSections in den Vector aufgenommen werden
   *          sollen.
   * @return eine Liste aller TextSections, die an oder nach pos starten oder eine
   *         leere Liste, wenn es Fehler gab oder keine Textsection gefunden wurde.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static List<XTextSection> getSectionsFromPosition(XTextDocument doc,
      XTextRange pos)
  {
    Vector<XTextSection> v = new Vector<XTextSection>();
    if (pos == null)
      return v;
    XTextRangeCompare comp = UNO.XTextRangeCompare(pos.getText());
    if (comp == null)
      return v;
    XTextSectionsSupplier suppl = UNO.XTextSectionsSupplier(doc);
    if (suppl == null)
      return v;

    XNameAccess sections = suppl.getTextSections();
    String[] names = sections.getElementNames();
    for (int i = 0; i < names.length; i++)
    {
      XTextSection section = null;
      try
      {
        section = UNO.XTextSection(sections.getByName(names[i]));
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }

      if (section != null)
      {
        try
        {
          int diff = comp.compareRegionStarts(pos, section.getAnchor());
          if (diff >= 0)
            v.add(section);
        }
        catch (IllegalArgumentException e)
        {
          // kein Fehler, da die Exceptions immer fliegt, wenn die ranges in
          // unterschiedlichen Textobjekten liegen.
        }
      }
    }
    return v;
  }

  /**
   * Erzeugt im Dokument doc ein neues Absatzformat (=ParagraphStyle) mit dem Namen
   * name und dem ParentStyle parentStyleName und liefert das neu erzeugte
   * Absatzformat zurück oder null, falls das Erzeugen nicht funktionierte.
   *
   * @param doc
   *          das Dokument in dem das Absatzformat name erzeugt werden soll.
   * @param name
   *          der Name des zu erzeugenden Absatzformates
   * @param parentStyleName
   *          Name des Vorgänger-Styles von dem die Eigenschaften dieses Styles
   *          abgeleitet werden soll oder null, wenn kein Vorgänger gesetzt werden
   *          soll (in diesem Fall wird automatisch "Standard" verwendet)
   * @return das neu erzeugte Absatzformat oder null, falls das Absatzformat nicht
   *         erzeugt werden konnte.
   */
  private static XStyle createParagraphStyle(XTextDocument doc, String name,
      String parentStyleName)
  {
    XNameContainer pss = getStyleContainer(doc, PARAGRAPH_STYLES);
    XStyle style = null;
    try
    {
      style =
        UNO.XStyle(UNO.XMultiServiceFactory(doc).createInstance(
          "com.sun.star.style.ParagraphStyle"));
      pss.insertByName(name, style);
      if (style != null && parentStyleName != null)
        style.setParentStyle(parentStyleName);
      return UNO.XStyle(pss.getByName(name));
    }
    catch (Exception e)
    {}
    return null;
  }

  /**
   * Liefert den Styles vom Typ type des Dokuments doc.
   *
   * @param doc
   *          Das Dokument, dessen StyleContainer zurückgeliefert werden soll.
   * @param type
   *          kann z.B. CHARACTER_STYLE oder PARAGRAPH_STYLE sein.
   * @return Liefert den Container der Styles vom Typ type des Dokuments doc oder
   *         null, falls der Container nicht bestimmt werden konnte.
   */
  private static XNameContainer getStyleContainer(XTextDocument doc,
      String containerName)
  {
    try
    {
      return UNO.XNameContainer(UNO.XNameAccess(
        UNO.XStyleFamiliesSupplier(doc).getStyleFamilies()).getByName(containerName));
    }
    catch (java.lang.Exception e)
    {}
    return null;
  }

  /**
   * Wertet die wollmux,conf-Direktive ABDRUCK_NAME aus und setzt diese entsprechend
   * in der OOo Erweiterung. Ist kein ABDRUCK_NAME gegeben, so wird "Abdruck" als
   * Standardwert gesetzt.
   *
   * @return Kopiebezeichner als String
   *
   * @author Jan Gerrit Möltgen (JanGerrit@burg-borgholz.de), Christoph Lutz
   */
  private static String getCopyName()
  {
    String name = L.m("Abdruck");
    ConfigThingy conf = WollMuxFiles.getWollmuxConf();
    ConfigThingy nan = conf.query("SachleitendeVerfuegungen").query("ABDRUCK_NAME");
    try
    {
      name = L.m(nan.getLastChild().toString());
      Logger.debug(L.m("Verwende ABDRUCK_NAME '%1'", name));
    }
    catch (NodeNotFoundException x)
    {}
    return name;
  }

  /**
   * Wertet die wollmux,conf-Direktive NUMBERS aus und setzt diese entsprechend in
   * der OOo Erweiterung. Ist kein Wert gegeben, so werden römische Ziffern
   * verwendet.
   *
   * @return Ziffern als String array
   *
   * @author Jan Gerrit Möltgen (JanGerrit@burg-borgholz.de), Christoph Lutz
   */
  private static String[] getNumbers()
  {
    String numbers = "roman";
    ConfigThingy conf = WollMuxFiles.getWollmuxConf();
    ConfigThingy nan = conf.query("SachleitendeVerfuegungen").query("NUMBERS");
    try
    {
      numbers = nan.getLastChild().toString();
      Logger.debug(L.m("Verwende Zahlenformat '%1' aus Attribut NUMBERS.", numbers));
    }
    catch (NodeNotFoundException x)
    {}

    // if arabic is selected set numberArray to arabic numbers
    if ("arabic".equalsIgnoreCase(numbers))
    {
      return new String[] {
        "1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.", "11.", "12.",
        "13.", "14.", "15." };
    }
    else
    {
      // roman is default
      if (!"roman".equalsIgnoreCase(numbers))
        Logger.error(L.m(
          "Ungültiger Wert '%1' für Attribut NUMBERS (zulässig: 'roman' und 'arabic'). Verwende 'roman' statt dessen.",
          numbers));
      return new String[] {
        "I.", "II.", "III.", "IV.", "V.", "VI.", "VII.", "VIII.", "IX.", "X.",
        "XI.", "XII.", "XIII.", "XIV.", "XV." };
    }
  }

  /**
   * Sorgt ohne Verlust von sichtbaren Formatierungseigenschaften dafür, dass alle
   * Formatvorlagen des Dokuments doc, die in Sachleitenden Verfügungen eine
   * besondere Rolle spielen, zukünftig nicht mehr vom WollMux interpretiert werden.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void deMuxSLVStyles(XTextDocument doc)
  {
    if (doc == null)
      return;

    HashMap<String, String> mapOldNameToNewName = new HashMap<String, String>();
    XParagraphCursor cursor =
      UNO.XParagraphCursor(doc.getText().createTextCursorByRange(
        doc.getText().getStart()));
    if (cursor != null)
      do
      {
        cursor.gotoEndOfParagraph(true);

        if (isVerfuegungspunkt(cursor) || isZuleitungszeile(cursor)
          || isVerfuegungspunktMitZuleitung(cursor))
        {
          String oldName = "";
          try
          {
            oldName =
              AnyConverter.toString(UNO.getProperty(cursor, "ParaStyleName"));
          }
          catch (IllegalArgumentException e)
          {}

          // Einmalig Style NO<number>_<oldName> erzeugen, der von <oldName> erbt.
          String newName = mapOldNameToNewName.get(oldName);
          XStyle newStyle = null;
          if (newName == null)
          {
            do
            {
              newName = "NO" + new Random().nextInt(1000) + "_" + oldName;
              mapOldNameToNewName.put(oldName, newName);
              newStyle = createParagraphStyle(doc, newName, oldName);
            } while (newStyle == null);
          }

          if (oldName != null)
          {
            // Das Setzen von ParaStyleName setzt mindestens CharHidden des cursors
            // auf Default zurück. Daher muss der bisherige Stand von CharHidden nach
            // dem Setzen wieder hergestellt werden:
            Object hidden = UNO.getProperty(cursor, "CharHidden");
            UNO.setProperty(cursor, "ParaStyleName", newName);
            UNO.setProperty(cursor, "CharHidden", hidden);
          }
        }
      } while (cursor.gotoNextParagraph(false));

    // Extra-Frame für Verfügungspunkt1 umbenennen
    try
    {
      XNamed frame =
        UNO.XNamed(UNO.XTextFramesSupplier(doc).getTextFrames().getByName(
          FrameNameVerfuegungspunkt1));
      if (frame != null)
        frame.setName("NON_" + FrameNameVerfuegungspunkt1);
    }
    catch (java.lang.Exception e)
    {}

  }
}

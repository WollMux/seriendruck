/*
 * Dateiname: WollMuxEventHandler.java
 * Projekt  : WollMux
 * Funktion : Ermöglicht die Einstellung neuer WollMuxEvents in die EventQueue.
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
 * 24.10.2005 | LUT | Erstellung als EventHandler.java
 * 01.12.2005 | BNK | +on_unload() das die Toolbar neu erzeugt (böser Hack zum 
 *                  | Beheben des Seitenansicht-Toolbar-Verschwindibus-Problems)
 *                  | Ausgabe des hashCode()s in den Debug-Meldungen, um Events 
 *                  | Objekten zuordnen zu können beim Lesen des Logfiles
 * 27.03.2005 | LUT | neues Kommando openDocument
 * 21.04.2006 | LUT | +ConfigurationErrorException statt NodeNotFoundException bei
 *                    fehlendem URL-Attribut in Textfragmenten
 * 06.06.2006 | LUT | + Ablösung der Event-Klasse durch saubere Objektstruktur
 *                    + Überarbeitung vieler Fehlermeldungen
 *                    + Zeilenumbrüche in showInfoModal, damit keine unlesbaren
 *                      Fehlermeldungen mehr ausgegeben werden.
 * 16.12.2009 | ERT | Cast XTextContent-Interface entfernt
 * 03.03.2010 | ERT | getBookmarkNamesStartingWith nach UnoHelper TextDocument verschoben
 * 03.03.2010 | ERT | Verhindern von Überlagern von insertFrag-Bookmarks
 * 23.03.2010 | ERT | [R59480] Meldung beim Ausführen von "Textbausteinverweis einfügen"
 * 02.06.2010 | BED | +handleSaveTempAndOpenExt
 * 08.05.2012 | jub | fakeSymLink behandlung eingebaut: auflösung und test der FRAG_IDs berücksichtigt
 *                    auch die möglichkeit, dass im conifg file auf einen fake SymLink verwiesen wird.
 * 11.12.2012 | jub | fakeSymLinks werden doch nicht gebraucht; wieder aus dem code entfernt                   
 *
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.event;

import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import com.sun.star.beans.PropertyValue;
import com.sun.star.document.XEventListener;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager.TextDocumentInfo;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnAddDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCloseTextDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCollectNonWollMuxFormFieldsViaPrintModel;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnExecutePrintFunction;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnHandleMailMergeNewReturned;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnInitialize;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnNotifyDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPrint;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRegisterDispatchInterceptor;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRemoveDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSeriendruck;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetFormValue;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetPrintBlocksPropsViaPrintModel;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetVisibleState;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextDocumentClosed;
import de.muenchen.allg.itd51.wollmux.event.handlers.WollMuxEvent;

/**
 * Ermöglicht die Einstellung neuer WollMuxEvents in die EventQueue.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class WollMuxEventHandler
{
  /**
   * Name des OnWollMuxProcessingFinished-Events.
   */
  public static final String ON_WOLLMUX_PROCESSING_FINISHED =
    "OnWollMuxProcessingFinished";

  /**
   * Mit dieser Methode ist es möglich die Entgegennahme von Events zu blockieren.
   * Alle eingehenden Events werden ignoriert, wenn accept auf false gesetzt ist und
   * entgegengenommen, wenn accept auf true gesetzt ist.
   * 
   * @param accept
   */
  public static void setAcceptEvents(boolean accept)
  {
    EventProcessor.getInstance().setAcceptEvents(accept);
  }

  /**
   * Der EventProcessor sorgt für eine synchronisierte Verarbeitung aller
   * Wollmux-Events. Alle Events werden in eine synchronisierte eventQueue
   * hineingepackt und von einem einzigen eventProcessingThread sequentiell
   * abgearbeitet.
   * 
   * @author lut
   */
  public static class EventProcessor
  {
    /**
     * Gibt an, ob der EventProcessor überhaupt events entgegennimmt. Ist
     * acceptEvents=false, werden alle Events ignoriert.
     */
    private boolean acceptEvents = false;

    private List<WollMuxEvent> eventQueue = new LinkedList<WollMuxEvent>();

    private static EventProcessor singletonInstance;

    private static Thread eventProcessorThread;

    private static EventProcessor getInstance()
    {
      if (singletonInstance == null)
      {
        singletonInstance = new EventProcessor();
        singletonInstance.start();
      }
      return singletonInstance;
    }

    /**
     * Mit dieser Methode ist es möglich die Entgegennahme von Events zu blockieren.
     * Alle eingehenden Events werden ignoriert, wenn accept auf false gesetzt ist
     * und entgegengenommen, wenn accept auf true gesetzt ist.
     * 
     * @param accept
     */
    private void setAcceptEvents(boolean accept)
    {
      acceptEvents = accept;
      if (accept)
        Logger.debug(L.m("EventProcessor: akzeptiere neue Events."));
      else
        Logger.debug(L.m("EventProcessor: blockiere Entgegennahme von Events!"));
    }

    private EventProcessor()
    {
      // starte den eventProcessorThread
      eventProcessorThread = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          Logger.debug(L.m("Starte EventProcessor-Thread"));
          try
          {
            while (true)
            {
              WollMuxEvent event;
              synchronized (eventQueue)
              {
                while (eventQueue.isEmpty())
                  eventQueue.wait();
                event = eventQueue.remove(0);
              }

              event.process();
            }
          }
          catch (InterruptedException e)
          {
            Logger.error(L.m("EventProcessor-Thread wurde unterbrochen:"));
            Logger.error(e);
          }
          Logger.debug(L.m("Beende EventProcessor-Thread"));
        }
      });
    }

    /**
     * Startet den {@link #eventProcessorThread}.
     * 
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    private void start()
    {
      eventProcessorThread.start();
    }

    /**
     * Diese Methode fügt ein Event an die eventQueue an wenn der WollMux erfolgreich
     * initialisiert wurde und damit events akzeptieren darf. Anschliessend weckt sie
     * den EventProcessor-Thread.
     * 
     * @param event
     */
    private void addEvent(WollMuxEvent event)
    {
      if (acceptEvents) synchronized (eventQueue)
      {
        eventQueue.add(event);
        eventQueue.notifyAll();
      }
    }
  }

  /**
   * Stellt das WollMuxEvent event in die EventQueue des EventProcessors.
   * 
   * @param event
   */
  private static void handle(WollMuxEvent event)
  {
    EventProcessor.getInstance().addEvent(event);
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent zum Registrieren des übergebenen XEventListeners
   * und wird vom WollMux-Service aufgerufen.
   * 
   * @param listener
   *          der zu registrierende XEventListener.
   */
  public static void handleAddDocumentEventListener(XEventListener listener)
  {
    handle(new OnAddDocumentEventListener(listener));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das den übergebenen XEventListener zu
   * deregistriert.
   * 
   * @param listener
   *          der zu deregistrierende XEventListener
   */
  public static void handleRemoveDocumentEventListener(XEventListener listener)
  {
    handle(new OnRemoveDocumentEventListener(listener));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das aufgerufen wird, wenn ein FormularMax4000
   * beendet wird und die entsprechenden internen Referenzen gelöscht werden können.
   * 
   * Dieses Event wird vom EventProcessor geworfen, wenn der FormularMax zurückkehrt.
   */
  public static void handleMailMergeNewReturned(TextDocumentController documentController)
  {
    handle(new OnHandleMailMergeNewReturned(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das Auskunft darüber gibt, dass ein TextDokument
   * geschlossen wurde und damit auch das TextDocumentModel disposed werden soll.
   * 
   * Dieses Event wird ausgelöst, wenn ein TextDokument geschlossen wird.
   * 
   * @param docInfo
   *          ein {@link DocumentManager.Info} Objekt, an dem das TextDocumentModel
   *          dranhängt des Dokuments, das geschlossen wurde. ACHTUNG! docInfo hat
   *          nicht zwingend ein TextDocumentModel. Es muss
   *          {@link DocumentManager.Info#hasTextDocumentModel()} verwendet werden.
   * 
   * 
   *          ACHTUNG! ACHTUNG! Die Implementierung wurde extra so gewählt, dass hier
   *          ein DocumentManager.Info anstatt direkt eines TextDocumentModel
   *          übergeben wird. Es kam nämlich bei einem Dokument, das schnell geöffnet
   *          und gleich wieder geschlossen wurde zu folgendem Deadlock:
   * 
   *          {@link OnProcessTextDocument} =>
   *          {@link de.muenchen.allg.itd51.wollmux.document.DocumentManager.TextDocumentInfo#getTextDocumentController()}
   *          => {@link TextDocumentModel#TextDocumentModel(XTextDocument)} =>
   *          {@link DispatchProviderAndInterceptor#registerDocumentDispatchInterceptor(XFrame)}
   *          => OOo Proxy =>
   *          {@link GlobalEventListener#notifyEvent(com.sun.star.document.EventObject)}
   *          ("OnUnload") =>
   *          {@link de.muenchen.allg.itd51.wollmux.document.DocumentManager.TextDocumentInfo#hasTextDocumentModel()}
   * 
   *          Da {@link TextDocumentInfo} synchronized ist kam es zum Deadlock.
   * 
   */
  public static void handleTextDocumentClosed(DocumentManager.Info docInfo)
  {
    handle(new OnTextDocumentClosed(docInfo));
  }

  

  // *******************************************************************************************

  

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, welches dafür sorgt, dass alle
   * Sichtbarkeitselemente (Dokumentkommandos oder Bereiche mit Namensanhang 'GROUPS
   * ...') im übergebenen Dokument, die einer bestimmten Gruppe groupId zugehören
   * ein- oder ausgeblendet werden.
   * 
   * Dieses Event wird (derzeit) vom FormModelImpl ausgelöst, wenn in der
   * Formular-GUI bestimmte Text-Teile des übergebenen Dokuments ein- oder
   * ausgeblendet werden sollen. Auch das PrintModel verwendet dieses Event, wenn
   * XPrintModel.setGroupVisible() aufgerufen wurde.
   * 
   * @param documentController
   *          Das TextDocumentModel, welches die Sichtbarkeitselemente enthält.
   * @param groupId
   *          Die GROUP (ID) der ein/auszublendenden Gruppe.
   * @param visible
   *          Der neue Sichtbarkeitsstatus (true=sichtbar, false=ausgeblendet)
   * @param listener
   *          Der listener, der nach Durchführung des Events benachrichtigt wird
   *          (kann auch null sein, dann gibt's keine Nachricht).
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void handleSetVisibleState(TextDocumentController documentController, String groupId,
      boolean visible, ActionListener listener)
  {
    handle(new OnSetVisibleState(documentController, groupId, visible, listener));
  }

  
  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das das übergebene Dokument schließt.
   * 
   * @param documentController
   *          Das zu schließende TextDocumentModel.
   */
  public static void handleCloseTextDocument(TextDocumentController documentController)
  {
    handle(new OnCloseTextDocument(documentController));
  }


  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das ggf. notwendige interaktive
   * Initialisierungen vornimmt.
   */
  public static void handleInitialize()
  {
    handle(new OnInitialize());
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent zum Registrieren eines (frischen)
   * {@link DispatchProviderAndInterceptor} auf frame.
   * 
   * @param frame
   *          der {@link XFrame} auf den der {@link DispatchProviderAndInterceptor}
   *          registriert werden soll.
   */
  public static void handleRegisterDispatchInterceptor(TextDocumentController documentController)
  {
      handle(new OnRegisterDispatchInterceptor(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Über dieses Event werden alle registrierten DocumentEventListener (falls
   * listener==null) oder ein bestimmter registrierter DocumentEventListener (falls
   * listener != null) (XEventListener-Objekte) über Statusänderungen der
   * Dokumentbearbeitung informiert
   * 
   * @param listener
   *          der zu benachrichtigende XEventListener. Falls null werden alle
   *          registrierten Listener benachrichtig. listener wird auf jeden Fall nur
   *          benachrichtigt, wenn er zur Zeit der Abarbeitung des Events noch
   *          registriert ist.
   * @param eventName
   *          Name des Events
   * @param source
   *          Das von der Statusänderung betroffene Dokument (üblicherweise eine
   *          XComponent)
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void handleNotifyDocumentEventListener(XEventListener listener,
      String eventName, Object source)
  {
    handle(new OnNotifyDocumentEventListener(listener, eventName, source));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent das signaisiert, dass die Druckfunktion
   * aufgerufen werden soll, die im TextDocumentModel model aktuell definiert ist.
   * Die Methode erwartet, dass vor dem Aufruf geprüft wurde, ob model eine
   * Druckfunktion definiert. Ist dennoch keine Druckfunktion definiert, so erscheint
   * eine Fehlermeldung im Log.
   * 
   * Das Event wird ausgelöst, wenn der registrierte WollMuxDispatchInterceptor eines
   * Dokuments eine entsprechende Nachricht bekommt.
   */
  public static void handleExecutePrintFunctions(TextDocumentController documentController)
  {
    handle(new OnExecutePrintFunction(documentController));
  }

  

  // *******************************************************************************************

  /**
   * Diese Methode erzeugt ein neues WollMuxEvent, mit dem die Eigenschaften der
   * Druckblöcke (z.B. allVersions) gesetzt werden können.
   * 
   * Das Event dient als Hilfe für die Komfortdruckfunktionen und wird vom
   * XPrintModel aufgerufen und mit diesem synchronisiert.
   * 
   * @param blockName
   *          Der Blocktyp dessen Druckblöcke behandelt werden sollen.
   * @param visible
   *          Der Block wird sichtbar, wenn visible==true und unsichtbar, wenn
   *          visible==false.
   * @param showHighlightColor
   *          gibt an ob die Hintergrundfarbe angezeigt werden soll (gilt nur, wenn
   *          zu einem betroffenen Druckblock auch eine Hintergrundfarbe angegeben
   *          ist).
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void handleSetPrintBlocksPropsViaPrintModel(XTextDocument doc,
      String blockName, boolean visible, boolean showHighlightColor,
      ActionListener listener)
  {
    handle(new OnSetPrintBlocksPropsViaPrintModel(doc, blockName, visible,
      showHighlightColor, listener));
  }

  

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass im Textdokument doc das
   * Formularfeld mit der ID id auf den Wert value gesetzt wird. Ist das Dokument ein
   * Formulardokument (also mit einer angezeigten FormGUI), so wird die Änderung über
   * die FormGUI vorgenommen, die zugleich dafür sorgt, dass von id abhängige
   * Formularfelder mit angepasst werden. Besitzt das Dokument keine
   * Formularbeschreibung, so wird der Wert direkt gesetzt, ohne Äbhängigkeiten zu
   * beachten. Nach der erfolgreichen Ausführung aller notwendigen Anpassungen wird
   * der unlockActionListener benachrichtigt.
   * 
   * Das Event wird aus den Implementierungen von XPrintModel (siehe
   * TextDocumentModel) und XWollMuxDocument (siehe compo.WollMux) geworfen, wenn
   * dort die Methode setFormValue aufgerufen wird.
   * 
   * @param doc
   *          Das Dokument, in dem das Formularfeld mit der ID id neu gesetzt werden
   *          soll.
   * @param id
   *          Die ID des Formularfeldes, dessen Wert verändert werden soll. Ist die
   *          FormGUI aktiv, so werden auch alle von id abhängigen Formularwerte neu
   *          gesetzt.
   * @param value
   *          Der neue Wert des Formularfeldes id
   * @param unlockActionListener
   *          Der unlockActionListener wird immer informiert, wenn alle notwendigen
   *          Anpassungen durchgeführt wurden.
   */
  public static void handleSetFormValue(XTextDocument doc, String id, String value,
      ActionListener unlockActionListener)
  {
    handle(new OnSetFormValue(doc, id, value, unlockActionListener));
  }

  

  // *******************************************************************************************

  /**
   * Sammelt alle Formularfelder des Dokuments model auf, die nicht von
   * WollMux-Kommandos umgeben sind, jedoch trotzdem vom WollMux verstanden und
   * befüllt werden (derzeit c,s,s,t,textfield,Database-Felder).
   * 
   * Das Event wird aus der Implementierung von XPrintModel (siehe TextDocumentModel)
   * geworfen, wenn dort die Methode collectNonWollMuxFormFields aufgerufen wird.
   * 
   * @param documentController
   * @param unlockActionListener
   *          Der unlockActionListener wird immer informiert, wenn alle notwendigen
   *          Anpassungen durchgeführt wurden.
   */
  public static void handleCollectNonWollMuxFormFieldsViaPrintModel(
      TextDocumentController documentController, ActionListener listener)
  {
    handle(new OnCollectNonWollMuxFormFieldsViaPrintModel(documentController, listener));
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass die neue
   * Seriendruckfunktion des WollMux gestartet werden soll.
   * 
   * Das Event wird über den DispatchHandler aufgerufen, wenn z.B. über das Menü
   * "Extras->Seriendruck (WollMux)" die dispatch-url wollmux:SeriendruckNeu
   * abgesetzt wurde.
   */
  public static void handleSeriendruck(TextDocumentController documentController,
      boolean useDocPrintFunctions)
  {
    handle(new OnSeriendruck(documentController, useDocPrintFunctions));
  }

  

  // *******************************************************************************************

  /**
   * Der Handler für das Drucken eines TextDokuments führt in Abhängigkeit von der
   * Existenz von Serienbrieffeldern und Druckfunktion die entsprechenden Aktionen
   * aus.
   * 
   * Das Event wird über den DispatchHandler aufgerufen, wenn z.B. über das Menü
   * "Datei->Drucken" oder über die Symbolleiste die dispatch-url .uno:Print bzw.
   * .uno:PrintDefault abgesetzt wurde.
   */
  public static void handlePrint(TextDocumentController documentController, XDispatch origDisp,
      com.sun.star.util.URL origUrl, PropertyValue[] origArgs)
  {
    handle(new OnPrint(documentController, origDisp, origUrl, origArgs));
  }

  

  // *******************************************************************************************

}

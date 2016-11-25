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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.document.XEventListener;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFrames;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.RuntimeException;
import com.sun.star.view.DocumentZoomType;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.ModalDialogs;
import de.muenchen.allg.itd51.wollmux.OpenExt;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.Workarounds;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager.TextDocumentInfo;
import de.muenchen.allg.itd51.wollmux.document.FrameController;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.print.PrintModels;

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
    private void addEvent(WollMuxEventHandler.WollMuxEvent event)
    {
      if (acceptEvents) synchronized (eventQueue)
      {
        eventQueue.add(event);
        eventQueue.notifyAll();
      }
    }
  }

  /**
   * Interface für die Events, die dieser EventHandler abarbeitet.
   */
  public interface WollMuxEvent
  {
    /**
     * Startet die Ausführung des Events und darf nur aus dem EventProcessor
     * aufgerufen werden.
     */
    public void process();
  }

  /**
   * Dient als Basisklasse für konkrete Event-Implementierungen.
   */
  private static class BasicEvent implements WollMuxEvent
  {

    private boolean[] lock = new boolean[] { true };

    /**
     * Diese Method ist für die Ausführung des Events zuständig. Nach der Bearbeitung
     * entscheidet der Rückgabewert ob unmittelbar die Bearbeitung des nächsten
     * Events gestartet werden soll oder ob das GUI blockiert werden soll bis das
     * nächste actionPerformed-Event beim EventProcessor eintrifft.
     */
    @Override
    public void process()
    {
      Logger.debug("Process WollMuxEvent " + this.toString());
      try
      {
        doit();
      }
      catch (WollMuxFehlerException e)
      {
        // hier wäre ein showNoConfigInfo möglich - ist aber nicht eindeutig auf no config zurückzuführen
        errorMessage(e);
      }
      // Notnagel für alle Runtime-Exceptions.
      catch (Throwable t)
      {
        Logger.error(t);
      }
    }

    /**
     * Logged die übergebene Fehlermeldung nach Logger.error() und erzeugt ein
     * Dialogfenster mit der Fehlernachricht.
     */
    protected void errorMessage(Throwable t)
    {
      Logger.error(t);
      String msg = "";
      if (t.getMessage() != null) msg += t.getMessage();
      Throwable c = t.getCause();
      /*
       * Bei RuntimeExceptions keine Benutzersichtbare Meldung, weil
       * 
       * 1. der Benutzer damit eh nix anfangen kann
       * 
       * 2. dies typischerweise passiert, wenn der Benutzer das Dokument geschlossen
       * hat, bevor der WollMux fertig war. In diesem Fall will er nicht mit einer
       * Meldung belästigt werden.
       */
      if (c instanceof RuntimeException) return;

      if (c != null)
      {
        msg += "\n\n" + c;
      }
      ModalDialogs.showInfoModal(L.m("WollMux-Fehler"), msg);
    }

    /**
     * Jede abgeleitete Event-Klasse sollte die Methode doit redefinieren, in der die
     * eigentlich event-Bearbeitung erfolgt. Die Methode doit muss alle auftretenden
     * Exceptions selbst behandeln, Fehler die jedoch benutzersichtbar in einem
     * Dialog angezeigt werden sollen, können über eine WollMuxFehlerException nach
     * oben weitergereicht werden.
     */
    protected void doit() throws WollMuxFehlerException
    {};

    /**
     * Diese Methode kann am Ende einer doit()-Methode aufgerufen werden und versucht
     * die Absturzwahrscheinlichkeit von OOo/WollMux zu senken in dem es den
     * GarbageCollector der JavaVM triggert freien Speicher freizugeben. Durch
     * derartige Aufräumaktionen insbesondere nach der Bearbeitung von Events, die
     * viel mit Dokumenten/Cursorn/Uno-Objekten interagieren, wird die Stabilität des
     * WollMux spürbar gesteigert.
     * 
     * In der Vergangenheit gab es z.B. sporadische, nicht immer reproduzierbare
     * Abstürze von OOo, die vermutlich in einem fehlerhaften Speichermanagement in
     * der schwer zu durchschauenden Kette JVM->UNO-Proxies->OOo begründet waren.
     */
    protected void stabilize()
    {
      System.gc();
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName();
    }

    /**
     * Setzt den Enable-Status aller OOo-Fenster, die der desktop aktuell liefert auf
     * enabled. Über den Status kann gesteuert werden, ob das Fenster
     * Benutzerinteraktionen wie z.B. Mausklicks auf Menüpunkte oder Tastendrücke
     * verarbeitet. Die Verarbeitung findet nicht statt, wenn enabled==false gesetzt
     * ist, ansonsten schon.
     * 
     * @param enabled
     */
    static void enableAllOOoWindows(boolean enabled)
    {
      try
      {
        XFrames frames = UNO.XFramesSupplier(UNO.desktop).getFrames();
        for (int i = 0; i < frames.getCount(); i++)
        {
          try
          {
            XFrame frame = UNO.XFrame(frames.getByIndex(i));
            XWindow contWin = frame.getContainerWindow();
            if (contWin != null) contWin.setEnable(enabled);
          }
          catch (java.lang.Exception e)
          {
            Logger.error(e);
          }
        }
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
    }

    /**
     * Setzt einen lock, der in Verbindung mit setUnlock und der
     * waitForUnlock-Methode verwendet werden kann, um quasi Modalität für nicht
     * modale Dialoge zu realisieren und setzt alle OOo-Fenster auf enabled==false.
     * setLock() sollte stets vor dem Aufruf des nicht modalen Dialogs erfolgen, nach
     * dem Aufruf des nicht modalen Dialogs folgt der Aufruf der
     * waitForUnlock()-Methode. Der nicht modale Dialog erzeugt bei der Beendigung
     * ein ActionEvent, das dafür sorgt, dass setUnlock aufgerufen wird.
     */
    protected void setLock()
    {
      enableAllOOoWindows(false);
      synchronized (lock)
      {
        lock[0] = true;
      }
    }

    /**
     * Macht einen mit setLock() gesetzten Lock rückgängig, und setzt alle
     * OOo-Fenster auf enabled==true und bricht damit eine evtl. wartende
     * waitForUnlock()-Methode ab.
     */
    protected void setUnlock()
    {
      synchronized (lock)
      {
        lock[0] = false;
        lock.notifyAll();
      }
      enableAllOOoWindows(true);
    }

    /**
     * Wartet so lange, bis der vorher mit setLock() gesetzt lock mit der Methode
     * setUnlock() aufgehoben wird. So kann die quasi Modalität nicht modale Dialoge
     * zu realisiert werden. setLock() sollte stets vor dem Aufruf des nicht modalen
     * Dialogs erfolgen, nach dem Aufruf des nicht modalen Dialogs folgt der Aufruf
     * der waitForUnlock()-Methode. Der nicht modale Dialog erzeugt bei der
     * Beendigung ein ActionEvent, das dafür sorgt, dass setUnlock aufgerufen wird.
     */
    protected void waitForUnlock()
    {
      try
      {
        synchronized (lock)
        {
          while (lock[0] == true)
            lock.wait();
        }
      }
      catch (InterruptedException e)
      {}
    }

    /**
     * Dieser ActionListener kann nicht modalen Dialogen übergeben werden und sorgt
     * in Verbindung mit den Methoden setLock() und waitForUnlock() dafür, dass quasi
     * modale Dialoge realisiert werden können.
     */
    protected UnlockActionListener unlockActionListener = new UnlockActionListener();

    protected class UnlockActionListener implements ActionListener
    {
      public ActionEvent actionEvent = null;

      @Override
      public void actionPerformed(ActionEvent arg0)
      {
        actionEvent = arg0;
        setUnlock();
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

  private static class OnAddDocumentEventListener extends BasicEvent
  {
    private XEventListener listener;

    public OnAddDocumentEventListener(XEventListener listener)
    {
      this.listener = listener;
    }

    @Override
    protected void doit()
    {
      DocumentManager.getDocumentManager().addDocumentEventListener(listener);

      List<XComponent> processedDocuments = new Vector<XComponent>();
      DocumentManager.getDocumentManager().getProcessedDocuments(processedDocuments);

      for (XComponent compo : processedDocuments)
      {
        handleNotifyDocumentEventListener(listener, ON_WOLLMUX_PROCESSING_FINISHED,
          compo);
      }
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
    }
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

  private static class OnRemoveDocumentEventListener extends BasicEvent
  {
    private XEventListener listener;

    public OnRemoveDocumentEventListener(XEventListener listener)
    {
      this.listener = listener;
    }

    @Override
    protected void doit()
    {
      DocumentManager.getDocumentManager().removeDocumentEventListener(listener);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
    }
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

  private static class OnHandleMailMergeNewReturned extends BasicEvent
  {
    private TextDocumentController documentController;

    private OnHandleMailMergeNewReturned(TextDocumentController documentController)
    {
      this.documentController = documentController;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      DocumentManager.getDocumentManager().setCurrentMailMergeNew(documentController.getModel().doc, null);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + documentController.getModel().hashCode() + ")";
    }
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

  private static class OnTextDocumentClosed extends BasicEvent
  {
    private DocumentManager.Info docInfo;

    private OnTextDocumentClosed(DocumentManager.Info doc)
    {
      this.docInfo = doc;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      if (docInfo.hasTextDocumentModel()) DocumentManager.getDocumentManager().dispose(docInfo.getTextDocumentController().getModel().doc);
    }

    @Override
    public String toString()
    {
      String code = "unknown";
      if (docInfo.hasTextDocumentModel())
        code = "" + docInfo.getTextDocumentController().hashCode();
      return this.getClass().getSimpleName() + "(#" + code + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, welches dafür sorgt, dass alle Formularfelder
   * Dokument auf den neuen Wert gesetzt werden. Bei Formularfeldern mit
   * TRAFO-Funktion wird die Transformation entsprechend durchgeführt.
   * 
   * Dieses Event wird (derzeit) vom FormModelImpl ausgelöst, wenn in der
   * Formular-GUI der Wert des Formularfeldes fieldID geändert wurde und sorgt dafür,
   * dass die Wertänderung auf alle betroffenen Formularfelder im Dokument doc
   * übertragen werden.
   * 
   * @param idToFormValues
   *          Eine HashMap die unter dem Schlüssel fieldID den Vektor aller
   *          FormFields mit der ID fieldID liefert.
   * @param fieldId
   *          Die ID der Formularfelder, deren Werte angepasst werden sollen.
   * @param newValue
   *          Der neue untransformierte Wert des Formularfeldes.
   * @param funcLib
   *          Die Funktionsbibliothek, die zur Gewinnung der Trafo-Funktion verwendet
   *          werden soll.
   */
  public static void handleFormValueChanged(TextDocumentController documentController, String fieldId,
      String newValue)
  {
    handle(new OnFormValueChanged(documentController, fieldId, newValue));
  }

  private static class OnFormValueChanged extends BasicEvent
  {
    private String fieldId;

    private String newValue;

    private TextDocumentController documentController;

    public OnFormValueChanged(TextDocumentController documentController, String fieldId,
        String newValue)
    {
      this.fieldId = fieldId;
      this.newValue = newValue;
      this.documentController = documentController;
    }

    @Override
    protected void doit()
    {
      documentController.addFormFieldValue(fieldId, newValue);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + fieldId + "', '" + newValue
        + "')";
    }
  }

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

  private static class OnSetVisibleState extends BasicEvent
  {
    private String groupId;

    private boolean visible;

    private ActionListener listener;

    private TextDocumentController documentController;

    public OnSetVisibleState(TextDocumentController documentController, String groupId,
        boolean visible, ActionListener listener)
    {
      this.documentController = documentController;
      this.groupId = groupId;
      this.visible = visible;
      this.listener = listener;
    }

    @Override
    protected void doit()
    {
      documentController.setVisibleState(groupId, visible);
      if (listener != null) listener.actionPerformed(null);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "('" + groupId + "', " + visible
        + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das den ViewCursor des Dokuments auf das aktuell in der
   * Formular-GUI bearbeitete Formularfeld setzt.
   * 
   * Dieses Event wird (derzeit) vom FormModelImpl ausgelöst, wenn in der
   * Formular-GUI ein Formularfeld den Fokus bekommen hat und es sorgt dafür, dass
   * der View-Cursor des Dokuments das entsprechende FormField im Dokument anspringt.
   * 
   * @param idToFormValues
   *          Eine HashMap die unter dem Schlüssel fieldID den Vektor aller
   *          FormFields mit der ID fieldID liefert.
   * @param fieldId
   *          die ID des Formularfeldes das den Fokus bekommen soll. Besitzen mehrere
   *          Formularfelder diese ID, so wird bevorzugt das erste Formularfeld aus
   *          dem Vektor genommen, das keine Trafo enthält. Ansonsten wird das erste
   *          Formularfeld im Vektor verwendet.
   */
  public static void handleFocusFormField(TextDocumentController documentController, String fieldId)
  {
    handle(new OnFocusFormField(documentController, fieldId));
  }

  private static class OnFocusFormField extends BasicEvent
  {
    private String fieldId;
    private TextDocumentController documentController;

    public OnFocusFormField(TextDocumentController documentController, String fieldId)
    {
      this.documentController = documentController;
      this.fieldId = fieldId;
    }

    @Override
    protected void doit()
    {
      documentController.getModel().focusFormField(fieldId);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + documentController.getModel().doc + ", '" + fieldId
        + "')";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das die Position und Größe des übergebenen Dokument-Fensters
   * auf die vorgegebenen Werte setzt. ACHTUNG: Die Maßangaben beziehen sich auf die
   * linke obere Ecke des Fensterinhalts OHNE die Titelzeile und die
   * Fensterdekoration des Rahmens. Um die linke obere Ecke des gesamten Fensters
   * richtig zu setzen, müssen die Größenangaben des Randes der Fensterdekoration und
   * die Höhe der Titelzeile VOR dem Aufruf der Methode entsprechend eingerechnet
   * werden.
   * 
   * @param model
   *          Das XModel-Interface des Dokuments dessen Position/Größe gesetzt werden
   *          soll.
   * @param docX
   *          Die linke obere Ecke des Fensterinhalts X-Koordinate der Position in
   *          Pixel, gezählt von links oben.
   * @param docY
   *          Die Y-Koordinate der Position in Pixel, gezählt von links oben.
   * @param docWidth
   *          Die Größe des Dokuments auf der X-Achse in Pixel
   * @param docHeight
   *          Die Größe des Dokuments auf der Y-Achse in Pixel. Auch hier wird die
   *          Titelzeile des Rahmens nicht beachtet und muss vorher entsprechend
   *          eingerechnet werden.
   */
  public static void handleSetWindowPosSize(TextDocumentController documentController, int docX,
      int docY, int docWidth, int docHeight)
  {
    handle(new OnSetWindowPosSize(documentController, docX, docY, docWidth, docHeight));
  }

  /**
   * Dieses Event wird vom FormModelImpl ausgelöst, wenn die Formular-GUI die
   * Position und die Ausmasse des Dokuments verändert. Ruft direkt setWindowsPosSize
   * der UNO-API auf.
   * 
   * @author christoph.lutz
   */
  private static class OnSetWindowPosSize extends BasicEvent
  {
    private int docX, docY, docWidth, docHeight;

    private TextDocumentController documentController;

    public OnSetWindowPosSize(TextDocumentController documentController, int docX, int docY,
        int docWidth, int docHeight)
    {
      this.documentController = documentController;
      this.docX = docX;
      this.docY = docY;
      this.docWidth = docWidth;
      this.docHeight = docHeight;
    }

    @Override
    protected void doit()
    {
      documentController.getFrameController().setWindowPosSize(docX, docY, docWidth, docHeight);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + docX + ", " + docY + ", "
        + docWidth + ", " + docHeight + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das die Anzeige des übergebenen Dokuments auf sichtbar oder
   * unsichtbar schaltet. Dabei wird direkt die entsprechende Funktion der UNO-API
   * verwendet.
   * 
   * @param documentController
   *          Das XModel interface des dokuments, welches sichtbar oder unsichtbar
   *          geschaltet werden soll.
   * @param visible
   *          true, wenn das Dokument sichtbar geschaltet werden soll und false, wenn
   *          das Dokument unsichtbar geschaltet werden soll.
   */
  public static void handleSetWindowVisible(TextDocumentController documentController, boolean visible)
  {
    handle(new OnSetWindowVisible(documentController, visible));
  }

  /**
   * Dieses Event wird vom FormModelImpl ausgelöst, wenn die Formular-GUI das
   * bearbeitete Dokument sichtbar/unsichtbar schalten möchte. Ruft direkt setVisible
   * der UNO-API auf.
   * 
   * @author christoph.lutz
   */
  private static class OnSetWindowVisible extends BasicEvent
  {
    boolean visible;

    private TextDocumentController documentController;

    public OnSetWindowVisible(TextDocumentController documentController, boolean visible)
    {
      this.documentController = documentController;
      this.visible = visible;
    }

    @Override
    protected void doit()
    {
      documentController.getFrameController().setWindowVisible(visible);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + visible + ")";
    }
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

  /**
   * Dieses Event wird vom FormModelImpl ausgelöst, wenn der Benutzer die
   * Formular-GUI schließt und damit auch das zugehörige TextDokument geschlossen
   * werden soll.
   * 
   * @author christoph.lutz
   */
  private static class OnCloseTextDocument extends BasicEvent
  {
    private TextDocumentController documentController;

    public OnCloseTextDocument(TextDocumentController documentController)
    {
      this.documentController = documentController;
    }

    @Override
    protected void doit()
    {
      documentController.getModel().close();
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + documentController.getModel().hashCode() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das das übergebene Dokument in eine temporäre Datei
   * speichert, eine externe Anwendung mit dieser aufruft und das Dokument dann
   * schließt, wobei der ExterneAnwendungen-Abschnitt zu ext die näheren Details wie
   * den FILTER regelt.
   * 
   * @param documentController
   *          Das an die externe Anwendung weiterzureichende TextDocumentModel.
   * @param ext
   *          identifiziert den entsprechenden Eintrag im Abschnitt
   *          ExterneAnwendungen.
   */
  public static void handleCloseAndOpenExt(TextDocumentController documentController, String ext)
  {
    handle(new OnCloseAndOpenExt(documentController, ext));
  }

  /**
   * Dieses Event wird vom FormModelImpl ausgelöst, wenn der Benutzer die Aktion
   * "closeAndOpenExt" aktiviert hat.
   * 
   * @author matthias.benkmann
   */
  private static class OnCloseAndOpenExt extends BasicEvent
  {
    private String ext;
    private TextDocumentController documentController;

    public OnCloseAndOpenExt(TextDocumentController documentController, String ext)
    {
      this.documentController = documentController;
      this.ext = ext;
    }

    @Override
    protected void doit()
    {
      try
      {
        OpenExt openExt = new OpenExt(ext, WollMuxFiles.getWollmuxConf());
        openExt.setSource(UNO.XStorable(documentController.getModel().doc));
        openExt.storeIfNecessary();
        openExt.launch(new OpenExt.ExceptionHandler()
        {
          @Override
          public void handle(Exception x)
          {
            Logger.error(x);
          }
        });
      }
      catch (Exception x)
      {
        Logger.error(x);
        return;
      }

      documentController.getModel().setDocumentModified(false);
      documentController.getModel().close();
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + documentController.getModel().hashCode() + ", " + ext
        + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein Event, das das übergebene Dokument in eine temporäre Datei speichert
   * und eine externe Anwendung mit dieser aufruft, wobei der
   * ExterneAnwendungen-Abschnitt zu ext die näheren Details wie den FILTER regelt.
   * 
   * @param documentController
   *          Das an die externe Anwendung weiterzureichende TextDocumentModel.
   * @param ext
   *          identifiziert den entsprechenden Eintrag im Abschnitt
   *          ExterneAnwendungen.
   */
  public static void handleSaveTempAndOpenExt(TextDocumentController documentController, String ext)
  {
    handle(new OnSaveTempAndOpenExt(documentController, ext));
  }

  /**
   * Dieses Event wird vom FormModelImpl ausgelöst, wenn der Benutzer die Aktion
   * "saveTempAndOpenExt" aktiviert hat.
   * 
   * @author matthias.benkmann
   */
  private static class OnSaveTempAndOpenExt extends BasicEvent
  {
    private String ext;

    private TextDocumentController documentController;

    public OnSaveTempAndOpenExt(TextDocumentController documentController, String ext)
    {
      this.documentController = documentController;
      this.ext = ext;
    }

    @Override
    protected void doit()
    {
      try
      {
        OpenExt openExt = new OpenExt(ext, WollMuxFiles.getWollmuxConf());
        openExt.setSource(UNO.XStorable(documentController.getModel().doc));
        openExt.storeIfNecessary();
        openExt.launch(new OpenExt.ExceptionHandler()
        {
          @Override
          public void handle(Exception x)
          {
            Logger.error(x);
          }
        });
      }
      catch (Exception x)
      {
        Logger.error(x);
        return;
      }
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + documentController.getModel().hashCode() + ", " + ext
        + ")";
    }
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

  /**
   * Dieses Event wird als erstes WollMuxEvent bei der Initialisierung des WollMux im
   * WollMuxSingleton erzeugt und übernimmt alle benutzersichtbaren (interaktiven)
   * Initialisierungen.
   * 
   * @author christoph.lutz TESTED
   */
  private static class OnInitialize extends BasicEvent
  {
    @Override
    protected void doit()
    {
    }
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

  private static class OnRegisterDispatchInterceptor extends BasicEvent
  {
    private TextDocumentController documentController;

    public OnRegisterDispatchInterceptor(TextDocumentController documentController)
    {
      this.documentController = documentController;
    }

    @Override
    protected void doit()
    {
      FrameController fc = documentController.getFrameController(); 
      if (fc.getFrame() == null)
      {
        Logger.debug(L.m("Ignoriere handleRegisterDispatchInterceptor(null)"));
        return;
      }
      try
      {
        DispatchProviderAndInterceptor.registerDocumentDispatchInterceptor(fc.getFrame());
      }
      catch (java.lang.Exception e)
      {
        Logger.error(L.m("Kann DispatchInterceptor nicht registrieren:"), e);
      }

      // Sicherstellen, dass die Schaltflächen der Symbolleisten aktiviert werden:
      try
      {
        fc.getFrame().contextChanged();
      }
      catch (java.lang.Exception e)
      {}
    }

    @Override
    public String toString()
    {
      XFrame frame = documentController.getFrameController().getFrame();
      return this.getClass().getSimpleName() + "(#" + ((frame != null) ? frame.hashCode() : "Kein Frame") + ")";
    }
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

  private static class OnNotifyDocumentEventListener extends BasicEvent
  {
    private String eventName;

    private Object source;

    private XEventListener listener;

    public OnNotifyDocumentEventListener(XEventListener listener, String eventName,
        Object source)
    {
      this.listener = listener;
      this.eventName = eventName;
      this.source = source;
    }

    @Override
    protected void doit()
    {
      final com.sun.star.document.EventObject eventObject =
        new com.sun.star.document.EventObject();
      eventObject.Source = source;
      eventObject.EventName = eventName;

      Iterator<XEventListener> i =
          DocumentManager.getDocumentManager().documentEventListenerIterator();
      while (i.hasNext())
      {
        Logger.debug2("notifying XEventListener (event '" + eventName + "')");
        try
        {
          final XEventListener listener = i.next();
          if (this.listener == null || this.listener == listener) new Thread()
          {
            @Override
            public void run()
            {
              try
              {
                listener.notifyEvent(eventObject);
              }
              catch (java.lang.Exception x)
              {}
            }
          }.start();
        }
        catch (java.lang.Exception e)
        {
          i.remove();
        }
      }

      XComponent compo = UNO.XComponent(source);
      if (compo != null && eventName.equals(ON_WOLLMUX_PROCESSING_FINISHED))
        DocumentManager.getDocumentManager().setProcessingFinished(
          compo);

    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "('" + eventName + "', "
        + ((source != null) ? "#" + source.hashCode() : "null") + ")";
    }
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

  private static class OnExecutePrintFunction extends BasicEvent
  {
    private TextDocumentController documentController;

    public OnExecutePrintFunction(TextDocumentController documentController)
    {
      this.documentController = documentController;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      // Prüfen, ob alle gesetzten Druckfunktionen im aktuellen Kontext noch
      // Sinn machen:
      checkPrintPreconditions(documentController);
      stabilize();

      // Die im Dokument gesetzten Druckfunktionen ausführen:
      final XPrintModel pmod = PrintModels.createPrintModel(documentController, true);

      // Drucken im Hintergrund, damit der WollMuxEventHandler weiterläuft.
      new Thread()
      {
        @Override
        public void run()
        {
          pmod.printWithProps();
        }
      }.start();
    }

    /**
     * Es kann sein, dass zum Zeitpunkt des Drucken-Aufrufs eine Druckfunktion
     * gesetzt hat, die in der aktuellen Situation nicht mehr sinnvoll ist; Dieser
     * Umstand wird in checkPreconditons geprüft und die betroffene Druckfunktion
     * ggf. aus der Liste der Druckfunktionen entfernt.
     * 
     * @param printFunctions
     *          Menge der aktuell gesetzten Druckfunktionen.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    protected static void checkPrintPreconditions(TextDocumentController documentController)
    {
      Set<String> printFunctions = documentController.getModel().getPrintFunctions();

      // Ziffernanpassung der Sachleitenden Verfügungen durlaufen lassen, um zu
      // erkennen, ob Verfügungspunkte manuell aus dem Dokument gelöscht
      // wurden ohne die entsprechenden Knöpfe zum Einfügen/Entfernen von
      // Ziffern zu drücken.
      if (printFunctions.contains(SachleitendeVerfuegung.PRINT_FUNCTION_NAME))
      {
        SachleitendeVerfuegung.ziffernAnpassen(documentController);
      }

      // ...Platz für weitere Prüfungen.....
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
    }
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

  private static class OnSetPrintBlocksPropsViaPrintModel extends BasicEvent
  {
    private XTextDocument doc;

    private String blockName;

    private boolean visible;

    private boolean showHighlightColor;

    private ActionListener listener;

    public OnSetPrintBlocksPropsViaPrintModel(XTextDocument doc, String blockName,
        boolean visible, boolean showHighlightColor, ActionListener listener)
    {
      this.doc = doc;
      this.blockName = blockName;
      this.visible = visible;
      this.showHighlightColor = showHighlightColor;
      this.listener = listener;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      TextDocumentController documentController =
        DocumentManager.getTextDocumentController(doc);
      try
      {
        documentController.setPrintBlocksProps(blockName, visible, showHighlightColor);
      }
      catch (java.lang.Exception e)
      {
        errorMessage(e);
      }

      stabilize();
      if (listener != null) listener.actionPerformed(null);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", '"
        + blockName + "', '" + visible + "', '" + showHighlightColor + "')";
    }
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

  private static class OnSetFormValue extends BasicEvent
  {
    private XTextDocument doc;

    private String id;

    private String value;

    private final ActionListener listener;

    public OnSetFormValue(XTextDocument doc, String id, String value,
        ActionListener listener)
    {
      this.doc = doc;
      this.id = id;
      this.value = value;
      this.listener = listener;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      TextDocumentController documentController = DocumentManager.getTextDocumentController(doc);

      // Werte selber setzen:
      documentController.addFormFieldValue(id, value);
      if (listener != null)
        listener.actionPerformed(null);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", id='" + id
        + "', value='" + value + "')";
    }
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

  private static class OnCollectNonWollMuxFormFieldsViaPrintModel extends BasicEvent
  {
    private ActionListener listener;
    private TextDocumentController documentController;

    public OnCollectNonWollMuxFormFieldsViaPrintModel(TextDocumentController documentController,
        ActionListener listener)
    {
      this.documentController = documentController;
      this.listener = listener;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      documentController.collectNonWollMuxFormFields();

      stabilize();
      if (listener != null) listener.actionPerformed(null);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Dieses WollMuxEvent ist das Gegenstück zu handleSetFormValue und wird dann
   * erzeugt, wenn nach einer Änderung eines Formularwertes - gesteuert durch die
   * FormGUI - alle abhängigen Formularwerte angepasst wurden. In diesem Fall ist die
   * einzige Aufgabe dieses Events, den unlockActionListener zu informieren, den
   * handleSetFormValueViaPrintModel() nicht selbst informieren konnte.
   * 
   * Das Event wird aus der Implementierung vom OnSetFormValueViaPrintModel.doit()
   * erzeugt, wenn Feldänderungen über die FormGUI laufen.
   * 
   * @param unlockActionListener
   *          Der zu informierende unlockActionListener.
   */
  public static void handleSetFormValueFinished(ActionListener unlockActionListener)
  {
    handle(new OnSetFormValueFinished(unlockActionListener));
  }

  private static class OnSetFormValueFinished extends BasicEvent
  {
    private ActionListener listener;

    public OnSetFormValueFinished(ActionListener unlockActionListener)
    {
      this.listener = unlockActionListener;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      if (listener != null) listener.actionPerformed(null);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, das die nächste Marke
   * 'setJumpMark' angesprungen werden soll. Wird im
   * DocumentCommandInterpreter.DocumentExpander.fillPlaceholders aufgerufen wenn
   * nach dem Einfügen von Textbausteine keine Einfügestelle vorhanden ist aber eine
   * Marke 'setJumpMark'
   */
  public static void handleJumpToMark(XTextDocument doc, boolean msg)
  {
    handle(new OnJumpToMark(doc, msg));
  }

  private static class OnJumpToMark extends BasicEvent
  {
    private XTextDocument doc;

    private boolean msg;

    public OnJumpToMark(XTextDocument doc, boolean msg)
    {
      this.doc = doc;
      this.msg = msg;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {

      TextDocumentController documentController =
        DocumentManager.getTextDocumentController(doc);

      XTextCursor viewCursor = documentController.getModel().getViewCursor();
      if (viewCursor == null) return;

      DocumentCommand cmd = documentController.getModel().getFirstJumpMark();

      if (cmd != null)
      {
        try
        {
          XTextRange range = cmd.getTextCursor();
          if (range != null) viewCursor.gotoRange(range.getStart(), false);
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        boolean modified = documentController.getModel().isDocumentModified();
        cmd.markDone(true);
        documentController.getModel().setDocumentModified(modified);

        documentController.getModel().getDocumentCommands().update();

      }
      else
      {
        if (msg)
        {
          ModalDialogs.showInfoModal(L.m("WollMux"),
            L.m("Kein Platzhalter und keine Marke 'setJumpMark' vorhanden!"));
        }
      }

      stabilize();
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", " + msg
        + ")";
    }
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

  private static class OnSeriendruck extends BasicEvent
  {
    private TextDocumentController documentController;

    public OnSeriendruck(TextDocumentController documentController, boolean useDocumentPrintFunctions)
    {
      this.documentController = documentController;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      // Bestehenden Max in den Vordergrund holen oder neuen Max erzeugen.
      MailMergeNew mmn = DocumentManager.getDocumentManager().getCurrentMailMergeNew(documentController.getModel().doc);
      if (mmn != null)
      {
        return;
      }
      else
      {
        mmn = new MailMergeNew(documentController, new ActionListener()
        {
          @Override
          public void actionPerformed(ActionEvent actionEvent)
          {
            if (actionEvent.getSource() instanceof MailMergeNew)
              WollMuxEventHandler.handleMailMergeNewReturned(documentController);
          }
        });
        DocumentManager.getDocumentManager().setCurrentMailMergeNew(documentController.getModel().doc, mmn);
      }
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
    }
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

  private static class OnPrint extends BasicEvent
  {
    private XDispatch origDisp;

    private com.sun.star.util.URL origUrl;

    private PropertyValue[] origArgs;

    private TextDocumentController documentController;

    public OnPrint(TextDocumentController documentController, XDispatch origDisp,
        com.sun.star.util.URL origUrl, PropertyValue[] origArgs)
    {
      this.documentController = documentController;
      this.origDisp = origDisp;
      this.origUrl = origUrl;
      this.origArgs = origArgs;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      boolean hasPrintFunction = documentController.getModel().getPrintFunctions().size() > 0;

      if (Workarounds.applyWorkaroundForOOoIssue96281())
      {
        try
        {
          Object viewSettings =
            UNO.XViewSettingsSupplier(documentController.getModel().doc.getCurrentController()).getViewSettings();
          UNO.setProperty(viewSettings, "ZoomType", DocumentZoomType.BY_VALUE);
          UNO.setProperty(viewSettings, "ZoomValue", Short.valueOf((short) 100));
        }
        catch (java.lang.Exception e)
        {}
      }

      if (hasPrintFunction)
      {
        // Druckfunktion aufrufen
        handleExecutePrintFunctions(documentController);
      }
      else
      {
        // Forward auf Standardfunktion
        if (origDisp != null) origDisp.dispatch(origUrl, origArgs);
      }
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
    }
  }

  // *******************************************************************************************

  /**
   * Erzeugt ein neues WollMuxEvent, das signasisiert, dass der FormController (der
   * zeitgleich mit einer FormGUI zum TextDocument model gestartet wird) vollständig
   * initialisiert ist und notwendige Aktionen wie z.B. das Zurücksetzen des
   * Modified-Status des Dokuments durchgeführt werden können. Vor dem Zurücksetzen
   * des Modified-Status, wird auf die erste Seite des Dokuments gesprungen.
   * 
   * Das Event wird vom FormModel erzeugt, wenn es vom FormController eine
   * entsprechende Nachricht erhält.
   */
  public static void handleFormControllerInitCompleted(TextDocumentController documentController)
  {
    handle(new OnFormControllerInitCompleted(documentController));
  }

  private static class OnFormControllerInitCompleted extends BasicEvent
  {
    private TextDocumentController documentController;

    public OnFormControllerInitCompleted(TextDocumentController documentController)
    {
      this.documentController = documentController;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      // Springt zum Dokumentenanfang
      try
      {
        documentController.getModel().getViewCursor().gotoRange(documentController.getModel().doc.getText().getStart(), false);
      }
      catch (java.lang.Exception e)
      {
        Logger.debug(e);
      }

      // Beim Öffnen eines Formulars werden viele Änderungen am Dokument
      // vorgenommen (z.B. das Setzen vieler Formularwerte), ohne dass jedoch
      // eine entsprechende Benutzerinteraktion stattgefunden hat. Der
      // Modified-Status des Dokuments wird daher zurückgesetzt, damit nur
      // wirkliche Interaktionen durch den Benutzer modified=true setzen.
      documentController.getModel().setDocumentModified(false);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
    }
  }
}

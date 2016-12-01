/*
 * Dateiname: DocumentCommandInterpreter.java
 * Projekt  : WollMux
 * Funktion : Interpretiert die in einem Dokument enthaltenen Dokumentkommandos.
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
 * 14.10.2005 | LUT | Erstellung als WMCommandInterpreter
 * 24.10.2005 | LUT | + Sauberes umschliessen von Bookmarks in 
 *                      executeInsertFrag.
 *                    + Abschalten der lock-Controllers  
 * 02.05.2006 | LUT | Komplett-Überarbeitung und Umbenennung in
 *                    DocumentCommandInterpreter.
 * 05.05.2006 | BNK | Dummy-Argument zum Aufruf des FormGUI Konstruktors hinzugefügt.
 * 17.05.2006 | LUT | Doku überarbeitet.
 * 22.08.2006 | BNK | cleanInsertMarks() und EmptyParagraphCleaner verschmolzen zu
 *                  | SurroundingGarbageCollector und dabei komplettes Rewrite.
 * 23.08.2006 | BNK | nochmal Rewrite. Ich glaube dieser Code hält den Rekord im WollMux
 *                  | was rewrites angeht.
 * 08.07.2009 | BED | Anpassung an die Änderungen in DocumentCommand (R48539)
 * 16.12.2009 | ERT | Cast XTextField-Interface entfernt
 * 08.03.2010 | ERT | [R33088]Bessere Fehlermeldungen im Zusammenhang mit overrideFrag
 * 29.05.2013 | JuB | execute() auf 50 begrenzt, damit potentielle endlos-Loops beim Einfügen 
 *                    von Fragmenten abgefangen werden.
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.document.commands;

import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Diese Klasse repräsentiert den Kommando-Interpreter zur Auswertung von
 * WollMux-Kommandos in einem gegebenen Textdokument.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class DocumentCommandInterpreter
{

  private TextDocumentController documentController;

  /**
   * Enthält die Instanz auf das zentrale WollMuxSingleton.
   */
  boolean debugMode;

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle
   * Dokumentkommandos im übergebenen Dokument xDoc scannen und interpretieren kann.
   * 
   * @param xDoc
   *          Das Dokument, dessen Kommandos ausgeführt werden sollen.
   * @param mux
   *          Die Instanz des zentralen WollMux-Singletons
   * @param frag_urls
   *          Eine Liste mit fragment-urls, die für das Kommando insertContent
   *          benötigt wird.
   */
  public DocumentCommandInterpreter(TextDocumentController documentController, boolean debugMode)
  {
    this.documentController = documentController;
    this.debugMode = debugMode;
  }

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle
   * Dokumentkommandos im übergebenen Dokument xDoc scannen und interpretieren kann.
   * 
   * @param xDoc
   *          Das Dokument, dessen Kommandos ausgeführt werden sollen.
   * @param frag_urls
   *          Eine Liste mit fragment-urls, die für das Kommando insertContent
   *          benötigt wird.
   */
  public DocumentCommandInterpreter(TextDocumentController documentController)
  {
    this.documentController = documentController;
    this.debugMode = false;
  }
  
  public TextDocumentController getDocumentController()
  {
    return documentController;
  }

  public TextDocumentModel getModel()
  {
    return getDocumentController().getModel();
  }

  /**
   * Diese Methode sollte vor {@link #executeTemplateCommands()} aufgerufen werden
   * und sorgt dafür, dass alle globalen Einstellungen des Dokuments (setType,
   * setPrintFunction) an das TextDocumentModel weitergereicht werden.
   */
  public void scanGlobalDocumentCommands()
  {
    Logger.debug("scanGlobalDocumentCommands");
    boolean modified = getDocumentController().getModel().isDocumentModified();
    
    try
    {
      getDocumentController().getModel().setDocumentModifiable(false);
      GlobalDocumentCommandsScanner s = new GlobalDocumentCommandsScanner(this);
      s.execute(getDocumentController().getModel().getDocumentCommands());
    }
    finally
    {
      getDocumentController().getModel().setDocumentModified(modified);
      getDocumentController().getModel().setDocumentModifiable(true);
    }
  }

  /**
   * Diese Methode scannt alle insertFormValue-Kommandos des Dokuments, verarbeitet
   * diese und reicht das gefundene Mapping von IDs zu FormFields an das
   * TextDocumentModel weiter. Zudem wird von dieser Methode auch noch
   * {@link TextDocumentModel#collectNonWollMuxFormFields()} aufgerufen, so dass auch
   * alle Formularfelder aufgesammelt werden, die nicht von WollMux-Kommandos umgeben
   * sind, jedoch trotzdem vom WollMux verstanden und befüllt werden.
   * 
   * Diese Methode wurde aus der Methode {@link #scanGlobalDocumentCommands()}
   * ausgelagert, die früher neben den globalen Dokumentkommandos auch die
   * insertFormValue-Kommandos bearbeitet hat. Die Auslagerung geschah hauptsächlich
   * aus Performance-Optimierungsgründen, da so beim OnProcessTextDocument-Event nur
   * einmal die insertFormValue-Kommandos ausgewertet werden müssen.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public void scanInsertFormValueCommands()
  {
    Logger.debug("scanInsertFormValueCommands");
    boolean modified = getDocumentController().getModel().isDocumentModified();
    
    try
    {
      getDocumentController().getModel().setDocumentModifiable(false);
      InsertFormValueCommandsScanner s = new InsertFormValueCommandsScanner(this);
      s.execute(getDocumentController().getModel().getDocumentCommands());

      getDocumentController().getModel().setIDToFormFields(s.idToFormFields);
      getDocumentController().collectNonWollMuxFormFields();
    }
    finally
    {
      getDocumentController().getModel().setDocumentModified(modified);
      getDocumentController().getModel().setDocumentModifiable(true);
    }
  }
}

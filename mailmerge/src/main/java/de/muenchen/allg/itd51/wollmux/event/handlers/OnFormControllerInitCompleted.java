package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnFormControllerInitCompleted extends BasicEvent
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
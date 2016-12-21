package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Erzeugt ein neues WollMuxEvent, das aufgerufen wird, wenn ein MailMerge-Dialog
 * beendet wird und die entsprechenden internen Referenzen gelöscht werden können.
 * 
 * Dieses Event wird geworfen, wenn MailMerge zurückkehrt.
 */
public class OnHandleMailMergeNewReturned extends BasicEvent
{
  private TextDocumentController documentController;

  public OnHandleMailMergeNewReturned(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + documentController.getModel().hashCode() + ")";
  }
}
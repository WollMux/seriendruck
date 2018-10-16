package de.muenchen.mailmerge.event.handlers;

import de.muenchen.mailmerge.MailMergeFehlerException;
import de.muenchen.mailmerge.document.TextDocumentController;

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
  protected void doit() throws MailMergeFehlerException
  {
    documentController.setFormFieldsPreviewMode(true);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + documentController.getModel().hashCode() + ")";
  }
}
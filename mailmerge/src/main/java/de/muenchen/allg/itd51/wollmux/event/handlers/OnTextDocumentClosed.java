package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;

public class OnTextDocumentClosed extends BasicEvent
{
  private DocumentManager.Info docInfo;

  public OnTextDocumentClosed(DocumentManager.Info doc)
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
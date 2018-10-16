package de.muenchen.mailmerge.event.handlers;

import com.sun.star.frame.XFrame;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.mailmerge.MailMergeFehlerException;
import de.muenchen.mailmerge.dialog.mailmerge.MailMergeNew;
import de.muenchen.mailmerge.document.DocumentManager;
import de.muenchen.mailmerge.document.TextDocumentController;
import de.muenchen.mailmerge.document.DocumentManager.TextDocumentInfo;
import de.muenchen.mailmerge.event.DispatchProviderAndInterceptor;
import de.muenchen.mailmerge.event.GlobalEventListener;

/**
 * Erzeugt ein neues WollMuxEvent, das Auskunft darüber gibt, dass ein
 * TextDokument geschlossen wurde und damit auch das TextDocumentModel disposed
 * werden soll.
 * 
 * Dieses Event wird ausgelöst, wenn ein TextDokument geschlossen wird.
 * 
 */
public class OnTextDocumentClosed extends BasicEvent
{
  private DocumentManager.Info docInfo;

  /**
   * @param docInfo
   *          ein {@link DocumentManager.Info} Objekt, an dem das
   *          TextDocumentModel dranhängt des Dokuments, das geschlossen wurde.
   *          ACHTUNG! docInfo hat nicht zwingend ein TextDocumentModel. Es muss
   *          {@link DocumentManager.Info#hasTextDocumentModel()} verwendet
   *          werden.
   * 
   * 
   *          ACHTUNG! ACHTUNG! Die Implementierung wurde extra so gewählt, dass
   *          hier ein DocumentManager.Info anstatt direkt eines
   *          TextDocumentModel übergeben wird. Es kam nämlich bei einem
   *          Dokument, das schnell geöffnet und gleich wieder geschlossen wurde
   *          zu folgendem Deadlock:
   * 
   *          {@link OnProcessTextDocument} =>
   *          {@link de.muenchen.mailmerge.document.DocumentManager.TextDocumentInfo#getTextDocumentController()}
   *          => {@link TextDocumentModel#TextDocumentModel(XTextDocument)} =>
   *          {@link DispatchProviderAndInterceptor#registerDocumentDispatchInterceptor(XFrame)}
   *          => OOo Proxy =>
   *          {@link GlobalEventListener#notifyEvent(com.sun.star.document.EventObject)}
   *          ("OnUnload") =>
   *          {@link de.muenchen.mailmerge.document.DocumentManager.TextDocumentInfo#hasTextDocumentModel()}
   * 
   *          Da {@link TextDocumentInfo} synchronized ist kam es zum Deadlock.
   * 
   */
  public OnTextDocumentClosed(DocumentManager.Info doc)
  {
    this.docInfo = doc;
  }

  @Override
  protected void doit() throws MailMergeFehlerException
  {
    if (docInfo.hasTextDocumentModel())
    {
      TextDocumentController documentController = docInfo.getTextDocumentController();
      MailMergeNew.disposeInstance(documentController);
      DocumentManager.getDocumentManager().dispose(documentController.getModel().doc);
    }
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
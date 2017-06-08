package de.muenchen.mailmerge.event.handlers;

import java.util.List;
import java.util.Vector;

import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;

import de.muenchen.mailmerge.document.DocumentManager;
import de.muenchen.mailmerge.event.MailMergeEventHandler;

/**
 * Erzeugt ein neues WollMuxEvent zum Registrieren des übergebenen XEventListeners
 * und wird vom WollMux-Service aufgerufen.
 *
 */
public class OnAddDocumentEventListener extends BasicEvent
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

    List<XComponent> processedDocuments = new Vector<>();
    DocumentManager.getDocumentManager().getProcessedDocuments(processedDocuments);

    for (XComponent compo : processedDocuments)
    {
      MailMergeEventHandler.getInstance().handleNotifyDocumentEventListener(listener, MailMergeEventHandler.ON_WOLLMUX_PROCESSING_FINISHED,
        compo);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
  }
}
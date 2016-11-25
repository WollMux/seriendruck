package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.util.List;
import java.util.Vector;

import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;

import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

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

    List<XComponent> processedDocuments = new Vector<XComponent>();
    DocumentManager.getDocumentManager().getProcessedDocuments(processedDocuments);

    for (XComponent compo : processedDocuments)
    {
      WollMuxEventHandler.handleNotifyDocumentEventListener(listener, WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED,
        compo);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
  }
}
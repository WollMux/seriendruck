package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;

/**
 * Event, dass beim ersten Erzeugen eines Dokuments aufgerufen wird. 
 * 
 *
 */
public class OnCreateDocument extends BasicEvent
{
  private XComponent comp;

  /**
   * 
   * @param comp
   *          Neues Dokument. Wird zum {@link DocumentManager} hinzugefügt.
   */
  public OnCreateDocument(XComponent comp)
  {
    super();
    this.comp = comp;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    DocumentManager docManager = DocumentManager.getDocumentManager();
    XTextDocument xTextDoc = UNO.XTextDocument(comp);
    
    // durch das Hinzufügen zum docManager kann im Event onViewCreated erkannt
    // werden, dass das Dokument frisch erzeugt wurde.
    if (xTextDoc != null)
    {
      docManager.addTextDocument(xTextDoc);
    }
    else
    {
      docManager.add(comp);
    }
  }
}

package de.muenchen.mailmerge.event.handlers;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.frame.XModel;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.mailmerge.WollMuxFehlerException;
import de.muenchen.mailmerge.document.DocumentManager;
import de.muenchen.mailmerge.document.DocumentManager.Info;
import de.muenchen.mailmerge.event.GlobalEventListener;
import de.muenchen.mailmerge.event.MailMergeEventHandler;

/**
 * Event wird aufgerufen, wenn die Erstellung eines neuen
 * Dokuments abgeschlossen wird.
 *
 *  @see GlobalEventListener
 *
 */
public class OnViewCreated extends BasicEvent
{
  private XModel comp;

  public OnViewCreated(XModel comp)
  {
    super();
    this.comp = comp;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    DocumentManager docManager = DocumentManager.getDocumentManager();
    Info docInfo = docManager.getInfo(comp);
    // docInfo ist hier nur dann ungleich null, wenn das Dokument mit Create erzeugt
    // wurde.
    XTextDocument xTextDoc = UNO.XTextDocument(comp);
    if (xTextDoc != null && docInfo != null && isDocumentLoadedHidden(comp))
    {
      docManager.remove(comp);
      return;
    }

    if (xTextDoc != null && docInfo == null)
    {
      docManager.addTextDocument(xTextDoc);
    }

    if (xTextDoc != null)
    {
      /**
       * Dispatch Handler in eigenem Event registrieren, da es Deadlocks gegeben hat.
       */
      MailMergeEventHandler.getInstance().handleRegisterDispatchInterceptor(DocumentManager.getTextDocumentController(xTextDoc));
    }
  }

  /**
   * Liefert nur dann true zurück, wenn das Dokument mit der
   * MediaDescriptor-Eigenschaft Hidden=true geöffnet wurde.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private boolean isDocumentLoadedHidden(XModel compo)
  {
    UnoProps props = new UnoProps(compo.getArgs());
    try
    {
      return AnyConverter.toBoolean(props.getPropertyValue("Hidden"));
    }
    catch (UnknownPropertyException e)
    {
      return false;
    }
    catch (IllegalArgumentException e)
    {
      Logger.error(L.m("das darf nicht vorkommen!"), e);
      return false;
    }
  }
}

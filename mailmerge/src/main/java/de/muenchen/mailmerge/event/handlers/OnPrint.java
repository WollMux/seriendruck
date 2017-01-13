package de.muenchen.mailmerge.event.handlers;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.view.DocumentZoomType;

import de.muenchen.allg.afid.UNO;
import de.muenchen.mailmerge.WollMuxFehlerException;
import de.muenchen.mailmerge.Workarounds;
import de.muenchen.mailmerge.document.TextDocumentController;
import de.muenchen.mailmerge.event.MailMergeEventHandler;

/**
 * Der Handler für das Drucken eines TextDokuments führt in Abhängigkeit von der
 * Existenz von Serienbrieffeldern und Druckfunktion die entsprechenden Aktionen
 * aus.
 * 
 * Das Event wird über den DispatchHandler aufgerufen, wenn z.B. über das Menü
 * "Datei->Drucken" oder über die Symbolleiste die dispatch-url .uno:Print bzw.
 * .uno:PrintDefault abgesetzt wurde.
 */
public class OnPrint extends BasicEvent
{
  private XDispatch origDisp;

  private com.sun.star.util.URL origUrl;

  private PropertyValue[] origArgs;

  private TextDocumentController documentController;

  public OnPrint(TextDocumentController documentController, XDispatch origDisp,
      com.sun.star.util.URL origUrl, PropertyValue[] origArgs)
  {
    this.documentController = documentController;
    this.origDisp = origDisp;
    this.origUrl = origUrl;
    this.origArgs = origArgs;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    boolean hasPrintFunction = documentController.getModel().getPrintFunctions().size() > 0;

    if (Workarounds.applyWorkaroundForOOoIssue96281())
    {
      try
      {
        Object viewSettings =
          UNO.XViewSettingsSupplier(documentController.getModel().doc.getCurrentController()).getViewSettings();
        UNO.setProperty(viewSettings, "ZoomType", DocumentZoomType.BY_VALUE);
        UNO.setProperty(viewSettings, "ZoomValue", Short.valueOf((short) 100));
      }
      catch (java.lang.Exception e)
      {}
    }

    if (hasPrintFunction)
    {
      // Druckfunktion aufrufen
      MailMergeEventHandler.getInstance().handleExecutePrintFunctions(documentController);
    }
    else
    {
      // Forward auf Standardfunktion
      if (origDisp != null) origDisp.dispatch(origUrl, origArgs);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
  }
}
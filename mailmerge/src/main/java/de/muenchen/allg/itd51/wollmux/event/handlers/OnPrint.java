package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.view.DocumentZoomType;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.Workarounds;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

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
      WollMuxEventHandler.handleExecutePrintFunctions(documentController);
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
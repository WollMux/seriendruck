package de.muenchen.mailmerge.event.handlers;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;

import de.muenchen.mailmerge.MailMergeFehlerException;
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
  protected void doit() throws MailMergeFehlerException
  {
    boolean hasPrintFunction = !documentController.getModel().getPrintFunctions().isEmpty();

    if (hasPrintFunction)
    {
      // Druckfunktion aufrufen
      MailMergeEventHandler.getInstance().handleExecutePrintFunctions(documentController);
    }
    else
    {
      // Forward auf Standardfunktion
      if (origDisp != null)
        origDisp.dispatch(origUrl, origArgs);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
  }
}
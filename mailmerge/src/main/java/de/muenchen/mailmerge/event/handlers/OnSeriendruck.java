package de.muenchen.mailmerge.event.handlers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import de.muenchen.mailmerge.MailMergeFehlerException;
import de.muenchen.mailmerge.dialog.mailmerge.MailMergeNew;
import de.muenchen.mailmerge.document.TextDocumentController;
import de.muenchen.mailmerge.event.MailMergeEventHandler;

/**
 * Erzeugt ein neues WollMuxEvent, das signasisiert, dass die neue
 * Seriendruckfunktion des WollMux gestartet werden soll.
 * 
 * Das Event wird über den DispatchHandler aufgerufen, wenn z.B. über das Menü
 * "Extras->Seriendruck (WollMux)" die dispatch-url wollmux:SeriendruckNeu
 * abgesetzt wurde.
 */
public class OnSeriendruck extends BasicEvent
{
  private TextDocumentController documentController;

  public OnSeriendruck(TextDocumentController documentController, boolean useDocumentPrintFunctions)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws MailMergeFehlerException
  {
    // Bestehenden Max in den Vordergrund holen oder neuen Max erzeugen.
    MailMergeNew mmn = new MailMergeNew(documentController, new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent actionEvent)
      {
        if (actionEvent.getSource() instanceof MailMergeNew)
        {
          ((MailMergeNew)actionEvent.getSource()).dispose();
          MailMergeEventHandler.getInstance().handleMailMergeNewReturned(documentController);
        }
      }
    });

    mmn.run();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
  }
}
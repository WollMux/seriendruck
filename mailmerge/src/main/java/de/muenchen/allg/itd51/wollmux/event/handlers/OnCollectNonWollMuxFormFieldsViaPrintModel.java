package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnCollectNonWollMuxFormFieldsViaPrintModel extends BasicEvent
{
  private ActionListener listener;
  private TextDocumentController documentController;

  public OnCollectNonWollMuxFormFieldsViaPrintModel(TextDocumentController documentController,
      ActionListener listener)
  {
    this.documentController = documentController;
    this.listener = listener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    documentController.collectNonWollMuxFormFields();

    stabilize();
    if (listener != null) listener.actionPerformed(null);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
  }
}
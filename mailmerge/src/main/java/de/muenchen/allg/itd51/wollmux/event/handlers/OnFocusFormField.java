package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnFocusFormField extends BasicEvent
{
  private String fieldId;
  private TextDocumentController documentController;

  public OnFocusFormField(TextDocumentController documentController, String fieldId)
  {
    this.documentController = documentController;
    this.fieldId = fieldId;
  }

  @Override
  protected void doit()
  {
    documentController.getModel().focusFormField(fieldId);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + documentController.getModel().doc + ", '" + fieldId
      + "')";
  }
}
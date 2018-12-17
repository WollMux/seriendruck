package de.muenchen.mailmerge.event.handlers;

import de.muenchen.mailmerge.document.TextDocumentController;

public class OnFormValueChanged extends BasicEvent
{
  private String fieldId;

  private String newValue;

  private TextDocumentController documentController;

  public OnFormValueChanged(TextDocumentController documentController, String fieldId,
      String newValue)
  {
    this.fieldId = fieldId;
    this.newValue = newValue;
    this.documentController = documentController;
  }

  @Override
  protected void doit()
  {
    documentController.addFormFieldValue(fieldId, newValue);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + fieldId + "', '" + newValue + "')";
  }
}

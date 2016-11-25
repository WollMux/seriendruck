package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnSetVisibleState extends BasicEvent
{
  private String groupId;

  private boolean visible;

  private ActionListener listener;

  private TextDocumentController documentController;

  public OnSetVisibleState(TextDocumentController documentController, String groupId,
      boolean visible, ActionListener listener)
  {
    this.documentController = documentController;
    this.groupId = groupId;
    this.visible = visible;
    this.listener = listener;
  }

  @Override
  protected void doit()
  {
    documentController.setVisibleState(groupId, visible);
    if (listener != null) listener.actionPerformed(null);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "('" + groupId + "', " + visible
      + ")";
  }
}
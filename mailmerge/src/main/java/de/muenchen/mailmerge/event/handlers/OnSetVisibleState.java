package de.muenchen.mailmerge.event.handlers;

import java.awt.event.ActionListener;

import de.muenchen.mailmerge.document.TextDocumentController;

/**
 * Erzeugt ein neues WollMuxEvent, welches dafür sorgt, dass alle
 * Sichtbarkeitselemente (Dokumentkommandos oder Bereiche mit Namensanhang
 * 'GROUPS ...') im übergebenen Dokument, die einer bestimmten Gruppe groupId
 * zugehören ein- oder ausgeblendet werden.
 * 
 * Dieses Event wird (derzeit) vom FormModelImpl ausgelöst, wenn in der
 * Formular-GUI bestimmte Text-Teile des übergebenen Dokuments ein- oder
 * ausgeblendet werden sollen. Auch das PrintModel verwendet dieses Event, wenn
 * XPrintModel.setGroupVisible() aufgerufen wurde.
 * 
 */
public class OnSetVisibleState extends BasicEvent
{
  private String groupId;

  private boolean visible;

  private ActionListener listener;

  private TextDocumentController documentController;

  /**
   * 
   * @param documentController
   *          Das TextDocumentModel, welches die Sichtbarkeitselemente enthält.
   * @param groupId
   *          Die GROUP (ID) der ein/auszublendenden Gruppe.
   * @param visible
   *          Der neue Sichtbarkeitsstatus (true=sichtbar, false=ausgeblendet)
   * @param listener
   *          Der listener, der nach Durchführung des Events benachrichtigt wird
   *          (kann auch null sein, dann gibt's keine Nachricht).
   * 
   */
  public OnSetVisibleState(TextDocumentController documentController, String groupId, boolean visible,
      ActionListener listener)
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
    if (listener != null)
      listener.actionPerformed(null);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "('" + groupId + "', " + visible + ")";
  }
}
package de.muenchen.mailmerge.event.handlers;

import java.awt.event.ActionListener;

import com.sun.star.text.XTextDocument;

import de.muenchen.mailmerge.WollMuxFehlerException;
import de.muenchen.mailmerge.document.DocumentManager;
import de.muenchen.mailmerge.document.TextDocumentController;

/**
 * Diese Methode erzeugt ein neues WollMuxEvent, mit dem die Eigenschaften der
 * Druckblöcke (z.B. allVersions) gesetzt werden können.
 * 
 * Das Event dient als Hilfe für die Komfortdruckfunktionen und wird vom
 * XPrintModel aufgerufen und mit diesem synchronisiert.
 * 
 */
public class OnSetPrintBlocksPropsViaPrintModel extends BasicEvent
{
  private XTextDocument doc;

  private String blockName;

  private boolean visible;

  private boolean showHighlightColor;

  private ActionListener listener;

  /**
   * 
 * @param blockName
 *          Der Blocktyp dessen Druckblöcke behandelt werden sollen.
 * @param visible
 *          Der Block wird sichtbar, wenn visible==true und unsichtbar, wenn
 *          visible==false.
 * @param showHighlightColor
 *          gibt an ob die Hintergrundfarbe angezeigt werden soll (gilt nur, wenn
 *          zu einem betroffenen Druckblock auch eine Hintergrundfarbe angegeben
 *          ist).
   */
  public OnSetPrintBlocksPropsViaPrintModel(XTextDocument doc, String blockName,
      boolean visible, boolean showHighlightColor, ActionListener listener)
  {
    this.doc = doc;
    this.blockName = blockName;
    this.visible = visible;
    this.showHighlightColor = showHighlightColor;
    this.listener = listener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    TextDocumentController documentController =
      DocumentManager.getTextDocumentController(doc);
    try
    {
      documentController.setPrintBlocksProps(blockName, visible, showHighlightColor);
    }
    catch (java.lang.Exception e)
    {
      errorMessage(e);
    }

    stabilize();
    if (listener != null) listener.actionPerformed(null);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", '"
      + blockName + "', '" + visible + "', '" + showHighlightColor + "')";
  }
}